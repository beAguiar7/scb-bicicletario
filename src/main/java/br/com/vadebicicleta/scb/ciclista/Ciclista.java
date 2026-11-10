package br.com.vadebicicleta.scb.ciclista;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ciclista", uniqueConstraints = @UniqueConstraint(name = "uk_ciclista_email", columnNames = "email"))
public class Ciclista {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String nome;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Nacionalidade nacionalidade;

	@Column(nullable = false, length = 30)
	private String numeroDocumento;

	@Column(nullable = false, unique = true, length = 120)
	private String email;

	@JsonIgnore
	@Column(nullable = false, length = 100)
	private String senhaHash;

	/**
	 * Conteudo da foto. Nao entra nas respostas.
	 * VARBINARY vira bytea no PostgreSQL e varbinary no H2.
	 * BLOB no PostgreSQL usaria oid, que nao serve para esta foto.
	 */
	@JsonIgnore
	@JdbcTypeCode(SqlTypes.VARBINARY)
	@Column(nullable = false, length = 5_242_880)
	private byte[] fotoDocumento;

	public Long getId() {
		return this.id;
	}

	public String getNome() {
		return this.nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Nacionalidade getNacionalidade() {
		return this.nacionalidade;
	}

	public void setNacionalidade(Nacionalidade nacionalidade) {
		this.nacionalidade = nacionalidade;
	}

	public String getNumeroDocumento() {
		return this.numeroDocumento;
	}

	public void setNumeroDocumento(String numeroDocumento) {
		this.numeroDocumento = numeroDocumento;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenhaHash() {
		return this.senhaHash;
	}

	public void setSenhaHash(String senhaHash) {
		this.senhaHash = senhaHash;
	}

	public byte[] getFotoDocumento() {
		return this.fotoDocumento;
	}

	public void setFotoDocumento(byte[] fotoDocumento) {
		this.fotoDocumento = fotoDocumento;
	}

	@Override
	public String toString() {
		return "Ciclista{id=" + this.id + ", email=" + this.email + "}";
	}

}
