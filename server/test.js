'use strict';
// Integration test for the Cool Client API in insecure (offline) mode with entitlements enforced: node test.js
process.env.AURORA_INSECURE = '1';
process.env.ENFORCE_ENTITLEMENTS = '1';
process.env.ADMIN_TOKEN = 'test-admin-token';
process.env.WEBHOOK_SECRET = 'test-webhook-secret';
process.env.TEBEX_SECRET = 'test-tebex-secret';
process.env.FOUNDER_LIMIT = '1';
process.env.STORE_URL = 'https://store.example.test/';
process.env.DATA_DIR = require('path').join(require('os').tmpdir(), 'coolclient-api-test-' + Date.now());

const assert = require('assert');
const crypto = require('crypto');
const { start, sanitizeLoadout } = require('./server');

async function call(base, method, path, body, token, headers) {
  const res = await fetch(base + path, {
    method,
    headers: Object.assign({ 'Content-Type': 'application/json' }, token ? { Authorization: 'Bearer ' + token } : {}, headers || {}),
    body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
  });
  return { status: res.status, body: await res.json() };
}

function tebex(body) {
  const raw = JSON.stringify(body);
  const hash = crypto.createHash('sha256').update(raw).digest('hex');
  return { raw, sig: crypto.createHmac('sha256', 'test-tebex-secret').update(hash).digest('hex') };
}

function tebexPayment(id, type, username, packages) {
  return {
    id,
    type,
    date: '2026-10-29T12:00:00+00:00',
    subject: {
      transaction_id: 'tbx-' + id,
      customer: { username: { id: '1234', username } },
      products: packages.map((name, i) => ({ id: 100 + i, name, quantity: 1, username: { id: '1234', username } })),
    },
  };
}

function signed(body) {
  const raw = JSON.stringify(body);
  return { raw, sig: 'sha256=' + crypto.createHmac('sha256', 'test-webhook-secret').update(raw).digest('hex') };
}

(async () => {
  const server = await start(0);
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    let r = await call(base, 'GET', '/api/health');
    assert.strictEqual(r.status, 200);
    assert.strictEqual(r.body.ok, true);

    r = await call(base, 'GET', '/api/news');
    assert.ok(r.body.news.length > 0, 'news returned');

    const uuid = '0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0';
    r = await call(base, 'POST', '/api/auth/challenge', { name: 'Tester', uuid });
    assert.strictEqual(r.status, 200);
    const serverId = r.body.serverId;

    r = await call(base, 'POST', '/api/auth/verify', { name: 'Tester', uuid, serverId, offline: true });
    assert.strictEqual(r.status, 200, JSON.stringify(r.body));
    const token = r.body.token;

    // Challenges are single use.
    r = await call(base, 'POST', '/api/auth/verify', { name: 'Tester', uuid, serverId, offline: true });
    assert.strictEqual(r.status, 401);

    r = await call(base, 'PUT', '/api/me/loadout', {
      equipped: { CAPE: 'cape_aurora', WINGS: 'wings_energy', BOGUS: 'x', HAT: '../etc' },
      colors: { wings_energy: 'FF2DE2C1', cape_aurora: 'nothex' },
      chroma: { wings_energy: true, other: true },
    }, token);
    assert.strictEqual(r.status, 200);
    assert.deepStrictEqual(r.body.loadout.equipped, { CAPE: 'cape_aurora', WINGS: 'wings_energy' });
    assert.deepStrictEqual(r.body.loadout.colors, { wings_energy: 'FF2DE2C1' });
    assert.deepStrictEqual(r.body.loadout.chroma, { wings_energy: true });

    r = await call(base, 'PUT', '/api/me/loadout', { equipped: {} }, 'f'.repeat(64));
    assert.strictEqual(r.status, 401, 'bad token rejected');

    r = await call(base, 'POST', '/api/heartbeat', {}, token);
    assert.strictEqual(r.status, 200);

    r = await call(base, 'POST', '/api/players', { uuids: [uuid, '11111111-2222-3333-4444-555555555555', 'garbage'] });
    assert.ok(r.body.players[uuid], 'known player returned');
    assert.strictEqual(r.body.players[uuid].online, true);
    assert.strictEqual(Object.keys(r.body.players).length, 1);

    // cape_aurora (store) and wings_energy (store) are not owned yet, so others don't see them.
    r = await call(base, 'GET', '/api/players/' + uuid.replace(/-/g, ''));
    assert.strictEqual(r.status, 200);
    assert.deepStrictEqual(r.body.loadout.equipped, {}, 'unowned cosmetics hidden');
    assert.strictEqual(r.body.supporter, false);

    r = await call(base, 'GET', '/api/store');
    assert.deepStrictEqual([r.body.enforced, r.body.url], [true, 'https://store.example.test/']);

    // Admin grants need the token.
    r = await call(base, 'POST', '/api/admin/grant', { uuid, items: ['cape_aurora'] }, 'wrong');
    assert.strictEqual(r.status, 401);
    r = await call(base, 'POST', '/api/admin/grant', { username: 'tester', items: ['cape_aurora', 'not_a_real_item'] }, 'test-admin-token');
    assert.strictEqual(r.status, 200, JSON.stringify(r.body));
    assert.deepStrictEqual(r.body.entitlements, ['cape_aurora', 'cape_founder'], 'first player is a founder');
    r = await call(base, 'GET', '/api/players/' + uuid);
    assert.deepStrictEqual(r.body.loadout.equipped, { CAPE: 'cape_aurora' }, 'owned cape visible, wings still hidden');

    // Signed webhook: bad signature rejected, good one grants, replays are no-ops.
    const ev = signed({ id: 'evt_1', type: 'grant', uuid, items: ['supporter_plus'] });
    r = await call(base, 'POST', '/api/webhooks/payment', ev.raw, null, { 'X-Signature': 'sha256=' + '0'.repeat(64) });
    assert.strictEqual(r.status, 401);
    r = await call(base, 'POST', '/api/webhooks/payment', ev.raw, null, { 'X-Signature': ev.sig });
    assert.strictEqual(r.status, 200, JSON.stringify(r.body));
    assert.deepStrictEqual(r.body.entitlements, ['cape_aurora', 'cape_founder', 'supporter_plus']);
    r = await call(base, 'POST', '/api/webhooks/payment', ev.raw, null, { 'X-Signature': ev.sig });
    assert.strictEqual(r.body.duplicate, true);

    r = await call(base, 'POST', '/api/players', { uuids: [uuid] });
    assert.deepStrictEqual(r.body.players[uuid].loadout.equipped, { CAPE: 'cape_aurora', WINGS: 'wings_energy' });
    assert.strictEqual(r.body.players[uuid].supporter, true);

    r = await call(base, 'GET', '/api/me', undefined, token);
    assert.deepStrictEqual(r.body.entitlements, ['cape_aurora', 'cape_founder', 'supporter_plus']);

    const rev = signed({ id: 'evt_2', type: 'revoke', uuid, items: ['supporter_plus'] });
    r = await call(base, 'POST', '/api/webhooks/payment', rev.raw, null, { 'X-Signature': rev.sig });
    assert.deepStrictEqual(r.body.entitlements, ['cape_aurora', 'cape_founder']);

    // Tebex: validation handshake, signature check, purchase, gift to a new player, refund, replay.
    let tb = tebex({ id: 'val-1', type: 'validation.webhook', date: '2026-10-29T12:00:00+00:00', subject: {} });
    r = await call(base, 'POST', '/api/webhooks/tebex', tb.raw, null, { 'X-Signature': tb.sig });
    assert.deepStrictEqual(r.body, { id: 'val-1' }, 'validation echoes the id');
    r = await call(base, 'POST', '/api/webhooks/tebex', tb.raw, null, { 'X-Signature': 'f'.repeat(64) });
    assert.strictEqual(r.status, 401);

    tb = tebex(tebexPayment('pay-1', 'payment.completed', 'Tester', ['Supporter']));
    r = await call(base, 'POST', '/api/webhooks/tebex', tb.raw, null, { 'X-Signature': tb.sig });
    assert.strictEqual(r.status, 200, JSON.stringify(r.body));
    assert.deepStrictEqual(r.body.applied[0].entitlements, ['cape_aurora', 'cape_founder', 'supporter']);
    r = await call(base, 'POST', '/api/webhooks/tebex', tb.raw, null, { 'X-Signature': tb.sig });
    assert.strictEqual(r.body.duplicate, true, 'replays are ignored');

    tb = tebex(tebexPayment('pay-2', 'payment.completed', 'NewFriend', ['Supporter+']));
    r = await call(base, 'POST', '/api/webhooks/tebex', tb.raw, null, { 'X-Signature': tb.sig });
    assert.strictEqual(r.body.applied[0].pending, true, 'unknown buyer is parked until they sign in');
    const friend = '22222222-3333-4444-5555-666666666666';
    r = await call(base, 'POST', '/api/auth/challenge', { name: 'NewFriend', uuid: friend });
    r = await call(base, 'POST', '/api/auth/verify', { name: 'NewFriend', uuid: friend, serverId: r.body.serverId, offline: true });
    const friendToken = r.body.token;
    r = await call(base, 'GET', '/api/me', undefined, friendToken);
    assert.deepStrictEqual(r.body.entitlements, ['supporter_plus'], 'pending purchase settled; founder slots used up');

    // Supporter+ covers sold cosmetics but never exclusives: a non-founder's Founder cape stays hidden.
    await call(base, 'PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_founder', WINGS: 'wings_energy' } }, friendToken);
    r = await call(base, 'GET', '/api/players/' + friend);
    assert.deepStrictEqual(r.body.loadout.equipped, { WINGS: 'wings_energy' });

    tb = tebex(tebexPayment('pay-3', 'payment.refunded', 'Tester', ['Supporter']));
    r = await call(base, 'POST', '/api/webhooks/tebex', tb.raw, null, { 'X-Signature': tb.sig });
    assert.deepStrictEqual(r.body.applied[0].entitlements, ['cape_aurora', 'cape_founder'], 'refund revokes');

    assert.deepStrictEqual(sanitizeLoadout(null), { equipped: {}, colors: {}, chroma: {} });
    console.log('All API tests passed');
  } finally {
    server.close();
  }
})().catch((e) => {
  console.error('TEST FAILED:', e);
  process.exit(1);
});
