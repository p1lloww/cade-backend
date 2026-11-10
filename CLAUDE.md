# CLAUDE.md

Back-end do **Cadê?**, sistema de achados e perdidos da UCSal. É um projeto acadêmico do POOALAB (disciplina Programação Orientada a Objetos Avançada, 2026.2), feito por uma equipe de 4 estudantes. A entrega final é em **23/11/2026** e a mostra com pitch em 01/12 (ADS) e 03/12 (BES).

O front-end (Angular, publicado na Vercel) fica em outro repositório: `p1lloww/cade-frontend`. O backlog está no GitHub Project `p1lloww/projects/4`.

## Fonte da verdade

- **Requisitos:** documento "Requisitos v3 — Cadê?", com RFxx, RNxx, RNFxx, estados (seção 6) e prioridades P1/P2/P3. Ele não fica neste repositório; a equipe o compartilha à parte. Quando uma tarefa citar um código (ex.: RF13, RN07), siga o documento e pergunte se ele não estiver disponível.
- **Manual do POOALAB:** define as entregas obrigatórias (README, `docs/`, declaração de IA, pitch). Os modelos já estão em `docs/`.

## Comandos

```bash
docker compose up -d postgres mailpit   # dependências para desenvolver
./mvnw spring-boot:run                  # sobe a API em http://localhost:8080
./mvnw verify                           # build + testes (precisa do Docker: Testcontainers)
docker compose up -d --build            # tudo em contêiner, como na entrega
docker compose down -v                  # zera o banco local
```

E-mails enviados em desenvolvimento aparecem no Mailpit: http://localhost:8025.

## Stack

Java 25, Spring Boot 4.1 (Web MVC, Security, OAuth2 Resource Server para o JWT, Data JPA, Mail, Validation, Actuator), PostgreSQL 17 e Docker Compose. O esquema do banco é gerado pelo Hibernate (`ddl-auto: update`); não há Flyway. Todas as configurações sensíveis vêm de variáveis de ambiente (ver `.env.example`).

## Arquitetura

Monólito modular. Cada módulo é um pacote em `br.ucsal.cade`, e o `package-info.java` de cada um descreve sua responsabilidade:

| Pacote | Módulo |
|---|---|
| `acesso` | M1: login por código no e-mail, JWT, perfis, `@RequerPerfil` |
| `achados` | M2: objetos achados, situações (State), fotos, busca |
| `perdas` | M3: registros de perda |
| `cruzamento` | M4: critérios (Strategy + Composite), nota, sugestões |
| `retirada` | M5: convocação, conferência por risco, retirada e recusa |
| `avisos` | M6: e-mails (`ServicoEmail`) |
| `prazos` | M7: objetos não reclamados e destino |
| `publico` | M8: dados da página pública (só contagens genéricas) |
| `administracao` | M9: histórico imutável, gestão de funcionários, categorias e prazos |
| `demo` | M10: dados e usuários fictícios, só no profile `demo` |
| `compartilhado` | Configuração, erros e utilitários comuns; sem regra de negócio |

Dentro de cada módulo, os subpacotes são `dominio`, `aplicacao`, `web` e `infra`, criados conforme a necessidade.

Decisões que valem para todo o código:

- **Autenticação sem estado:** o login envia um código de uso único ao e-mail @ucsal.edu.br e devolve um JWT, que o front manda no header `Authorization`. Não há senha nem cookie de sessão. O domínio do e-mail é validado no back-end.
- **Código de login:** gerado com `SecureRandom`, válido por 10 minutos, de uso único, guardado só como hash, com no máximo 5 tentativas e 1 envio por minuto.
- **Estados e histórico** são gravados na mesma transação da ação. As transições permitidas são só as da seção 6 dos requisitos.
- **Efeitos colaterais** (cruzamento, e-mails) rodam depois do commit, com eventos do Spring: `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`, em Virtual Threads. Uma falha neles nunca desfaz nem bloqueia a ação principal.
- **Dependências externas ficam atrás de interfaces:** `PublicadorEventos`, `ServicoEmail`, `ArmazenamentoFotos`, `CriterioCompatibilidade`.
- **Privacidade:** foto, descrição interna, detalhes reservados e local exato de um objeto nunca são expostos a estudantes nem a visitantes (RN03).

## Convenções

- **Idioma:** nomes do domínio, classes, métodos e mensagens em português, seguindo os termos do documento de requisitos (Objeto, RegistroPerda, Sugestao, Convocacao). Termos técnicos consagrados (Controller, Repository, DTO) podem ficar em inglês.
- **Padrões de POOA** (State, Strategy, Composite, Observer, Streams, Virtual Threads, anotações e reflexão, SOLID) valem bônus quando aparecem de forma justificada. Use-os onde o problema pede e nunca de forma artificial.
- **Testes:** cobrir as transições de estado, a nota do cruzamento, as regras por risco e os prazos (RNF13). Testes de integração usam o `TestcontainersConfiguration`.
- **Sem dados reais:** nada de dados pessoais, credenciais ou segredos no código, nos testes ou nos commits. Use só dados fictícios.

## Fluxo de trabalho

- A `main` é protegida: todo trabalho vai numa branch (`feat/...`, `fix/...`, `docs/...`, `chore/...`) e entra por Pull Request com aprovação de outro integrante.
- Cada PR resolve uma issue e cita `Closes #N` na descrição.
- Mantenha `docs/declaracao-ia-terceiros.md` atualizada quando a IA ajudar em uma parte relevante ou quando entrar uma biblioteca nova.
