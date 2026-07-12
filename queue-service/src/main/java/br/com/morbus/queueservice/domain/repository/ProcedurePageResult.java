package br.com.morbus.queueservice.domain.repository;

import br.com.morbus.queueservice.domain.entity.Procedure;

import java.util.List;

public record ProcedurePageResult(List<Procedure> content, long totalElements) {
}
