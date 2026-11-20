package com.marcotulio.financeiro.cadastro;

import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Porta de entrada pública do cadastro de usuários e de suas senhas (RF06, RNF02). */
@Service
public class ServicoUsuarios {

    private final RepositorioDeUsuarios usuarios;
    private final RepositorioDeDestinatarios destinatarios;
    private final PasswordEncoder codificadorDeSenha;

    ServicoUsuarios(
            RepositorioDeUsuarios usuarios,
            RepositorioDeDestinatarios destinatarios,
            PasswordEncoder codificadorDeSenha) {
        this.usuarios = usuarios;
        this.destinatarios = destinatarios;
        this.codificadorDeSenha = codificadorDeSenha;
    }

    /**
     * Cadastra o usuário, a menos que o login já exista: nesse caso nada muda, nem a senha. Assim
     * a carga de dados iniciais pode rodar de novo sem desfazer a senha que o usuário trocou.
     *
     * @param senhaInicial a senha do arquivo de dados iniciais; só o hash dela é gravado
     * @param marcacaoDestinatario marcação do destinatário que o usuário representa, ex.: {@code @ana}
     * @throws IllegalArgumentException se faltar a senha ou se a marcação não existir
     */
    @Transactional
    public void cadastrarSeAusente(
            String nome, String login, String senhaInicial, String marcacaoDestinatario) {
        if (usuarios.findByLogin(login).isPresent()) {
            return;
        }
        if (senhaInicial == null || senhaInicial.isBlank()) {
            throw new IllegalArgumentException(
                    "Usuário sem senha inicial no arquivo de dados iniciais.");
        }
        Destinatario destinatario = destinatarios.findByMarcacaoBot(marcacaoDestinatario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Destinatário " + marcacaoDestinatario + " não cadastrado."));
        String senhaHash = codificadorDeSenha.encode(senhaInicial);
        usuarios.save(new Usuario(nome, login, senhaHash, destinatario));
    }

    /** O login como está gravado e o hash da senha, para o Spring Security conferir o acesso. */
    @Transactional(readOnly = true)
    public Optional<CredencialDoUsuario> buscarCredencial(String login) {
        return usuarios.findByLogin(login).map(Usuario::paraAutenticacao);
    }

    /**
     * O id do usuário com esse login, sem diferenciar maiúsculas. Outros módulos guardam só o id
     * (ex.: o titular de um cartão), pois a entidade {@link Usuario} não sai do cadastro (M5).
     */
    @Transactional(readOnly = true)
    public Optional<Long> buscarIdPeloLogin(String login) {
        return usuarios.findByLogin(login).map(Usuario::getId);
    }

    /**
     * Troca a senha do próprio usuário. Exigir a senha atual impede que alguém com uma sessão
     * aberta nas mãos (o celular esquecido na mesa) troque a senha e tome a conta.
     *
     * @return {@code false} se a senha atual não confere; nesse caso nada é alterado
     */
    @Transactional
    public boolean trocarSenha(String login, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarios.findByLogin(login)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não cadastrado."));
        if (!codificadorDeSenha.matches(senhaAtual, usuario.getSenhaHash())) {
            return false;
        }
        usuario.trocarSenhaHash(codificadorDeSenha.encode(novaSenha));
        return true;
    }
}
