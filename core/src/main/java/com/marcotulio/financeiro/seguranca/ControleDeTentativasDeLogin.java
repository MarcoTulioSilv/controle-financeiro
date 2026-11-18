package com.marcotulio.financeiro.seguranca;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Conta as senhas erradas seguidas de cada login e bloqueia o login por um tempo quando elas
 * chegam ao limite. Torna inviável adivinhar a senha por tentativa e erro (força bruta).
 *
 * <p>A contagem fica na memória, e não no banco: o modelo de dados não tem colunas para ela, e
 * reiniciar a aplicação apenas libera um bloqueio de minutos, o que é aceitável aqui.
 *
 * <p>Tentativas com um login inexistente também são contadas. Assim o bloqueio se comporta igual
 * para qualquer login, e não serve para descobrir quais logins existem.
 */
@Component
class ControleDeTentativasDeLogin {

    static final int MAXIMO_DE_FALHAS_SEGUIDAS = 5;
    static final Duration DURACAO_DO_BLOQUEIO = Duration.ofMinutes(15);

    /** Falhas seguidas de um login e, se ele estiver bloqueado, até quando. */
    private record Situacao(int falhasSeguidas, Instant bloqueadoAte) {
    }

    // ConcurrentHashMap porque duas pessoas podem tentar entrar ao mesmo tempo (duas threads).
    private final Map<String, Situacao> situacoes = new ConcurrentHashMap<>();
    private final Clock relogio;

    ControleDeTentativasDeLogin(Clock relogio) {
        this.relogio = relogio;
    }

    void registrarFalha(String login) {
        // compute() faz ler e gravar a situação de uma vez, sem outra thread no meio.
        situacoes.compute(chave(login), (chave, atual) -> {
            int falhas = (atual == null || bloqueioExpirou(atual)) ? 1 : atual.falhasSeguidas() + 1;
            Instant bloqueadoAte = falhas >= MAXIMO_DE_FALHAS_SEGUIDAS
                    ? relogio.instant().plus(DURACAO_DO_BLOQUEIO)
                    : null;
            return new Situacao(falhas, bloqueadoAte);
        });
    }

    void registrarSucesso(String login) {
        situacoes.remove(chave(login));
    }

    boolean estaBloqueado(String login) {
        Situacao situacao = situacoes.get(chave(login));
        return situacao != null && situacao.bloqueadoAte() != null && !bloqueioExpirou(situacao);
    }

    private boolean bloqueioExpirou(Situacao situacao) {
        return situacao.bloqueadoAte() != null
                && !relogio.instant().isBefore(situacao.bloqueadoAte());
    }

    /**
     * Para o banco, {@code Ana}, {@code ANA} e {@code Ána} são o mesmo login (collation
     * {@code utf8mb4_0900_ai_ci}). A contagem precisa enxergar o mesmo, senão bastaria variar
     * maiúsculas e acentos para ganhar novas tentativas.
     */
    private static String chave(String login) {
        String semAcentos = Normalizer.normalize(login, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcentos.toLowerCase(Locale.ROOT);
    }
}
