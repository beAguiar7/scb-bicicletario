# Testes da Sprint 2

Registro do que foi executado em 10 de novembro de 2026. Nada que não tenha sido executado está marcado como aprovado.

A verificação visual no navegador não foi feita: não havia ferramenta de navegador nesta sessão. O envio real de e-mail e o aplicativo móvel não foram implementados e não estão concluídos.

## Testes automatizados

Comando, na pasta do projeto, com o truststore do Windows por causa do antivírus:

```powershell
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\mvnw.cmd test
```

Resultado geral: **BUILD SUCCESS**. 42 testes, 0 falhas, 0 erros, 0 ignorados. O log mostrou `jdbc:h2:mem:scbteste`. O banco de arquivo `dados/scb` não foi aberto pelos testes.

Os 20 testes da Sprint 1 continuam na mesma suíte e passaram de novo: `CadastroCiclistaTest` (8), `AutenticacaoCiclistaTest` (6), `ScbApplicationTests` (5) e `FalhaInesperadaTest` (1).

### Atualização válida

- História: HU03.
- Procedimento: `AtualizacaoCiclistaTest.atualizaDadosSemTrocarSenhaNemFoto`.
- Resultado esperado: nome, nacionalidade e documento mudam; o e-mail e a senha permanecem; a resposta não traz senha nem foto; o login antigo continua válido; um id na consulta é ignorado.
- Resultado obtido: o teste passou. A mensagem foi `Cadastro atualizado.`
- Situação: passou.

### Atualização inválida

- História: HU03, reutilizando as validações do cadastro.
- Procedimento: `AtualizacaoCiclistaTest.dadosInvalidosNaoAlteramOCadastro`.
- Resultado esperado: HTTP 400, código `DADOS_INVALIDOS`, e o nome antigo permanece.
- Resultado obtido: o teste passou.
- Situação: passou.

### E-mail duplicado

- História: HU03.
- Procedimento: `AtualizacaoCiclistaTest.emailDuplicadoERecusado`.
- Resultado esperado: HTTP 409, `EMAIL_JA_CADASTRADO` e a mensagem `Este e-mail já está cadastrado.` O e-mail original não muda.
- Resultado obtido: o teste passou.
- Situação: passou.

### Foto preservada, substituída e rejeitada

- História: HU03.
- Procedimento: `atualizaDadosSemTrocarSenhaNemFoto`, `substituiAFoto` e `fotoInvalidaNaoApagaAAtual`.
- Resultado esperado: sem arquivo novo, os bytes da foto continuam iguais; um PNG válido substitui o JPEG; um arquivo que não é imagem responde 400 e a foto antiga permanece.
- Resultado obtido: os três testes passaram.
- Situação: passou.

### Novo login depois da troca de e-mail

- História: HU03.
- Procedimento: `AtualizacaoCiclistaTest.emailAlteradoEncerraASessaoEPedeNovoLogin`.
- Resultado esperado: a resposta traz `emailAlterado=true` e a mensagem `E-mail alterado. Entre novamente com o novo e-mail.` A sessão antiga passa a responder 401. O login com o novo e-mail e a senha antiga funciona. O e-mail antigo responde `Login ou senha inválidos`.
- Resultado obtido: o teste passou.
- Situação: passou.

### Atualização sem sessão e sem CSRF

- História: RT01.
- Procedimento: `AtualizacaoCiclistaTest.atualizacaoSemSessaoOuSemCsrfERecusada`.
- Resultado esperado: consulta sem sessão responde 401; alteração sem CSRF responde 403 `CSRF_INVALIDO`.
- Resultado obtido: o teste passou.
- Situação: passou.

### Cartão fictício e troca

- História: HU04.
- Procedimento: `CartaoDemonstracaoTest.cadastraETrocaOCartaoFicticio` e `trocaInvalidaMantemOCartaoAnterior`.
- Resultado esperado: os dois cartões de demonstração aparecem; `DEMO-APROVADO` e `DEMO-RECUSADO` podem ser cadastrados; a resposta distingue cadastro de aprovação da cobrança; uma referência desconhecida responde `CARTAO_DESCONHECIDO` e o cartão anterior continua.
- Resultado obtido: os testes passaram. A resposta do aprovado diz que a cobrança simulada será aprovada. A do recusado diz que será recusada. As duas avisam que nenhuma cobrança real é feita. Não há CVV na resposta.
- Situação: passou.

### Cartão sem sessão e sem CSRF

- História: RT01.
- Procedimento: `CartaoDemonstracaoTest.cartaoSemSessaoOuSemCsrfERecusado`.
- Resultado esperado: GET sem sessão responde 401; POST sem CSRF responde 403.
- Resultado obtido: o teste passou.
- Situação: passou.

### Dados iniciais

- História: totens, trancas e bicicletas.
- Procedimento: `AluguelTest.dadosIniciaisNaoSeDuplicamNemDevolvemBicicleta`.
- Resultado esperado: 2 totens, 40 trancas e 40 bicicletas, 20 trancas em cada totem, números e RFIDs únicos. Executar a carga de novo não duplica e não restaura uma bicicleta marcada como `EM_USO`.
- Resultado obtido: o teste passou.
- Situação: passou.

### Taxa antes do aluguel

- História: HU05.
- Procedimento: `AluguelTest.mostraATaxaAntesDoAluguel`.
- Resultado esperado: valor `10.00`, duração 2 horas, simulação identificada. A lista de totens não expõe RFID.
- Resultado obtido: o teste passou.
- Situação: passou.

### Aluguel sem cartão

- História: HU05.
- Procedimento: `AluguelTest.aluguelSemCartaoNaoCobra`.
- Resultado esperado: HTTP 409, código `SEM_CARTAO`, nenhuma cobrança e a bicicleta continua disponível.
- Resultado obtido: o teste passou.
- Situação: passou.

### Tranca inexistente e tranca sem bicicleta

- História: HU05.
- Procedimento: `AluguelTest.trancaInexistenteNaoCobra` e, no fim de `pagamentoAprovadoAbreATrancaEGeraUmaConfirmacao`, um segundo ciclista tenta a tranca já usada.
- Resultado esperado: tranca `ZZ-99` responde 404 `TRANCA_INEXISTENTE` sem cobrança. Tranca sem bicicleta responde 409 `TRANCA_SEM_BICICLETA` sem cobrança para o segundo ciclista.
- Resultado obtido: os testes passaram.
- Situação: passou.

### Pagamento aprovado e recusado

- História: HU05.
- Procedimento: `pagamentoAprovadoAbreATrancaEGeraUmaConfirmacao` e `pagamentoRecusadoNaoMoveABicicleta`.
- Resultado esperado: os dois resultados usam HTTP 200. No aprovado, a bicicleta fica `EM_USO`, sai da tranca, a cobrança e o aluguel guardam `10.00` e a data/hora. No recusado, a bicicleta continua disponível, não há aluguel nem mensagem, e a cobrança fica `RECUSADO`.
- Resultado obtido: os testes passaram. A diferença entre o instante gravado e o relógio do teste ficou abaixo de 120 segundos.
- Situação: passou.

### Segundo aluguel

- História: HU05.
- Procedimento: `AluguelTest.segundoAluguelDoMesmoCiclistaEBloqueado`.
- Resultado esperado: HTTP 409 `ALUGUEL_ATIVO`. Continua um aluguel ativo, uma cobrança, e a segunda tranca segue com bicicleta.
- Resultado obtido: o teste passou.
- Situação: passou.

### Repetição da confirmação

- História: HU05 e HU09.
- Procedimento: `AluguelTest.repetirAMesmaConfirmacaoNaoDuplicaCobrancaNemMensagem`.
- Resultado esperado: a segunda chamada com a mesma chave responde o mesmo `cobrancaId`, a mensagem `Esta confirmação já foi processada.` e continua havendo uma cobrança, um aluguel e uma mensagem.
- Resultado obtido: o teste passou.
- Situação: passou.

### Concorrência

- História: HU05.
- Procedimento: `ConcorrenciaAluguelTest.doisCiclistasNaoRetiramAMesmaBicicleta` e `oMesmoCiclistaNaoCriaDoisAlugueis`. Duas threads chamam o serviço ao mesmo tempo.
- Resultado esperado: uma aprovação e uma recusa de negócio (`TRANCA_SEM_BICICLETA` ou `CONFLITO` na disputa da bicicleta; `ALUGUEL_ATIVO` ou `CONFLITO` na disputa do ciclista). Fica um aluguel ativo para a bicicleta e um para o ciclista.
- Resultado obtido: os dois testes passaram.
- Situação: passou.

### Confirmação simulada e falha da notificação

- História: HU09.
- Procedimento: `pagamentoAprovadoAbreATrancaEGeraUmaConfirmacao` e `FalhaNotificacaoTest.falhaDaMensagemNaoDesfazOAluguelENaoRepeteAConfirmacao`.
- Resultado esperado: o texto contém `Envio de e-mail simulado — nenhuma mensagem externa foi enviada.`, a bicicleta, o totem, a tranca, o horário, a taxa e a duração. Outro ciclista não recebe essa mensagem. Se a gravação da mensagem lança exceção, o aluguel aprovado permanece e a repetição da mesma chave não grava outra mensagem.
- Resultado obtido: os testes passaram. A consulta do outro ciclista não continha o RFID alugado.
- Situação: passou.

### Aluguel sem sessão e sem CSRF

- História: RT01.
- Procedimento: `AluguelTest.aluguelSemSessaoOuSemCsrfERecusado`.
- Resultado esperado: totens, aluguel ativo e mensagens sem sessão respondem 401. Confirmar aluguel sem CSRF responde 403 `CSRF_INVALIDO`.
- Resultado obtido: o teste passou.
- Situação: passou.

## Chamadas HTTP na aplicação em execução

A aplicação foi iniciada com `.\mvnw.cmd spring-boot:run` e ficou em `http://localhost:8080`, usando `jdbc:h2:file:./dados/scb`. O processo anterior deste mesmo projeto, que ainda executava a Sprint 1, foi encerrado para liberar a compilação e a porta. Nenhum processo de fora do projeto foi encerrado.

Foram usadas contas fictícias `sprint2.manual.1794280877@exemplo.test` e `sprint2.outro.1794280939@exemplo.test`. Elas ficaram no banco de demonstração, junto com os ciclistas já existentes.

- `POST /api/ciclistas`: 201, sem senha, documento ou foto no corpo.
- `POST /api/login`: 200, também sem o número do documento.
- `GET /api/ciclista?id=999`: 200, devolveu o ciclista da sessão (id 33) e o próprio documento.
- `PUT /api/ciclista`: 200, `Cadastro atualizado.`, nome e documento novos, `emailAlterado=false`.
- `POST /api/cartao` com `DEMO-RECUSADO`: 200, máscara `**** **** **** 0002`, cobrança simulada recusada.
- `GET /api/aluguel/taxa`: 200, `valor` `10.00`, texto `R$ 10,00 por 2 horas`, simulação identificada.
- `GET /api/totens`: Totem Praça Central e Totem Campus Norte, capacidade 20, 20 trancas disponíveis em cada um antes do aluguel.
- Aluguel da tranca `PC-01` com o cartão recusado: 200 `RECUSADO`, `aluguel` nulo, valor `10.00`, data `2026-11-10T00:22:18`. A tranca continuou na lista.
- Troca para `DEMO-APROVADO` e novo aluguel de `PC-01`: 200 `APROVADO`, bicicleta `RFID-PC-01`, totem Totem Praça Central, início `2026-11-10T00:22:19`, duração 2.
- Repetição da mesma chave: 200, o mesmo `cobrancaId` 2 e a mensagem `Esta confirmação já foi processada.`
- `GET /api/aluguel`: aluguel ativo com a mesma bicicleta, horário e período.
- `GET /api/mensagens`: uma confirmação com a frase de envio simulado, bicicleta, totem, tranca, horário, taxa e duração. O outro ciclista recebeu uma lista vazia.
- Segundo aluguel do mesmo ciclista: 409 `ALUGUEL_ATIVO`.
- `POST /api/alugueis` sem CSRF: 403 `CSRF_INVALIDO`.
- `GET /api/totens` sem sessão: 401.
- Páginas `/editar.html`, `/cartao.html`, `/alugar.html`, `/aluguel.html`, `/confirmacoes.html`, `/inicio.html` e `GET /api/status`: 200.

O log `logs/scb.log` registrou `resultado=RECUSADO` e `aluguel iniciado id=1` com o e-mail da sessão. Não continha a senha, os documentos usados nesse teste, `JSESSIONID` nem a referência do cartão.

## Pendências

- Verificação visual no navegador: não executada. Falta conferir teclado, rótulos, foco, mensagens e o layout no celular.
- Envio real de e-mail: pendente. A confirmação só existe na caixa de saída do H2.
- Aplicativo móvel: não concluído. As telas são páginas web.
- Devolução, taxas extras e cobranças pendentes: fora do escopo desta sprint e não implementadas.
