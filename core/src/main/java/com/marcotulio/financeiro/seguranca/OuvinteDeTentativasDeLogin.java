package com.marcotulio.financeiro.seguranca;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * O Spring Security publica um evento a cada tentativa de login. Este ouvinte repassa ao
 * {@link ControleDeTentativasDeLogin} as senhas erradas e os acertos.
 *
 * <p>Só "credenciais erradas" contam como falha: uma tentativa recusada porque o login já está
 * bloqueado não prolonga o bloqueio.
 */
@Component
class OuvinteDeTentativasDeLogin {

    private final ControleDeTentativasDeLogin tentativas;

    OuvinteDeTentativasDeLogin(ControleDeTentativasDeLogin tentativas) {
        this.tentativas = tentativas;
    }

    @EventListener
    void aoErrarASenha(AuthenticationFailureBadCredentialsEvent evento) {
        tentativas.registrarFalha(evento.getAuthentication().getName());
    }

    @EventListener
    void aoEntrar(AuthenticationSuccessEvent evento) {
        tentativas.registrarSucesso(evento.getAuthentication().getName());
    }
}
