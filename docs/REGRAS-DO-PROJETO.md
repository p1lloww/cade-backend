# Regras do projeto

Regras para quem escreve código neste repositório, inclusive assistentes de IA. Elas resumem o que já vale no
`CLAUDE.md`, nos requisitos e nas decisões, para que ninguém (pessoa ou IA) saia do contexto do projeto ou pule os
testes. Em caso de conflito, vale a ordem: **decisão (`DECISOES.md`) > requisito (`requisitos.md`) > este arquivo**.

## 1. O que é o projeto

Back-end do **Cadê?**, sistema de achados e perdidos da UCSal, feito para o POOALAB 2026.2 (manual em
[PooaLAB_2026-2.pdf](PooaLAB_2026-2.pdf), quando presente). A entrega é um **MVP executável, estável no fluxo
principal, reproduzível e documentado**. A banca avalia mais um recorte menor e completo do que um projeto amplo e
instável.

Fluxo principal, que nunca pode quebrar: o estudante registra a perda, o funcionário cadastra o objeto, o sistema
sugere o par, o funcionário convoca, e a retirada é registrada no balcão.

## 2. Escopo e prioridade

- Antes de começar uma tarefa, leia o card, o requisito citado (RF/RN) e a decisão (D) relacionada.
- Prioridades: **P1** é o núcleo, **P2** completa o MVP, **P3** sai primeiro (plano de corte, `requisitos.md` seção 9).
- Os critérios de sucesso 1 a 5 nunca saem do escopo.
- Não implemente o que está em "Fora do escopo" (seção 9.3 dos requisitos).
- Mudança de comportamento que contradiz um requisito vira decisão nova (D31 em diante), com data. Decisão antiga
  nunca é apagada, só marcada como substituída.

## 3. Arquitetura

- Monólito modular (D10): um pacote por módulo em `br.ucsal.cade`, com `api`, `controller`, `service` e `persistence`.
- Entre módulos, só pela `api`, só nas direções da tabela do `CLAUDE.md`, e por id (sem chave estrangeira cruzando
  módulos). Mudar a tabela exige atualizar a D10, o `ModuleBoundariesTest` e o `CLAUDE.md` no mesmo PR.
- Toda rota fora das públicas declara `@RequiresRole`. Rota pública nova entra em `SecurityConfiguration.PUBLIC_ROUTES`.
- Ação relevante publica o `AuditEvent` dentro da transação (RF43). Ação do sistema usa `AuditEvent.SYSTEM_ACTOR`.
- Efeitos colaterais (cruzamento, e-mails) rodam depois do commit e nunca desfazem a ação principal. E-mail ao
  estudante passa pelo `Outbox.enqueue` (D30), nunca pelo `EmailSender` direto.
- O esquema do banco vem do Hibernate (`ddl-auto: update`): coluna nova é **nullable** ou tem `@ColumnDefault`.

## 4. Privacidade (RN03)

Foto, descrição interna, detalhes reservados e local exato de um objeto **nunca** vão para estudantes nem visitantes.
O detalhe secreto da perda nunca aparece para outro estudante nem em evento publicado. Foto nunca tem URL pública.
Registro de outro estudante responde 404, igual a registro inexistente (RF14).

## 5. Convenções de código

- Código em inglês; mensagens ao usuário, Javadoc e documentação em português (D11). Termo novo entra no
  `GLOSSARIO.md` no mesmo PR.
- DTOs em `controller/<assunto>/dto`, com sufixo `RequestDTO` ou `ResponseDTO`. O service não conhece DTO.
- Padrões de POOA só onde o problema pede; todo uso novo ganha uma linha em `docs/entrega/pooa-no-projeto.md`,
  começando pelo problema que resolve.
- Sem dados reais, credenciais ou segredos no código, nos testes ou nos commits.

## 6. Testes obrigatórios (RNF13)

Toda tarefa traz seus testes no mesmo PR. O que **sempre** precisa de teste:

- transições de estado (objeto, registro de perda, sugestão, convocação);
- a nota do cruzamento;
- as regras de conferência por risco;
- os prazos, com o `Clock` de `shared.time` (nos testes, o `MutableClock`), nunca com `sleep`;
- permissão por perfil e o caso "registro de outro estudante";
- conflito de versão (`@Version`) quando a tarefa envolve concorrência.

Testes de integração usam o `TestcontainersConfiguration` (precisa do Docker). Tarefa agendada nova leva o intervalo
em propriedade, com `1h` em `src/test/resources/config/application.yml`, e o teste chama o método direto.

## 7. Definição de pronto de uma issue

1. Os critérios de aceite do card estão atendidos, um por um.
2. `./mvnw verify` passa, incluindo `ModuleBoundariesTest` e `RouteAuthorizationCoverageTest`.
3. Os testes novos cobrem o caminho feliz, os erros previsíveis e os limites da seção 6.
4. A documentação foi atualizada: glossário, `pooa-no-projeto.md` (se usou padrão), `declaracao-ia-terceiros.md`
   (se a IA ajudou ou entrou biblioteca), documento de `docs/tecnico/` (se o fluxo é novo) e decisão (se houve).
5. Nenhum segredo, dado pessoal ou arquivo gerado foi versionado.

## 8. Git e PR

- A `main` é protegida. Uma branch por card (`feat/...`, `fix/...`, `docs/...`, `chore/...`) e PR direto para a
  `main`, com `Closes #N`, aprovação de outro integrante e CI verde. Merge sempre squash.
- Mensagem de commit e título do PR: `[COD-NN] Título do card (#N)`.
- Commit e PR escritos com IA terminam com a linha de co-autoria; o autor é o integrante dono da máquina.
- Não fazer push, criar issues ou convidar pessoas sem a equipe pedir.

## 9. O que não fazer

- Não alterar o repositório do front-end a partir daqui.
- Não esconder limitação: se algo ficou de fora ou falhou, diga no PR e no README.
- Não marcar tarefa como pronta com o build quebrado ou sem ter rodado os testes.
- Não adicionar funcionalidade fora do card só porque "ficaria bom".
