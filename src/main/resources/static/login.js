const formulario = document.getElementById("formulario-login");
const mensagem = document.getElementById("mensagem-formulario");
const botao = document.getElementById("botao-entrar");
const campos = ["email", "senha"];

const params = new URLSearchParams(window.location.search);
if (params.get("cadastro") === "ok") {
	mostrarAviso(mensagem, "Cadastro realizado. Entre com seu e-mail e senha.", "ok");
	history.replaceState(null, "", "index.html");
}
if (params.get("email") === "alterado") {
	mostrarAviso(mensagem, "E-mail alterado. Entre novamente com o novo e-mail.", "ok");
	history.replaceState(null, "", "index.html");
}

prepararCsrf().catch(function () {
	mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
});

fetch("/api/ciclista", { credentials: "same-origin", cache: "no-store" })
	.then(function (resposta) {
		if (resposta.ok) {
			window.location.replace("inicio.html");
		}
	})
	.catch(function () {
	});

formulario.addEventListener("submit", function (evento) {
	evento.preventDefault();
	limparErros(campos);
	mostrarAviso(mensagem, "", "");

	const email = document.getElementById("email").value.trim();
	const senha = document.getElementById("senha").value;
	let valido = true;
	if (!email) {
		definirErro("email", "Informe o e-mail.");
		valido = false;
	}
	else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
		definirErro("email", "Informe um e-mail válido.");
		valido = false;
	}
	if (!senha) {
		definirErro("senha", "Informe a senha.");
		valido = false;
	}
	if (!valido) {
		return;
	}

	const textoOriginal = botao.textContent;
	botao.disabled = true;
	botao.textContent = "Entrando...";

	fetch("/api/login", {
		method: "POST",
		credentials: "same-origin",
		headers: cabecalhos({ "Content-Type": "application/json" }),
		body: JSON.stringify({ email: email, senha: senha })
	})
		.then(function (resposta) {
			return lerCorpo(resposta).then(function (dados) {
				return { resposta: resposta, dados: dados };
			});
		})
		.then(function (resultado) {
			if (resultado.resposta.ok) {
				window.location.href = "inicio.html";
				return;
			}
			aplicarErrosDoServidor(resultado.dados);
			mostrarAviso(mensagem, resultado.dados.mensagem || "Não foi possível entrar.", "erro");
			document.getElementById("senha").value = "";
		})
		.catch(function () {
			mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
		})
		.finally(function () {
			botao.disabled = false;
			botao.textContent = textoOriginal;
		});
});
