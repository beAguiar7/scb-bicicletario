const formulario = document.getElementById("formulario-edicao");
const mensagem = document.getElementById("mensagem-formulario");
const botao = document.getElementById("botao-salvar");
const campos = ["nome", "nacionalidade", "numeroDocumento", "foto", "email"];
const LIMITE_FOTO = 5 * 1024 * 1024;

formulario.hidden = true;

exigirSessao()
	.then(function (perfil) {
		if (!perfil) {
			return null;
		}
		document.getElementById("nome").value = perfil.nome || "";
		document.getElementById("nacionalidade").value = perfil.nacionalidade || "";
		document.getElementById("numeroDocumento").value = perfil.numeroDocumento || "";
		document.getElementById("email").value = perfil.email || "";
		formulario.hidden = false;
		mostrarAviso(mensagem, "", "");
		return prepararCsrf();
	})
	.catch(function () {
		mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
	});

formulario.addEventListener("submit", function (evento) {
	evento.preventDefault();
	if (botao.disabled) {
		return;
	}
	limparErros(campos);
	mostrarAviso(mensagem, "", "");

	const nome = document.getElementById("nome").value.trim();
	const nacionalidade = document.getElementById("nacionalidade").value;
	const documento = document.getElementById("numeroDocumento").value.trim();
	const foto = document.getElementById("foto").files[0];
	const email = document.getElementById("email").value.trim();
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
	if (foto && foto.size > LIMITE_FOTO) {
		definirErro("foto", "A foto deve ter no máximo 5 MB.");
		valido = false;
	}
	if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
		definirErro("email", "Informe um e-mail válido.");
		valido = false;
	}
	if (!valido) {
		return;
	}

	const dados = new FormData(formulario);
	const textoOriginal = botao.textContent;
	botao.disabled = true;
	botao.textContent = "Salvando...";
	fetch("/api/ciclista", {
		method: "PUT",
		credentials: "same-origin",
		headers: cabecalhos(),
		body: dados
	})
		.then(function (resposta) {
			return lerCorpo(resposta).then(function (corpo) {
				return { resposta: resposta, corpo: corpo };
			});
		})
		.then(function (resultado) {
			if (resultado.resposta.status === 401) {
				window.location.replace("index.html");
				return;
			}
			if (!resultado.resposta.ok) {
				mostrarAviso(mensagem, resultado.corpo.mensagem || "Revise os campos informados.", "erro");
				aplicarErrosDoServidor(resultado.corpo);
				botao.disabled = false;
				botao.textContent = textoOriginal;
				return;
			}
			if (resultado.corpo.emailAlterado) {
				window.location.replace("index.html?email=alterado");
				return;
			}
			mostrarAviso(mensagem, resultado.corpo.mensagem || "Cadastro atualizado.", "ok");
			botao.disabled = false;
			botao.textContent = textoOriginal;
		})
		.catch(function () {
			mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
			botao.disabled = false;
			botao.textContent = textoOriginal;
		});
});
