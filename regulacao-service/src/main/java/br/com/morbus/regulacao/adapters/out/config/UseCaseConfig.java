package br.com.morbus.regulacao.adapters.out.config;

import br.com.morbus.regulacao.domain.CriarSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.ListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ICriarSolicitacaoUseCase criarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository) {
        return new CriarSolicitacaoUseCase(solicitacaoRepository);
    }

    @Bean
    public IListarSolicitacoesUseCase listarSolicitacoesUseCase(ISolicitacaoRepository solicitacaoRepository) {
        return new ListarSolicitacoesUseCase(solicitacaoRepository);
    }
}
