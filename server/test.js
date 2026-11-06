'use strict';
// Integration test for the Cool Client API in insecure (offline) mode with entitlements enforced: node test.js
// API_BASE=http://127.0.0.1:8799 node test.js runs the HTTP part against a running server instead (e.g. the
// Cloudflare worker under `wrangler dev`, started with the same settings as below and empty storage).
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
const { createApp, changedRecords, dbFromRecords } = require('./app');
const REMOTE = process.env.API_BASE;

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
  const server = REMOTE ? null : await start(0);
  const base = REMOTE ? REMOTE.replace(/\/+$/, '') : `http://127.0.0.1:${server.address().port}`;
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

    // Media: never sold, given by an admin. It covers everything Supporter+ does plus the Media capes.
    const creator = '33333333-4444-5555-6666-777777777777';
    r = await call(base, 'POST', '/api/auth/challenge', { name: 'Creator', uuid: creator });
    r = await call(base, 'POST', '/api/auth/verify', { name: 'Creator', uuid: creator, serverId: r.body.serverId, offline: true });
    const creatorToken = r.body.token;
    await call(base, 'PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_media', WINGS: 'wings_energy' } }, creatorToken);
    r = await call(base, 'GET', '/api/players/' + creator);
    assert.deepStrictEqual(r.body.loadout.equipped, {}, 'media cape and store wings hidden before the grant');
    assert.strictEqual(r.body.media, false);
    r = await call(base, 'POST', '/api/admin/grant', { username: 'Creator', items: ['media'] }, 'test-admin-token');
    assert.deepStrictEqual(r.body.entitlements, ['media']);
    r = await call(base, 'POST', '/api/players', { uuids: [creator] });
    assert.deepStrictEqual(r.body.players[creator].loadout.equipped, { CAPE: 'cape_media', WINGS: 'wings_energy' });
    assert.strictEqual(r.body.players[creator].media, true);
    r = await call(base, 'GET', '/api/me', undefined, creatorToken);
    assert.strictEqual(r.body.media, true);
    await call(base, 'PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_onair' } }, friendToken);
    r = await call(base, 'GET', '/api/players/' + friend);
    assert.deepStrictEqual(r.body.loadout.equipped, {}, 'Supporter+ does not cover Media capes');
    tb = tebex(tebexPayment('pay-4', 'payment.completed', 'NewFriend', ['Media', 'cape_media', 'Owner', 'cape_owner', 'cape_founder']));
    r = await call(base, 'POST', '/api/webhooks/tebex', tb.raw, null, { 'X-Signature': tb.sig });
    assert.deepStrictEqual(r.body.applied, [], 'the store cannot sell Media, Owner or Founder');
    r = await call(base, 'GET', '/api/store');
    assert.deepStrictEqual(r.body.ranks, ['supporter', 'supporter_plus'], 'only sold ranks are listed');

    // Owner: the Owner cape is only ever shown on the owner, and the Owner rank covers everything.
    r = await call(base, 'PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_owner' } }, creatorToken);
    r = await call(base, 'GET', '/api/players/' + creator);
    assert.deepStrictEqual(r.body.loadout.equipped, {}, 'Media does not cover the Owner cape');
    await call(base, 'PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_owner', WINGS: 'wings_energy', HALO: 'halo_gold' } }, friendToken);
    r = await call(base, 'POST', '/api/admin/grant', { uuid: friend, items: ['owner'] }, 'test-admin-token');
    r = await call(base, 'POST', '/api/players', { uuids: [friend] });
    assert.deepStrictEqual(r.body.players[friend].loadout.equipped, { CAPE: 'cape_owner', WINGS: 'wings_energy', HALO: 'halo_gold' });
    assert.strictEqual(r.body.players[friend].owner, true);
    await call(base, 'PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_founder' } }, friendToken);
    r = await call(base, 'GET', '/api/players/' + friend);
    assert.deepStrictEqual([r.body.loadout.equipped, r.body.owner], [{ CAPE: 'cape_founder' }, true], 'Owner covers Founder too');

    assert.deepStrictEqual(sanitizeLoadout(null), { equipped: {}, colors: {}, chroma: {} });
    console.log('HTTP API tests passed' + (REMOTE ? ' against ' + base : ''));
  } finally {
    if (server) server.close();
  }

  // In process, enforcement off (the launch setting): everything shows except Founder and Media
  // cosmetics on people without them. Data goes through the record store the worker uses.
  const records = new Map();
  const persist = (db, changed) => {
    const { puts, deletes } = changedRecords(db, changed);
    for (const [k, v] of Object.entries(puts)) records.set(k, JSON.parse(JSON.stringify(v)));
    for (const k of deletes) records.delete(k);
  };
  const env = { AURORA_INSECURE: '1', ADMIN_TOKEN: 'a', FOUNDER_LIMIT: '0' };
  let app = createApp({ env, persist });
  const local = async (method, url, body, token) =>
    (await app.handle({ method, url, ip: '10.0.0.1', headers: token ? { authorization: 'Bearer ' + token } : {}, body: body === undefined ? null : JSON.stringify(body) })).body;
  const yt = '44444444-5555-6666-7777-888888888888';
  let b = await local('POST', '/api/auth/challenge', { name: 'Tuber', uuid: yt });
  const ytToken = (await local('POST', '/api/auth/verify', { name: 'Tuber', uuid: yt, serverId: b.serverId, offline: true })).token;
  await local('PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_premiere', WINGS: 'wings_energy' } }, ytToken);
  b = await local('GET', '/api/players/' + yt);
  assert.deepStrictEqual(b.loadout.equipped, { WINGS: 'wings_energy' }, 'unenforced: store wings show, Media cape does not');
  await local('POST', '/api/admin/grant', { username: 'Tuber', items: ['media'] }, 'a');
  b = await local('GET', '/api/players/' + yt);
  assert.deepStrictEqual(b.loadout.equipped, { CAPE: 'cape_premiere', WINGS: 'wings_energy' });
  assert.ok(records.has('users/' + yt) && records.has('entitlements/' + yt), 'records written');

  // A restart rebuilt from the records keeps the sign-in, the loadout and the rank.
  app = createApp({ env, persist, db: dbFromRecords(records) });
  b = await local('GET', '/api/me', undefined, ytToken);
  assert.deepStrictEqual([b.uuid, b.media, b.entitlements], [yt, true, ['media']]);
  assert.strictEqual(b.user.loadout.equipped.CAPE, 'cape_premiere');

  // Seasonal drops: wearing one during its season keeps it; afterwards only keepers show it.
  let when = Date.UTC(2026, 10, 20); // Nov 20, Thanksgiving season
  const seasonal = createApp({ env: { AURORA_INSECURE: '1', FOUNDER_LIMIT: '0', ADMIN_TOKEN: 'a' }, clock: () => when });
  const sl = async (method, url, body, token) =>
    (await seasonal.handle({ method, url, ip: '10.0.0.4', headers: token ? { authorization: 'Bearer ' + token } : {}, body: body === undefined ? null : JSON.stringify(body) })).body;
  const signIn = async (name, uuid) => {
    const c = await sl('POST', '/api/auth/challenge', { name, uuid });
    return (await sl('POST', '/api/auth/verify', { name, uuid, serverId: c.serverId, offline: true })).token;
  };
  const early = '55555555-0000-0000-0000-000000000001', late = '55555555-0000-0000-0000-000000000002';
  const earlyToken = await signIn('Early', early), lateToken = await signIn('Late', late);
  b = await sl('PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_gobbler', HAT: 'hat_turkey', WINGS: 'wings_angel' } }, earlyToken);
  assert.deepStrictEqual(b.claimed.sort(), ['cape_gobbler', 'hat_turkey'], 'worn in season: kept');
  assert.deepStrictEqual((await sl('GET', '/api/me', undefined, earlyToken)).entitlements, ['cape_gobbler', 'hat_turkey']);
  b = await sl('PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_gobbler' } }, earlyToken);
  assert.deepStrictEqual(b.claimed, [], 'nothing new to keep');
  assert.deepStrictEqual((await sl('GET', '/api/players/' + early)).loadout.equipped, { CAPE: 'cape_gobbler' });
  when = Date.UTC(2026, 11, 3); // Dec 3: season over
  b = await sl('PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_autumn', AURA: 'aura_leaves' } }, lateToken);
  assert.deepStrictEqual(b.claimed, [], 'too late to keep');
  assert.deepStrictEqual((await sl('GET', '/api/players/' + late)).loadout.equipped, {}, 'not kept: hidden after the season');
  assert.deepStrictEqual((await sl('GET', '/api/players/' + early)).loadout.equipped, { CAPE: 'cape_gobbler' }, 'kept: still shows');
  await sl('POST', '/api/admin/grant', { uuid: late, items: ['owner'] }, 'a');
  assert.deepStrictEqual((await sl('GET', '/api/players/' + late)).loadout.equipped, { CAPE: 'cape_autumn', AURA: 'aura_leaves' }, 'Owner rank shows everything');
  // A whole drop as a bundle, any time of year (here in December), and taken back the same way.
  const gift = '55555555-0000-0000-0000-000000000003';
  const giftToken = await signIn('Gifted', gift);
  b = await sl('POST', '/api/admin/grant', { uuid: gift, items: ['thanksgiving', 'not_a_season'] }, 'a');
  assert.deepStrictEqual(b.entitlements, ['aura_leaves', 'cape_autumn', 'cape_gobbler', 'cape_harvestmoon', 'hat_pilgrim', 'hat_turkey']);
  assert.deepStrictEqual(b.ignored, ['not_a_season'], 'typos are reported');
  await sl('PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_harvestmoon', HAT: 'hat_turkey' } }, giftToken);
  assert.deepStrictEqual((await sl('GET', '/api/players/' + gift)).loadout.equipped, { CAPE: 'cape_harvestmoon', HAT: 'hat_turkey' }, 'given bundle shows after the season');
  b = await sl('POST', '/api/admin/revoke', { uuid: gift, items: ['thanksgiving'] }, 'a');
  assert.deepStrictEqual(b.entitlements, []);
  b = await sl('POST', '/api/admin/grant', { username: 'NeverSignedIn', items: ['thanksgiving'] }, 'a');
  assert.strictEqual(b.pending, true, 'held for a name that has not signed in');
  const ns = '55555555-0000-0000-0000-000000000004';
  await signIn('NeverSignedIn', ns);
  assert.strictEqual((await sl('GET', '/api/me', undefined, await signIn('NeverSignedIn', ns))).entitlements.length, 6, 'applied on first sign-in');

  when = Date.UTC(2027, 10, 1); // back next November
  b = await sl('PUT', '/api/me/loadout', { equipped: { CAPE: 'cape_harvestmoon' } }, earlyToken);
  assert.deepStrictEqual(b.claimed, ['cape_harvestmoon'], 'next year: back again');

  // Real sign-ins: Mojang's answer decides. 204 = not joined (401), an outage or block = 502, 200 = signed in.
  for (const [status, body, expected] of [[204, null, 401], [403, null, 502], [200, { id: '069a79f444e94726a5befca90e38aaf5', name: 'Notch' }, 200]]) {
    const real = createApp({ env: {}, fetchJson: async () => ({ status, body }) });
    const ask = async (url, data) => real.handle({ method: 'POST', url, ip: '10.0.0.2', headers: {}, body: JSON.stringify(data) });
    const ch = (await ask('/api/auth/challenge', { name: 'Notch', uuid: '069a79f4-44e9-4726-a5be-fca90e38aaf5' })).body;
    const r2 = await ask('/api/auth/verify', { name: 'Notch', serverId: ch.serverId });
    assert.strictEqual(r2.status, expected, `Mojang ${status} -> ${expected}`);
  }

  // Through the sign-in relay (Cloudflare): the key goes along, Mojang's answer comes back wrapped.
  const seen = [];
  const relayed = createApp({
    env: { MOJANG_RELAY: 'https://relay.example.test/mojang/', RELAY_KEY: 'k1', ADMIN_TOKEN: 'a' },
    fetchJson: async (url, headers) => {
      seen.push([url, headers && headers['X-Relay-Key']]);
      if (url.includes('kind=profile')) return { status: 200, body: { status: 200, body: { id: 'eeb4bf9812fa4fbcb461ee5d4478e7ce', name: 'KingIceyYT' } } };
      return { status: 200, body: { status: 200, body: { id: '069a79f444e94726a5befca90e38aaf5', name: 'Notch' } } };
    },
  });
  const rq = async (url, data, token) => relayed.handle({ method: 'POST', url, ip: '10.0.0.3', headers: token ? { authorization: 'Bearer ' + token } : {}, body: JSON.stringify(data) });
  const ch2 = (await rq('/api/auth/challenge', { name: 'Notch', uuid: '069a79f4-44e9-4726-a5be-fca90e38aaf5' })).body;
  assert.strictEqual((await rq('/api/auth/verify', { name: 'Notch', serverId: ch2.serverId })).status, 200, 'sign-in through the relay');
  assert.deepStrictEqual(seen[0], [`https://relay.example.test/mojang?kind=hasJoined&username=Notch&serverId=${ch2.serverId}`, 'k1']);
  const g = await rq('/api/admin/grant', { username: 'KingIceyYT', items: ['media'] }, 'a');
  assert.deepStrictEqual(g.body, { uuid: 'eeb4bf98-12fa-4fbc-b461-ee5d4478e7ce', entitlements: ['media'] }, 'name lookup through the relay');
  console.log('All API tests passed');
})().catch((e) => {
  console.error('TEST FAILED:', e);
  process.exit(1);
});
