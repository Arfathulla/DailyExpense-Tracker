import { api, qs } from './api.js';
import { initShell, money, esc, toast, fmtDate, today, debounce, catColor } from './ui.js';

initShell('transactions');
const $ = (id) => document.getElementById(id);
const state = { page: 0, size: 10, sortBy: 'txnDate', dir: 'desc' };
let categories = [];
let rows = new Map();

const filters = () => ({ from: $('f-from').value, to: $('f-to').value, categoryId: $('f-cat').value, type: $('f-type').value, q: $('f-q').value.trim() });

async function load() {
  try {
    const data = await api('/transactions' + qs({ ...filters(), page: state.page, size: state.size, sortBy: state.sortBy, dir: state.dir }));
    rows = new Map(data.content.map((t) => [String(t.id), t]));
    $('rows').innerHTML = data.content.length ? data.content.map((t) => `
      <tr><td class="num">${fmtDate(t.txnDate)}</td><td><span class="cat"><i class="dot" style="background:${catColor(t.categoryName)}"></i>${esc(t.categoryName)}</span></td><td class="muted">${esc(t.note) || '–'}</td>
        <td class="amt num ${t.type === 'INCOME' ? 'income' : 'expense'}">${t.type === 'INCOME' ? '+' : '−'}${money(t.amount)}</td>
        <td style="white-space:nowrap;text-align:right"><button class="btn ghost sm" data-act="edit" data-id="${t.id}">Edit</button>
          <button class="btn danger sm" data-act="del" data-id="${t.id}">Delete</button></td></tr>`).join('')
      : '<tr><td colspan="5" class="empty">No transactions match these filters. Add one to get started.</td></tr>';
    const from = data.totalElements === 0 ? 0 : data.page * data.size + 1;
    $('info').textContent = `${from}–${Math.min((data.page + 1) * data.size, data.totalElements)} of ${data.totalElements}`;
    $('prev').disabled = data.page <= 0;
    $('next').disabled = data.page + 1 >= data.totalPages;
    document.querySelectorAll('th.sort').forEach((th) => {
      const active = th.dataset.field === state.sortBy;
      th.textContent = th.textContent.replace(/ [▲▼]$/, '') + (active ? (state.dir === 'asc' ? ' ▲' : ' ▼') : '');
    });
  } catch (e) { toast(e.message, 'error'); }
}

function fillCategorySelect(select, type, selected) {
  select.innerHTML = categories.filter((c) => c.type === type)
    .map((c) => `<option value="${c.id}" ${String(c.id) === String(selected) ? 'selected' : ''}>${esc(c.name)}</option>`).join('');
}

function openDialog(t) {
  $('d-error').textContent = '';
  $('dlg-title').textContent = t ? 'Edit transaction' : 'Add transaction';
  $('d-id').value = t ? t.id : '';
  $('d-type').value = t ? t.type : 'EXPENSE';
  fillCategorySelect($('d-cat'), $('d-type').value, t?.categoryId);
  $('d-amount').value = t ? t.amount : '';
  $('d-date').value = t ? t.txnDate : today();
  $('d-note').value = t?.note || '';
  $('dlg').showModal();
}

$('d-type').addEventListener('change', () => fillCategorySelect($('d-cat'), $('d-type').value));
$('add').onclick = () => openDialog(null);
$('cancel').onclick = () => $('dlg').close();

$('form').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = $('d-id').value;
  const body = { categoryId: Number($('d-cat').value), amount: $('d-amount').value, type: $('d-type').value, txnDate: $('d-date').value, note: $('d-note').value };
  $('save').disabled = true;
  try {
    await api(id ? '/transactions/' + id : '/transactions', { method: id ? 'PUT' : 'POST', body });
    $('dlg').close();
    toast(id ? 'Transaction updated' : 'Transaction added');
    load();
  } catch (err) { $('d-error').textContent = err.message; }
  finally { $('save').disabled = false; }
});

$('rows').addEventListener('click', async (e) => {
  const btn = e.target.closest('button[data-act]');
  if (!btn) return;
  if (btn.dataset.act === 'edit') return openDialog(rows.get(btn.dataset.id));
  if (!confirm('Delete this transaction? This cannot be undone.')) return;
  try { await api('/transactions/' + btn.dataset.id, { method: 'DELETE' }); toast('Transaction deleted'); load(); }
  catch (err) { toast(err.message, 'error'); }
});

document.querySelectorAll('th.sort').forEach((th) => th.addEventListener('click', () => {
  if (state.sortBy === th.dataset.field) state.dir = state.dir === 'asc' ? 'desc' : 'asc';
  else { state.sortBy = th.dataset.field; state.dir = 'desc'; }
  state.page = 0; load();
}));

const reset = () => { state.page = 0; load(); };
['f-from', 'f-to', 'f-type', 'f-cat'].forEach((id) => $(id).addEventListener('change', reset));
$('f-q').addEventListener('input', debounce(reset));
$('clear').onclick = () => { ['f-from', 'f-to', 'f-type', 'f-cat', 'f-q'].forEach((id) => ($(id).value = '')); reset(); };
$('prev').onclick = () => { state.page--; load(); };
$('next').onclick = () => { state.page++; load(); };

$('export').onclick = async () => {
  try {
    const res = await api('/transactions/export' + qs(filters()), { raw: true });
    if (!res.ok) throw new Error('Export failed');
    const a = document.createElement('a');
    a.href = URL.createObjectURL(await res.blob());
    a.download = 'transactions.csv';
    a.click();
    URL.revokeObjectURL(a.href);
  } catch (e) { toast(e.message, 'error'); }
};

(async function init() {
  try {
    categories = await api('/categories');
    $('f-cat').innerHTML = '<option value="">All</option>' + categories.map((c) => `<option value="${c.id}">${esc(c.name)} (${c.type.toLowerCase()})</option>`).join('');
  } catch (e) { toast(e.message, 'error'); }
  load();
})();
