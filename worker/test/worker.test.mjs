import { test } from 'node:test';
import assert from 'node:assert/strict';
import worker from '../dist/index.js';

const env = { WORKER_TOKEN: 'internal-secret', SENDGRID_API_KEY: 'fake-key', FROM_EMAIL: 'verified@example.com' };
const payload = { to: 'recipient@example.com', subject: 'Hola', message: 'Prueba', requestId: '11111111-1111-4111-8111-111111111111' };
function request(body = payload, token = 'internal-secret') {
  return new Request('https://worker.example/api/send-email', { method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
}
test('rejects unauthenticated requests and invalid input before calling provider', async () => {
  assert.equal((await worker.fetch(request(payload, 'wrong'), env)).status, 401);
  assert.equal((await worker.fetch(request({ ...payload, to: 'invalid' }), env)).status, 400);
  assert.equal((await worker.fetch(request(null), env)).status, 400);
  assert.equal((await worker.fetch(request({ ...payload, message: 'x'.repeat(41000) }), env)).status, 413);
  assert.equal((await worker.fetch(request(), {})).status, 503);
});
test('uses fixed sender, server secret, and plain text; only 202 counts as accepted', async () => {
  const original = globalThis.fetch;
  try {
    let captured;
    globalThis.fetch = async (url, init) => { captured = { url, init }; return new Response(null, { status: 202 }); };
    const response = await worker.fetch(request({ ...payload, from: 'attacker@example.com' }), env);
    assert.equal(response.status, 202);
    assert.equal((await response.json()).status, 'ACCEPTED');
    assert.equal(captured.url, 'https://api.sendgrid.com/v3/mail/send');
    assert.equal(captured.init.headers.Authorization, 'Bearer fake-key');
    const body = JSON.parse(captured.init.body);
    assert.equal(body.from.email, env.FROM_EMAIL);
    assert.equal(body.content[0].type, 'text/plain');
    globalThis.fetch = async () => new Response('private provider error', { status: 403 });
    const rejected = await worker.fetch(request(), env);
    assert.equal(rejected.status, 502);
    assert.equal((await rejected.text()).includes('private provider error'), false);
    globalThis.fetch = async () => { throw new Error('timeout'); };
    assert.equal((await worker.fetch(request(), env)).status, 502);
  } finally { globalThis.fetch = original; }
});
