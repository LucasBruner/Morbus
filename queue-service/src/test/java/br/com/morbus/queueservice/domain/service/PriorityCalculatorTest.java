package br.com.morbus.queueservice.domain.service;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.QueueEntry;
import br.com.morbus.queueservice.domain.enums.EPriorityGroup;
import br.com.morbus.queueservice.domain.enums.ERiskColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a PriorityCalculator seguindo as regras do SUS.
 * 
 * Cenários testados:
 * 1. Vermelho vs Azul (mesmo grupo deve ter cor como prioridade)
 * 2. Idoso vs Geral (mesma cor deve ter grupo como prioridade)
 * 3. Desempate por timestamp (mais antigo primeiro)
 * 4. Múltiplas prioridades (Gestante vs Idoso)
 * 5. Todos os critérios iguais (desempate exclusivo por timestamp)
 */
@DisplayName("PriorityCalculator - Testes de Priorização de Fila do SUS")
class PriorityCalculatorTest {
    
    private Patient pacienteGeral;
    private Patient pacienteIdoso;
    private Patient pacienteGestante;
    
    @BeforeEach
    void setUp() {
        pacienteGeral = criarPaciente("João Silva", 35);
        pacienteIdoso = criarPaciente("Maria Santos", 65);
        pacienteGestante = criarPaciente("Ana Oliveira", 28, EPriorityGroup.GESTANTE);
    }
    
    // Cenário 1: Vermelho vs Azul
    // Mesmo grupo deve ter cor como prioridade
    @Test
    @DisplayName("Cenário 1: Risco Vermelho tem prioridade sobre Azul")
    void testVermelhoPrioritarioSobreAzul() {
        QueueEntry filaVermelho = criarQueueEntry(pacienteGeral, ERiskColor.VERMELHO, LocalDateTime.now());
        QueueEntry filaAzul = criarQueueEntry(pacienteGeral, ERiskColor.AZUL, LocalDateTime.now());
        
        int resultado = PriorityCalculator.compare(filaVermelho, filaAzul);
        
        assertTrue(resultado < 0, "Vermelho deve ter menor valor (maior prioridade)");
    }
    
    @Test
    @DisplayName("Cenário 1b: Ordem completa de cores - Vermelho > Amarelo > Verde > Azul")
    void testOrdenacaoComplexaDeCores() {
        LocalDateTime agora = LocalDateTime.now();
        
        QueueEntry filaVermelho = criarQueueEntry(pacienteGeral, ERiskColor.VERMELHO, agora);
        QueueEntry filaAmarelo = criarQueueEntry(pacienteGeral, ERiskColor.AMARELO, agora);
        QueueEntry filaVerde = criarQueueEntry(pacienteGeral, ERiskColor.VERDE, agora);
        QueueEntry filaAzul = criarQueueEntry(pacienteGeral, ERiskColor.AZUL, agora);
        
        // Verifica todas as comparações
        assertTrue(PriorityCalculator.compare(filaVermelho, filaAmarelo) < 0);
        assertTrue(PriorityCalculator.compare(filaAmarelo, filaVerde) < 0);
        assertTrue(PriorityCalculator.compare(filaVerde, filaAzul) < 0);
    }
    
    // Cenário 2: Idoso vs Geral na mesma cor
    // Mesma cor deve ter grupo como prioridade
    @Test
    @DisplayName("Cenário 2: Idoso tem prioridade sobre geral na mesma cor de risco")
    void testIdosoPrioritarioSobreGeralMesmaCor() {
        QueueEntry filaIdoso = criarQueueEntry(pacienteIdoso, ERiskColor.VERDE, LocalDateTime.now());
        QueueEntry filaGeral = criarQueueEntry(pacienteGeral, ERiskColor.VERDE, LocalDateTime.now());
        
        int resultado = PriorityCalculator.compare(filaIdoso, filaGeral);
        
        assertTrue(resultado < 0, "Idoso deve ter menor valor (maior prioridade)");
    }
    
    @Test
    @DisplayName("Cenário 2b: Idoso vermelho vs Geral vermelho")
    void testIdosoVermelhoVsGeralVermelho() {
        QueueEntry filaIdoso = criarQueueEntry(pacienteIdoso, ERiskColor.VERMELHO, LocalDateTime.now());
        QueueEntry filaGeral = criarQueueEntry(pacienteGeral, ERiskColor.VERMELHO, LocalDateTime.now());
        
        int resultado = PriorityCalculator.compare(filaIdoso, filaGeral);
        
        assertTrue(resultado < 0, "Idoso deve ter prioridade mesmo em vermelho");
    }
    
    // Cenário 3: Desempate por timestamp
    // Mais antigo deve ter prioridade
    @Test
    @DisplayName("Cenário 3: Paciente que chegou primeiro tem prioridade")
    void testDesempateTimestamp() {
        LocalDateTime hora1 = LocalDateTime.of(2024, 6, 1, 8, 0, 0);
        LocalDateTime hora2 = LocalDateTime.of(2024, 6, 1, 8, 30, 0);
        
        QueueEntry filaPrimeira = criarQueueEntry(pacienteGeral, ERiskColor.AZUL, hora1);
        QueueEntry filaSegunda = criarQueueEntry(pacienteGeral, ERiskColor.AZUL, hora2);
        
        int resultado = PriorityCalculator.compare(filaPrimeira, filaSegunda);
        
        assertTrue(resultado < 0, "Quem chegou primeiro deve ter prioridade");
    }
    
    @Test
    @DisplayName("Cenário 3b: Múltiplos pacientes no mesmo timestamp")
    void testMultiplosPacientesMesmoTimestamp() {
        QueueEntry fila1 = criarQueueEntry(pacienteGeral, ERiskColor.AZUL, LocalDateTime.now());
        QueueEntry fila2 = criarQueueEntry(pacienteGeral, ERiskColor.AZUL, LocalDateTime.now());
        
        int resultado = PriorityCalculator.compare(fila1, fila2);
        
        assertEquals(0, resultado, "Mesmo timestamp deve retornar 0");
    }
    
    // Cenário 4: Gestante vs Idoso
    @Test
    @DisplayName("Cenário 4: Gestante vs Idoso - ambos pacientes prioritários")
    void testGestanteVsIdoso() {
        QueueEntry filaIdoso = criarQueueEntry(pacienteIdoso, ERiskColor.AZUL, LocalDateTime.now());
        QueueEntry filaGestante = criarQueueEntry(pacienteGestante, ERiskColor.AZUL, LocalDateTime.now());
        
        int resultado = PriorityCalculator.compare(filaIdoso, filaGestante);

        assertTrue(resultado < 0, "Idoso (1) deve ter prioridade sobre Gestante (2)");
    }
    
    // Cenário 5: Critério completo de desempate
    @Test
    @DisplayName("Cenário 5: Mesmo tudo (cor, grupo, timestamp)")
    void testMesmoTudoDesempate() {
        // Pacientes diferentes, mas com mesmas características de prioridade
        Patient paciente1 = criarPaciente("Pedro Costa", 35);
        Patient paciente2 = criarPaciente("Paulo Silva", 35);
        
        QueueEntry fila1 = criarQueueEntry(paciente1, ERiskColor.VERDE, LocalDateTime.now());
        QueueEntry fila2 = criarQueueEntry(paciente2, ERiskColor.VERDE, LocalDateTime.now());
        
        int resultado = PriorityCalculator.compare(fila1, fila2);
        
        assertEquals(0, resultado, "Mesmas características devem resultar em 0");
    }
    
    // Testes do isPriorityGroup()
    @Test
    @DisplayName("Teste: isPriorityGroup retorna true para paciente com 60 anos exatos")
    void testIsPriorityGroupComSessenta() {
        Patient paciente60 = criarPaciente("Idoso", 60);
        
        assertTrue(PriorityCalculator.isPriorityGroup(paciente60),
                   "Paciente com 60 anos deve ser considerado IDOSO");
    }
    
    @Test
    @DisplayName("Teste: isPriorityGroup retorna true para paciente com 60+ anos")
    void testIsPriorityGroupMaiorQueSessenta() {
        Patient paciente70 = criarPaciente("Muito Idoso", 70);
        
        assertTrue(PriorityCalculator.isPriorityGroup(paciente70),
                   "Paciente com 70 anos deve ser considerado IDOSO");
    }
    
    @Test
    @DisplayName("Teste: isPriorityGroup retorna false para paciente com <60 anos")
    void testIsPriorityGroupMenorQueSessenta() {
        Patient paciente59 = criarPaciente("Quase Idoso", 59);
        
        assertFalse(PriorityCalculator.isPriorityGroup(paciente59),
                    "Paciente com 59 anos não deve ser considerado IDOSO");
    }
    
    @Test
    @DisplayName("Teste: isPriorityGroup retorna false para paciente nulo")
    void testIsPriorityGroupPacienteNulo() {
        assertFalse(PriorityCalculator.isPriorityGroup(null),
                    "Paciente nulo deve retornar false");
    }
    
    // Testes do getPriorityGroup()
    @Test
    @DisplayName("Teste: getPriorityGroup retorna IDOSO para paciente 60+")
    void testGetPriorityGroupRetornaIdoso() {
        Patient paciente = criarPaciente("Paciente Idoso", 65);
        
        EPriorityGroup prioridade = PriorityCalculator.getPriorityGroup(paciente);
        
        assertEquals(EPriorityGroup.IDOSO, prioridade,
                    "Paciente com 60+ anos deve retornar IDOSO");
    }
    
    @Test
    @DisplayName("Teste: getPriorityGroup retorna grupoLegal para paciente <60")
    void testGetPriorityGroupRetornaGrupoLegal() {
        Patient paciente = criarPaciente("Paciente Gestante", 30, EPriorityGroup.GESTANTE);
        
        EPriorityGroup prioridade = PriorityCalculator.getPriorityGroup(paciente);
        
        assertEquals(EPriorityGroup.GESTANTE, prioridade,
                    "Paciente <60 sem ser idoso deve retornar seu grupoLegal");
    }
    
    @Test
    @DisplayName("Teste: getPriorityGroup retorna GERAL para paciente sem grupo legal definido")
    void testGetPriorityGroupRetornaGeral() {
        Patient paciente = criarPaciente("Paciente Geral", 35);
        
        EPriorityGroup prioridade = PriorityCalculator.getPriorityGroup(paciente);
        
        assertEquals(EPriorityGroup.GERAL, prioridade,
                    "Paciente sem prioridade deve retornar GERAL");
    }
    
    // Cenário 6: Ordem correta de múltiplas filas
    // Verifica a ordem correta verificando todos os critérios de prioridade (cor, grupo e timestamp)
    @Test
    @DisplayName("Cenário 6: Ordem correta de múltiplas filas")
    void testIntegracaoComplexa() {
        LocalDateTime hora1 = LocalDateTime.of(2024, 6, 1, 8, 0, 0);
        LocalDateTime hora2 = LocalDateTime.of(2024, 6, 1, 8, 15, 0);
        LocalDateTime hora3 = LocalDateTime.of(2024, 6, 1, 8, 30, 0);
        
        // Múltiplas filas com diferentes prioridades
        QueueEntry fila1 = criarQueueEntry(pacienteGeral, ERiskColor.AZUL, hora1);
        QueueEntry fila2 = criarQueueEntry(pacienteIdoso, ERiskColor.VERDE, hora2);
        QueueEntry fila3 = criarQueueEntry(pacienteGeral, ERiskColor.VERMELHO, hora3);
        
        // A ordem esperada é:
        // 1. fila3 (Vermelho) - maior prioridade de cor
        // 2. fila2 (Verde + Idoso) - cor verde, mas paciente idoso
        // 3. fila1 (Azul + Geral) - menor prioridade
        
        assertTrue(PriorityCalculator.compare(fila3, fila2) < 0);
        assertTrue(PriorityCalculator.compare(fila2, fila1) < 0);
    }
    
    /**
     * Cria um paciente com idade específica e grupo legal GERAL
     */
    private Patient criarPaciente(String nome, int idadeAtual) {
        return criarPaciente(nome, idadeAtual, EPriorityGroup.GERAL);
    }
    
    /**
     * Cria um paciente com idade específica e grupo legal definido
     */
    private Patient criarPaciente(String nome, int idadeAtual, EPriorityGroup grupoLegal) {
        Patient paciente = new Patient();
        paciente.setId(UUID.randomUUID());
        paciente.setNome(nome);
        paciente.setDataNascimento(LocalDate.now().minusYears(idadeAtual));
        paciente.setGrupoLegal(grupoLegal);
        paciente.setCpf("12345678901");
        paciente.setCns("123456789012345");
        return paciente;
    }
    
    /**
     * Cria uma entrada de fila
     */
    private QueueEntry criarQueueEntry(Patient paciente, ERiskColor riskColor, LocalDateTime registeredAt) {
        QueueEntry queueEntry = new QueueEntry();
        queueEntry.setId(UUID.randomUUID());
        queueEntry.setPatient(paciente);
        queueEntry.setRiskColor(riskColor);
        queueEntry.setRegisteredAt(registeredAt);
        queueEntry.setUpdatedAt(registeredAt);
        return queueEntry;
    }
}
