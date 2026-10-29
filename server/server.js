'use strict';
/**
 * Cool Client API: cosmetics loadouts, entitlements, presence and launcher news. Zero dependencies.
 *
 * Identity: the client asks for a random serverId (/api/auth/challenge), performs the Mojang
 * session "join" with it, then calls /api/auth/verify. We confirm with Mojang's hasJoined
 * endpoint (exactly what Minecraft servers do) before issuing a session token.
 *
 * Monetization: the client is open source, so anyone can equip anything locally. What this
 * server sells is visibility: with ENFORCE_ENTITLEMENTS=1 other players only see cosmetics the
 * wearer owns (catalog.json says which are free). Ownership is granted by an admin call or a
 * signed payment webhook. See MONETIZATION.md.
 *
 * Environment:
 *   PORT                  listen port (default 8787)
 *   DATA_DIR              where db.json lives (default ./data)
 *   AURORA_INSECURE       "1" to accept offline/cracked identities (development only!)
 *   ENFORCE_ENTITLEMENTS  "1" to hide unowned paid cosmetics from other players
 *   STORE_URL             store page opened by the client's Unlock button
 *   ADMIN_TOKEN           bearer token for /api/admin/* (unset = admin API off)
 *   WEBHOOK_SECRET        HMAC-SHA256 secret for /api/webhooks/payment (unset = webhook off)
 *   TEBEX_SECRET          Tebex webhook secret for /api/webhooks/tebex (unset = Tebex off)
 *   TEBEX_PACKAGES        optional JSON {"<package id>": ["supporter"], ...}; without it a package
 *                         whose name is a rank or cosmetic id ("Supporter", "Supporter+",
 *                         "cape_aurora") grants that
 *   FOUNDER_LIMIT         how many first sign-ins get the Founder cape (default 1000, 0 = off)
 *   TRUST_PROXY           "1" behind a reverse proxy (Caddy, nginx) so rate limits use X-Forwarded-For
 */
const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const VERSION = '0.1.0-beta';
const PORT = parseInt(process.env.PORT || '8787', 10);
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, 'data');
const INSECURE = process.env.AURORA_INSECURE === '1';
const ENFORCE = process.env.ENFORCE_ENTITLEMENTS === '1';
const STORE_URL = process.env.STORE_URL || '';
const ADMIN_TOKEN = process.env.ADMIN_TOKEN || '';
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || '';
const TEBEX_SECRET = process.env.TEBEX_SECRET || '';
const TEBEX_PACKAGES = parseJsonEnv('TEBEX_PACKAGES');
const FOUNDER_LIMIT = parseInt(process.env.FOUNDER_LIMIT || '1000', 10);
const FOUNDER_ITEM = 'cape_founder';
const TRUST_PROXY = process.env.TRUST_PROXY === '1';
const DB_FILE = path.join(DATA_DIR, 'db.json');
const ONLINE_WINDOW_MS = 3 * 60 * 1000;
const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const CHALLENGE_TTL_MS = 2 * 60 * 1000;
const SLOTS = ['CAPE', 'WINGS', 'HAT', 'HALO', 'BANDANA', 'AURA'];
const RANKS = ['supporter', 'supporter_plus'];
const CATALOG = JSON.parse(fs.readFileSync(path.join(__dirname, 'catalog.json'), 'utf8')).cosmetics;

function parseJsonEnv(name) {
  try {
    return JSON.parse(process.env[name] || '{}');
  } catch (e) {
    console.error(`${name} is not valid JSON; ignoring it`);
    return {};
  }
}

// ---------------------------------------------------------------- storage

let db = { users: {}, tokens: {}, news: null, entitlements: {}, payments: {}, pending: {}, founders: 0 };
let saveTimer = null;

function load() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  if (fs.existsSync(DB_FILE)) {
    try {
      db = Object.assign(db, JSON.parse(fs.readFileSync(DB_FILE, 'utf8')));
    } catch (e) {
      console.error('Could not read db.json, starting fresh:', e.message);
      fs.renameSync(DB_FILE, DB_FILE + '.broken-' + Date.now());
    }
  }
  const now = Date.now();
  for (const [t, v] of Object.entries(db.tokens)) if (now - v.created > TOKEN_TTL_MS) delete db.tokens[t];
}

function save() {
  if (saveTimer) return;
  saveTimer = setTimeout(() => {
    saveTimer = null;
    const tmp = DB_FILE + '.tmp';
    fs.writeFileSync(tmp, JSON.stringify(db, null, 2));
    fs.renameSync(tmp, DB_FILE);
  }, 500);
}

const challenges = new Map(); // serverId -> { name, uuid, created }
const online = new Map(); // uuid -> lastSeen

// ---------------------------------------------------------------- helpers

const UUID_RE = /^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$/i;
const NAME_RE = /^[A-Za-z0-9_]{1,16}$/;
const ID_RE = /^[a-z0-9_]{1,40}$/;
const HEX_RE = /^[0-9A-Fa-f]{8}$/;

function normUuid(u) {
  const h = String(u).replace(/-/g, '').toLowerCase();
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

function send(res, status, body) {
  const data = JSON.stringify(body);
  res.writeHead(status, {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(data),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, PUT, OPTIONS',
  });
  res.end(data);
}

function readBody(req) {
  return readRaw(req).then((raw) => {
    if (!raw.length) return {};
    try {
      return JSON.parse(raw.toString('utf8'));
    } catch (e) {
      throw new Error('Invalid JSON');
    }
  });
}

function readRaw(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > 64 * 1024) {
        reject(new Error('Body too large'));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });
}

function getJson(url) {
  return new Promise((resolve, reject) => {
    https
      .get(url, { timeout: 5000 }, (res) => {
        let text = '';
        res.on('data', (c) => (text += c));
        res.on('end', () => resolve({ status: res.statusCode, body: text ? safeParse(text) : null }));
      })
      .on('error', reject)
      .on('timeout', function () {
        this.destroy(new Error('Mojang timeout'));
      });
  });
}

function safeParse(t) {
  try {
    return JSON.parse(t);
  } catch (e) {
    return null;
  }
}

function auth(req) {
  const h = req.headers['authorization'] || '';
  const m = /^Bearer ([0-9a-f]{64})$/.exec(h);
  if (!m) return null;
  const t = db.tokens[m[1]];
  if (!t || Date.now() - t.created > TOKEN_TTL_MS) return null;
  return t;
}

/** Keeps only well-formed loadout data so clients can't store arbitrary blobs. */
function sanitizeLoadout(input) {
  const out = { equipped: {}, colors: {}, chroma: {} };
  if (!input || typeof input !== 'object') return out;
  const eq = input.equipped || {};
  for (const slot of SLOTS) {
    if (typeof eq[slot] === 'string' && ID_RE.test(eq[slot])) out.equipped[slot] = eq[slot];
  }
  const ids = new Set(Object.values(out.equipped));
  for (const [k, v] of Object.entries(input.colors || {})) {
    if (ids.has(k) && typeof v === 'string' && HEX_RE.test(v)) out.colors[k] = v.toUpperCase();
  }
  for (const [k, v] of Object.entries(input.chroma || {})) {
    if (ids.has(k) && v === true) out.chroma[k] = true;
  }
  return out;
}

// ---------------------------------------------------------------- entitlements

function entitlementsOf(uuid) {
  const e = db.entitlements[uuid];
  return e && Array.isArray(e.items) ? e.items : [];
}

function isSupporter(uuid) {
  return entitlementsOf(uuid).some((i) => RANKS.includes(i));
}

/** Free items, owned items, and whatever the owner's rank includes. Unknown ids are never owned. */
function owns(uuid, id) {
  const tier = CATALOG[id];
  if (!tier) return false;
  if (tier === 'free') return true;
  const items = entitlementsOf(uuid);
  if (items.includes(id)) return true;
  if (tier === 'exclusive') return false;
  if (items.includes('supporter_plus')) return true;
  return tier === 'supporter' && items.includes('supporter');
}

/**
 * What other players get to see. With enforcement on, unowned cosmetics are dropped;
 * exclusives (the Founder cape) are always checked, even while everything else is free.
 */
function visibleLoadout(uuid, loadout) {
  const l = loadout || sanitizeLoadout(null);
  const out = { equipped: {}, colors: {}, chroma: {} };
  for (const [slot, id] of Object.entries(l.equipped || {})) {
    if ((ENFORCE || CATALOG[id] === 'exclusive') && !owns(uuid, id)) continue;
    out.equipped[slot] = id;
    if (l.colors && l.colors[id]) out.colors[id] = l.colors[id];
    if (l.chroma && l.chroma[id]) out.chroma[id] = true;
  }
  return out;
}

/** Adds or removes items/ranks and returns the new list. Only catalog ids and ranks are accepted. */
function changeEntitlements(uuid, items, grant) {
  const valid = (Array.isArray(items) ? items : []).filter((i) => typeof i === 'string' && (RANKS.includes(i) || CATALOG[i]));
  const current = new Set(entitlementsOf(uuid));
  for (const i of valid) {
    if (grant) current.add(i);
    else current.delete(i);
  }
  db.entitlements[uuid] = { items: [...current].sort(), updated: Date.now() };
  save();
  return db.entitlements[uuid].items;
}

/** Resolves {uuid} or {username}: known players first, then Mojang (skipped in insecure dev mode). */
async function resolveUser(body) {
  if (typeof body.uuid === 'string' && UUID_RE.test(body.uuid)) return normUuid(body.uuid);
  if (typeof body.username !== 'string' || !NAME_RE.test(body.username)) return null;
  const lower = body.username.toLowerCase();
  for (const [u, user] of Object.entries(db.users)) if ((user.name || '').toLowerCase() === lower) return u;
  if (INSECURE) return null;
  try {
    const r = await getJson(`https://api.mojang.com/users/profiles/minecraft/${encodeURIComponent(body.username)}`);
    if (r.status === 200 && r.body && UUID_RE.test(r.body.id || '')) return normUuid(r.body.id);
  } catch (e) {
    console.error('Mojang name lookup failed:', e.message);
  }
  return null;
}

/**
 * Grants or revokes for a buyer. Buyers who can't be resolved yet (no Mojang lookup, or a name
 * that never signed in) are parked under their name and settled when they first sign in.
 */
async function applyPurchase(target, items, grant) {
  const u = await resolveUser(target);
  if (u) return { uuid: u, entitlements: changeEntitlements(u, items, grant) };
  if (typeof target.username !== 'string' || !NAME_RE.test(target.username)) return null;
  const key = target.username.toLowerCase();
  const list = (db.pending[key] = db.pending[key] || []);
  list.push({ grant, items, at: Date.now() });
  save();
  return { pending: true, username: target.username };
}

function settlePending(name, uuid) {
  const key = String(name).toLowerCase();
  const list = db.pending[key];
  if (!list) return;
  for (const p of list) changeEntitlements(uuid, p.items, p.grant);
  delete db.pending[key];
  save();
}

/** Items a set of Tebex packages grants: TEBEX_PACKAGES first, else a package named after a rank or id. */
function tebexItems(products) {
  const out = [];
  for (const p of Array.isArray(products) ? products : []) {
    const mapped = TEBEX_PACKAGES[String(p && p.id)];
    if (Array.isArray(mapped)) {
      out.push(...mapped);
      continue;
    }
    const key = String((p && p.name) || '')
      .trim()
      .toLowerCase()
      .replace(/\+/g, ' plus')
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '');
    if (RANKS.includes(key) || CATALOG[key]) out.push(key);
  }
  return out;
}

/** Groups a Tebex payment's products by the Minecraft account they were bought for (gifts included). */
function tebexBuyers(payment) {
  const buyers = new Map();
  const customer = (payment && payment.customer && payment.customer.username) || {};
  for (const p of Array.isArray(payment && payment.products) ? payment.products : []) {
    const who = (p && p.username && p.username.username) || customer.username;
    const id = (p && p.username && p.username.id) || customer.id;
    if (!who) continue;
    const key = String(who).toLowerCase();
    if (!buyers.has(key)) buyers.set(key, { target: { username: String(who), uuid: id ? String(id) : undefined }, products: [] });
    buyers.get(key).products.push(p);
  }
  return [...buyers.values()];
}

function safeEqual(a, b) {
  const x = Buffer.from(String(a));
  const y = Buffer.from(String(b));
  return x.length === y.length && crypto.timingSafeEqual(x, y);
}

function isAdmin(req) {
  if (!ADMIN_TOKEN) return false;
  const h = req.headers['authorization'] || '';
  return h.startsWith('Bearer ') && safeEqual(h.slice(7), ADMIN_TOKEN);
}

function clientIp(req) {
  if (TRUST_PROXY) {
    const fwd = String(req.headers['x-forwarded-for'] || '').split(',')[0].trim();
    if (fwd) return fwd;
  }
  return req.socket.remoteAddress || 'unknown';
}

// Per-IP token bucket: 120 requests per minute.
const buckets = new Map();
function rateLimited(req) {
  const ip = clientIp(req);
  const now = Date.now();
  let b = buckets.get(ip);
  if (!b) {
    b = { tokens: 120, last: now };
    buckets.set(ip, b);
  }
  b.tokens = Math.min(120, b.tokens + ((now - b.last) / 60000) * 120);
  b.last = now;
  if (b.tokens < 1) return true;
  b.tokens -= 1;
  return false;
}

const DEFAULT_NEWS = [
  {
    id: 'beta-launch',
    title: 'Cool Client beta is here',
    body: 'Over 60 mods, animated cosmetics, and a brand new launcher for 1.8.9 and 1.21.4.',
    date: '2026-10-19',
    tag: 'Release',
  },
  {
    id: 'cosmetics',
    title: 'Every cosmetic is free in the beta',
    body: 'Capes, wings, halos, hats and auras. Open the locker from the launcher or press Right Shift in game.',
    date: '2026-10-19',
    tag: 'Cosmetics',
  },
];

// ---------------------------------------------------------------- routes

async function handle(req, res) {
  if (req.method === 'OPTIONS') return send(res, 204, {});
  if (rateLimited(req)) return send(res, 429, { error: 'Too many requests' });
  const url = new URL(req.url, 'http://localhost');
  const p = url.pathname;

  if (req.method === 'GET' && p === '/api/health') {
    const now = Date.now();
    let count = 0;
    for (const seen of online.values()) if (now - seen < ONLINE_WINDOW_MS) count++;
    return send(res, 200, {
      ok: true,
      version: VERSION,
      users: Object.keys(db.users).length,
      online: count,
      insecure: INSECURE,
      enforced: ENFORCE,
      founderSlotsLeft: Math.max(0, FOUNDER_LIMIT - (db.founders || 0)),
    });
  }

  if (req.method === 'GET' && p === '/api/store') {
    return send(res, 200, { enforced: ENFORCE, url: STORE_URL, ranks: RANKS });
  }

  if (req.method === 'GET' && p === '/api/news') {
    return send(res, 200, { news: db.news || DEFAULT_NEWS });
  }

  if (req.method === 'POST' && p === '/api/auth/challenge') {
    const body = await readBody(req);
    if (!NAME_RE.test(body.name || '') || !UUID_RE.test(body.uuid || '')) return send(res, 400, { error: 'Bad name or uuid' });
    const serverId = crypto.randomBytes(10).toString('hex');
    challenges.set(serverId, { name: body.name, uuid: normUuid(body.uuid), created: Date.now() });
    return send(res, 200, { serverId });
  }

  if (req.method === 'POST' && p === '/api/auth/verify') {
    const body = await readBody(req);
    const ch = challenges.get(body.serverId);
    challenges.delete(body.serverId);
    if (!ch || Date.now() - ch.created > CHALLENGE_TTL_MS || ch.name !== body.name) {
      return send(res, 401, { error: 'Challenge expired or invalid' });
    }
    let uuid = ch.uuid;
    let name = ch.name;
    if (body.offline) {
      if (!INSECURE) return send(res, 401, { error: 'Offline accounts are not accepted by this server' });
    } else {
      const r = await getJson(
        `https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${encodeURIComponent(name)}&serverId=${encodeURIComponent(body.serverId)}`
      );
      if (r.status !== 200 || !r.body || !r.body.id) return send(res, 401, { error: 'Mojang could not verify this session' });
      uuid = normUuid(r.body.id);
      name = r.body.name;
      if (uuid !== ch.uuid) return send(res, 401, { error: 'UUID mismatch' });
    }
    const token = crypto.randomBytes(32).toString('hex');
    db.tokens[token] = { uuid, name, created: Date.now() };
    settlePending(name, uuid);
    if (!db.users[uuid]) {
      db.users[uuid] = { name, loadout: sanitizeLoadout(null), created: Date.now() };
      // The first players get the Founder cape for good (real accounts only, except in dev mode).
      if ((db.founders || 0) < FOUNDER_LIMIT && (!body.offline || INSECURE)) {
        db.founders = (db.founders || 0) + 1;
        changeEntitlements(uuid, [FOUNDER_ITEM], true);
      }
    }
    db.users[uuid].name = name;
    online.set(uuid, Date.now());
    save();
    return send(res, 200, { token, uuid, name });
  }

  if (req.method === 'GET' && p === '/api/me') {
    const a = auth(req);
    if (!a) return send(res, 401, { error: 'Not signed in' });
    return send(res, 200, { uuid: a.uuid, user: db.users[a.uuid] || null, entitlements: entitlementsOf(a.uuid), supporter: isSupporter(a.uuid) });
  }

  if (req.method === 'PUT' && p === '/api/me/loadout') {
    const a = auth(req);
    if (!a) return send(res, 401, { error: 'Not signed in' });
    const body = await readBody(req);
    const user = db.users[a.uuid] || (db.users[a.uuid] = { name: a.name, created: Date.now() });
    user.loadout = sanitizeLoadout(body);
    user.updated = Date.now();
    online.set(a.uuid, Date.now());
    save();
    return send(res, 200, { ok: true, loadout: user.loadout });
  }

  if (req.method === 'POST' && p === '/api/heartbeat') {
    const a = auth(req);
    if (!a) return send(res, 401, { error: 'Not signed in' });
    online.set(a.uuid, Date.now());
    return send(res, 200, { ok: true });
  }

  if (req.method === 'POST' && p === '/api/players') {
    const body = await readBody(req);
    const list = Array.isArray(body.uuids) ? body.uuids.slice(0, 200) : [];
    const now = Date.now();
    const players = {};
    for (const raw of list) {
      if (typeof raw !== 'string' || !UUID_RE.test(raw)) continue;
      const u = normUuid(raw);
      const user = db.users[u];
      if (!user) continue;
      players[u] = {
        name: user.name,
        loadout: visibleLoadout(u, user.loadout),
        supporter: isSupporter(u),
        online: now - (online.get(u) || 0) < ONLINE_WINDOW_MS,
      };
    }
    return send(res, 200, { players });
  }

  const single = /^\/api\/players\/([0-9a-fA-F-]{32,36})$/.exec(p);
  if (req.method === 'GET' && single && UUID_RE.test(single[1])) {
    const u = normUuid(single[1]);
    const user = db.users[u];
    if (!user) return send(res, 404, { error: 'Not a Cool Client user' });
    return send(res, 200, { uuid: u, name: user.name, loadout: visibleLoadout(u, user.loadout), supporter: isSupporter(u) });
  }

  // Admin: grant or revoke cosmetics/ranks by hand (support tickets, giveaways, creators).
  const admin = /^\/api\/admin\/(grant|revoke)$/.exec(p);
  if (req.method === 'POST' && admin) {
    if (!isAdmin(req)) return send(res, 401, { error: 'Admin token required' });
    const body = await readBody(req);
    const result = await applyPurchase(body, body.items, admin[1] === 'grant');
    if (!result) return send(res, 400, { error: 'Give a uuid or a valid Minecraft username' });
    return send(res, 200, result);
  }

  // Payment provider webhook (or a small relay in front of one). The raw body must be signed:
  //   X-Signature: sha256=<hex HMAC-SHA256 of the body with WEBHOOK_SECRET>
  // Body: { id, type: "grant" | "revoke", uuid | username, items: [...] }. The id makes retries safe.
  if (req.method === 'POST' && p === '/api/webhooks/payment') {
    if (!WEBHOOK_SECRET) return send(res, 404, { error: 'Not found' });
    const raw = await readRaw(req);
    const sig = String(req.headers['x-signature'] || '');
    const expected = 'sha256=' + crypto.createHmac('sha256', WEBHOOK_SECRET).update(raw).digest('hex');
    if (!safeEqual(sig, expected)) return send(res, 401, { error: 'Bad signature' });
    const body = safeParse(raw.toString('utf8')) || {};
    if (typeof body.id !== 'string' || !body.id || body.id.length > 128) return send(res, 400, { error: 'Missing event id' });
    if (db.payments[body.id]) return send(res, 200, { ok: true, duplicate: true });
    const revoke = body.type === 'revoke';
    const result = await applyPurchase(body, body.items, !revoke);
    if (!result) return send(res, 400, { error: 'Give a uuid or a valid Minecraft username' });
    db.payments[body.id] = { type: revoke ? 'revoke' : 'grant', items: body.items, at: Date.now() };
    save();
    return send(res, 200, Object.assign({ ok: true }, result));
  }

  // Tebex (https://tebex.io) webhooks, straight from Tebex: no relay needed. Signature is
  // hex(HMAC-SHA256(secret, hex(SHA-256(raw body)))). Purchases grant, refunds and chargebacks
  // revoke, and an ended subscription revokes what its first payment granted.
  if (req.method === 'POST' && p === '/api/webhooks/tebex') {
    if (!TEBEX_SECRET) return send(res, 404, { error: 'Not found' });
    const raw = await readRaw(req);
    const bodyHash = crypto.createHash('sha256').update(raw).digest('hex');
    const expected = crypto.createHmac('sha256', TEBEX_SECRET).update(bodyHash).digest('hex');
    if (!safeEqual(String(req.headers['x-signature'] || ''), expected)) return send(res, 401, { error: 'Bad signature' });
    const ev = safeParse(raw.toString('utf8')) || {};
    if (ev.type === 'validation.webhook') return send(res, 200, { id: ev.id });
    if (!ev.id) return send(res, 400, { error: 'Missing event id' });
    const eventId = 'tebex:' + String(ev.id);
    if (db.payments[eventId]) return send(res, 200, { ok: true, duplicate: true });
    let payment = null;
    let grant = true;
    if (ev.type === 'payment.completed' || ev.type === 'payment.dispute.won') {
      payment = ev.subject;
    } else if (['payment.refunded', 'payment.dispute.opened', 'payment.dispute.lost'].includes(ev.type)) {
      payment = ev.subject;
      grant = false;
    } else if (ev.type === 'recurring-payment.ended') {
      payment = ev.subject && ev.subject.initial_payment;
      grant = false;
    }
    const results = [];
    if (payment) {
      for (const b of tebexBuyers(payment)) {
        const items = tebexItems(b.products);
        if (items.length) results.push(await applyPurchase(b.target, items, grant));
      }
    }
    db.payments[eventId] = { type: ev.type, at: Date.now(), results: results.length };
    save();
    return send(res, 200, { ok: true, applied: results });
  }

  return send(res, 404, { error: 'Not found' });
}

// ---------------------------------------------------------------- startup

function start(port) {
  load();
  const server = http.createServer((req, res) => {
    handle(req, res).catch((e) => {
      console.error(req.method, req.url, e.message);
      if (!res.headersSent) send(res, 400, { error: e.message });
    });
  });
  setInterval(() => {
    const now = Date.now();
    for (const [k, v] of challenges) if (now - v.created > CHALLENGE_TTL_MS) challenges.delete(k);
    for (const [k, v] of online) if (now - v > ONLINE_WINDOW_MS * 4) online.delete(k);
  }, 60000).unref();
  return new Promise((resolve) => {
    server.listen(port, () => {
      console.log(`Cool Client API ${VERSION} listening on :${server.address().port}${INSECURE ? ' (INSECURE dev mode: offline accounts allowed)' : ''}${ENFORCE ? ' (entitlements enforced)' : ''}`);
      resolve(server);
    });
  });
}

if (require.main === module) start(PORT);

module.exports = { start, sanitizeLoadout, normUuid, owns, visibleLoadout, tebexItems };
