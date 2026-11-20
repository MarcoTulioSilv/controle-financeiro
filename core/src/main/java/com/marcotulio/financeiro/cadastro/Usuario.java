package com.marcotulio.financeiro.cadastro;

import com.marcotulio.financeiro.compartilhado.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Quem entra na interface web (RF06). Cada usuário representa um destinatário: a Ana logada é a
 * mesma Ana do "para quem".
 *
 * <p>Guarda só o hash da senha, e não há como "ler a senha" de volta: para conferir, o Spring
 * Security calcula o hash do que foi digitado e compara com o gravado.
 */
@Entity
@Table(name = "usuario")
class Usuario extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 50)
    private String login;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Destinatario destinatario;

    protected Usuario() {
        // exigido pelo JPA
    }

    Usuario(String nome, String login, String senhaHash, Destinatario destinatario) {
        this.nome = nome;
        this.login = login;
        this.senhaHash = senhaHash;
        this.destinatario = destinatario;
    }

    Long getId() {
        return id;
    }

    String getSenhaHash() {
        return senhaHash;
    }

    void trocarSenhaHash(String novoSenhaHash) {
        this.senhaHash = novoSenhaHash;
    }

    CredencialDoUsuario paraAutenticacao() {
        return new CredencialDoUsuario(login, senhaHash);
    }
}
