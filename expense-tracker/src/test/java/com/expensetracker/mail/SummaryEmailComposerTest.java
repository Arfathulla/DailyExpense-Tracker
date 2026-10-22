package com.expensetracker.mail;

import com.expensetracker.dto.Dtos.CategoryTotal;
import com.expensetracker.dto.Dtos.Summary;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryEmailComposerTest {
    private final SummaryEmailComposer composer = new SummaryEmailComposer("₹");

    @Test
    void formatsSubjectAmountsAndCategories() {
        var m = composer.compose("Asha", YearMonth.of(2026, 9),
                new Summary(new BigDecimal("85000"), new BigDecimal("1234.5"), new BigDecimal("83765.5")),
                List.of(new CategoryTotal("Food", new BigDecimal("1000.00"))));

        assertThat(m.subject()).isEqualTo("Your September 2026 spending summary");
        assertThat(m.text()).contains("Hi Asha", "₹85,000.00", "₹1,234.50", "₹83,765.50", "Food: ₹1,000.00");
        assertThat(m.html()).contains("<b>September 2026</b>", "₹1,234.50", "Food");
    }

    @Test
    void negativeBalanceKeepsSignBeforeCurrency() {
        var m = composer.compose("A", YearMonth.of(2026, 9),
                new Summary(BigDecimal.ZERO, new BigDecimal("10"), new BigDecimal("-10")), List.of());
        assertThat(m.text()).contains("Balance: -₹10.00").doesNotContain("Where the money went");
    }

    @Test
    void htmlEscapesUserControlledText() {
        var m = composer.compose("<script>alert(1)</script>", YearMonth.of(2026, 9),
                new Summary(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO),
                List.of(new CategoryTotal("<img src=x onerror=1>", BigDecimal.ONE)));
        assertThat(m.html()).doesNotContain("<script>", "<img").contains("&lt;script&gt;", "&lt;img");
    }
}
