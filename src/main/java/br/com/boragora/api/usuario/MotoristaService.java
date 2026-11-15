package br.com.boragora.api.usuario;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.boragora.api.seguranca.AutenticacaoService;

@Service
public class MotoristaService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;

    public MotoristaService(UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    /** RF02: o motorista-chefe cadastra motoristas comuns; nunca outro chefe. */
    @Transactional
    public MotoristaResposta cadastrar(MotoristaRequisicao requisicao) {
        if (usuarios.existsByEmail(requisicao.email())) {
            throw AutenticacaoService.emailJaCadastrado();
        }
        Usuario motorista = new Usuario(requisicao.nome().trim(), requisicao.email(),
                senhas.encode(requisicao.senha()), Papel.MOTORISTA);
        try {
            usuarios.saveAndFlush(motorista);
        } catch (DataIntegrityViolationException e) {
            throw AutenticacaoService.emailJaCadastrado();
        }
        return MotoristaResposta.de(motorista);
    }

    /** Motoristas ativos, incluindo o chefe, em ordem alfabética. */
    @Transactional(readOnly = true)
    public List<MotoristaResposta> listar() {
        return usuarios.findByPapelAndAtivoTrueOrderByNomePessoaAscIdAsc(Papel.MOTORISTA).stream()
                .map(MotoristaResposta::de)
                .toList();
    }
}
