package br.ucsal.cade.demo.service;

import br.ucsal.cade.access.api.DirectSignIn;
import br.ucsal.cade.access.api.SignedIn;
import br.ucsal.cade.founditems.api.FoundItemData;
import br.ucsal.cade.founditems.api.FoundItemRegistration;
import br.ucsal.cade.lossreports.api.LossReportData;
import br.ucsal.cade.lossreports.api.LossReportRegistration;
import br.ucsal.cade.shared.category.Category;
import br.ucsal.cade.shared.category.CategoryRepository;
import br.ucsal.cade.shared.category.RiskLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cria na subida as contas e os dados de exemplo do modo demonstração (RF47, RNF13): objetos na sala, perdas de um
 * estudante e o caso do registro genérico ("iPhone"), que recebe nota baixa no cruzamento (critério de sucesso 4).
 * Todos os dados são inventados.
 *
 * <p>Os cadastros passam pelas mesmas portas de achados e perdas que as rotas usam, então o histórico e o cruzamento
 * funcionam como no uso real: as sugestões aparecem para o funcionário logo depois da subida. Só roda se o estudante
 * de demonstração ainda não tem registros, então reiniciar a aplicação não duplica nada.
 */
@Component
@Profile("demo")
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final DirectSignIn signIn;
    private final FoundItemRegistration foundItems;
    private final LossReportRegistration lossReports;
    private final CategoryRepository categories;
    private final Clock clock;

    DemoDataSeeder(DirectSignIn signIn, FoundItemRegistration foundItems, LossReportRegistration lossReports,
                   CategoryRepository categories, Clock clock) {
        this.signIn = signIn;
        this.foundItems = foundItems;
        this.lossReports = lossReports;
        this.categories = categories;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        SignedIn student = signIn.signIn(DemoAccount.STUDENT.email(), DemoAccount.STUDENT.role());
        SignedIn staff = signIn.signIn(DemoAccount.STAFF.email(), DemoAccount.STAFF.role());
        signIn.signIn(DemoAccount.ADMIN.email(), DemoAccount.ADMIN.role());

        if (lossReports.hasAnyOf(student.userId())) {
            log.info("Demo data already present; nothing to create");
            return;
        }
        LocalDate today = LocalDate.now(clock);
        registerFoundItems(staff.userId(), today);
        registerLossReports(student.userId(), today);
        log.info("Demo accounts and sample data created");
    }

    private void registerFoundItems(UUID staffId, LocalDate today) {
        foundItems.register(new FoundItemData(DemoPhotos.solid(new Color(40, 40, 48)), category("Fone", RiskLevel.MEDIUM),
                "Bloco 400", "Sala 421", today.minusDays(2), "Fone JBL preto, arranhão na haste",
                "Adesivo de estrela na caixa"), staffId);
        foundItems.register(new FoundItemData(DemoPhotos.solid(new Color(120, 120, 130)),
                category("Celular", RiskLevel.HIGH), "Bloco 100", "Sala 105", today.minusDays(1),
                "iPhone com capa transparente", "Foto de um cachorro como papel de parede"), staffId);
        foundItems.register(new FoundItemData(DemoPhotos.solid(new Color(40, 110, 200)),
                category("Garrafa", RiskLevel.LOW), "Bloco 300", "Sala 310", today.minusDays(3),
                "Garrafa térmica azul", null), staffId);
        foundItems.register(new FoundItemData(DemoPhotos.solid(new Color(30, 30, 30)),
                category("Mochila", RiskLevel.MEDIUM), "Bloco 200", "Sala 201", today.minusDays(4),
                "Mochila preta com chaveiro vermelho", "Caderno de cálculo no bolso da frente"), staffId);
    }

    private void registerLossReports(UUID studentId, LocalDate today) {
        // Registro bom: descrição, detalhe e local batem com o fone cadastrado, então a nota é alta.
        lossReports.register(new LossReportData(category("Fone", RiskLevel.MEDIUM), "Fone de ouvido JBL preto",
                "Adesivo de estrela na caixa", "Bloco 400", "Sala 421", today.minusDays(2)), studentId);
        // Registro genérico: só "iPhone", sem detalhe nem local. Recebe nota baixa e, se for convocado, o balcão barra.
        lossReports.register(new LossReportData(category("Celular", RiskLevel.HIGH), "iPhone", null, null, null,
                today.minusDays(1)), studentId);
        lossReports.register(new LossReportData(category("Garrafa", RiskLevel.LOW), "Garrafa térmica azul",
                "Meu nome gravado na tampa", "Bloco 300", "Sala 310", today.minusDays(3)), studentId);
    }

    /** A categoria padrão com o nome, criada se o inicializador de categorias ainda não rodou. */
    private UUID category(String name, RiskLevel risk) {
        return categories.findByNameIgnoreCase(name)
                .orElseGet(() -> categories.save(new Category(name, risk)))
                .getId();
    }
}
