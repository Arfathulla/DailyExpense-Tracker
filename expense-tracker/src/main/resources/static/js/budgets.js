import { api } from './api.js';
import { initShell, money, esc, toast, currentMonth } from './ui.js';

initShell('budgets');
const $ = (id) => document.getElementById(id);
$('month').value = currentMonth();

const STATUS_TEXT = { OK: 'On track', WARNING: 'Nearly at limit', EXCEEDED: 'Over budget' };

async function load() {
  const m = $('month').value || currentMonth();
  try {
    const list = await api('/budgets?month=' + m);
    $('list').innerHTML = list.length ? list.map((b) => `
      <div class="budget"><div class="budget-top"><strong>${esc(b.categoryName)}</strong>
        <span class="row"><span class="pill ${b.status}">${STATUS_TEXT[b.status]}</span>
        <button class="btn ghost sm" data-act="edit" data-cat="${b.categoryId}" data-limit="${b.limitAmount}">Edit</button>
        <button class="btn danger sm" data-act="del" data-id="${b.id}">Remove</button></span></div>
        <div class="bar ${b.status}"><span style="width:${Math.min(b.percentUsed, 100)}%"></span></div>
        <span class="muted num">${money(b.spent)} of ${money(b.limitAmount)} · ${b.percentUsed}% used</span></div>`).join('')
      : '<p class="empty">No budgets for this month yet. Pick a category and a limit to add one.</p>';
  } catch (e) { toast(e.message, 'error'); }
}

$('list').addEventListener('click', async (e) => {
  const b = e.target.closest('button[data-act]');
  if (!b) return;
  if (b.dataset.act === 'edit') {
    $('cat').value = b.dataset.cat;
    $('limit').value = b.dataset.limit;
    $('limit').focus();
    return;
  }
  if (!confirm('Remove this budget?')) return;
  try { await api('/budgets/' + b.dataset.id, { method: 'DELETE' }); toast('Budget removed'); load(); }
  catch (err) { toast(err.message, 'error'); }
});

$('form').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('error').textContent = '';
  try {
    await api('/budgets', { method: 'POST', body: { categoryId: Number($('cat').value), month: $('month').value || currentMonth(), limitAmount: $('limit').value } });
    toast('Budget saved');
    $('limit').value = '';
    load();
  } catch (err) { $('error').textContent = err.message; }
});

$('month').addEventListener('change', load);

(async function init() {
  try {
    const cats = (await api('/categories')).filter((c) => c.type === 'EXPENSE');
    $('cat').innerHTML = cats.map((c) => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
  } catch (e) { toast(e.message, 'error'); }
  load();
})();
