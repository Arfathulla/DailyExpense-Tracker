const TOKEN = 'et_token', USER = 'et_user';

export const auth = {
  get token() { try { return localStorage.getItem(TOKEN); } catch { return null; } },
  save(d) {
    localStorage.setItem(TOKEN, d.token);
    localStorage.setItem(USER, JSON.stringify({ name: d.name, email: d.email }));
  },
  user() { try { return JSON.parse(localStorage.getItem(USER)) || {}; } catch { return {}; } },
  logout() {
    localStorage.removeItem(TOKEN);
    localStorage.removeItem(USER);
    location.href = '/login.html';
  },
};

/** Builds a query string, skipping empty values. */
export function qs(params) {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => { if (v !== '' && v != null) p.set(k, v); });
  const s = p.toString();
  return s ? '?' + s : '';
}

/** Wraps fetch: adds the JWT, parses JSON, turns API errors into Error(message), logs out on 401. */
export async function api(path, { method = 'GET', body, raw = false } = {}) {
  const headers = {};
  if (body) headers['Content-Type'] = 'application/json';
  if (auth.token) headers.Authorization = 'Bearer ' + auth.token;

  const res = await fetch('/api' + path, { method, headers, body: body ? JSON.stringify(body) : undefined });
  if (res.status === 401 && !path.startsWith('/auth/')) {
    auth.logout();
    throw new Error('Your session expired. Please log in again.');
  }
  if (raw) return res;
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.message || 'Request failed (' + res.status + ')');
  return data;
}
