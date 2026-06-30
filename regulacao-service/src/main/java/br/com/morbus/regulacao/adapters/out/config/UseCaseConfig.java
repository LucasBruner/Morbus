package br.com.morbus.regulacao.adapters.out.config;

import br.com.morbus.regulacao.domain.usecase.solicitacao.ConsultarStatusSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.CriarSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.CancelarSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.in.IConsultarStatusSolicitacao;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.ICancelarSolicitacaoUseCase;
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

    @Bean
    public ICancelarSolicitacaoUseCase cancelarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository) {
        return new CancelarSolicitacaoUseCase(solicitacaoRepository);
    }

    @Bean
    public IConsultarStatusSolicitacao consultarStatusSolicitacao(ISolicitacaoRepository solicitacaoRepository) {
        return new ConsultarStatusSolicitacaoUseCase(solicitacaoRepository);
    }
}
