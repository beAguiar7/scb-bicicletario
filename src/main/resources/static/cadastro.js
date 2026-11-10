const formulario = document.getElementById("formulario-cadastro");
const mensagem = document.getElementById("mensagem-formulario");
const botao = document.getElementById("botao-cadastrar");
const campos = ["nome", "nacionalidade", "numeroDocumento", "foto", "email", "senha", "confirmacaoSenha"];
const LIMITE_FOTO = 5 * 1024 * 1024;

prepararCsrf().catch(function () {
	mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
});

formulario.addEventListener("submit", function (evento) {
	evento.preventDefault();
	limparErros(campos);
	mostrarAviso(mensagem, "", "");

	const nome = document.getElementById("nome").value.trim();
	const nacionalidade = document.getElementById("nacionalidade").value;
	const documento = document.getElementById("numeroDocumento").value.trim();
	const foto = document.getElementById("foto").files[0];
	const email = document.getElementById("email").value.trim();
	const senha = document.getElementById("senha").value;
	const confirmacao = document.getElementById("confirmacaoSenha").value;
	let valido = true;

	if (nome.length < 3 || nome.length > 120) {
		definirErro("nome", "O nome deve ter entre 3 e 120 caracteres.");
		valido = false;
	}
	if (!nacionalidade) {
		definirErro("nacionalidade", "Informe a nacionalidade.");
		valido = false;
	}
	if (!/^[A-Za-z0-9]{5,30}$/.test(documento)) {
		definirErro("numeroDocumento", "O documento deve ter de 5 a 30 letras ou números.");
		valido = false;
	}
	if (!foto) {
		definirErro("foto", "Envie a foto do documento.");
		valido = false;
	}
	else if (foto.type && foto.type !== "image/jpeg" && foto.type !== "image/png") {
		definirErro("foto", "A foto deve ser uma imagem JPEG ou PNG válida.");
		valido = false;
	}
	else if (foto.size > LIMITE_FOTO) {
		definirErro("foto", "A foto deve ter no máximo 5 MB.");
		valido = false;
	}
	if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
		definirErro("email", "Informe um e-mail válido.");
		valido = false;
	}
	if (senha.length < 8 || senha.length > 72) {
		definirErro("senha", "A senha deve ter entre 8 e 72 caracteres.");
		valido = false;
	}
	if (senha !== confirmacao) {
		definirErro("confirmacaoSenha", "A confirmação deve ser igual à senha.");
		valido = false;
	}
	if (!valido) {
		return;
	}

	const dados = new FormData(formulario);
	dados.set("nome", nome);
	dados.set("numeroDocumento", documento);
	dados.set("email", email);

	const textoOriginal = botao.textContent;
	botao.disabled = true;
	botao.textContent = "Enviando...";

	fetch("/api/ciclistas", {
		method: "POST",
		credentials: "same-origin",
		headers: cabecalhos(),
		body: dados
	})
		.then(function (resposta) {
			return lerCorpo(resposta).then(function (corpo) {
				return { resposta: resposta, dados: corpo };
			});
		})
		.then(function (resultado) {
			if (resultado.resposta.status === 201) {
				window.location.href = "index.html?cadastro=ok";
				return;
			}
			aplicarErrosDoServidor(resultado.dados);
			mostrarAviso(mensagem, resultado.dados.mensagem || "Não foi possível concluir o cadastro.", "erro");
			document.getElementById("senha").value = "";
			document.getElementById("confirmacaoSenha").value = "";
			document.getElementById("foto").value = "";
		})
		.catch(function () {
			mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
		})
		.finally(function () {
			botao.disabled = false;
			botao.textContent = textoOriginal;
		});
});
