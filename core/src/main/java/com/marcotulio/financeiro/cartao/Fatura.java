package com.marcotulio.financeiro.cartao;

import com.marcotulio.financeiro.compartilhado.EntidadeAuditavel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * A fatura de um cartão em um mês de referência (RF14). Há no máximo uma por cartão e mês.
 *
 * <p>A coluna {@code total_informado_banco} não está mapeada: ela só é usada na revisão de fatura
 * (RF15), fora do MVP 1, e fica nula até lá.
 */
@Entity
@Table(name = "fatura")
class Fatura extends EntidadeAuditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cartao_id", nullable = false)
    private Long cartaoId;

    /** Gravado como DATE com dia 1 pelo {@code ConversorMesReferencia} (C5). */
    @Column(name = "mes_referencia", nullable = false)
    private YearMonth mesReferencia;

    @Column(name = "data_fechamento", nullable = false)
    private LocalDate dataFechamento;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_vencimento_efetiva", nullable = false)
    private LocalDate dataVencimentoEfetiva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusFatura status;

    protected Fatura() {
        // exigido pelo JPA
    }

    /** Uma fatura nova nasce aberta. */
    Fatura(long cartaoId, FaturaCalculada calculada, LocalDate dataVencimentoEfetiva) {
        this.cartaoId = cartaoId;
        this.mesReferencia = calculada.mesReferencia();
        this.dataFechamento = calculada.fechamento();
        this.dataVencimento = calculada.vencimento();
        this.dataVencimentoEfetiva = dataVencimentoEfetiva;
        this.status = StatusFatura.ABERTA;
    }

    FaturaRegistrada paraConsulta() {
        return new FaturaRegistrada(id, cartaoId, mesReferencia, dataFechamento, dataVencimento,
                dataVencimentoEfetiva, status);
    }
}
