package com.marcotulio.financeiro.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Relógio que só anda quando o teste manda: permite testar "15 minutos depois" sem esperar. */
class RelogioDeTeste extends Clock {

    private Instant agora;

    RelogioDeTeste(Instant inicio) {
        this.agora = inicio;
    }

    void avancar(Duration tempo) {
        agora = agora.plus(tempo);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.of("America/Sao_Paulo");
    }

    @Override
    public Clock withZone(ZoneId zona) {
        throw new UnsupportedOperationException("Não usado nos testes.");
    }
}
