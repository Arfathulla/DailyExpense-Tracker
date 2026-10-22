package com.expensetracker.mail;

import com.expensetracker.dto.Dtos.CategoryTotal;
import com.expensetracker.dto.Dtos.Summary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

/** Builds the monthly summary email. Pure formatting, no I/O, so it is easy to unit test. */
@Component
public class SummaryEmailComposer {
    public record Composed(String subject, String text, String html) {}

    private final String currency;

    public SummaryEmailComposer(@Value("${app.mail.currency-symbol:₹}") String currency) {
        this.currency = currency;
    }

    public Composed compose(String userName, YearMonth month, Summary s, List<CategoryTotal> topExpenses) {
        String label = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
        String subject = "Your " + label + " spending summary";

        StringBuilder text = new StringBuilder();
        text.append("Hi ").append(userName).append(",\n\nHere is your summary for ").append(label).append(":\n\n")
            .append("  Income:  ").append(money(s.income())).append('\n')
            .append("  Expense: ").append(money(s.expense())).append('\n')
            .append("  Balance: ").append(money(s.balance())).append('\n');
        if (!topExpenses.isEmpty()) {
            text.append("\nWhere the money went:\n");
            topExpenses.forEach(c -> text.append("  ").append(c.category()).append(": ").append(money(c.total())).append('\n'));
        }
        text.append("\n- SpendLedger\n");

        StringBuilder html = new StringBuilder();
        html.append("<div style=\"font-family:Arial,sans-serif;max-width:480px\">")
            .append("<p>Hi ").append(esc(userName)).append(",</p>")
            .append("<p>Here is your summary for <b>").append(esc(label)).append("</b>:</p>")
            .append("<table cellpadding=\"4\">")
            .append(row("Income", money(s.income())))
            .append(row("Expense", money(s.expense())))
            .append(row("Balance", money(s.balance())))
            .append("</table>");
        if (!topExpenses.isEmpty()) {
            html.append("<p>Where the money went:</p><table cellpadding=\"4\">");
            topExpenses.forEach(c -> html.append(row(c.category(), money(c.total()))));
            html.append("</table>");
        }
        html.append("<p>- SpendLedger</p></div>");
        return new Composed(subject, text.toString(), html.toString());
    }

    private String money(BigDecimal v) {
        return (v.signum() < 0 ? "-" : "") + currency + String.format(Locale.US, "%,.2f", v.abs());
    }

    private static String row(String k, String v) {
        return "<tr><td>" + esc(k) + "</td><td align=\"right\"><b>" + esc(v) + "</b></td></tr>";
    }

    /** User-controlled text (name, category names) must never reach the HTML unescaped. */
    private static String esc(String s) { return HtmlUtils.htmlEscape(s); }
}
