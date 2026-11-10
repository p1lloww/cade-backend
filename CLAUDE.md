# CLAUDE.md

Back-end do **Cadê?**, sistema de achados e perdidos da UCSal. É um projeto acadêmico do POOALAB (disciplina Programação Orientada a Objetos Avançada, 2026.2), feito por uma equipe de 4 estudantes. A entrega final é em **23/11/2026**, e a mostra com pitch é em 01/12 (ADS) e 03/12 (BES).

Responder em português (BR).

O front-end (Angular, publicado na Vercel) fica em `p1lloww/cade-frontend`. O backlog está no GitHub Project https://github.com/users/p1lloww/projects/4.

## Fonte da verdade

Antes de modelar, codar ou criar tarefas, consulte:

- `docs/requisitos.md`: RF, RN e RNF (v3), com prioridades P1/P2/P3, estados (seção 6) e plano de corte (seção 9). Os códigos são citados nas issues e no campo "Requisitos" do Project.
- `docs/DECISOES.md`: decisões D01–D15 sobre stack, arquitetura e processo. Decisão nova entra lá com data; decisão antiga nunca é apagada, só marcada como substituída. Em conflito com os requisitos, vale a decisão.
- `docs/GLOSSARIO.md`: correspondência entre os termos dos requisitos (português) e os nomes do código (inglês, D11).

## Comandos

```bash
docker compose up -d postgres mailpit   # dependências para desenvolver
./mvnw spring-boot:run                  # API em http://localhost:8080
./mvnw verify                           # build + testes (precisa do Docker: Testcontainers)
./mvnw test -Dtest=ModuleBoundariesTest # só o teste de fronteiras dos módulos
docker compose up -d --build            # tudo em contêiner, como na entrega
docker compose down -v                  # zera o banco local
```

E-mails enviados em desenvolvimento aparecem no Mailpit: http://localhost:8025.

## Stack

Java 25, Spring Boot 4.1 (Web MVC, Security, OAuth2 Resource Server para o JWT, Data JPA, Mail, Validation, Actuator), PostgreSQL 17, Docker Compose, Testcontainers e ArchUnit. O esquema do banco é gerado pelo Hibernate (`ddl-auto: update`, D06); não há Flyway. Configurações sensíveis vêm de variáveis de ambiente (ver `.env.example`).

## Arquitetura (D10)

Monólito modular. Um pacote por módulo em `br.ucsal.cade`, cada um com as camadas `api` (o único pacote visível de fora: interfaces, DTOs e eventos), `controller`, `service` e `persistence`:

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
| `shared` | Núcleo: `audit`, `event`, `exception`, `mail`, `security`, `time`, `web` | nenhum módulo |

- **Entidades** ficam em `persistence`, com anotações JPA e os métodos de regra sobre o próprio estado (ex.: transições de situação). Regra que depende de outros registros ou módulos fica no `service`.
- **Entre módulos:** só pela `api`, só nas direções da tabela, referência por id (sem chave estrangeira cruzando módulos). Para avisar que algo aconteceu, publique um evento. O `ModuleBoundariesTest` (ArchUnit) quebra o build se essas regras forem violadas; mudar a tabela exige registrar na D10.
- **Histórico (RF43):** publique o evento de `shared.audit` na mesma transação da ação; `administration` grava.
- **Efeitos colaterais** (cruzamento, e-mails) rodam depois do commit: `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`, em Virtual Threads (D05). Uma falha neles nunca desfaz nem bloqueia a ação principal.
- **Login (D02, D03):** código de uso único enviado ao e-mail @ucsal.edu.br (`SecureRandom`, 10 min, uso único, só hash, 5 tentativas, 1 envio por minuto) trocado por um JWT no header `Authorization`. Sem senha e sem cookie (D09).
- **Privacidade:** foto, descrição interna, detalhes reservados e local exato de um objeto nunca vão para estudantes nem visitantes (RN03).

## Convenções

- **Idioma (D11):** código em inglês (classes, métodos, pacotes, rotas); mensagens ao usuário, Javadoc e documentação em português. Termo novo entra no `GLOSSARIO.md` no mesmo PR.
- **Padrões de POOA** (State, Strategy, Composite, Observer, Streams, Virtual Threads, anotações e reflexão, SOLID) valem bônus quando aparecem de forma justificada. Use-os onde o problema pede, nunca de forma artificial.
- **Testes:** cobrir transições de estado, a nota do cruzamento, as regras por risco e os prazos (RNF13). Testes de integração usam o `TestcontainersConfiguration`; prazos usam o `Clock` de `shared.time`.
- **Sem dados reais:** nada de dados pessoais, credenciais ou segredos no código, nos testes ou nos commits.

## Fluxo de trabalho

- A `main` é protegida (D12): todo trabalho vai numa branch (`feat/...`, `fix/...`, `docs/...`, `chore/...`) e entra por PR com aprovação de outro integrante.
- Cada PR resolve um card e cita `Closes #N`. Os cards têm código no título (`[ACE-02]`) e as dependências no campo "Depende de" (D13).
- Mantenha `docs/declaracao-ia-terceiros.md` atualizada quando a IA ajudar numa parte relevante ou quando entrar uma biblioteca nova.
- Não fazer commit, push, criar issues ou convidar pessoas sem a equipe pedir.
