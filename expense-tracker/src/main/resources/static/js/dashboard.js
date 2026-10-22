import { api, auth } from './api.js';
import { initShell, money, esc, toast, currentMonth, PALETTE, catColor, fmtDate } from './ui.js';

initShell('dashboard');
const monthInput = document.getElementById('month');
monthInput.value = currentMonth();
let pie, bar;
if (typeof Chart !== 'undefined') { Chart.defaults.font.family = "'Instrument Sans', system-ui, sans-serif"; }

const hour = new Date().getHours();
const first = (auth.user().name || '').split(' ')[0];
document.getElementById('greeting').textContent =
  (hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening') + (first ? ', ' + first : '');

const css = (v) => getComputedStyle(document.documentElement).getPropertyValue(v).trim();

async function load() {
  const m = monthInput.value || currentMonth();
  try {
    const [summary, cats, trend, budgets, recent] = await Promise.all([
      api('/analytics/summary?month=' + m),
      api('/analytics/by-category?month=' + m),
      api('/analytics/monthly-trend?months=6'),
      api('/budgets?month=' + m),
      api('/transactions?size=6'),
    ]);
    renderStats(summary);
    renderRecent(recent.content);
    renderBudgets(budgets);
    renderPie(cats);
    renderBar(trend);
  } catch (e) { toast(e.message, 'error'); }
}

function renderStats(s) {
  const inc = Number(s.income), exp = Number(s.expense), bal = Number(s.balance);
  const total = inc + exp;
  const label = new Date((monthInput.value || currentMonth()) + '-01T00:00:00').toLocaleDateString('en-IN', { month: 'long', year: 'numeric' });
  const note = inc > 0
    ? (bal >= 0 ? `You kept ${Math.round((bal / inc) * 100)}% of your income.` : 'You spent more than you earned this month.')
    : 'No income recorded this month.';
  document.getElementById('stats').innerHTML = `
    <div class="hero">
      <div><p class="hero-label">Balance for ${label}</p>
        <p class="hero-value num ${bal < 0 ? 'neg' : ''}">${money(bal)}</p><p class="hero-note">${note}</p></div>
      <div class="hero-split">
        <div class="hero-figs">
          <div class="fig"><small><i style="background:#3ccf9b"></i>Income</small><b class="num">${money(inc)}</b></div>
          <div class="fig"><small><i style="background:#ff7b85"></i>Expenses</small><b class="num">${money(exp)}</b></div>
        </div>
        <div class="split-bar" role="img" aria-label="Income compared with expenses">
          ${total > 0 ? `<span class="in" style="width:${(inc / total) * 100}%"></span><span class="ex" style="width:${(exp / total) * 100}%"></span>` : ''}
        </div>
      </div>
    </div>`;
}

function renderRecent(list) {
  document.getElementById('recent').innerHTML = list.length ? list.map((t) => `
    <div class="txn">
      <span class="ico" style="background:color-mix(in srgb, ${catColor(t.categoryName)} 18%, transparent)">${esc(t.categoryName.slice(0, 1))}</span>
      <span><b>${esc(t.note || t.categoryName)}</b><small>${esc(t.categoryName)} · ${fmtDate(t.txnDate)}</small></span>
      <span class="amt num ${t.type === 'INCOME' ? 'income' : 'expense'}">${t.type === 'INCOME' ? '+' : '−'}${money(t.amount)}</span>
    </div>`).join('')
    : '<p class="empty">No transactions yet. <a href="/transactions.html">Add your first one</a>.</p>';
}

function renderPie(cats) {
  const canvas = document.getElementById('pie');
  document.getElementById('pie-empty').hidden = cats.length > 0;
  canvas.parentElement.hidden = cats.length === 0;
  if (pie) pie.destroy();
  if (!cats.length) return;
  pie = new Chart(canvas, {
    type: 'doughnut',
    data: { labels: cats.map((c) => c.category), datasets: [{ data: cats.map((c) => Number(c.total)), backgroundColor: PALETTE, borderColor: css('--surface'), borderWidth: 3 }] },
    options: { maintainAspectRatio: false, cutout: '58%',
      plugins: { legend: { position: 'right', labels: { color: css('--ink'), boxWidth: 10, usePointStyle: true } },
                 tooltip: { callbacks: { label: (c) => ' ' + c.label + ': ' + money(c.parsed) } } } },
  });
}

function renderBar(trend) {
  if (bar) bar.destroy();
  const label = (m) => new Date(m + '-01T00:00:00').toLocaleDateString('en-IN', { month: 'short' });
  bar = new Chart(document.getElementById('bar'), {
    type: 'bar',
    data: { labels: trend.map((t) => label(t.month)), datasets: [
      { label: 'Income', data: trend.map((t) => Number(t.income)), backgroundColor: css('--income'), borderRadius: 4 },
      { label: 'Expenses', data: trend.map((t) => Number(t.expense)), backgroundColor: css('--expense'), borderRadius: 4 }] },
    options: { maintainAspectRatio: false,
      scales: { x: { ticks: { color: css('--muted') }, grid: { display: false } },
                y: { ticks: { color: css('--muted'), callback: (v) => money(v) }, grid: { color: css('--line') } } },
      plugins: { legend: { labels: { color: css('--ink') } }, tooltip: { callbacks: { label: (c) => ' ' + c.dataset.label + ': ' + money(c.parsed.y) } } } },
  });
}

function renderBudgets(list) {
  const box = document.getElementById('budgets');
  if (!list.length) { box.innerHTML = '<p class="empty">No budgets for this month. Set one to get warned before you overspend.</p>'; return; }
  const flagged = list.filter((b) => b.status !== 'OK');
  if (!flagged.length) { box.innerHTML = '<p class="empty">All ' + list.length + ' budgets are on track.</p>'; return; }
  box.innerHTML = flagged.map((b) => `
    <div class="budget"><div class="budget-top"><strong>${esc(b.categoryName)}</strong>
      <span class="pill ${b.status}">${b.status === 'EXCEEDED' ? 'Over budget' : 'Nearly at limit'}</span></div>
      <div class="bar ${b.status}"><span style="width:${Math.min(b.percentUsed, 100)}%"></span></div>
      <span class="muted num">${money(b.spent)} of ${money(b.limitAmount)} (${b.percentUsed}%)</span></div>`).join('');
}

monthInput.addEventListener('change', load);
load();
