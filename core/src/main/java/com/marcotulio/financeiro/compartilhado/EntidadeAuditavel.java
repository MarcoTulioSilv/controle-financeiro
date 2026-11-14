package com.marcotulio.financeiro.compartilhado;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Colunas que toda tabela do sistema tem (convenção C7): quem criou e quando, quem alterou e
 * quando (RNF06), e a versão para concorrência otimista.
 *
 * <p>{@code @MappedSuperclass}: não é uma tabela: as colunas abaixo são herdadas por cada
 * entidade. O {@link AuditingEntityListener} é quem preenche os quatro primeiros campos ao
 * gravar, usando o autor fornecido por {@code ConfiguracaoAuditoria}.
 *
 * <p>A {@code versao} resolve o caso em que o Marco e a Ana editam o mesmo lançamento ao mesmo
 * tempo: o segundo a gravar recebe erro em vez de apagar silenciosamente a alteração do outro.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class EntidadeAuditavel {

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @CreatedBy
    @Column(name = "criado_por", nullable = false, updatable = false, length = 50)
    private String criadoPor;

    @LastModifiedDate
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @LastModifiedBy
    @Column(name = "atualizado_por", nullable = false, length = 50)
    private String atualizadoPor;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public String getAtualizadoPor() {
        return atualizadoPor;
    }

    public long getVersao() {
        return versao;
    }
}
