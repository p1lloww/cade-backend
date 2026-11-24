# Roteiro da gravação (contingência)

Gravação curta do fluxo principal, exigida pelo manual do POOALAB como contingência da demonstração ao vivo. Ela não
substitui o MVP executável. Duração-alvo: **cerca de 2 minutos**. Mostra o sistema funcionando; arquitetura e código
ficam para o pitch.

Se o front-end já faz o fluxo inteiro, grave pelo front seguindo os mesmos passos; senão, grave pelo Swagger, como abaixo.

## 1. Preparar (antes de apertar o REC)

1. No `.env`, deixe `SPRING_PROFILES_ACTIVE=demo` (sem o `#` na frente) e suba do zero:
   ```bash
   docker compose down -v
   docker compose up -d --build
   ```
2. Espere http://localhost:8080/actuator/health responder `UP` e abra três abas:
   - Swagger: http://localhost:8080/swagger-ui.html
   - Mailpit: http://localhost:8025 (caixa vazia)
   - Front-end, se for usá-lo.
3. Pegue os tokens e os ids num **bloco de notas fora da tela**:
   - No Swagger, `POST /demo/login` três vezes, com `{"role": "STUDENT"}`, `{"role": "STAFF"}` e `{"role": "ADMIN"}`.
     Guarde o `accessToken` de cada um.
   - **Authorize** (cadeado no topo) com o token do estudante → `GET /loss-reports/mine` → guarde o `id` do registro
     "Fone de ouvido JBL preto" e o do registro "iPhone".
   - **Authorize** com o token do funcionário → `GET /loss-reports/{lossReportId}/suggestions` no registro do fone e no do
     iPhone → guarde o `id` da sugestão de cada um.
4. No Authorize, cole **só o token**, sem a palavra `Bearer`. Para trocar de perfil: **Logout** e cole o outro.
5. Feche notificações, aumente o zoom do navegador para 125% a 150% e esconda abas com dados pessoais.
6. Faça um ensaio completo. Depois, `docker compose down -v`, suba de novo e **refaça o passo 3** (os ids mudam), para
   gravar com os dados limpos.

## 2. Gravar

| Tempo | O que fazer na tela | O que falar |
|---|---|---|
| 0:00–0:15 | Swagger aberto, rolar a lista de rotas | "Este é o Cadê?, o sistema de achados e perdidos da UCSal. Hoje os objetos ficam só numa etiqueta, em caixas por mês, sem nenhuma lista. Aqui ele está rodando localmente, no modo demonstração, só com dados fictícios." |
| 0:15–0:30 | Authorize com o token do **estudante**; `GET /loss-reports/mine` → Execute | "Este é o estudante. Ele registrou de casa três perdas, entre elas um fone JBL preto com um adesivo de estrela na caixa. Ele não vê nada dos objetos guardados na sala: nem foto, nem descrição, nem local." |
| 0:30–0:55 | Logout; Authorize com o token do **funcionário**; `GET /loss-reports/{lossReportId}/suggestions` com o id do fone → Execute, apontar `score` e `bandLabel`; depois `GET /suggestions/{id}` com o id da sugestão | "Agora o lado da sala. O sistema já cruzou as perdas com os objetos cadastrados e sugeriu o fone achado no bloco 400, sala 421, com compatibilidade alta. O funcionário compara os dois lados e vê a nota de cada critério: categoria, data, local e palavras em comum." |
| 0:55–1:10 | `POST /suggestions/{id}/summon` com o id da sugestão do fone → Execute; copiar o `id` da resposta (é o id da convocação); abrir o **Mailpit** e o e-mail | "Ele convoca o estudante. O objeto fica reservado para ele, e o estudante recebe este e-mail com o local, o prazo e o que levar." |
| 1:10–1:35 | Voltar ao Swagger; `GET /summons/{id}` com o id da convocação, apontar o que conferir; `POST /summons/{id}/pickup` com o corpo **A** abaixo → Execute | "No balcão, o sistema mostra o que conferir conforme o risco do objeto: documento com foto, matrícula e o detalhe que só o dono conhece. Conferido, o funcionário registra a retirada, e tudo fica no histórico." |
| 1:35–1:55 | `GET /loss-reports/{lossReportId}/suggestions` com o id do **iPhone**, apontar a nota baixa; `POST /suggestions/{id}/summon` na sugestão do iPhone, copiar o `id`; `POST /summons/{id}/refusal` com o corpo **B** → Execute | "Agora o caso de fraude que ouvimos dos estudantes: alguém registra só 'iPhone'. A sugestão vem com nota baixa. E, se mesmo assim for convocado, a verificação falha no balcão, porque ele não consegue desbloquear o celular. A recusa fica registrada, e três recusas bloqueiam novos registros." |
| 1:55–2:05 | Rolar até o topo do Swagger | "Esse é o fluxo principal do Cadê?: do registro da perda, de casa, até a retirada segura no balcão." |

**Corpo A, retirada do fone** (risco médio: a verificação aceita é o detalhe reservado):

```json
{
  "documentType": "STUDENT_CARD",
  "documentNumber": "000000",
  "enrollment": "000000000",
  "verification": "DETAIL_CONFIRMED",
  "notes": "Descreveu o adesivo de estrela na caixa."
}
```

**Corpo B, recusa do iPhone:**

```json
{
  "reason": "Não conseguiu desbloquear o celular na frente do funcionário."
}
```

## 3. Depois de gravar

- Assista uma vez inteira: as respostas e o e-mail precisam aparecer legíveis.
- Se algo der errado no meio de um passo, regrave aquele passo inteiro (não corte no meio da fala).
- Suba o vídeo no YouTube como **não listado** ou no Google Drive com **"qualquer pessoa com o link pode ver"**: o
  professor precisa abrir sem pedir permissão.
- Coloque o link no README, na seção "Roteiro do fluxo principal".

## Se der erro

| O que aparece | O que fazer |
|---|---|
| `401` | O token não foi colado ou venceu: faça `POST /demo/login` de novo e cole no Authorize |
| `403` | Perfil errado: o passo do estudante usa o token do estudante; sugestões, convocação e balcão usam o do funcionário |
| `404` em `/demo/login` | O profile `demo` não está ligado: confira `SPRING_PROFILES_ACTIVE=demo` no `.env` e suba de novo |
| `409` ao convocar | Essa sugestão já foi convocada (provavelmente no ensaio): suba do zero com `docker compose down -v` |
| O e-mail não aparece no Mailpit | Espere alguns segundos e atualize a página do Mailpit |
