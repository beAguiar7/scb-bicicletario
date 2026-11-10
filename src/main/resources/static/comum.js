// Funcoes usadas pelas telas. O token CSRF fica so nesta pagina, nao no localStorage.

let csrf = null;

async function prepararCsrf() {
	const resposta = await fetch("/api/csrf", { credentials: "same-origin", cache: "no-store" });
	if (!resposta.ok) {
		throw new Error("csrf");
	}
	csrf = await resposta.json();
}

function cabecalhos(extras) {
	const headers = extras || {};
	if (csrf && csrf.token && csrf.headerName) {
		headers[csrf.headerName] = csrf.token;
	}
	return headers;
}

async function lerCorpo(resposta) {
	const texto = await resposta.text();
	if (!texto) {
		return {};
	}
	try {
		return JSON.parse(texto);
	}
	catch (erro) {
		return { mensagem: "O servidor retornou uma resposta inesperada." };
	}
}

function definirErro(campo, texto) {
	const aviso = document.getElementById("erro-" + campo);
	const campoTela = document.getElementById(campo);
	if (aviso) {
		aviso.textContent = texto || "";
	}
	if (campoTela) {
		if (texto) {
			campoTela.setAttribute("aria-invalid", "true");
		}
		else {
			campoTela.removeAttribute("aria-invalid");
		}
	}
}

function limparErros(campos) {
	campos.forEach(function (campo) {
		definirErro(campo, "");
	});
}

function aplicarErrosDoServidor(dados) {
	if (!dados || !Array.isArray(dados.erros)) {
		return;
	}
	dados.erros.forEach(function (erro) {
		const aviso = document.getElementById("erro-" + erro.campo);
		if (aviso && aviso.textContent) {
			return;
		}
		definirErro(erro.campo, erro.mensagem);
	});
}

function mostrarAviso(elemento, texto, tipo) {
	elemento.textContent = texto || "";
	elemento.className = tipo ? "aviso " + tipo : "aviso";
}

function adicionarLinha(lista, rotulo, valor) {
	if (valor === undefined || valor === null || valor === "") {
		return;
	}
	const termo = document.createElement("dt");
	termo.textContent = rotulo;
	const descricao = document.createElement("dd");
	descricao.textContent = String(valor);
	lista.append(termo, descricao);
}

function mensagemIndisponivel() {
	return "Não foi possível falar com o servidor. Verifique se a aplicação está em execução e tente novamente.";
}

function exigirSessao() {
	return fetch("/api/ciclista", { credentials: "same-origin", cache: "no-store" }).then(function (resposta) {
		if (resposta.status === 401) {
			window.location.replace("index.html");
			return null;
		}
		if (!resposta.ok) {
			throw new Error("sessao");
		}
		return resposta.json();
	});
}

function prepararSaida() {
	const botao = document.getElementById("botao-sair");
	if (!botao || botao.dataset.pronto === "sim") {
		return;
	}
	botao.dataset.pronto = "sim";
	botao.addEventListener("click", function () {
		botao.disabled = true;
		const sair = function () {
			fetch("/api/logout", {
				method: "POST",
				credentials: "same-origin",
				headers: cabecalhos()
			})
				.then(function (resposta) {
					if (resposta.ok || resposta.status === 401) {
						window.location.replace("index.html");
						return;
					}
					const aviso = document.getElementById("mensagem-formulario");
					if (aviso) {
						mostrarAviso(aviso, "Não foi possível sair. Tente novamente.", "erro");
					}
					botao.disabled = false;
				})
				.catch(function () {
					const aviso = document.getElementById("mensagem-formulario");
					if (aviso) {
						mostrarAviso(aviso, mensagemIndisponivel(), "erro");
					}
					botao.disabled = false;
				});
		};
		if (csrf) {
			sair();
			return;
		}
		prepararCsrf().then(sair).catch(function () {
			const aviso = document.getElementById("mensagem-formulario");
			if (aviso) {
				mostrarAviso(aviso, mensagemIndisponivel(), "erro");
			}
			botao.disabled = false;
		});
	});
}

prepararSaida();
