package com.marcotulio.financeiro.cadastro;

import com.marcotulio.financeiro.compartilhado.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A quem um lançamento se destina: um membro do domicílio ou a casa (RF13).
 *
 * <p>Sem {@code public}: só o pacote {@code cadastro} enxerga a entidade. Os outros módulos usam
 * o {@link ServicoDestinatarios}, que devolve um {@link DestinatarioCadastrado} (regra M5).
 */
@Entity
@Table(name = "destinatario")
class Destinatario extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nome;

    /** Como o destinatário é marcado na mensagem do bot, ex.: {@code @casa}. */
    @Column(name = "marcacao_bot", nullable = false, length = 20)
    private String marcacaoBot;

    @Column(nullable = false)
    private boolean ativo = true;

    protected Destinatario() {
        // exigido pelo JPA
    }

    Destinatario(String nome, String marcacaoBot) {
        this.nome = nome;
        this.marcacaoBot = marcacaoBot;
    }

    DestinatarioCadastrado paraConsulta() {
        return new DestinatarioCadastrado(id, nome, marcacaoBot, ativo);
    }
}
