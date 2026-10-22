package com.expensetracker.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end through the real HTTP layer, security filter, JPA and MySQL. */
class ApiFlowIT extends AbstractMySqlIT {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private String register(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"email\":\"" + email + "\",\"password\":\"secret1\"}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        MvcResult r = mvc.perform(get(url).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(r.getResponse().getContentAsString());
    }

    private long categoryId(String token, String name) throws Exception {
        for (JsonNode c : getJson("/api/categories", token)) if (c.get("name").asText().equals(name)) return c.get("id").asLong();
        throw new AssertionError("category not found: " + name);
    }

    private long addExpense(String token, long cat, String amount) throws Exception {
        MvcResult r = mvc.perform(post("/api/transactions").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":" + cat + ",\"amount\":" + amount + ",\"type\":\"EXPENSE\",\"note\":\"it\",\"txnDate\":\""
                        + LocalDate.now() + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void protectedEndpointsRequireAToken() throws Exception {
        mvc.perform(get("/api/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        String email = UUID.randomUUID() + "@x.com";
        register(email);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Again\",\"email\":\"" + email + "\",\"password\":\"secret1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerLoginAddTransactionsAndSeeAggregates() throws Exception {
        String email = UUID.randomUUID() + "@x.com";
        String token = register(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"secret1\"}")).andExpect(status().isOk());

        long food = categoryId(token, "Food");
        addExpense(token, food, "250.50");
        addExpense(token, food, "100.25");

        JsonNode summary = getJson("/api/analytics/summary", token);
        assertThat(summary.get("expense").decimalValue()).isEqualByComparingTo("350.75");
        assertThat(summary.get("income").decimalValue()).isEqualByComparingTo("0");

        JsonNode byCat = getJson("/api/analytics/by-category", token);
        assertThat(byCat.get(0).get("category").asText()).isEqualTo("Food");
        assertThat(byCat.get(0).get("total").decimalValue()).isEqualByComparingTo("350.75");

        // uses YEAR()/MONTH() + GROUP BY in JPQL: worth proving on real MySQL
        JsonNode trend = getJson("/api/analytics/monthly-trend?months=3", token);
        assertThat(trend).hasSize(3);
        assertThat(trend.get(2).get("month").asText()).isEqualTo(YearMonth.now().toString());
        assertThat(trend.get(2).get("expense").decimalValue()).isEqualByComparingTo("350.75");
    }

    @Test
    void budgetStatusReflectsSpending() throws Exception {
        String token = register(UUID.randomUUID() + "@x.com");
        long food = categoryId(token, "Food");
        mvc.perform(post("/api/budgets").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":" + food + ",\"month\":\"" + YearMonth.now() + "\",\"limitAmount\":1000}"))
                .andExpect(status().isCreated());
        addExpense(token, food, "850");

        JsonNode budgets = getJson("/api/budgets?month=" + YearMonth.now(), token);
        assertThat(budgets).hasSize(1);
        assertThat(budgets.get(0).get("status").asText()).isEqualTo("WARNING");
        addExpense(token, food, "200");
        assertThat(getJson("/api/budgets?month=" + YearMonth.now(), token).get(0).get("status").asText()).isEqualTo("EXCEEDED");
    }

    @Test
    void usersCannotSeeOrChangeEachOthersData() throws Exception {
        String alice = register(UUID.randomUUID() + "@x.com");
        String bob = register(UUID.randomUUID() + "@x.com");
        long txn = addExpense(alice, categoryId(alice, "Food"), "42");

        assertThat(getJson("/api/transactions", bob).get("totalElements").asInt()).isZero();
        mvc.perform(delete("/api/transactions/" + txn).header("Authorization", "Bearer " + bob))
                .andExpect(status().isNotFound());
        assertThat(getJson("/api/transactions", alice).get("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void transactionSearchAndCsvExportWork() throws Exception {
        String token = register(UUID.randomUUID() + "@x.com");
        addExpense(token, categoryId(token, "Travel"), "10");
        assertThat(getJson("/api/transactions?q=it&type=EXPENSE", token).get("content")).hasSize(1);
        MvcResult csv = mvc.perform(get("/api/transactions/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertThat(csv.getResponse().getContentAsString()).contains("Travel");
    }
}
