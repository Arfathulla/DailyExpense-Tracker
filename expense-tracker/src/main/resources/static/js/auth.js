import { api, auth } from './api.js';
import './ui.js';

if (auth.token) location.replace('/dashboard.html');

const form = document.getElementById('form');
const err = document.getElementById('error');
const btn = document.getElementById('submit');
const isRegister = !!document.getElementById('name');

async function submit(email, password) {
  err.textContent = '';
  btn.disabled = true;
  try {
    const body = isRegister
      ? { name: document.getElementById('name').value.trim(), email, password }
      : { email, password };
    const data = await api(isRegister ? '/auth/register' : '/auth/login', { method: 'POST', body });
    auth.save(data);
    location.href = '/dashboard.html';
  } catch (e) {
    err.textContent = e.message;
  } finally {
    btn.disabled = false;
  }
}

form.addEventListener('submit', (e) => {
  e.preventDefault();
  submit(document.getElementById('email').value.trim(), document.getElementById('password').value);
});

const demo = document.getElementById('demo');
if (demo) demo.onclick = () => submit('demo@example.com', 'Demo@123');

const toggle = document.getElementById('toggle-pw');
if (toggle) toggle.onclick = () => {
  const pw = document.getElementById('password');
  const show = pw.type === 'password';
  pw.type = show ? 'text' : 'password';
  toggle.textContent = show ? 'Hide' : 'Show';
  toggle.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
};
