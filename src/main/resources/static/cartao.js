const formulario = document.getElementById("formulario-cartao");
const mensagem = document.getElementById("mensagem-formulario");
const atual = document.getElementById("cartao-atual");
const opcoes = document.getElementById("opcoes");
const botao = document.getElementById("botao-salvar");

formulario.hidden = true;

function mostrarAtual(dados) {
	atual.replaceChildren();
	if (!dados || !dados.cadastrado) {
		atual.className = "cartao-visual vazio";
		const texto = document.createElement("p");
		texto.textContent = dados && dados.aviso ? dados.aviso : "Nenhum cartão de demonstração cadastrado.";
		atual.appendChild(texto);
		return;
	}
	atual.className = "cartao-visual";
	const selo = document.createElement("p");
	selo.className = "selo";
	selo.textContent = "Simulação";
	const numero = document.createElement("p");
	numero.className = "cartao-numero";
	numero.textContent = dados.mascara || "";
	const titular = document.createElement("p");
	titular.textContent = dados.titular || "";
	const aviso = document.createElement("p");
	aviso.textContent = dados.aviso || "";
	atual.append(selo, numero, titular, aviso);
}

function marcarSelecionado(referencia) {
	const campo = document.querySelector("input[name='referencia'][value='" + referencia + "']");
	if (campo) {
		campo.checked = true;
	}
}

exigirSessao()
	.then(function (perfil) {
		if (!perfil) {
			return null;
		}
		return prepararCsrf();
	})
	.then(function (token) {
		if (!token && !csrf) {
			return null;
		}
		return Promise.all([
			fetch("/api/cartoes-demonstracao", { credentials: "same-origin", cache: "no-store" }).then(lerCorpo),
			fetch("/api/cartao", { credentials: "same-origin", cache: "no-store" }).then(lerCorpo)
		]);
	})
	.then(function (respostas) {
		if (!respostas) {
			return;
		}
		const catalogo = respostas[0];
		const corrente = respostas[1];
		opcoes.replaceChildren();
		(catalogo || []).forEach(function (cartao) {
			const rotulo = document.createElement("label");
			rotulo.className = "opcao";
			const entrada = document.createElement("input");
			entrada.type = "radio";
			entrada.name = "referencia";
			entrada.value = cartao.referencia;
			const texto = document.createElement("span");
			texto.textContent = cartao.titular + " " + cartao.mascara + ". " + cartao.descricao;
			rotulo.append(entrada, texto);
			opcoes.appendChild(rotulo);
		});
		mostrarAtual(corrente);
		if (corrente && corrente.referencia) {
			marcarSelecionado(corrente.referencia);
		}
		formulario.hidden = false;
	})
	.catch(function () {
		atual.className = "cartao-visual vazio";
		atual.textContent = "";
		mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
	});

formulario.addEventListener("submit", function (evento) {
	evento.preventDefault();
	if (botao.disabled) {
		return;
	}
	definirErro("referencia", "");
	mostrarAviso(mensagem, "", "");
	const escolhido = document.querySelector("input[name='referencia']:checked");
	if (!escolhido) {
		definirErro("referencia", "Escolha um cartão de demonstração.");
		return;
	}
	const textoOriginal = botao.textContent;
	botao.disabled = true;
	botao.textContent = "Salvando...";
	fetch("/api/cartao", {
		method: "POST",
		credentials: "same-origin",
		headers: cabecalhos({ "Content-Type": "application/json" }),
		body: JSON.stringify({ referencia: escolhido.value })
	})
		.then(function (resposta) {
			return lerCorpo(resposta).then(function (corpo) {
				return { ok: resposta.ok, status: resposta.status, corpo: corpo };
			});
		})
		.then(function (resultado) {
			if (resultado.status === 401) {
				window.location.replace("index.html");
				return;
			}
			if (!resultado.ok) {
				mostrarAviso(mensagem, resultado.corpo.mensagem || "Não foi possível salvar o cartão.", "erro");
				aplicarErrosDoServidor(resultado.corpo);
				return fetch("/api/cartao", { credentials: "same-origin", cache: "no-store" })
					.then(lerCorpo)
					.then(mostrarAtual);
			}
			mostrarAtual(resultado.corpo);
			mostrarAviso(mensagem, "Cartão de demonstração salvo.", "ok");
		})
		.catch(function () {
			mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
		})
		.finally(function () {
			botao.disabled = false;
			botao.textContent = textoOriginal;
		});
});
