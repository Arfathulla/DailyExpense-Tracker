package com.expensetracker.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** If the app context starts at all, Hibernate's schema validation already passed against the migrated schema. */
class FlywayMigrationIT extends AbstractMySqlIT {
    @Autowired JdbcTemplate jdbc;

    @Test
    void baselineMigrationIsApplied() {
        String state = jdbc.queryForObject(
                "select success from flyway_schema_history where version = '1'", String.class);
        assertThat(state).isIn("1", "true");
    }

    @Test
    void allTablesAndTheDateIndexExist() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = database()", String.class)
                .stream().map(String::toLowerCase).toList();
        assertThat(tables).contains("users", "categories", "transactions", "budgets", "recurring_transactions");

        Integer idx = jdbc.queryForObject(
                "select count(*) from information_schema.statistics where table_schema = database() " +
                "and table_name = 'transactions' and index_name = 'idx_txn_user_date'", Integer.class);
        assertThat(idx).isEqualTo(2); // two columns: user_id, txn_date
    }

    @Test
    void checkConstraintRejectsUnknownTransactionType() {
        jdbc.update("insert into users(name,email,password_hash) values ('c','check@x.com','h')");
        Long uid = jdbc.queryForObject("select id from users where email='check@x.com'", Long.class);
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class,
                () -> jdbc.update("insert into categories(user_id,name,type) values (?, 'Bad', 'NOPE')", uid));
    }
}
