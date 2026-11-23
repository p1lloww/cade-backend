package br.ucsal.cade.demo.controller.login;

import br.ucsal.cade.access.api.DirectSignIn;
import br.ucsal.cade.demo.controller.login.dto.DemoAccountResponseDTO;
import br.ucsal.cade.demo.controller.login.dto.DemoLoginRequestDTO;
import br.ucsal.cade.demo.controller.login.dto.DemoLoginResponseDTO;
import br.ucsal.cade.demo.service.DemoAccount;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Login direto das contas fictícias (RF47). O controller só existe no profile {@code demo}, e a porta de entrada
 * direta ({@link DirectSignIn}) também: sem o profile, as rotas respondem 404 e nada emite token sem código (D02). As
 * rotas estão em {@code PUBLIC_ROUTES} porque não há token antes do login.
 */
@RestController
@RequestMapping("/demo")
@Profile("demo")
class DemoLoginController implements DemoApi {

    private final DirectSignIn signIn;

    DemoLoginController(DirectSignIn signIn) {
        this.signIn = signIn;
    }

    @Override
    @GetMapping("/accounts")
    public List<DemoAccountResponseDTO> accounts() {
        return Arrays.stream(DemoAccount.values()).map(DemoAccountResponseDTO::from).toList();
    }

    @Override
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public DemoLoginResponseDTO login(@Valid @RequestBody DemoLoginRequestDTO request) {
        DemoAccount account = DemoAccount.of(request.role()).orElseThrow();
        return DemoLoginResponseDTO.from(signIn.signIn(account.email(), account.role()));
    }
}
