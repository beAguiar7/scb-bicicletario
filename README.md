# SCB — Sistema de Controle de Bicicletário

Trabalho acadêmico do quarto período de Ciência da Computação. O nome da aplicação é **VáDeBicicleta**.

A Sprint 1 cobre cadastro, login, sessão, BCrypt, CSRF e o registro de falhas. A Sprint 2 acrescenta a alteração do próprio cadastro, o cartão de demonstração, o aluguel pelo número da tranca e a confirmação simulada de empréstimo. Devolução, taxas extras e cobrança pendente não fazem parte desta etapa.

O envio real de e-mail e o aplicativo móvel não estão concluídos. A verificação visual no navegador também continua pendente.

A execução local continua no H2 em arquivo. O perfil `prod` está preparado para PostgreSQL e para um contêiner na Vercel, mas a publicação não foi feita.

## Tecnologias e versões

| Tecnologia | Versão |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.2 (versão estável padrão do Spring Initializr) |
| Maven | 3.10.0, obtido pelo Maven Wrapper 3.3.4 |
| Spring Web MVC | `spring-boot-starter-webmvc` |
| Spring Data JPA | `spring-boot-starter-data-jpa` |
| Bean Validation | `spring-boot-starter-validation` |
| Spring Security | 7.1.2, pelo `spring-boot-starter-security` (sessão HTTP, sem JWT) |
| Banco local | H2 2.4.240, arquivo em `dados/` |
| Banco de produção | PostgreSQL, driver 42.7.14 gerenciado pelo Spring Boot, somente no perfil `prod` |
| Sessão de produção | Spring Session JDBC 4.1.1 no PostgreSQL, somente no perfil `prod` |
| Testes | JUnit 6.0.3 (Jupiter) |
| Frontend | HTML, CSS e JavaScript, sem frameworks |

O pacote principal é `br.com.vadebicicleta.scb`. O empacotamento é `jar`. As versões não foram alteradas na Sprint 2.

## Regras de cadastro e sessão

- A senha tem no mínimo 8 e no máximo 72 caracteres. O mínimo de 8 é a regra do grupo. Ela é gravada com BCrypt. Esta etapa não troca a senha.
- O e-mail é o login. Espaços nas pontas são removidos e as letras são gravadas em minúsculas. O e-mail é único na tabela `ciclista`.
- A nacionalidade é `BRASILEIRO` ou `ESTRANGEIRO`. Não há validação de CPF. O documento aceita de 5 a 30 letras ou números, sem pontuação.
- A foto do documento é obrigatória no cadastro, somente JPEG ou PNG, com no máximo 5 MB. Na alteração, uma nova foto substitui a atual; se nenhuma foto for enviada, a anterior permanece.
- Cadastro e login respondem `id`, `nome`, `nacionalidade` e `email`. A consulta do ciclista autenticado também traz o próprio `numeroDocumento`, para preencher a edição. Senha, hash e foto não saem nas respostas. Um id enviado pelo navegador é ignorado.
- Se o e-mail de login mudar, a sessão é encerrada e a mensagem pede um novo login: `E-mail alterado. Entre novamente com o novo e-mail.`
- O cookie `JSESSIONID` é HttpOnly. O identificador da sessão muda depois do login.
- O CSRF permanece ligado. As telas pedem o token em `GET /api/csrf` e o enviam no cabeçalho. Senha e identificador de sessão não vão para o `localStorage`.
- Páginas, arquivos estáticos, `GET /api/status`, `GET /api/csrf`, cadastro e login são públicos. As demais rotas da API exigem sessão.
- Credencial incorreta responde `Login ou senha inválidos`. API sem sessão responde 401. Falha inesperada responde uma mensagem genérica, sem detalhe interno.
- As falhas vão para `logs/scb.log` com data, usuário quando a sessão existe, código, descrição e o endereço da requisição. Senha, documento, corpo do cadastro, cookies e dados de cartão não são gravados.

## Cartão de demonstração

Não há cobrança real nem integração externa. A tela não pede cartão verdadeiro nem CVV. O sistema só guarda a referência fictícia, a identificação mascarada, o titular e se a simulação de cobrança deve ser aprovada.

| Referência | Máscara | Titular | Cobrança simulada |
| --- | --- | --- | --- |
| `DEMO-APROVADO` | `**** **** **** 1111` | Demonstração Aprovado | aprovada |
| `DEMO-RECUSADO` | `**** **** **** 0002` | Demonstração Recusado | recusada |

Cadastrar qualquer um dos dois dá certo. A recusa acontece só na hora do aluguel. Se a troca do cartão falhar, o cartão anterior permanece.

## Totens, trancas e bicicletas

Na primeira execução, se ainda não existir totem, o sistema cria:

- **Totem Praça Central**, capacidade 20, trancas `PC-01` a `PC-20`, bicicletas `RFID-PC-01` a `RFID-PC-20`.
- **Totem Campus Norte**, capacidade 20, trancas `CN-01` a `CN-20`, bicicletas `RFID-CN-01` a `RFID-CN-20`.

O número da tranca é único. O RFID é fictício e fica reservado para uso futuro. Reiniciar a aplicação não duplica esses dados e não devolve uma bicicleta que já foi alugada. Não há tela administrativa.

## Aluguel e taxa

A taxa básica desta etapa é **R$ 10,00 por 2 horas**. O PDF do trabalho não definiu esse valor. Ele está em `application.properties`:

```properties
scb.aluguel.valor=10.00
scb.aluguel.duracao-horas=2
```

O cálculo usa `BigDecimal` no servidor. A tela mostra a taxa antes da confirmação. O aluguel exige sessão e um cartão de demonstração já cadastrado. O ciclista informa o número da tranca. Só depois do pagamento simulado aprovado a bicicleta passa a `EM_USO`, a tranca fica sem bicicleta e o aluguel ativo é gravado. Na recusa, bicicleta e tranca não mudam, e a cobrança fica registrada como `RECUSADO`.

Cada ciclista pode ter apenas um aluguel ativo. Duas confirmações ao mesmo tempo não retiram a mesma bicicleta nem criam dois aluguéis para o mesmo ciclista. Repetir a mesma confirmação, identificada pela chave enviada pela tela, não gera outra cobrança, outro aluguel nem outra mensagem. Não há devolução nesta sprint.

## Confirmação simulada

Depois do aluguel aprovado, o texto da confirmação é gravado na tabela `mensagem_simulada`. No uso local essa tabela fica no H2; no perfil `prod`, no PostgreSQL. Ele inclui o identificador do aluguel, a bicicleta, o totem, a tranca, o horário, a taxa e a duração inicial, além da frase:

`Envio de e-mail simulado — nenhuma mensagem externa foi enviada.`

O ciclista vê somente as próprias mensagens. Repetir a operação não cria outra confirmação. Se a gravação da mensagem falhar, o aluguel aprovado permanece. O envio real de e-mail continua pendente.

## Estrutura básica

```text
scb-bicicletario/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── Dockerfile.vercel              # imagem do perfil prod; nao substitui o uso local
├── .dockerignore
├── docs/testes-sprint-1.md
├── docs/testes-sprint-2.md
├── dados/                         # banco de arquivo; fora do Git
├── logs/                          # log da aplicacao local; fora do Git
└── src/
    ├── main/java/br/com/vadebicicleta/scb/
    │   ├── ciclista/
    │   ├── cartao/
    │   ├── aluguel/
    │   ├── seguranca/
    │   └── erro/
    ├── main/resources/
    │   ├── application.properties
    │   ├── application-prod.properties
    │   └── static/                # login, cadastro, inicio, edicao, cartao e aluguel
    └── test/
        ├── resources/application.properties
        └── java/br/com/vadebicicleta/scb/
```

## Como executar no PowerShell

Na pasta do projeto:

```powershell
.\mvnw.cmd spring-boot:run
```

Se aparecer `PKIX path building failed`, o antivírus está inspecionando o HTTPS. Nesta máquina, use antes:

```powershell
$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
.\mvnw.cmd spring-boot:run
```

Para encerrar, use `Ctrl+C`.

## Como rodar os testes

```powershell
.\mvnw.cmd test
```

Os testes usam H2 em memória (`jdbc:h2:mem:scbteste`) e não abrem `dados/scb`. O log dos testes vai para `target/logs-teste/`.

## Endereços

Com a aplicação em execução, na porta 8080:

- Entrar: [http://localhost:8080](http://localhost:8080)
- Cadastro: [http://localhost:8080/cadastro.html](http://localhost:8080/cadastro.html)
- Início: [http://localhost:8080/inicio.html](http://localhost:8080/inicio.html)
- Meu cadastro: [http://localhost:8080/editar.html](http://localhost:8080/editar.html)
- Cartão de demonstração: [http://localhost:8080/cartao.html](http://localhost:8080/cartao.html)
- Alugar bicicleta: [http://localhost:8080/alugar.html](http://localhost:8080/alugar.html)
- Aluguel ativo: [http://localhost:8080/aluguel.html](http://localhost:8080/aluguel.html)
- Confirmações: [http://localhost:8080/confirmacoes.html](http://localhost:8080/confirmacoes.html)
- Status: [http://localhost:8080/api/status](http://localhost:8080/api/status)

## Como testar manualmente

1. Cadastre um ciclista com foto JPEG ou PNG de até 5 MB e entre com o e-mail e a senha.
2. No início, abra Meu cadastro. Os dados atuais aparecem. Altere o nome e salve. Cancelar volta ao início sem enviar o formulário.
3. Abra Cartão de demonstração, escolha o cartão recusado e salve. A tela avisa que a cobrança simulada será recusada.
4. Abra Alugar bicicleta. A taxa de R$ 10,00 por 2 horas aparece antes do botão. Escolha uma tranca e confirme. O resultado deve ser recusa, e a tranca continua na lista.
5. Troque para o cartão aprovado, alugue outra vez a mesma tranca e confira o aluguel ativo e a confirmação simulada.
6. Tente um segundo aluguel. O sistema deve informar que já existe um aluguel em andamento.
7. Use Sair e entre de novo. Se o e-mail tiver sido alterado, a entrada pede o novo login.

## Onde os dados ficam

O uso local grava `dados/scb.mv.db`. A pasta é criada ao iniciar e os dados permanecem depois de encerrar. Apagar `dados/` zera o banco local, inclusive ciclistas já cadastrados e a ocupação das trancas. Esse arquivo não é enviado para a nuvem. Os cadastros feitos aqui não aparecem automaticamente no PostgreSQL.

O console web do H2 está desabilitado. A dependência `spring-boot-h2console` não faz parte do projeto e `/h2-console` não é público.

O log fica em `logs/scb.log`. Essa pasta está no `.gitignore`.

## Perfil de produção

O perfil `prod` usa PostgreSQL externo. As credenciais não ficam nos arquivos. Elas entram por variáveis de ambiente:

| Variável | Conteúdo |
| --- | --- |
| `DB_URL` | URL JDBC do PostgreSQL, com SSL. Exemplo de formato: `jdbc:postgresql://HOST:5432/scb?sslmode=require` |
| `DB_USERNAME` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `PORT` | Na Vercel, definir `8080`. A plataforma usa a porta 80 se essa variável não existir |
| `SPRING_PROFILES_ACTIVE` | `prod`. O `Dockerfile.vercel` já define este valor |

A foto do documento é gravada como binário (`bytea` no PostgreSQL e `varbinary` no H2). A sessão continua sendo sessão do Spring Security, com CSRF e troca do identificador depois do login. Não há JWT. No perfil `prod`, a sessão fica nas tabelas `SPRING_SESSION` e `SPRING_SESSION_ATTRIBUTES`, criadas pelo script oficial `schema-postgresql.sql` do Spring Session. O cookie `JSESSIONID` permanece HttpOnly e `SameSite=Lax`; em produção ele também é `Secure`.

O log local continua em `logs/scb.log`. No perfil `prod` o log vai somente para o console, com data, usuário quando a sessão existe, nível, código e descrição. Senha, documento, cookie e dados de cartão não entram na mensagem.

Cada instância abre o próprio pool de conexões, limitado a 5. O contêiner da Vercel pode desligar depois de um período sem tráfego; por isso a sessão fica no PostgreSQL e não na memória do processo.

## Como preparar o PostgreSQL

1. Crie um banco vazio e um usuário com permissão para criar tabelas. Não importe `dados/scb.mv.db` e não apague a pasta `dados/`.
2. Guarde a URL JDBC com `sslmode=require`, o usuário e a senha nas variáveis de ambiente. Não os escreva em `application.properties`.
3. Na primeira subida do perfil `prod`, o Hibernate atualiza as tabelas da aplicação e o Spring Session executa o script oficial das tabelas de sessão. Isso cria o esquema vazio. Não copia ciclistas, fotos, cartões nem aluguéis locais.
4. Se duas instâncias iniciarem juntas, o nome único do totem impede uma segunda cópia dos totens iniciais.

## Publicação na Vercel

Ainda não foi publicada. O procedimento previsto, com o preset Container da documentação em `https://vercel.com/docs/functions/runtimes/container`, é:

1. Importe o repositório na Vercel e escolha o preset **Container**. O arquivo `Dockerfile.vercel` seleciona esse preset.
2. Configure `PORT=8080`, `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`. O perfil `prod` já é ativado pela imagem.
3. A construção da imagem usa Java 21, executa os testes e gera o jar. Banco local, logs, `.git` e segredos ficam de fora pelo `.dockerignore`.
4. A mesma aplicação serve as páginas e a API no mesmo domínio. As telas continuam com URLs relativas, sessão e CSRF.
5. Os cadastros feitos no H2 local não aparecem nessa publicação. É preciso cadastrar de novo no ambiente online.

Nesta máquina não há Docker, então a imagem não foi construída. O cliente `psql` existe, mas não havia uma conexão utilizável sem credenciais. PostgreSQL e a sessão JDBC ainda não foram exercitados em um servidor real.

## Limites desta etapa

- Não há devolução, taxa extra nem cobrança pendente.
- O cartão e o pagamento são simulação local. Nenhuma cobrança externa é feita.
- A confirmação fica só na caixa de saída do banco da aplicação. O envio real de e-mail não está concluído.
- As telas são um protótipo web. O requisito de aplicativo móvel não está concluído.
- A interface ainda não foi exercitada em um navegador. A verificação visual está pendente.
- A publicação na Vercel não foi executada. A imagem Docker não foi construída e o PostgreSQL com sessão JDBC não foi testado em um servidor.
- Os cadastros do H2 local não são migrados para o PostgreSQL.
