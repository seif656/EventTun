package com.eventtun.ticket.service;

import com.eventtun.ticket.model.Ticket;

import java.util.List;

/** Resultat de l'emission : created = true (201) pour une premiere emission, false (200) pour un rejeu identique. */
public record IssueResult(List<Ticket> tickets, boolean created) {
}
