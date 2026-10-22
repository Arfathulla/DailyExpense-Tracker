import { api } from './api.js';
import { initShell, money, esc, toast } from './ui.js';

initShell('manage');
const $ = (id) => document.getElementById(id);
let categories = [];

const chip = (c) => `<span class="chip">${esc(c.name)}<button type="button" data-id="${c.id}" aria-label="Delete ${esc(c.name)}">×</button></span>`;

async function loadCategories() {
  categories = await api('/categories');
  $('cats-exp').innerHTML = categories.filter((c) => c.type === 'EXPENSE').map(chip).join('') || '<span class="muted">None yet</span>';
  $('cats-inc').innerHTML = categories.filter((c) => c.type === 'INCOME').map(chip).join('') || '<span class="muted">None yet</span>';
  fillRecurringCategories();
}

function fillRecurringCategories() {
  $('r-cat').innerHTML = categories.filter((c) => c.type === $('r-type').value)
    .map((c) => `<option value="${c.id}">${esc(c.name)}</option>`).join('');
}

async function loadRecurring() {
  const list = await api('/recurring');
  $('rec-list').innerHTML = list.length ? list.map((r) => `
    <div class="list-row"><span><strong>${esc(r.categoryName)}</strong> <span class="muted">· day ${r.dayOfMonth} of each month${r.note ? ' · ' + esc(r.note) : ''}</span></span>
      <span class="row"><strong class="num ${r.type === 'INCOME' ? 'income' : 'expense'}">${money(r.amount)}</strong>
      <button class="btn danger sm" data-rec="${r.id}">Remove</button></span></div>`).join('')
    : '<p class="empty">No recurring items yet. Rent, salary and subscriptions are good candidates.</p>';
}

async function refresh() {
  try { await loadCategories(); await loadRecurring(); } catch (e) { toast(e.message, 'error'); }
}

$('cat-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('cat-error').textContent = '';
  try {
    await api('/categories', { method: 'POST', body: { name: $('cat-name').value, type: $('cat-type').value } });
    $('cat-name').value = '';
    toast('Category added');
    loadCategories();
  } catch (err) { $('cat-error').textContent = err.message; }
});

document.addEventListener('click', async (e) => {
  const catBtn = e.target.closest('.chip button');
  const recBtn = e.target.closest('button[data-rec]');
  try {
    if (catBtn) {
      if (!confirm('Delete this category?')) return;
      await api('/categories/' + catBtn.dataset.id, { method: 'DELETE' });
      toast('Category deleted');
      loadCategories();
    } else if (recBtn) {
      if (!confirm('Remove this recurring item? Transactions it already created stay.')) return;
      await api('/recurring/' + recBtn.dataset.rec, { method: 'DELETE' });
      toast('Recurring item removed');
      loadRecurring();
    }
  } catch (err) {
    toast(err.message.includes('in use') ? 'This category is used by transactions or budgets, so it cannot be deleted.' : err.message, 'error');
  }
});

$('r-type').addEventListener('change', fillRecurringCategories);

$('rec-form').addEventListener('submit', async (e) => {
  e.preventDefault();
  $('rec-error').textContent = '';
  try {
    await api('/recurring', { method: 'POST', body: { categoryId: Number($('r-cat').value), amount: $('r-amount').value, type: $('r-type').value, dayOfMonth: Number($('r-day').value), note: $('r-note').value } });
    $('r-amount').value = ''; $('r-day').value = ''; $('r-note').value = '';
    toast('Recurring item added');
    loadRecurring();
  } catch (err) { $('rec-error').textContent = err.message; }
});

refresh();
