-- Queries worth being able to explain in an interview.
-- (The app itself uses JPQL/Specifications; these are the plain-SQL equivalents.)

-- 1. Spending by category for a month (pie chart)
SELECT c.name, SUM(t.amount) AS total
FROM transactions t
JOIN categories c ON c.id = t.category_id
WHERE t.user_id = ? AND t.type = 'EXPENSE' AND t.txn_date BETWEEN ? AND ?
GROUP BY c.name
ORDER BY total DESC;

-- 2. Budget vs actual, with percent used
SELECT c.name, b.limit_amount,
       COALESCE(SUM(t.amount), 0) AS spent,
       ROUND(COALESCE(SUM(t.amount), 0) / b.limit_amount * 100, 1) AS pct_used
FROM budgets b
JOIN categories c ON c.id = b.category_id
LEFT JOIN transactions t
       ON t.category_id = b.category_id AND t.user_id = b.user_id AND t.type = 'EXPENSE'
      AND DATE_FORMAT(t.txn_date, '%Y-%m') = b.budget_month
WHERE b.user_id = ? AND b.budget_month = ?
GROUP BY c.name, b.limit_amount;

-- 3. Running balance (window function)
SELECT txn_date, amount, type,
       SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END)
         OVER (ORDER BY txn_date, id) AS running_balance
FROM transactions
WHERE user_id = ?;

-- 4. Income vs expense per month (bar chart)
SELECT DATE_FORMAT(txn_date, '%Y-%m') AS month,
       SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS income,
       SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expense
FROM transactions
WHERE user_id = ? AND txn_date >= ?
GROUP BY DATE_FORMAT(txn_date, '%Y-%m')
ORDER BY month;

-- 5. Month-over-month change in spending per category (LAG)
SELECT category, month, total,
       total - LAG(total) OVER (PARTITION BY category ORDER BY month) AS change_vs_prev
FROM (
  SELECT c.name AS category, DATE_FORMAT(t.txn_date, '%Y-%m') AS month, SUM(t.amount) AS total
  FROM transactions t JOIN categories c ON c.id = t.category_id
  WHERE t.user_id = ? AND t.type = 'EXPENSE'
  GROUP BY c.name, DATE_FORMAT(t.txn_date, '%Y-%m')
) m;

-- 6. Top 5 biggest expenses this month
SELECT txn_date, amount, note FROM transactions
WHERE user_id = ? AND type = 'EXPENSE' AND txn_date BETWEEN ? AND ?
ORDER BY amount DESC LIMIT 5;
