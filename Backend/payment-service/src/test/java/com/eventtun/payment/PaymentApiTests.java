package com.eventtun.payment;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Scénarios de la fiche technique (rubrique 15), testés sur l'API réelle avec H2.
 * Chaque test utilise son propre orderId pour rester indépendant des autres.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentApiTests {

    @Autowired
    private MockMvc mvc;

    private ResultActions pay(long orderId, String amount) throws Exception {
        return mvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderId\":" + orderId + ",\"amount\":" + amount + ",\"method\":\"CARD\"}"));
    }

    private ResultActions refund(long paymentId, String amount) throws Exception {
        return mvc.perform(post("/api/payments/" + paymentId + "/refunds")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"amount\":" + amount + ",\"reason\":\"Echec de l'emission des billets\"}"));
    }

    private long idOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    @DisplayName("1. Paiement accepté : 201 et statut SUCCESS")
    void paymentAccepted() throws Exception {
        pay(1001, "90.00")
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("2. Rejeu identique : 200, même paiement, pas de second débit")
    void identicalReplay() throws Exception {
        long id = idOf(pay(1002, "90.00").andExpect(status().isCreated()));

        pay(1002, "90.00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id));

        mvc.perform(get("/api/payments").param("orderId", "1002"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("3. Paiement refusé : 409, enregistré FAILED, rejeu toujours 409")
    void paymentRefused() throws Exception {
        pay(1003, "800.00")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Paiement refusé"))
            .andExpect(jsonPath("$.paymentStatus").value("FAILED"));

        pay(1003, "800.00").andExpect(status().isConflict());

        mvc.perform(get("/api/payments").param("orderId", "1003"))
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].status").value("FAILED"));
    }

    @Test
    @DisplayName("4. Même commande, montant différent : 409")
    void differentAmountSameOrder() throws Exception {
        pay(1004, "90.00").andExpect(status().isCreated());
        pay(1004, "50.00").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("5. Remboursement : 201, paiement REFUNDED, rejeu 200")
    void refundPayment() throws Exception {
        long paymentId = idOf(pay(1005, "90.00"));

        long refundId = idOf(refund(paymentId, "90.00")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED")));

        mvc.perform(get("/api/payments/" + paymentId))
            .andExpect(jsonPath("$.status").value("REFUNDED"));

        refund(paymentId, "90.00")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(refundId));

        mvc.perform(get("/api/payments/" + paymentId + "/refunds"))
            .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("6. Remboursement supérieur au montant payé : 400")
    void refundTooHigh() throws Exception {
        long paymentId = idOf(pay(1006, "90.00"));
        refund(paymentId, "200.00").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("7. Remboursement d'un paiement refusé : 409")
    void refundFailedPayment() throws Exception {
        String body = pay(1007, "800.00").andReturn().getResponse().getContentAsString();
        long paymentId = ((Number) JsonPath.read(body, "$.paymentId")).longValue();
        refund(paymentId, "800.00").andExpect(status().isConflict());
    }

    @Test
    @DisplayName("8. Paiement inexistant : 404")
    void unknownPayment() throws Exception {
        mvc.perform(get("/api/payments/999999")).andExpect(status().isNotFound());
        refund(999999, "10.00").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("9. Requête invalide (montant négatif) : 400")
    void invalidRequest() throws Exception {
        pay(1009, "-5").andExpect(status().isBadRequest());
    }
}
