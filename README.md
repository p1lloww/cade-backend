# Cadê? — Achados e Perdidos UCSal

[![CI](https://github.com/p1lloww/cade-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/p1lloww/cade-backend/actions/workflows/ci.yml)

> **Status:** versão de entrega do POOALAB 2026.2 (23/11/2026). Mostra e pitch em 03/12 (BES).

O Cadê? liga o estudante que perdeu um objeto à sala de achados e perdidos da UCSal. O estudante registra a perda online, a sala cadastra o que chega e o sistema cruza os dois lados, sugerindo ao funcionário só os pares prováveis. A conferência e a entrega continuam presenciais, no balcão.

Este é o repositório do **back-end**. O front-end está em [cade-frontend](https://github.com/p1lloww/cade-frontend).

## Problema, público e proposta de valor

- **Público:** estudantes da UCSal que perdem objetos no campus e funcionários da sala de achados e perdidos.
- **Problema:** os objetos ficam registrados só numa etiqueta presa a eles, em caixas por mês, sem lista pesquisável. Muitos estudantes não sabem que a sala existe.
- **Proposta:** a sala ganha uma lista pesquisável, e o estudante resolve a distância: registra a perda e só vai à sala quando é convocado.

Detalhes e evidências em [docs/entrega/problema-validacao.md](docs/entrega/problema-validacao.md) e [docs/entrega/definicao-mvp.md](docs/entrega/definicao-mvp.md).

Gravação curta do fluxo principal, do registro da perda até a retirada no balcão:
[Assistir no YouTube](https://youtu.be/zeGMRuIDiHo)

O vídeo é apenas apoio. O MVP executável é o item principal da entrega e pode ser executado seguindo as seções abaixo.

## Integrantes e turma

Turma: Bacharelado em Engenharia de Software (BES), Programação Orientada a Objetos Avançada (POOA), T1, quinta-feira.

- Reynan Ferreira Correia
- Robson Santos Cardoso Filho
- Carlos Henrique Madureira Santos
- Erick Reis Santos de Jesus
- Enzo José Rodrigues Sacramento

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

## Estrutura do projeto

```
cade-backend/
├── src/
│   ├── main/
│   │   ├── java/br/ucsal/cade/     código-fonte, um pacote por módulo (veja abaixo)
│   │   └── resources/
│   │       └── application.yml     configuração; segredos vêm de variáveis de ambiente
│   └── test/
│       ├── java/br/ucsal/cade/     testes, nos mesmos pacotes do código testado
│       │   └── architecture/       ModuleBoundariesTest (fronteiras entre módulos)
│       └── resources/config/       configuração só dos testes (chave JWT de teste)
├── docs/
│   ├── requisitos.md               RF, RN e RNF (v3), estados e plano de corte
│   ├── DECISOES.md                 decisões de stack, arquitetura e processo (D01 em diante)
│   ├── GLOSSARIO.md                termos dos requisitos ↔ nomes do código
│   ├── REGRAS-DO-PROJETO.md        regras para quem escreve código no repositório
│   ├── tecnico/                    como as partes importantes funcionam por dentro, com diagramas
│   ├── entrega/                    documentos do manual: validação, MVP, POOA e declaração de IA
│   └── imagens/                    figuras usadas nos documentos
├── demo/
│   └── README.md                   modo demonstração: contas fictícias, dados e roteiro
├── .github/
│   ├── workflows/ci.yml            CI: ./mvnw verify em todo PR para a main
│   ├── workflows/project-dependencies.yml   atualiza as dependências dos cards no Project
│   └── scripts/                    script usado por esse workflow
├── compose.yaml                    PostgreSQL, Mailpit e back-end com um comando
├── Dockerfile                      imagem do back-end
├── .env.example                    modelo das variáveis de ambiente (o .env não é versionado)
└── pom.xml, mvnw                   build Maven (o ./mvnw baixa o Maven sozinho)
```

### Dentro de cada módulo

Todos os módulos seguem o mesmo desenho. Dentro de cada camada, um subpacote por assunto. Exemplo do módulo de perdas:

```
lossreports/
├── api/                    o único pacote que outros módulos podem usar
│   ├── candidate/          o que o cruzamento lê das perdas
│   ├── summons/            o que a convocação e o balcão usam (convocar, recusar)
│   ├── deadline/           prazos lidos pelos avisos
│   ├── registration/       cadastro usado pelo modo demonstração
│   └── event/              eventos publicados (perda registrada, encerrada...)
├── controller/             rotas HTTP; cada assunto tem a interface XxxApi (Swagger) e o XxxController
│   └── report/dto/         RequestDTO (o que chega) e ResponseDTO (o que sai)
├── service/                regras que dependem de outros registros ou módulos
│   ├── report/             registrar e corrigir a perda
│   ├── lifecycle/          cancelar, pedir informações, renovar e vencer
│   ├── reporter/           o estudante: limite de registros e bloqueio
│   └── port/               implementações da api, chamadas pelos outros módulos
├── persistence/            entidades JPA com as regras do próprio estado, e repositórios
│   ├── report/  status/  period/  location/  reporter/
└── exception/              erros do módulo (estendem os de shared.exception)
```

- **`api`:** interfaces, records e eventos. Outro módulo nunca importa `service` nem `persistence` de fora (D10).
- **`service/port`:** quando um módulo precisa de outro, ele chama uma interface da `api`, e a implementação fica aqui.
- **`persistence`:** as transições de situação ficam na própria entidade (padrão State em `status/`).
- **`shared`:** o que todos usam e que não pertence a nenhum módulo: `audit` (histórico), `category`, `event`, `exception`, `mail`, `security`, `settings` (prazos), `storage` (fotos), `time` (relógio) e `web` (erros e Swagger).

## Módulos e dependências

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
| `administration` | M9 Administração | access, founditems |
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

- O esquema do banco é gerado pelo Hibernate (`ddl-auto: update`), sem migrations versionadas: ele só acrescenta tabelas e colunas, e um valor novo numa situação (enum) esbarra na restrição antiga do banco. Um banco já existente precisa subir do zero (`docker compose down -v`) depois dessas mudanças. O próximo passo seria o Flyway, com o esquema atual como primeira versão.
- O bloqueio contra tentativas erradas é por e-mail: quem sabe o e-mail de alguém pode bloqueá-lo por 15 minutos (D18).
- O cruzamento é disparado por evento na memória da aplicação: se o back-end cair entre o commit e o cruzamento, aquele cadastro não é cruzado com o que já existia; ele só aparece em sugestões quando chegar um cadastro novo do outro lado. Os e-mails não têm esse problema, porque passam pela caixa de saída gravada no banco (D30).
- O e-mail com o código de login não é reenviado se o envio falhar, porque o código não fica gravado (D30): a pessoa pede outro depois de 1 minuto. Os demais e-mails são reenviados.
- A prova anexada ao registro de perda aceita só foto (JPEG, PNG ou WebP); nota fiscal em PDF precisa virar foto (D34).
- Corrigir um registro de perda não refaz o cruzamento: as sugestões já criadas mantêm a nota (D32).
- Os prazos (guarda, retirada e validade do registro) são configuráveis pelo administrador; os valores iniciais vêm das conversas com a sala e o administrador, mas só o de guarda foi validado com eles.

## Fontes, licenças, componentes de terceiros e uso de IA

Ver [docs/entrega/declaracao-ia-terceiros.md](docs/entrega/declaracao-ia-terceiros.md).
