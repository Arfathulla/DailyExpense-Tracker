package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TxnType;
import com.expensetracker.exception.BadRequestException;
import com.expensetracker.exception.NotFoundException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private static final Set<String> SORTABLE = Set.of("txnDate", "amount", "createdAt");

    private final TransactionRepository txns;
    private final CategoryRepository categories;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(Long uid, LocalDate from, LocalDate to, Long categoryId,
                                                    TxnType type, String q, int page, int size,
                                                    String sortBy, String dir) {
        String field = SORTABLE.contains(sortBy) ? sortBy : "txnDate";
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, field).and(Sort.by(Sort.Direction.DESC, "id"));
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sort);

        Page<Transaction> result = txns.findAll(spec(uid, from, to, categoryId, type, q), pageable);
        return new PageResponse<>(result.getContent().stream().map(TransactionService::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public TransactionResponse create(Long uid, TransactionRequest r) {
        Category c = ownedCategory(uid, r.categoryId(), r.type());
        Transaction t = new Transaction();
        t.setUser(users.getReferenceById(uid));
        apply(t, r, c);
        return toResponse(txns.save(t));
    }

    @Transactional
    public TransactionResponse update(Long uid, Long id, TransactionRequest r) {
        Transaction t = txns.findByIdAndUserId(id, uid).orElseThrow(() -> new NotFoundException("Transaction not found"));
        apply(t, r, ownedCategory(uid, r.categoryId(), r.type()));
        return toResponse(txns.save(t));
    }

    @Transactional
    public void delete(Long uid, Long id) {
        Transaction t = txns.findByIdAndUserId(id, uid).orElseThrow(() -> new NotFoundException("Transaction not found"));
        txns.delete(t);
    }

    @Transactional(readOnly = true)
    public String exportCsv(Long uid, LocalDate from, LocalDate to, Long categoryId, TxnType type, String q) {
        List<Transaction> list = txns.findAll(spec(uid, from, to, categoryId, type, q),
                Sort.by(Sort.Direction.DESC, "txnDate"));
        StringBuilder sb = new StringBuilder("Date,Type,Category,Amount,Note\n");
        for (Transaction t : list) {
            sb.append(t.getTxnDate()).append(',')
              .append(t.getType()).append(',')
              .append(csv(t.getCategory().getName())).append(',')
              .append(t.getAmount().toPlainString()).append(',')
              .append(csv(t.getNote())).append('\n');
        }
        return sb.toString();
    }

    // ---- helpers ----

    private Specification<Transaction> spec(Long uid, LocalDate from, LocalDate to, Long categoryId,
                                            TxnType type, String q) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.equal(root.get("user").get("id"), uid)); // every query is scoped to the logged-in user
            if (from != null) p.add(cb.greaterThanOrEqualTo(root.get("txnDate"), from));
            if (to != null) p.add(cb.lessThanOrEqualTo(root.get("txnDate"), to));
            if (categoryId != null) p.add(cb.equal(root.get("category").get("id"), categoryId));
            if (type != null) p.add(cb.equal(root.get("type"), type));
            if (q != null && !q.isBlank()) p.add(cb.like(cb.lower(root.get("note")), "%" + q.trim().toLowerCase() + "%"));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private Category ownedCategory(Long uid, Long categoryId, TxnType type) {
        Category c = categories.findByIdAndUserId(categoryId, uid)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        if (c.getType() != type) throw new BadRequestException("Category type does not match the transaction type");
        return c;
    }

    private void apply(Transaction t, TransactionRequest r, Category c) {
        t.setCategory(c);
        t.setAmount(r.amount());
        t.setType(r.type());
        t.setNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        t.setTxnDate(r.txnDate());
    }

    /** Quotes CSV fields and neutralises spreadsheet formula injection (=, +, -, @). */
    private static String csv(String v) {
        if (v == null) return "";
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0) v = "'" + v;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) v = "\"" + v.replace("\"", "\"\"") + "\"";
        return v;
    }

    static TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getCategory().getId(), t.getCategory().getName(),
                t.getAmount(), t.getType(), t.getNote(), t.getTxnDate());
    }
}
