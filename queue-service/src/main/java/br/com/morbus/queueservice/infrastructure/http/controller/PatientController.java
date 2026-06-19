package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.entity.Procedure;
import br.com.morbus.queueservice.domain.usecase.AssignProcedureToPatient;
import br.com.morbus.queueservice.domain.usecase.GetPatientByCpf;
import br.com.morbus.queueservice.domain.usecase.GetPatientById;
import br.com.morbus.queueservice.domain.usecase.InactivatePatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatient;
import br.com.morbus.queueservice.domain.usecase.RemoveProcedureFromPatient;
import br.com.morbus.queueservice.domain.usecase.UpdatePatient;
import br.com.morbus.queueservice.domain.usecase.dto.IdProcedureAndPatientDTO;
import br.com.morbus.queueservice.domain.usecase.dto.PatientRequestDTO;
import br.com.morbus.queueservice.domain.usecase.dto.PatientResponseDTO;
import br.com.morbus.queueservice.domain.usecase.dto.ProcedureResponseDTO;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;
import br.com.morbus.queueservice.domain.usecase.dto.UpdatePatientDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Endpoints para gerenciamento de pacientes")
@PreAuthorize("hasRole('MEDICO')")
public class PatientController {

    private final RegisterPatient registerPatient;
    private final GetPatientById getPatientById;
    private final GetPatientByCpf getPatientByCpf;
    private final UpdatePatient updatePatient;
    private final InactivatePatient inactivatePatient;
    private final AssignProcedureToPatient assignProcedureToPatient;
    private final RemoveProcedureFromPatient removeProcedureFromPatient;

    public PatientController(RegisterPatient registerPatient,
                             GetPatientById getPatientById,
                             GetPatientByCpf getPatientByCpf,
                             UpdatePatient updatePatient,
                             InactivatePatient inactivatePatient,
                             AssignProcedureToPatient assignProcedureToPatient,
                             RemoveProcedureFromPatient removeProcedureFromPatient) {
        this.registerPatient = registerPatient;
        this.getPatientById = getPatientById;
        this.getPatientByCpf = getPatientByCpf;
        this.updatePatient = updatePatient;
        this.inactivatePatient = inactivatePatient;
        this.assignProcedureToPatient = assignProcedureToPatient;
        this.removeProcedureFromPatient = removeProcedureFromPatient;
    }

    @PostMapping
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Cadastra novo paciente",
            description = "Cria um novo registro de paciente no sistema.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterPatientDTO.class),
                            examples = @ExampleObject(
                                    name = "Exemplo",
                                    value = """
                                            {
                                              "cpf": "123.456.789-09",
                                              "cns": "123456789012345",
                                              "nome": "Maria",
                                              "sobrenome": "Silva",
                                              "dataNascimento": "1990-05-15",
                                              "gender": "FEMININO",
                                              "contato": "maria.silva@email.com",
                                              "grupoLegal": "GERAL"
                                            }"""
                            )
                    )
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente criado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos (CPF malformado, campos obrigatórios ausentes)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_MEDICO)", content = @Content),
            @ApiResponse(responseCode = "409", description = "CPF já cadastrado no sistema", content = @Content)
    })
    public ResponseEntity<PatientResponseDTO> registerPatient(@RequestBody @Valid RegisterPatientDTO request) {
        Patient patient = registerPatient.run(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientResponseDTO.fromEntity(patient));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Busca paciente por ID",
            description = "Retorna os detalhes de um paciente através do seu UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_MEDICO)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado", content = @Content)
    })
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable UUID id) {
        Patient patient = getPatientById.run(id);
        return ResponseEntity.ok(PatientResponseDTO.fromEntity(patient));
    }

    @GetMapping(params = "cpf")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Busca paciente por CPF",
            description = "Localiza um paciente utilizando o número do CPF.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_MEDICO)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado", content = @Content)
    })
    public ResponseEntity<PatientResponseDTO> getPatientByCpf(@RequestParam String cpf) {
        Patient patient = getPatientByCpf.run(cpf);
        return ResponseEntity.ok(PatientResponseDTO.fromEntity(patient));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Atualiza dados do paciente",
            description = "Permite a edição de campos como nome, CNS, contato, etc.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = PatientResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_MEDICO)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado", content = @Content)
    })
    public ResponseEntity<PatientResponseDTO> updatePatient(@PathVariable UUID id,
                                                            @RequestBody @Valid PatientRequestDTO request) {
        UpdatePatientDTO updateDTO = new UpdatePatientDTO(
                id,
                request.cns(),
                request.nome(),
                request.sobrenome(),
                request.dataNascimento(),
                request.gender(),
                request.contato(),
                request.grupoLegal()
        );
        Patient updated = updatePatient.run(updateDTO);
        return ResponseEntity.ok(PatientResponseDTO.fromEntity(updated));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Inativa um paciente",
            description = "Altera o status do paciente para inativo. Bloqueado se houver entradas ativas na fila.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente inativado com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_MEDICO)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente não encontrado", content = @Content),
            @ApiResponse(responseCode = "422", description = "Paciente possui entradas ativas na fila", content = @Content)
    })
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        inactivatePatient.run(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{patientId}/procedures/{procedureId}")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Atribui procedimento ao paciente",
            description = "Vincula um procedimento SUS a um paciente, verificando elegibilidade de idade e status ativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Procedimento atribuído com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProcedureResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_MEDICO)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente ou procedimento não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Procedimento já atribuído ao paciente", content = @Content),
            @ApiResponse(responseCode = "422", description = "Paciente inativo ou fora da faixa etária do procedimento", content = @Content)
    })
    public ResponseEntity<ProcedureResponseDTO> assignProcedure(@PathVariable UUID patientId,
                                                                 @PathVariable UUID procedureId) {
        IdProcedureAndPatientDTO dto = new IdProcedureAndPatientDTO(patientId, procedureId);
        Procedure procedure = assignProcedureToPatient.run(dto);
        return ResponseEntity.ok(ProcedureResponseDTO.fromEntity(procedure));
    }

    @DeleteMapping("/{patientId}/procedures/{procedureId}")
    @PreAuthorize("hasRole('MEDICO')")
    @Operation(
            summary = "Remove procedimento do paciente",
            description = "Desvincula um procedimento SUS do paciente. Bloqueia remoção se houver entradas ativas na fila.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Procedimento removido com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "403", description = "Acesso negado (requer ROLE_MEDICO)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Paciente ou procedimento não encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "Existem entradas ativas na fila para este procedimento", content = @Content)
    })
    public ResponseEntity<Void> removeProcedure(@PathVariable UUID patientId,
                                                 @PathVariable UUID procedureId) {
        IdProcedureAndPatientDTO dto = new IdProcedureAndPatientDTO(patientId, procedureId);
        removeProcedureFromPatient.run(dto);
        return ResponseEntity.noContent().build();
    }
}
