# Declaração de uso de IA e de componentes de terceiros

Baseado no Anexo E do manual do POOALAB. Mantenha esta declaração atualizada durante o projeto.

**Solução:** Cadê? — Achados e Perdidos UCSal
**Turma:** BES, POOA T1 (quinta-feira) · **Equipe:** Cadê?
**Integrantes:** Reynan Ferreira Correia, Robson Santos Cardoso Filho, Carlos Henrique Madureira Santos, Erick Reis Santos de Jesus e Enzo José Rodrigues Sacramento

## 1. Uso de inteligência artificial

Houve uso de IA? (X) Sim ( ) Não

| Ferramenta ou modelo | Finalidade ou tarefa | Parte afetada | Revisão e validação realizadas |
|---|---|---|---|
| Claude (Claude Code) | Planejamento e estrutura: revisão do documento de requisitos e da stack, estrutura inicial dos repositórios, backlog e organização do Project, workflow de CI | `docs/requisitos.md`, estrutura inicial do back-end e do front-end, `.github/` | Revisado e ajustado pela equipe |
| Claude (Claude Code) | Implementação de funcionalidades do back-end. Exemplos: login por código e JWT (ACE-02), situações com o padrão State (ACH-01, PER-01), cruzamento com Strategy e Composite (CRU-01), conferência por risco no balcão (RET-03), avisos com Transactional Outbox (AVI-02) e modo demonstração (DEM-01) | Módulos `access`, `founditems`, `lossreports`, `matching`, `pickup`, `notifications`, `retention`, `administration`, `demo` e `shared` | Plano de cada tarefa discutido e aprovado pelo integrante responsável antes do código; PR revisado por outro integrante; `./mvnw verify` no CI |
| Claude (Claude Code) | Testes automatizados: unitários, de integração com Testcontainers, de concorrência e de prazos com relógio controlado | `src/test` | Executados localmente e no CI a cada PR (784 testes na versão final) |
| Claude (chat do claude.ai) | Histórico imutável (ADM-01): desenho, geração do código e revisão independente, em conversas separadas | `shared.audit`, `administration` | Código conferido pelo integrante e por uma revisão independente; `./mvnw verify` executado |
| Claude (Claude Code) | Revisão e integração das últimas PRs (23/11): correção de dois erros (a renovação do registro e o prazo de guarda ignoravam os prazos configurados pelo administrador), resolução de conflitos e correção de dois testes instáveis | PRs #83 e #86 a #92 | Testes novos para cada correção; CI verde antes de cada merge; aprovação registrada no PR |
| Claude (Claude Code) | Reorganização dos pacotes por assunto dentro de cada módulo (complemento da D10) | `src/main` e `src/test` | `./mvnw verify` completo, incluindo o teste de fronteiras entre módulos (ArchUnit) |
| Claude (Claude Code) | Documentação: documentos técnicos com diagramas, decisões, glossário, README, roteiro da gravação, redação da validação e da definição do MVP a partir das evidências registradas pela equipe, e o pitch | `docs/`, `demo/`, `README.md`, pitch | Revisado pela equipe; a validação usa só as evidências das conversas de campo (29/10 e 04/11) |

**Observações sobre o uso de IA:** a IA não gerou evidências de validação: as evidências vêm das conversas de campo feitas pela equipe. Todo código passou por PR, pelo CI e pela revisão de um integrante, e cada integrante é capaz de explicar as partes feitas com apoio de IA. A tabela traz exemplos; o histórico completo está nos PRs do repositório.

## 2. Componentes e recursos de terceiros

Houve uso de componentes de terceiros? (X) Sim ( ) Não

| Componente ou recurso | Origem ou link | Versão e licença | Função e alterações realizadas |
|---|---|---|---|
| Spring Boot | https://spring.io/projects/spring-boot | 4.1.2, Apache 2.0 | Framework do back-end; projeto gerado pelo Spring Initializr |
| PostgreSQL | https://www.postgresql.org | 17, PostgreSQL License | Banco de dados |
| Mailpit | https://github.com/axllent/mailpit | 1.27, MIT | Captura de e-mails em desenvolvimento |
| springdoc-openapi | https://springdoc.org | 3.0.2, Apache 2.0 | Gera o documento OpenAPI e serve o Swagger UI (D17) |
| Testcontainers | https://testcontainers.com | MIT | Banco temporário nos testes |
| ArchUnit | https://www.archunit.org | 1.4.1, Apache 2.0 | Teste das fronteiras entre módulos (D10) |
| GitHub Actions (`actions/checkout`, `actions/setup-java`, `actions/upload-artifact`) | https://github.com/actions | v7, v6 e v7, MIT | CI: build e testes a cada PR (QUA-01) |
| Angular | https://angular.dev | 22, MIT | Framework do front-end |

## 3. Materiais ou código preexistentes

Houve material produzido antes do PooaLAB? ( ) Sim (X) Não

## 4. Declaração da equipe

Declaramos que as informações acima são completas e verdadeiras; que revisamos e testamos as contribuições apoiadas por IA; que identificamos as fontes, licenças e partes preexistentes relevantes; que não apresentamos como autoria própria material de terceiros; e que não compartilhamos dados pessoais, credenciais ou informações restritas de forma indevida.

**Responsável pela submissão:** Reynan Ferreira Correia

### Ciência dos integrantes

| Nome completo | Assinatura ou registro de ciência |
|---|---|
| Reynan Ferreira Correia | |
| Robson Santos Cardoso Filho | |
| Carlos Henrique Madureira Santos | |
| Erick Reis Santos de Jesus | |
| Enzo José Rodrigues Sacramento | |

**Data:** 23/11/2026
