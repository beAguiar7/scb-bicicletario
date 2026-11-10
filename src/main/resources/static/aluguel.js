const mensagem = document.getElementById("mensagem-formulario");
const detalhe = document.getElementById("detalhe");
const vazio = document.getElementById("vazio");

exigirSessao()
	.then(function (perfil) {
		if (!perfil) {
			return null;
		}
		return fetch("/api/aluguel", { credentials: "same-origin", cache: "no-store" }).then(function (resposta) {
			if (resposta.status === 401) {
				window.location.replace("index.html");
				return null;
			}
			if (!resposta.ok) {
				throw new Error("aluguel");
			}
			return resposta.json();
		});
	})
	.then(function (dados) {
		if (!dados) {
			return;
		}
		if (!dados.ativo || !dados.aluguel) {
			mostrarAviso(mensagem, "", "");
			vazio.hidden = false;
			return;
		}
		const aluguel = dados.aluguel;
		const titulo = document.createElement("h2");
		titulo.textContent = "Bicicleta com você";
		const lista = document.createElement("dl");
		adicionarLinha(lista, "Bicicleta", aluguel.rfid);
		adicionarLinha(lista, "Retirada", aluguel.inicio ? String(aluguel.inicio).replace("T", " ") : "");
		if (aluguel.duracaoHoras !== undefined && aluguel.duracaoHoras !== null) {
			adicionarLinha(lista, "Período básico", aluguel.duracaoHoras + " horas");
		}
		adicionarLinha(lista, "Tranca", aluguel.numeroTranca);
		adicionarLinha(lista, "Totem", aluguel.totem);
		if (aluguel.valor) {
			adicionarLinha(lista, "Taxa", "R$ " + String(aluguel.valor).replace(".", ","));
		}
		detalhe.append(titulo, lista);
		detalhe.hidden = false;
		mostrarAviso(mensagem, "", "");
	})
	.catch(function () {
		mostrarAviso(mensagem, mensagemIndisponivel(), "erro");
	});
