# Cadê? — Achados e Perdidos UCSal

[![CI](https://github.com/p1lloww/cade-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/p1lloww/cade-backend/actions/workflows/ci.yml)

> **Status:** em desenvolvimento (POOALAB 2026.2). As seções marcadas com _A preencher_ são completadas ao longo do projeto.

O Cadê? liga o estudante que perdeu um objeto à sala de achados e perdidos da UCSal. O estudante registra a perda online, a sala cadastra o que chega e o sistema cruza os dois lados, sugerindo ao funcionário só os pares prováveis. A conferência e a entrega continuam presenciais, no balcão.

Este é o repositório do **back-end**. O front-end está em [cade-frontend](https://github.com/p1lloww/cade-frontend).

## Problema, público e proposta de valor

- **Público:** estudantes da UCSal que perdem objetos no campus e funcionários da sala de achados e perdidos.
- **Problema:** os objetos ficam registrados só numa etiqueta presa a eles, em caixas por mês, sem lista pesquisável. Muitos estudantes não sabem que a sala existe.
- **Proposta:** a sala ganha uma lista pesquisável, e o estudante resolve a distância: registra a perda e só vai à sala quando é convocado.

Detalhes e evidências em [docs/entrega/problema-validacao.md](docs/entrega/problema-validacao.md) e [docs/entrega/definicao-mvp.md](docs/entrega/definicao-mvp.md).

## Integrantes e turma

_A preencher:_ nome completo e turma de cada integrante.

## Tecnologias

| Camada | Tecnologia | Justificativa |
|---|---|---|
| Back-end | Java 25 + Spring Boot 4.1 | Linguagem da disciplina; padrões de POOA visíveis no código |
| Banco | PostgreSQL 17 | Relacional, transações para estados e histórico |
| Esquema do banco | JPA/Hibernate (`ddl-auto: update`) | Rápido para um MVP; migrations com Flyway ficam como próximo passo |
| Login | Código de uso único por e-mail @ucsal.edu.br + JWT | Sem senha e sem depender de provedor externo; códigos na própria base PostgreSQL (D18) |
| Documentação da API | springdoc-openapi (Swagger UI) | O front consulta e testa os endpoints sozinho; erros no formato RFC 9457 (D17) |
| E-mail | Gmail SMTP (Mailpit em desenvolvimento) | Grátis e simples; Mailpit captura os e-mails localmente |
| Assíncrono | Eventos do Spring + Virtual Threads | O cadastro responde na hora; cruzamento e e-mails rodam em paralelo |
| Infra | Docker Compose | Banco, Mailpit e back-end com um comando |
| CI | GitHub Actions | Build e testes em todo PR; o merge exige o CI passando |

## Pré-requisitos

- Docker 24+ com Docker Compose
- Para desenvolver: JDK 25 (o Maven vem pelo `./mvnw`)

## Como executar

### Tudo pelo Docker

```bash
cp .env.example .env    # preencha o JWT_SECRET: openssl rand -base64 48
docker compose up -d --build
```

O `JWT_SECRET` é obrigatório: sem ele o back-end não sobe, porque não existe chave padrão (uma chave publicada aqui permitiria a qualquer um forjar um token de administrador).

- API: http://localhost:8080 (saúde em `/actuator/health`)
- Documentação da API (Swagger): http://localhost:8080/swagger-ui.html
- E-mails capturados (Mailpit): http://localhost:8025

### Desenvolvimento (back-end pela IDE ou pelo Maven)

```bash
docker compose up -d postgres mailpit
./mvnw spring-boot:run    # lê o mesmo .env
```

Para zerar o banco local: `docker compose down -v`.

O esquema é gerado pelo Hibernate (`ddl-auto: update`), que só **acrescenta** tabelas e colunas: um campo removido ou renomeado deixa a coluna antiga no banco. Se aparecer um erro de banco estranho depois de puxar mudanças (por exemplo, `null value in column ...`), zere o banco com o comando acima. Antes de uma demonstração, suba o banco do zero.

## Estrutura do código

Monólito modular (D10). Cada módulo dos requisitos é um pacote em `br.ucsal.cade`, com as camadas `api` (o único pacote visível para outros módulos), `controller`, `service` e `persistence`. O código é em inglês (D11); a correspondência com os termos dos requisitos está no [glossário](docs/GLOSSARIO.md).

| Pacote | Módulo | Pode depender de |
|---|---|---|
| `access` | M1 Acesso | — |
| `founditems` | M2 Achados | — |
| `lossreports` | M3 Perdas | — |
| `matching` | M4 Cruzamento | founditems, lossreports |
| `pickup` | M5 Convocação e retirada | founditems, lossreports, matching |
| `notifications` | M6 Avisos | access, lossreports, pickup |
| `retention` | M7 Prazos e destino | founditems |
| `publicpage` | M8 Página pública | founditems |
| `administration` | M9 Administração | access |
| `demo` | M10 Modo demonstração | access, founditems, lossreports |
| `shared` | Núcleo compartilhado (histórico, eventos, erros, e-mail, segurança, relógio, web) | nenhum módulo |

O teste `ModuleBoundariesTest` (ArchUnit) garante essas regras: acesso entre módulos só pela `api` e nas direções da tabela, sem ciclos, e camadas na ordem `controller → service → persistence`.

Documentos de apoio em `docs/` (índice em [docs/README.md](docs/README.md)): [requisitos](docs/requisitos.md), [decisões](docs/DECISOES.md) e [glossário](docs/GLOSSARIO.md).

## Como contribuir

A `main` é protegida. Crie uma branch (`feat/...`, `fix/...`, `docs/...`), abra um Pull Request para a `main` citando o card (`Closes #N`) e peça a revisão de outro integrante. O merge só acontece com 1 aprovação e com o CI passando.

## Testes

```bash
./mvnw verify
```

Os testes de integração sobem um PostgreSQL temporário com Testcontainers, então o Docker precisa estar rodando.

O CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) roda o mesmo `./mvnw verify` a cada push e a cada Pull Request para a `main`. Um PR com o build quebrado não pode ser mesclado.

## Dados de teste e demonstração

Para a demonstração, o back-end tem o **modo `demo`**: contas fictícias de estudante, funcionário e administrador com login direto (sem código de e-mail) e dados de exemplo, inclusive o caso do registro genérico "iPhone". Liga-se com `SPRING_PROFILES_ACTIVE=demo` (veja o [.env.example](.env.example)) e só existe com esse profile: sem ele, `/demo/*` responde 404.

| Perfil | E-mail fictício |
|---|---|
| Estudante | estudante.demo@ucsal.edu.br |
| Funcionário | funcionario.demo@ucsal.edu.br |
| Administrador | admin.demo@ucsal.edu.br |

Como ligar, os dados criados e o roteiro passo a passo estão em [demo/README.md](demo/README.md). **Nunca use o profile `demo` num ambiente real**, porque ele dispensa o código de login.

## Roteiro do fluxo principal

Com o modo `demo` ligado (seção acima), o fluxo principal, de ponta a ponta, em cerca de 2 minutos pelo Swagger:

1. **Estudante:** vê as perdas registradas (`GET /loss-reports/mine`).
2. **Funcionário:** abre a sugestão do fone, de compatibilidade alta (`GET /loss-reports/{id}/suggestions`), e compara os dois lados (`GET /suggestions/{id}`).
3. **Convocar** (`POST /suggestions/{id}/summon`): o objeto fica reservado e o e-mail de convocação chega ao Mailpit (http://localhost:8025).
4. **Balcão:** confere pelo risco e registra a retirada (`POST /summons/{id}/pickup`).
5. **Caso do "iPhone" genérico:** a sugestão sai com nota baixa e, se convocada, a verificação falha no balcão e a recusa fica registrada (`POST /summons/{id}/refusal`).

Cada rota, com os perfis e os exemplos, está no [demo/README.md](demo/README.md) e no Swagger.

## Limitações conhecidas e próximos passos

- O esquema do banco é gerado pelo Hibernate (`ddl-auto: update`), sem migrations versionadas; o próximo passo seria o Flyway, com o esquema atual como primeira versão.
- O bloqueio contra tentativas erradas é por e-mail: quem sabe o e-mail de alguém pode bloqueá-lo por 15 minutos (D18).
- Os eventos ficam na memória da aplicação: se o back-end cair entre o commit e o processamento, aquele evento se perde.
- E-mail que falha no envio só é registrado no log, sem nova tentativa (RNF08): no login, a pessoa pede outro código depois de 1 minuto.

## Fontes, licenças, componentes de terceiros e uso de IA

Ver [docs/entrega/declaracao-ia-terceiros.md](docs/entrega/declaracao-ia-terceiros.md).
