const BASE = '';

async function request(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body !== undefined) opts.body = JSON.stringify(body);

  const res = await fetch(BASE + path, opts);

  if (!res.ok) {
    let msg = `HTTP ${res.status}`;
    try {
      const err = await res.json();
      msg = err.message || msg;
    } catch (_) {}
    throw new Error(msg);
  }

  if (res.status === 204) return null;
  return res.json();
}

export const Api = {
  get: (path) => request('GET', path),
  post: (path, body) => request('POST', path, body),
  put: (path, body) => request('PUT', path, body),
  delete: (path) => request('DELETE', path),
};
