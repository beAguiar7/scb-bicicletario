# Testes da Sprint 1

Registro do que foi executado em 9 de novembro de 2026. Nada que não tenha sido executado está marcado como aprovado.

A verificação visual no navegador não foi feita: não havia ferramenta de navegador nesta sessão. Por isso a Sprint 1 não está integralmente concluída.

## Testes automatizados

Comando: `.\mvnw.cmd test`

Resultado geral: **BUILD SUCCESS**. 20 testes, 0 falhas, 0 erros. O log do teste mostrou `jdbc:h2:mem:scbteste`, sem usar `dados/scb`.

### Contexto e banco em memória

- História ou requisito: base da aplicação e isolamento do banco de arquivo.
- Procedimento: `ScbApplicationTests.testesUsamBancoEmMemoria` e `contextLoads`.
- Resultado esperado: a URL do banco começa com `jdbc:h2:mem:` e não contém `dados/scb`.
- Resultado obtido: os dois testes passaram. O log registrou `jdbc:h2:mem:scbteste`.
- Situação: passou.

### Status público

- História ou requisito: `GET /api/status` continua público.
- Procedimento: `ScbApplicationTests.statusDeveIndicarQueOScbEstaFuncionando`.
- Resultado esperado: HTTP 200, sistema `SCB` e mensagem de funcionamento.
- Resultado obtido: o teste passou.
- Situação: passou.

### Páginas públicas

- História ou requisito: páginas de entrada, cadastro e início podem ser abertas sem sessão.
- Procedimento: `ScbApplicationTests.paginasPublicasContinuamAcessiveis`.
- Resultado esperado: HTTP 200 em `/`, `/index.html`, `/cadastro.html` e `/inicio.html`, com os textos das telas.
- Resultado obtido: o teste passou.
- Situação: passou.

### Console H2

- História ou requisito: o console web do H2 permanece desabilitado.
- Procedimento: `ScbApplicationTests.consoleDoH2NaoFicaPublico`.
- Resultado esperado: `GET /h2-console` sem sessão não retorna a console.
- Resultado obtido: HTTP 401.
- Situação: passou.

### Cadastro de brasileiro e de estrangeiro

- História ou requisito: HU01.
- Procedimento: `CadastroCiclistaTest.cadastraBrasileiroComJpegENaoDevolveSegredos` e `cadastraEstrangeiroComDocumentoAlfanumerico`.
- Resultado esperado: HTTP 201. Brasileiro com JPEG e documento numérico. Estrangeiro com PNG e documento com letras. A resposta não traz senha, hash, foto nem número do documento.
- Resultado obtido: os dois testes passaram. O hash gravado era diferente da senha e o BCrypt confirmou a senha fictícia `segredo12`.
- Situação: passou.

### E-mail normalizado e duplicado

- História ou requisito: HU01, e-mail único.
- Procedimento: `CadastroCiclistaTest.normalizaEmailERecusaDuplicata`.
- Resultado esperado: espaços e maiúsculas são normalizados. O segundo cadastro do mesmo e-mail responde 409.
- Resultado obtido: o teste passou, com a mensagem `Este e-mail já está cadastrado.`
- Situação: passou.

### Campos obrigatórios, e-mail inválido e senha curta

- História ou requisito: HU01 e regra do grupo de senha mínima de 8 caracteres.
- Procedimento: `CadastroCiclistaTest.recusaCamposObrigatoriosEmailInvalidoESenhaCurta` e `recusaNacionalidadeDesconhecida`.
- Resultado esperado: HTTP 400, código `DADOS_INVALIDOS`, sem detalhe interno.
- Resultado obtido: os testes passaram. A senha `1234567` foi recusada. Nacionalidade `MARCIANO` foi recusada.
- Situação: passou.

### Confirmação de senha

- História ou requisito: HU01.
- Procedimento: `CadastroCiclistaTest.recusaConfirmacaoDiferenteDaSenha`.
- Resultado esperado: HTTP 400 no campo `confirmacaoSenha`.
- Resultado obtido: o teste passou.
- Situação: passou.

### Foto ausente, inválida e acima de 5 MB

- História ou requisito: HU01.
- Procedimento: `CadastroCiclistaTest.recusaFotoAusenteInvalidaEAcimaDoLimite`.
- Resultado esperado: foto ausente e conteúdo que não é JPEG/PNG respondem 400. Arquivo maior que 5 MB responde 413.
- Resultado obtido: o teste passou. A imagem usada foi gerada no teste, com 8 por 8 pixels, sem ser um documento real.
- Situação: passou.

### CSRF no cadastro e no login

- História ou requisito: RT01.
- Procedimento: `CadastroCiclistaTest.cadastroSemCsrfERecusado` e `AutenticacaoCiclistaTest.loginSemCsrfERecusado`.
- Resultado esperado: HTTP 403, código `CSRF_INVALIDO`, sem gravar o ciclista e sem devolver a senha.
- Resultado obtido: os dois testes passaram.
- Situação: passou.

### Login correto, sessão e consulta

- História ou requisito: HU02 e RT01.
- Procedimento: `AutenticacaoCiclistaTest.loginCorretoRenovaSessaoEConsultaSomenteOUsuarioAutenticado`.
- Resultado esperado: login com o e-mail em maiúsculas responde 200, o identificador da sessão muda, e `GET /api/ciclista?id=999` devolve só o ciclista da sessão.
- Resultado obtido: o teste passou. A resposta não continha a senha nem o outro ciclista.
- Situação: passou.

### Login incorreto e campo vazio

- História ou requisito: HU02.
- Procedimento: `AutenticacaoCiclistaTest.loginIncorretoUsaAMesmaMensagem` e `loginComCampoVazioNaoUsaAMensagemDeCredencial`.
- Resultado esperado: senha errada e e-mail inexistente respondem 401 com `Login ou senha inválidos`. Campo vazio responde 400 e não usa essa frase.
- Resultado obtido: os dois testes passaram.
- Situação: passou.

### Consulta sem autenticação

- História ou requisito: RT01.
- Procedimento: `AutenticacaoCiclistaTest.consultaSemAutenticacaoRetorna401`.
- Resultado esperado: HTTP 401, código `NAO_AUTENTICADO`, sem nome de exceção.
- Resultado obtido: o teste passou.
- Situação: passou.

### Logout

- História ou requisito: HU02 e RT01.
- Procedimento: `AutenticacaoCiclistaTest.logoutInvalidaASessao`.
- Resultado esperado: `POST /api/logout` responde 204 e a consulta seguinte responde 401.
- Resultado obtido: o teste passou.
- Situação: passou.

### Falha inesperada

- História ou requisito: RT02.
- Procedimento: `FalhaInesperadaTest.falhaInesperadaNaoExpoeDetalheInterno`.
- Resultado esperado: HTTP 500, mensagem genérica, sem a exceção nem o texto interno do teste.
- Resultado obtido: o teste passou. A mensagem foi `Não foi possível concluir a operação. Tente novamente.`
- Situação: passou.

## Verificação dos endpoints em execução

A aplicação foi iniciada com `.\mvnw.cmd spring-boot:run` e ficou na porta 8080, usando `jdbc:h2:file:./dados/scb`. As chamadas usaram `curl`, com ciclistas fictícios. Isso não substitui o uso das telas.

### Cookie de sessão e token CSRF

- História ou requisito: RT01.
- Procedimento: `GET /api/csrf` e, depois, login.
- Resultado esperado: cookie `JSESSIONID` HttpOnly e troca do identificador após o login.
- Resultado obtido: o primeiro cookie foi `Set-Cookie: JSESSIONID=...; Path=/; HttpOnly; SameSite=Lax`. Depois do login o identificador mudou e o novo cookie também veio com `HttpOnly`. O logout respondeu 204 e enviou `Set-Cookie: JSESSIONID=;` com data de expiração no passado.
- Situação: passou.

### Cadastro, duplicidade e fotos pela API

- História ou requisito: HU01.
- Procedimento: cadastro de Ana Souza (brasileira, JPEG) e Maria Lopez (estrangeira, PNG); repetição do e-mail de Ana em maiúsculas; envio de texto no lugar da foto; envio de arquivo com 6 MB.
- Resultado esperado: 201, 201, 409, 400 e 413, sem senha na resposta.
- Resultado obtido: os códigos foram esses. As respostas 201 trouxeram id, nome, nacionalidade e e-mail. O arquivo grande respondeu `A foto deve ter no máximo 5 MB.`
- Situação: passou.

### Login, consulta, senha errada e saída pela API

- História ou requisito: HU02 e RT01.
- Procedimento: login da Ana com senha errada; login com o e-mail em maiúsculas; `GET /api/ciclista?id=999`; logout; nova consulta; login sem CSRF.
- Resultado esperado: 401, 200, 200 com os dados da Ana, 204, 401 e 403.
- Resultado obtido: os códigos e as mensagens `Login ou senha inválidos`, `Faça login para continuar.` e `CSRF_INVALIDO` foram os recebidos. A consulta com `id=999` continuou devolvendo a Ana.
- Situação: passou.

### Páginas e console em execução

- História ou requisito: acesso público e console desligado.
- Procedimento: `GET /`, `/cadastro.html`, `/inicio.html`, `/api/status` e `/h2-console`.
- Resultado esperado: 200 nas páginas e no status; console sem acesso público.
- Resultado obtido: os quatro primeiros responderam 200. `/h2-console` respondeu 401.
- Situação: passou.

### Log sem segredo

- História ou requisito: RT02.
- Procedimento: busca em `logs/scb.log` e na saída da aplicação por `segredo12`, pelo número do documento e por `JSESSIONID`.
- Resultado esperado: esses valores não aparecem. Falha de um usuário autenticado registra o e-mail.
- Resultado obtido: a busca não encontrou senha, documento nem cookie. Uma validação feita já com sessão registrou `usuario=ana.manual.1794279165@exemplo.test`, o código `DADOS_INVALIDOS` e os nomes dos campos, sem os valores.
- Situação: passou.

## Testes manuais pendentes

Estes itens não foram executados. A situação de todos é **não executado**.

### Cadastro pela tela

- História ou requisito: HU01.
- Procedimento: preencher o formulário no navegador, inclusive a foto, e enviar.
- Resultado esperado: mensagens junto aos campos quando algo falta; campos não sensíveis preservados se o servidor recusar; depois do sucesso, ida para a entrada com a confirmação.
- Resultado obtido: não executado.
- Situação: não executado.

### Login pela tela

- História ou requisito: HU02.
- Procedimento: entrar com campo vazio, com senha errada e com a conta criada.
- Resultado esperado: avisos de campo vazio, `Login ou senha inválidos` e ida para a tela inicial.
- Resultado obtido: não executado.
- Situação: não executado.

### Tela inicial e saída

- História ou requisito: HU02.
- Procedimento: abrir a tela inicial sem sessão, depois autenticado, e usar Sair.
- Resultado esperado: sem sessão volta à entrada; com sessão mostra o nome em texto; Sair invalida a sessão.
- Resultado obtido: não executado.
- Situação: não executado.

### Acessibilidade, celular e indisponibilidade

- História ou requisito: interface do protótipo web.
- Procedimento: navegar por teclado, reduzir a janela para largura de celular e desligar a aplicação durante o envio.
- Resultado esperado: rótulos ligados aos campos, layout utilizável e mensagem de que o servidor não respondeu.
- Resultado obtido: não executado.
- Situação: não executado.
