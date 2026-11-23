# Demonstração (modo `demo`)

O modo demonstração (RF47) sobe o back-end com **contas fictícias, login direto sem código e dados de exemplo**, para a
mostra funcionar sem internet e sem cadastros feitos na hora. Ele só existe com o profile `demo`: sem o profile, as rotas
`/demo/*` respondem 404 e nada emite token sem o código de e-mail (D02).

> **Nunca use o profile `demo` num ambiente real.** Ele dispensa o código de login.

## Como ligar

Pelo Docker (como na entrega), com o `.env` já preenchido (`JWT_SECRET`):

```bash
echo "SPRING_PROFILES_ACTIVE=demo" >> .env
docker compose down -v          # banco do zero, para os dados de exemplo entrarem limpos
docker compose up -d --build
```

Pelo Maven, em desenvolvimento:

```bash
docker compose up -d postgres mailpit
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run
```

Para desligar, tire a linha do `.env` (ou apague a variável) e suba de novo.

## Contas fictícias

Os e-mails são inventados e não recebem nada. Entre com `POST /demo/login` (no Swagger: http://localhost:8080/swagger-ui.html)
e use o `accessToken` como `Authorization: Bearer <accessToken>`; o token é igual ao do login normal.

| Perfil (`role`) | E-mail |
|---|---|
| `STUDENT` | estudante.demo@ucsal.edu.br |
| `STAFF` | funcionario.demo@ucsal.edu.br |
| `ADMIN` | admin.demo@ucsal.edu.br |

```bash
curl -s -X POST http://localhost:8080/demo/login -H 'Content-Type: application/json' -d '{"role":"STAFF"}'
```

`GET /demo/accounts` lista as três contas, para a tela de escolha do front.

## Dados de exemplo

Criados na subida, uma vez só (reiniciar não duplica; para recomeçar, `docker compose down -v`). Tudo inventado, com fotos
de degradê desenhadas na hora.

**Objetos na sala** (cadastrados pelo funcionário de demonstração):

| Categoria | Onde | Descrição interna | Detalhe reservado |
|---|---|---|---|
| Fone | Bloco 400, sala 421 | Fone JBL preto, arranhão na haste | Adesivo de estrela na caixa |
| Celular | Bloco 100, sala 105 | iPhone com capa transparente | Foto de um cachorro como papel de parede |
| Garrafa | Bloco 300, sala 310 | Garrafa térmica azul | — |
| Mochila | Bloco 200, sala 201 | Mochila preta com chaveiro vermelho | Caderno de cálculo no bolso da frente |

**Registros de perda do estudante de demonstração:**

| Registro | O que mostra |
|---|---|
| "Fone de ouvido JBL preto", detalhe "Adesivo de estrela na caixa", Bloco 400, sala 421 | Par certo: sugestão com compatibilidade **alta** com o fone |
| "iPhone", sem detalhe e sem local | **Registro genérico** (critério de sucesso 4): recebe nota baixa, na faixa média no máximo (50 de 100), nunca na alta |
| "Garrafa térmica azul", detalhe "Meu nome gravado na tampa", Bloco 300, sala 310 | Par certo de risco baixo |

As sugestões são calculadas logo depois da subida, por baixo, pelo mesmo cruzamento do uso real.

## Roteiro do fluxo principal (cerca de 2 minutos)

Siga pelo Swagger (http://localhost:8080/swagger-ui.html), trocando o token com **Authorize** a cada troca de perfil.

1. **Estudante** (`STUDENT`): `GET /loss-reports/mine` mostra as três perdas. Para registrar uma nova, `POST /loss-reports`.
2. **Funcionário** (`STAFF`): `GET /loss-reports/{id}/suggestions` no registro do fone mostra a sugestão com compatibilidade
   alta. No registro "iPhone", a sugestão vem com nota baixa. `GET /suggestions/{id}` compara os dois lados.
3. **Convocar:** `POST /suggestions/{id}/summon` na sugestão do fone. O objeto fica reservado e o e-mail do estudante chega
   ao Mailpit (http://localhost:8025), com local, horário, prazo e o que levar.
4. **Balcão:** `GET /summons` lista as convocações; `GET /summons/{id}` mostra o que conferir pelo risco; 
   `POST /summons/{id}/pickup` registra a retirada com documento, matrícula e verificação.
5. **Caso do "iPhone" genérico:** convoque a sugestão do iPhone e registre uma recusa em `POST /summons/{id}/refusal` (a
   verificação de celular falha). A recusa fica registrada e conta para o estudante (3 recusas bloqueiam novos registros).
6. **Administrador** (`ADMIN`): vê o histórico imutável de tudo o que foi feito e libera um estudante bloqueado
   (`/blocked-students`).

Os e-mails ficam na caixa interna do **Mailpit**, que roda na própria máquina e funciona sem internet (D31).
