import { auth } from './api.js';

// ---- theme (applied immediately on import to avoid a flash) ----
const savedTheme = (() => { try { return localStorage.getItem('et_theme'); } catch { return null; } })();
const prefersDark = window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches;
document.documentElement.dataset.theme = savedTheme || (prefersDark ? 'dark' : 'light');

export function toggleTheme() {
  const next = document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark';
  document.documentElement.dataset.theme = next;
  try { localStorage.setItem('et_theme', next); } catch { /* ignore */ }
}

// ---- helpers ----
const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2, minimumFractionDigits: 0 });
export const money = (n) => inr.format(Number(n) || 0);

export const esc = (s) => String(s ?? '').replace(/[&<>"']/g,
  (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

export function currentMonth() {
  const d = new Date();
  return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0');
}
export const today = () => {
  const d = new Date();
  return currentMonth() + '-' + String(d.getDate()).padStart(2, '0');
};
export const fmtDate = (iso) => new Date(iso + 'T00:00:00').toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });

export function toast(message, kind = 'ok') {
  let box = document.getElementById('toasts');
  if (!box) { box = document.createElement('div'); box.id = 'toasts'; box.setAttribute('role', 'status'); document.body.appendChild(box); }
  const t = document.createElement('div');
  t.className = 'toast ' + kind;
  t.textContent = message;
  box.appendChild(t);
  setTimeout(() => t.remove(), 3500);
}

export function debounce(fn, ms = 300) {
  let h;
  return (...a) => { clearTimeout(h); h = setTimeout(() => fn(...a), ms); };
}

const ICONS = {
  dashboard: '<path d="M4 13h6V4H4v9Zm0 7h6v-5H4v5Zm10 0h6v-9h-6v9Zm0-16v5h6V4h-6Z"/>',
  transactions: '<path d="M7 7h12M7 7l3-3M7 7l3 3M17 17H5m12 0-3-3m3 3-3 3"/>',
  budgets: '<circle cx="12" cy="12" r="8"/><circle cx="12" cy="12" r="4"/><path d="M12 12h.01"/>',
  manage: '<path d="M3 12V4h8l9 9-8 8-9-9Z"/><circle cx="7.5" cy="8.5" r="1.2"/>',
  moon: '<path d="M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5Z"/>',
  logout: '<path d="M14 4h4a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-4M10 16l-4-4 4-4M6 12h10"/>',
};
const icon = (n) => `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[n]}</svg>`;

const CAT_COLORS = ['#3340d8', '#e5484d', '#f2a900', '#0b8f63', '#8b5cf6', '#0891b2', '#db2777', '#64748b'];
/** Stable colour per category name, so the same category looks the same on every page. */
export function catColor(name) {
  let h = 0;
  for (const ch of String(name)) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return CAT_COLORS[h % CAT_COLORS.length];
}
export const initials = (name) => String(name || '?').trim().split(/\s+/).slice(0, 2).map((p) => p[0]).join('').toUpperCase();

/** Redirects to login if there is no token, then renders the sidebar (desktop) and top bar (mobile). */
export function initShell(active) {
  if (!auth.token) { location.href = '/login.html'; return; }
  const links = [['dashboard', 'Dashboard'], ['transactions', 'Transactions'], ['budgets', 'Budgets'], ['manage', 'Categories']];
  const u = auth.user();
  const brand = '<a class="brand" href="/dashboard.html"><span class="mark">₹</span>SpendLedger</a>';
  const actions = `<div class="side-actions">
      <button class="icon-btn" data-theme-btn type="button" aria-label="Switch theme">${icon('moon')}<span class="lbl">Theme</span></button>
      <button class="icon-btn" data-logout-btn type="button" aria-label="Log out">${icon('logout')}<span class="lbl">Log out</span></button></div>`;
  document.body.classList.add('app');
  document.body.insertAdjacentHTML('afterbegin', `
    <header class="topbar">${brand}${actions}</header>
    <aside class="side">${brand}
      <nav aria-label="Main">${links.map(([id, label]) =>
        `<a class="${id === active ? 'active' : ''}" href="/${id}.html" ${id === active ? 'aria-current="page"' : ''}>${icon(id)}<span>${label}</span></a>`).join('')}</nav>
      <div class="side-foot">
        <div class="me"><span class="avatar">${esc(initials(u.name))}</span><span class="me-txt"><b>${esc(u.name)}</b><small>${esc(u.email)}</small></span></div>
        ${actions}
      </div></aside>`);
  document.querySelectorAll('[data-theme-btn]').forEach((b) => (b.onclick = toggleTheme));
  document.querySelectorAll('[data-logout-btn]').forEach((b) => (b.onclick = () => auth.logout()));
}

export const PALETTE = CAT_COLORS;
