package com.eventtun.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Format d'erreur commun a l'equipe (Problem Details, application/problem+json).
 * Utilise ici pour documenter les reponses d'erreur dans Swagger.
 */
@Schema(description = "Erreur au format Problem Details (RFC 7807)")
public class ProblemDetailResponse {

    @Schema(example = "about:blank")
    private String type;
    @Schema(example = "Conflict")
    private String title;
    @Schema(example = "409")
    private int status;
    @Schema(example = "Le billet a deja ete utilise")
    private String detail;
    @Schema(example = "/api/tickets/665f1c2e9a1b2c3d4e5f6a7b/validations")
    private String instance;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getInstance() { return instance; }
    public void setInstance(String instance) { this.instance = instance; }
}
