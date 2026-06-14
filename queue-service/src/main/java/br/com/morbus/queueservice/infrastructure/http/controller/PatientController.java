package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.entity.Patient;
import br.com.morbus.queueservice.domain.usecase.GetPatientByCpf;
import br.com.morbus.queueservice.domain.usecase.GetPatientById;
import br.com.morbus.queueservice.domain.usecase.InactivatePatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatient;
import br.com.morbus.queueservice.domain.usecase.UpdatePatient;
import br.com.morbus.queueservice.domain.usecase.dto.RegisterPatientDTO;
import br.com.morbus.queueservice.domain.usecase.dto.UpdatePatientDTO;
import br.com.morbus.queueservice.domain.usecase.dto.PatientRequestDTO;
import br.com.morbus.queueservice.domain.usecase.dto.PatientResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class PatientController {

    private final RegisterPatient registerPatient;
    private final GetPatientById getPatientById;
    private final GetPatientByCpf getPatientByCpf;
    private final UpdatePatient updatePatient;
    private final InactivatePatient inactivatePatient;

    public PatientController(RegisterPatient registerPatient,
                             GetPatientById getPatientById,
                             GetPatientByCpf getPatientByCpf,
                             UpdatePatient updatePatient,
                             InactivatePatient inactivatePatient) {
        this.registerPatient = registerPatient;
        this.getPatientById = getPatientById;
        this.getPatientByCpf = getPatientByCpf;
        this.updatePatient = updatePatient;
        this.inactivatePatient = inactivatePatient;
    }

    @PostMapping
    @Operation(
            summary = "Cadastra novo paciente",
            description = "Cria um novo registro de paciente no sistema.",
            responses = {
                    @ApiResponse(description = "Criado com sucesso", responseCode = "201"),
                    @ApiResponse(description = "CPF já cadastrado", responseCode = "409")})
    public ResponseEntity<PatientResponseDTO> registerPatient(@RequestBody @Valid RegisterPatientDTO request) {
        Patient patient = registerPatient.run(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PatientResponseDTO.fromEntity(patient));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca paciente por ID",
            description = "Retorna os detalhes de um paciente através do seu UUID.")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    public ResponseEntity<PatientResponseDTO> getPatientById(@PathVariable UUID id) {
        Patient patient = getPatientById.run(id);
        return ResponseEntity.ok(PatientResponseDTO.fromEntity(patient));
    }

    @GetMapping(params = "cpf")
    @Operation(
            summary = "Busca paciente por CPF",
            description = "Localiza um paciente utilizando o número do CPF.")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    public ResponseEntity<PatientResponseDTO> getPatientByCpf(@RequestParam String cpf) {
        Patient patient = getPatientByCpf.run(cpf);
        return ResponseEntity.ok(PatientResponseDTO.fromEntity(patient));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Atualiza dados do paciente",
            description = "Permite a edição de campos como nome, CNS, contato, etc.")
    @ApiResponse(responseCode = "200", description = "Paciente atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
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
    @Operation(
            summary = "Inativa um paciente",
            description = "Altera o status do paciente para inativo.")
    @ApiResponse(responseCode = "204", description = "Paciente inativado com sucesso")
    @ApiResponse(responseCode = "404", description = "Paciente não encontrado")
    public ResponseEntity<Void> inactivate(@PathVariable UUID id) {
        inactivatePatient.run(id);
        return ResponseEntity.noContent().build();
    }
}
