package com.retailer.rewards.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.retailer.rewards.model.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryRewardsDataRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void loadsSeedDataSet() {
        var repository = new InMemoryRewardsDataRepository(new ClassPathResource("data/transactions.json"), objectMapper);

        assertThat(repository.findAllCustomers()).hasSize(5);
        assertThat(repository.findAllTransactions()).hasSize(19);
        assertThat(repository.findTransactionsBetween(LocalDate.of(2026, 7, 31), LocalDate.of(2026, 8, 1)))
                .extracting("id")
                .containsExactly("T1012", "T1013");
    }

    @Test
    void savesTransactionWithGeneratedId() {
        var repository = seededRepository();

        Transaction saved = repository.saveTransaction(
                new Transaction(null, 5, new BigDecimal("120.00"), LocalDate.of(2026, 9, 20)));

        assertThat(saved.id()).isEqualTo("T1020"); // continues after the last seed id, T1019
        assertThat(repository.findTransactionById(saved.id())).contains(saved);
        assertThat(repository.findTransactionsByCustomerId(5))
                .extracting(Transaction::id)
                .containsExactly("T1017", "T1018", "T1019", "T1020"); // sorted by date
        assertThat(repository.findAllTransactions()).hasSize(20);
    }

    @Test
    void rejectsSavingTransactionForUnknownCustomer() {
        var repository = seededRepository();

        assertThatThrownBy(() -> repository.saveTransaction(
                new Transaction(null, 99, BigDecimal.TEN, LocalDate.of(2026, 9, 1))))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.findAllTransactions()).hasSize(19);
    }

    @Test
    void concurrentSavesGetUniqueIds() {
        var repository = seededRepository();

        Set<String> ids = IntStream.range(0, 500).parallel()
                .mapToObj(i -> repository.saveTransaction(
                        new Transaction(null, 1, BigDecimal.ONE, LocalDate.of(2026, 9, 1))).id())
                .collect(Collectors.toSet());

        assertThat(ids).hasSize(500);
        assertThat(repository.findAllTransactions()).hasSize(519);
    }

    private InMemoryRewardsDataRepository seededRepository() {
        return new InMemoryRewardsDataRepository(new ClassPathResource("data/transactions.json"), objectMapper);
    }

    @Test
    void startsIdSequenceAtT1001WithoutSeedTransactions() {
        var repository = load("""
                {"customers": [{"id": 1, "name": "A"}], "transactions": []}
                """);

        Transaction saved = repository.saveTransaction(new Transaction(null, 1, BigDecimal.TEN, LocalDate.of(2026, 9, 1)));

        assertThat(saved.id()).isEqualTo("T1001");
    }

    @Test
    void rejectsSeedIdInWrongFormat() {
        assertThatThrownBy(() -> load("""
                {"customers": [{"id": 1, "name": "A"}],
                 "transactions": [{"id": "X-1", "customerId": 1, "amount": 5, "date": "2026-01-01"}]}
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must look like T1001");
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> load("""
                {"customers": [{"id": 1, "name": "A"}],
                 "transactions": [{"id": "T1", "customerId": 1, "amount": -5, "date": "2026-01-01"}]}
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invalid amount");
    }

    @Test
    void rejectsUnknownCustomer() {
        assertThatThrownBy(() -> load("""
                {"customers": [{"id": 1, "name": "A"}],
                 "transactions": [{"id": "T1", "customerId": 2, "amount": 5, "date": "2026-01-01"}]}
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unknown customer");
    }

    @Test
    void rejectsDuplicateTransactionId() {
        assertThatThrownBy(() -> load("""
                {"customers": [{"id": 1, "name": "A"}],
                 "transactions": [{"id": "T1", "customerId": 1, "amount": 5, "date": "2026-01-01"},
                                  {"id": "T1", "customerId": 1, "amount": 6, "date": "2026-01-02"}]}
                """))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("duplicate transaction id");
    }

    private InMemoryRewardsDataRepository load(String json) {
        return new InMemoryRewardsDataRepository(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)), objectMapper);
    }
}
