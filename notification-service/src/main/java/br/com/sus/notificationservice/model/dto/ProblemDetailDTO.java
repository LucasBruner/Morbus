package br.com.sus.notificationservice.model.dto;

public record ProblemDetailDTO(
        String type,
        String title,
        String detail,
        int status,
        String instance) {

    private static final String TYPE_BASE = "https://morbus.sus.gov.br/problems/";

    public static ProblemDetailDTO of(String slug, String title, String detail, int status, String instance) {
        return new ProblemDetailDTO(TYPE_BASE + slug, title, detail, status, instance);
    }
}
