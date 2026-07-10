package br.com.morbus.regulacao.adapters.out.config;

import br.com.morbus.regulacao.domain.usecase.quota.ConsultarCotasUseCase;
import br.com.morbus.regulacao.domain.usecase.quota.GerenciarCotaUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.AvaliarSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ComplementarSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ConsultarStatusSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.CriarSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.CancelarSolicitacaoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ListarSolicitacoesUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.ReclassificarRiscoUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.TransicionarParaAgendadaUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.TransicionarParaAtendidaUseCase;
import br.com.morbus.regulacao.domain.usecase.solicitacao.TransicionarParaFaltouUseCase;
import br.com.morbus.regulacao.domain.usecase.unidade.BuscarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.domain.usecase.unidade.CadastrarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.in.IAvaliarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IBuscarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.in.ICadastrarUnidadeSolicitanteUseCase;
import br.com.morbus.regulacao.ports.in.IConsultarCotasUseCase;
import br.com.morbus.regulacao.ports.in.IConsultarStatusSolicitacao;
import br.com.morbus.regulacao.ports.in.ICriarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.ICancelarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IComplementarSolicitacaoUseCase;
import br.com.morbus.regulacao.ports.in.IGerenciarCotaUseCase;
import br.com.morbus.regulacao.ports.in.IListarSolicitacoesUseCase;
import br.com.morbus.regulacao.ports.in.IReclassificarRiscoUseCase;
import br.com.morbus.regulacao.ports.in.ITransicionarParaAgendadaUseCase;
import br.com.morbus.regulacao.ports.in.ITransicionarParaAtendidaUseCase;
import br.com.morbus.regulacao.ports.in.ITransicionarParaFaltouUseCase;
import br.com.morbus.regulacao.ports.out.IParecerRepository;
import br.com.morbus.regulacao.ports.out.IQuotaRepository;
import br.com.morbus.regulacao.ports.out.IRegulacaoEventPublisher;
import br.com.morbus.regulacao.ports.out.ISolicitacaoRepository;
import br.com.morbus.regulacao.ports.out.IUnidadeSolicitanteRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ICriarSolicitacaoUseCase criarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository,
                                                              IQuotaRepository quotaRepository,
                                                              IUnidadeSolicitanteRepository unidadeSolicitanteRepository) {
        return new CriarSolicitacaoUseCase(solicitacaoRepository, quotaRepository, unidadeSolicitanteRepository);
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

    @Bean
    public IAvaliarSolicitacaoUseCase avaliarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository,
                                                                 IParecerRepository parecerRepository,
                                                                 IRegulacaoEventPublisher eventPublisher) {
        return new AvaliarSolicitacaoUseCase(solicitacaoRepository, parecerRepository, eventPublisher);
    }

    @Bean
    public IReclassificarRiscoUseCase reclassificarRiscoUseCase(ISolicitacaoRepository solicitacaoRepository,
                                                                  IRegulacaoEventPublisher eventPublisher) {
        return new ReclassificarRiscoUseCase(solicitacaoRepository, eventPublisher);
    }

    @Bean
    public IComplementarSolicitacaoUseCase complementarSolicitacaoUseCase(ISolicitacaoRepository solicitacaoRepository) {
        return new ComplementarSolicitacaoUseCase(solicitacaoRepository);
    }

    @Bean
    public ITransicionarParaAgendadaUseCase transicionarParaAgendadaUseCase(ISolicitacaoRepository solicitacaoRepository) {
        return new TransicionarParaAgendadaUseCase(solicitacaoRepository);
    }

    @Bean
    public IGerenciarCotaUseCase gerenciarCotaUseCase (IQuotaRepository quotaRepository,
                                                        IUnidadeSolicitanteRepository unidadeSolicitanteRepository) {
        return new GerenciarCotaUseCase(quotaRepository, unidadeSolicitanteRepository);
    }

    @Bean
    public IConsultarCotasUseCase consultarCotasUseCase (IQuotaRepository quotaRepository) {
        return new ConsultarCotasUseCase(quotaRepository);
    }

    @Bean
    public ICadastrarUnidadeSolicitanteUseCase cadastrarUnidadeSolicitanteUseCase(IUnidadeSolicitanteRepository unidadeSolicitanteRepository) {
        return new CadastrarUnidadeSolicitanteUseCase(unidadeSolicitanteRepository);
    }

    @Bean
    public IBuscarUnidadeSolicitanteUseCase buscarUnidadeSolicitanteUseCase(IUnidadeSolicitanteRepository unidadeSolicitanteRepository) {
        return new BuscarUnidadeSolicitanteUseCase(unidadeSolicitanteRepository);
    }

    @Bean
    public ITransicionarParaFaltouUseCase transicionarParaFaltouUseCase(ISolicitacaoRepository solicitacaoRepository) {
        return new TransicionarParaFaltouUseCase(solicitacaoRepository);
    }

    @Bean
    public ITransicionarParaAtendidaUseCase transicionarParaAtendidaUseCase(ISolicitacaoRepository solicitacaoRepository) {
        return new TransicionarParaAtendidaUseCase(solicitacaoRepository);
    }
}
