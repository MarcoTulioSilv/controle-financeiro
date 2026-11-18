package com.marcotulio.financeiro.seguranca;

import com.marcotulio.financeiro.cadastro.CredencialDoUsuario;
import com.marcotulio.financeiro.cadastro.ServicoUsuarios;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ponte entre o Spring Security e o cadastro de usuários: dado o login digitado, devolve o hash
 * da senha gravado e se o login está bloqueado. Quem confere a senha é o próprio Spring Security.
 *
 * <p>Um login bloqueado é recusado antes mesmo de a senha ser conferida: durante o bloqueio, nem
 * a senha certa entra.
 */
@Service
class UsuariosParaAutenticacao implements UserDetailsService {

    private final ServicoUsuarios usuarios;
    private final ControleDeTentativasDeLogin tentativas;

    UsuariosParaAutenticacao(ServicoUsuarios usuarios, ControleDeTentativasDeLogin tentativas) {
        this.usuarios = usuarios;
        this.tentativas = tentativas;
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        CredencialDoUsuario credencial = usuarios.buscarCredencial(login)
                .orElseThrow(() -> new UsernameNotFoundException("Login não cadastrado."));
        // O nome é o login gravado, não o digitado: "ANA" entra como "ana", e é esse nome que
        // aparece na auditoria.
        return User.withUsername(credencial.login())
                .password(credencial.senhaHash())
                .accountLocked(tentativas.estaBloqueado(login))
                .build();
    }
}
