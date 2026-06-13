package br.com.morbus.queueservice.infrastructure.http.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/procedures")
@Tag(name = "Procedures", description = "Endpoints para gerenciamento de procedimentos")
public class ProcedureController {
}
