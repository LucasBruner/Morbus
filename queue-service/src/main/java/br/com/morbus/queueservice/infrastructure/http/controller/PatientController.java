package br.com.morbus.queueservice.infrastructure.http.controller;

import br.com.morbus.queueservice.domain.usecase.InactivatePatient;
import br.com.morbus.queueservice.domain.usecase.RegisterPatient;
import br.com.morbus.queueservice.domain.usecase.UpdatePatient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/patients")
@Tag(name = "Patients", description = "Endpoints para gerenciamento de pacientes")
public class PatientController {

    private final RegisterPatient registerPatient;
    private final UpdatePatient updatePatient;
    private final InactivatePatient inactivatePatient;

    public PatientController(RegisterPatient registerPatient, UpdatePatient updatePatient, InactivatePatient inactivatePatient) {
        this.registerPatient = registerPatient;
        this.updatePatient = updatePatient;
        this.inactivatePatient = inactivatePatient;
    }

}
