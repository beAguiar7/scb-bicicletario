const mensagem = document.getElementById("mensagem-formulario");
const lista = document.getElementById("lista");
const vazio = document.getElementById("vazio");

function montarConfirmacao(item) {
	const artigo = document.createElement("article");
	artigo.className = "resultado confirmacao";
	const titulo = document.createElement("h2");
	titulo.textContent = item.assunto || "Confirmação";
	artigo.appendChild(titulo);
	const dados = document.createElement("dl");
	String(item.texto || "").split("\n").forEach(function (linha) {
		const limpa = linha.trim();
		if (!limpa) {
			return;
		}
		const separador = limpa.indexOf(": ");
		if (separador > 0) {
			adicionarLinha(dados, limpa.slice(0, separador), limpa.slice(separador + 2));
			return;
		}
		const paragrafo = document.createElement("p");
		paragrafo.textContent = limpa;
		artigo.appendChild(paragrafo);
	});
	if (dados.childNodes.length > 0) {
		artigo.appendChild(dados);
	}
	if (item.criadaEm) {
		const quando = document.createElement("p");
		quando.className = "texto-suave";
		quando.textContent = "Registrada em " + String(item.criadaEm).replace("T", " ");
		artigo.appendChild(quando);
	}
	return artigo;
}

exigirSessao()
	.then(function (perfil) {
		if (!perfil) {
			return null;
		}
		return fetch("/api/mensagens", { credentials: "same-origin", cache: "no-store" }).then(function (resposta) {
			if (resposta.status === 401) {
				window.location.replace("index.html");
				return null;
			}
			if (!resposta.ok) {
				throw new Error("mensagens");
			}
			return resposta.json();
		});
	})
	.then(function (mensagens) {
		if (!mensagens) {
			return;
		}
		lista.replaceChildren();
		if (!Array.isArray(mensagens) || mensagens.length === 0) {
			mostrarAviso(mensagem, "", "");
			vazio.hidden = false;
			return;
		}
		mostrarAviso(mensagem, "", "");
		mensagens.forEach(function (item) {
			lista.appendChild(montarConfirmacao(item));
		});
	})
	.catch(function () {
		mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
	});
