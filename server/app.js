'use strict';
/**
 * Cool Client API: cosmetics loadouts, entitlements, presence and launcher news. Zero dependencies.
 *
 * This file is the whole API and uses only web-standard APIs (fetch, Web Crypto), so the same code
 * runs on Node (server.js: Docker, any VPS) and on Cloudflare Workers (worker/). The host only
 * stores the data and passes requests in.
 *
 * Identity: the client asks for a random serverId (/api/auth/challenge), performs the Mojang
 * session "join" with it, then calls /api/auth/verify. We confirm with Mojang's hasJoined
 * endpoint (exactly what Minecraft servers do) before issuing a session token.
 *
 * Monetization: the client is open source, so anyone can equip anything locally. What this
 * server sells is visibility: with ENFORCE_ENTITLEMENTS=1 other players only see cosmetics the
 * wearer owns (catalog.json says which are free). Ownership is granted by an admin call or a
 * signed payment webhook. Exclusive and Media cosmetics are always checked. See MONETIZATION.md.
 *
 * Settings (environment variables on Node, vars/secrets on Cloudflare):
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
 *   MOJANG_RELAY          optional: URL of the sign-in relay (server/relay/) for hosts Mojang refuses,
 *                         such as Cloudflare; RELAY_KEY is the key it expects
 * Node-only settings (PORT, DATA_DIR, TRUST_PROXY) are in server.js.
 */
const CATALOG = require('./catalog.json').cosmetics;

const VERSION = '0.1.3-beta';
const FOUNDER_ITEM = 'cape_founder';
const MEDIA = 'media';
const OWNER = 'owner';
const ONLINE_WINDOW_MS = 3 * 60 * 1000;
const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const CHALLENGE_TTL_MS = 2 * 60 * 1000;
const MAX_BODY = 64 * 1024;
const SLOTS = ['CAPE', 'WINGS', 'HAT', 'HALO', 'BANDANA', 'AURA'];
/**
 * Ranks: Supporter and Supporter+ are sold; Media is given to creators (Supporter+ plus the Media
 * cosmetics); Owner is the project owner's (everything, plus the Owner cosmetics).
 */
const RANKS = ['supporter', 'supporter_plus', MEDIA, OWNER];
/** Ranks and cosmetic tiers that are only ever given by an admin, never sold. */
const GIVEN_RANKS = [MEDIA, OWNER];
const GIVEN_TIERS = ['exclusive', 'media', 'owner'];
/** Data that is stored, by collection. Everything else (challenges, presence, rate limits) lives in memory. */
const COLLECTIONS = ['users', 'tokens', 'entitlements', 'payments', 'pending'];

const HEADERS = {
  'Content-Type': 'application/json',
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'Content-Type, Authorization',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, OPTIONS',
};

const UUID_RE = /^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$/i;
const NAME_RE = /^[A-Za-z0-9_]{1,16}$/;
const ID_RE = /^[a-z0-9_]{1,40}$/;
const HEX_RE = /^[0-9A-Fa-f]{8}$/;

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

// ---------------------------------------------------------------- pure helpers

function emptyDb() {
  return { users: {}, tokens: {}, news: null, entitlements: {}, payments: {}, pending: {}, founders: 0 };
}

function normUuid(u) {
  const h = String(u).replace(/-/g, '').toLowerCase();
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

function safeParse(t) {
  try {
    return JSON.parse(t);
  } catch (e) {
    return null;
  }
}

function parseJson(text, name) {
  try {
    return JSON.parse(text || '{}');
  } catch (e) {
    console.error(`${name} is not valid JSON; ignoring it`);
    return {};
  }
}

const encoder = new TextEncoder();
const decoder = new TextDecoder();

function hex(bytes) {
  let s = '';
  for (const b of new Uint8Array(bytes)) s += b.toString(16).padStart(2, '0');
  return s;
}

function randomHex(n) {
  return hex(crypto.getRandomValues(new Uint8Array(n)));
}

async function sha256Hex(data) {
  return hex(await crypto.subtle.digest('SHA-256', typeof data === 'string' ? encoder.encode(data) : data));
}

async function hmacHex(secret, data) {
  const key = await crypto.subtle.importKey('raw', encoder.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
  return hex(await crypto.subtle.sign('HMAC', key, typeof data === 'string' ? encoder.encode(data) : data));
}

/** Constant-time string comparison (for tokens and signatures). */
function safeEqual(a, b) {
  const x = encoder.encode(String(a));
  const y = encoder.encode(String(b));
  if (x.length !== y.length) return false;
  let diff = 0;
  for (let i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
  return diff === 0;
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

/** Items a set of Tebex packages grants: TEBEX_PACKAGES first, else a package named after a rank or id. */
function tebexItems(products, packages) {
  const out = [];
  for (const p of Array.isArray(products) ? products : []) {
    const mapped = (packages || {})[String(p && p.id)];
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
    // Given ranks and cosmetics (Media, Owner, Founder) are never sold, so a store package can't grant them by name.
    if ((RANKS.includes(key) && !GIVEN_RANKS.includes(key)) || (CATALOG[key] && !GIVEN_TIERS.includes(CATALOG[key]))) out.push(key);
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

/**
 * For hosts that store one record per key (the Cloudflare worker): the records a persist() call
 * should write ({ "<collection>/<key>": value }) and delete. Keys are the `changed` strings.
 */
function changedRecords(db, changed) {
  const puts = {};
  const deletes = [];
  for (const key of changed) {
    const i = key.indexOf('/');
    const col = key.slice(0, i);
    const id = key.slice(i + 1);
    const v = col === 'meta' ? db[id] : (db[col] || {})[id];
    if (v === undefined) deletes.push(key);
    else puts[key] = v;
  }
  return { puts, deletes };
}

/** Rebuilds the data from stored records: an iterable of [key, value] as written by changedRecords. */
function dbFromRecords(entries) {
  const db = emptyDb();
  for (const [key, value] of entries) {
    const i = key.indexOf('/');
    const col = key.slice(0, i);
    const id = key.slice(i + 1);
    if (col === 'meta') db[id] = value;
    else if (COLLECTIONS.includes(col)) db[col][id] = value;
  }
  return db;
}

async function fetchJson(url, headers) {
  const res = await fetch(url, {
    headers: Object.assign({ 'User-Agent': `CoolClient-API/${VERSION} (+https://github.com/XonistReal/coolclient)` }, headers || {}),
    signal: AbortSignal.timeout(8000),
  });
  const text = await res.text();
  return { status: res.status, body: text ? safeParse(text) : null };
}

// ---------------------------------------------------------------- the app

/**
 * Creates the API.
 *   env      settings (see the header), all strings
 *   db       stored data to start from (emptyDb() shape); omitted = a fresh server
 *   persist  persist(db, changed) runs after any request that changed stored data. `changed` is a
 *            Set of "<collection>/<key>" strings plus "meta/founders"; a key whose value is now
 *            missing was deleted. It may return a promise; the response waits for it.
 *
 * Returns { handle(req) -> Promise<{status, body}>, db }. req is { method, url, headers, ip, body },
 * with lower-case header names and body as bytes (Uint8Array) or a string.
 */
function createApp(opts) {
  const env = (opts && opts.env) || {};
  const persist = (opts && opts.persist) || (() => {});
  const getJson = (opts && opts.fetchJson) || fetchJson;
  const RELAY = (env.MOJANG_RELAY || '').replace(/\/+$/, '');
  const RELAY_KEY = env.RELAY_KEY || '';

  /**
   * Asks Mojang: kind "hasJoined" (sign-in check) or "profile" (name to UUID). Hosts Mojang refuses
   * (Cloudflare) set MOJANG_RELAY to the relay in server/relay/, which makes the call for us.
   */
  async function mojang(kind, username, serverId) {
    if (RELAY) {
      const q = new URLSearchParams({ kind, username });
      if (serverId) q.set('serverId', serverId);
      const r = await getJson(`${RELAY}?${q}`, { 'X-Relay-Key': RELAY_KEY });
      if (r.status !== 200 || !r.body || typeof r.body.status !== 'number') throw new Error(`relay answered ${r.status}`);
      return { status: r.body.status, body: r.body.body };
    }
    const u = encodeURIComponent(username);
    return getJson(
      kind === 'hasJoined'
        ? `https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${u}&serverId=${encodeURIComponent(serverId)}`
        : `https://api.mojang.com/users/profiles/minecraft/${u}`
    );
  }
  const INSECURE = env.AURORA_INSECURE === '1';
  const ENFORCE = env.ENFORCE_ENTITLEMENTS === '1';
  const STORE_URL = env.STORE_URL || '';
  const ADMIN_TOKEN = env.ADMIN_TOKEN || '';
  const WEBHOOK_SECRET = env.WEBHOOK_SECRET || '';
  const TEBEX_SECRET = env.TEBEX_SECRET || '';
  const TEBEX_PACKAGES = parseJson(env.TEBEX_PACKAGES, 'TEBEX_PACKAGES');
  const FOUNDER_LIMIT = parseInt(env.FOUNDER_LIMIT || '1000', 10);

  const db = Object.assign(emptyDb(), (opts && opts.db) || {});
  for (const c of COLLECTIONS) if (!db[c] || typeof db[c] !== 'object') db[c] = {};
  const changed = new Set();
  const touch = (collection, key) => changed.add(`${collection}/${key}`);

  const challenges = new Map(); // serverId -> { name, uuid, created }
  const online = new Map(); // uuid -> lastSeen
  const buckets = new Map(); // ip -> { tokens, last }
  let lastSweep = Date.now();

  // Expired sign-ins are dropped on start (stored on the first request).
  for (const [t, v] of Object.entries(db.tokens)) {
    if (!v || Date.now() - v.created > TOKEN_TTL_MS) {
      delete db.tokens[t];
      touch('tokens', t);
    }
  }

  function sweep(now) {
    lastSweep = now;
    for (const [k, v] of challenges) if (now - v.created > CHALLENGE_TTL_MS) challenges.delete(k);
    for (const [k, v] of online) if (now - v > ONLINE_WINDOW_MS * 4) online.delete(k);
    for (const [k, v] of buckets) if (now - v.last > 120000) buckets.delete(k);
  }

  // ------------------------------------------------------------ entitlements

  function entitlementsOf(uuid) {
    const e = db.entitlements[uuid];
    return e && Array.isArray(e.items) ? e.items : [];
  }

  function isSupporter(uuid) {
    return entitlementsOf(uuid).some((i) => RANKS.includes(i));
  }

  function isMedia(uuid) {
    return entitlementsOf(uuid).includes(MEDIA);
  }

  function isOwner(uuid) {
    return entitlementsOf(uuid).includes(OWNER);
  }

  /**
   * Free items, owned items, and whatever the owner's rank includes. Unknown ids are never owned.
   * Media covers everything Supporter+ does, plus the Media cosmetics; exclusives need their own
   * grant. The Owner rank covers everything.
   */
  function owns(uuid, id) {
    const tier = CATALOG[id];
    if (!tier) return false;
    if (tier === 'free') return true;
    const items = entitlementsOf(uuid);
    if (items.includes(id) || items.includes(OWNER)) return true;
    if (tier === 'exclusive' || tier === 'owner') return false;
    if (tier === 'media') return items.includes(MEDIA);
    if (items.includes('supporter_plus') || items.includes(MEDIA)) return true;
    return tier === 'supporter' && items.includes('supporter');
  }

  /**
   * What other players get to see. With enforcement on, unowned cosmetics are dropped; given ones
   * (Founder, Media, Owner) are always checked, even while everything else is free.
   */
  function visibleLoadout(uuid, loadout) {
    const l = loadout || sanitizeLoadout(null);
    const out = { equipped: {}, colors: {}, chroma: {} };
    for (const [slot, id] of Object.entries(l.equipped || {})) {
      if ((ENFORCE || GIVEN_TIERS.includes(CATALOG[id])) && !owns(uuid, id)) continue;
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
    touch('entitlements', uuid);
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
      const r = await mojang('profile', body.username);
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
    touch('pending', key);
    return { pending: true, username: target.username };
  }

  function settlePending(name, uuid) {
    const key = String(name).toLowerCase();
    const list = db.pending[key];
    if (!list) return;
    for (const p of list) changeEntitlements(uuid, p.items, p.grant);
    delete db.pending[key];
    touch('pending', key);
  }

  function isAdmin(req) {
    if (!ADMIN_TOKEN) return false;
    const h = String(req.headers.authorization || '');
    return h.startsWith('Bearer ') && safeEqual(h.slice(7), ADMIN_TOKEN);
  }

  function auth(req) {
    const m = /^Bearer ([0-9a-f]{64})$/.exec(String(req.headers.authorization || ''));
    if (!m) return null;
    const t = db.tokens[m[1]];
    if (!t || Date.now() - t.created > TOKEN_TTL_MS) return null;
    return t;
  }

  // Per-IP token bucket: 120 requests per minute.
  function rateLimited(ip) {
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

  // ------------------------------------------------------------ routes

  const ok = (body) => ({ status: 200, body });
  const fail = (status, error) => ({ status, body: { error } });

  async function route(req, raw) {
    if (req.method === 'OPTIONS') return { status: 204, body: {} };
    if (rateLimited(req.ip || 'unknown')) return fail(429, 'Too many requests');
    const now = Date.now();
    if (now - lastSweep > 60000) sweep(now);
    const p = new URL(req.url, 'http://localhost').pathname;
    const text = () => decoder.decode(raw);
    const json = () => {
      if (!raw.length) return {};
      const v = safeParse(text());
      if (v === null) throw new Error('Invalid JSON');
      return v;
    };

    if (req.method === 'GET' && p === '/api/health') {
      let count = 0;
      for (const seen of online.values()) if (now - seen < ONLINE_WINDOW_MS) count++;
      return ok({
        ok: true,
        version: VERSION,
        users: Object.keys(db.users).length,
        online: count,
        insecure: INSECURE,
        enforced: ENFORCE,
        founderSlotsLeft: Math.max(0, FOUNDER_LIMIT - (db.founders || 0)),
      });
    }

    if (req.method === 'GET' && p === '/api/store') return ok({ enforced: ENFORCE, url: STORE_URL, ranks: RANKS.filter((r) => !GIVEN_RANKS.includes(r)) });

    if (req.method === 'GET' && p === '/api/news') return ok({ news: db.news || DEFAULT_NEWS });

    if (req.method === 'POST' && p === '/api/auth/challenge') {
      const body = json();
      if (!NAME_RE.test(body.name || '') || !UUID_RE.test(body.uuid || '')) return fail(400, 'Bad name or uuid');
      const serverId = randomHex(10);
      challenges.set(serverId, { name: body.name, uuid: normUuid(body.uuid), created: now });
      return ok({ serverId });
    }

    if (req.method === 'POST' && p === '/api/auth/verify') {
      const body = json();
      const ch = challenges.get(body.serverId);
      challenges.delete(body.serverId);
      if (!ch || now - ch.created > CHALLENGE_TTL_MS || ch.name !== body.name) return fail(401, 'Challenge expired or invalid');
      let uuid = ch.uuid;
      let name = ch.name;
      if (body.offline) {
        if (!INSECURE) return fail(401, 'Offline accounts are not accepted by this server');
      } else {
        let r;
        try {
          r = await mojang('hasJoined', name, String(body.serverId));
        } catch (e) {
          console.error('Mojang session check failed:', e.message);
          return fail(502, 'Could not reach Mojang; try again later');
        }
        // 204 means "this player didn't join with that serverId"; anything else unexpected is Mojang's problem, not the player's.
        if (r.status !== 200 && r.status !== 204) {
          console.error('Mojang session check returned', r.status);
          return fail(502, `Mojang returned ${r.status}; try again later`);
        }
        if (r.status !== 200 || !r.body || !r.body.id) return fail(401, 'Mojang could not verify this session');
        uuid = normUuid(r.body.id);
        name = r.body.name;
        if (uuid !== ch.uuid) return fail(401, 'UUID mismatch');
      }
      const token = randomHex(32);
      db.tokens[token] = { uuid, name, created: Date.now() };
      touch('tokens', token);
      settlePending(name, uuid);
      if (!db.users[uuid]) {
        db.users[uuid] = { name, loadout: sanitizeLoadout(null), created: Date.now() };
        // The first players get the Founder cape for good (real accounts only, except in dev mode).
        if ((db.founders || 0) < FOUNDER_LIMIT && (!body.offline || INSECURE)) {
          db.founders = (db.founders || 0) + 1;
          changed.add('meta/founders');
          changeEntitlements(uuid, [FOUNDER_ITEM], true);
        }
      }
      db.users[uuid].name = name;
      touch('users', uuid);
      online.set(uuid, Date.now());
      return ok({ token, uuid, name });
    }

    if (req.method === 'GET' && p === '/api/me') {
      const a = auth(req);
      if (!a) return fail(401, 'Not signed in');
      return ok({ uuid: a.uuid, user: db.users[a.uuid] || null, entitlements: entitlementsOf(a.uuid), supporter: isSupporter(a.uuid), media: isMedia(a.uuid), owner: isOwner(a.uuid) });
    }

    if (req.method === 'PUT' && p === '/api/me/loadout') {
      const a = auth(req);
      if (!a) return fail(401, 'Not signed in');
      const body = json();
      const user = db.users[a.uuid] || (db.users[a.uuid] = { name: a.name, created: Date.now() });
      user.loadout = sanitizeLoadout(body);
      user.updated = Date.now();
      touch('users', a.uuid);
      online.set(a.uuid, Date.now());
      return ok({ ok: true, loadout: user.loadout });
    }

    if (req.method === 'POST' && p === '/api/heartbeat') {
      const a = auth(req);
      if (!a) return fail(401, 'Not signed in');
      online.set(a.uuid, Date.now());
      return ok({ ok: true });
    }

    if (req.method === 'POST' && p === '/api/players') {
      const body = json();
      const list = Array.isArray(body.uuids) ? body.uuids.slice(0, 200) : [];
      const players = {};
      for (const rawId of list) {
        if (typeof rawId !== 'string' || !UUID_RE.test(rawId)) continue;
        const u = normUuid(rawId);
        const user = db.users[u];
        if (!user) continue;
        players[u] = {
          name: user.name,
          loadout: visibleLoadout(u, user.loadout),
          supporter: isSupporter(u),
          media: isMedia(u),
          owner: isOwner(u),
          online: now - (online.get(u) || 0) < ONLINE_WINDOW_MS,
        };
      }
      return ok({ players });
    }

    const single = /^\/api\/players\/([0-9a-fA-F-]{32,36})$/.exec(p);
    if (req.method === 'GET' && single && UUID_RE.test(single[1])) {
      const u = normUuid(single[1]);
      const user = db.users[u];
      if (!user) return fail(404, 'Not a Cool Client user');
      return ok({ uuid: u, name: user.name, loadout: visibleLoadout(u, user.loadout), supporter: isSupporter(u), media: isMedia(u), owner: isOwner(u) });
    }

    // Admin: grant or revoke cosmetics/ranks by hand (support tickets, giveaways, creators).
    const admin = /^\/api\/admin\/(grant|revoke)$/.exec(p);
    if (req.method === 'POST' && admin) {
      if (!isAdmin(req)) return fail(401, 'Admin token required');
      const body = json();
      const result = await applyPurchase(body, body.items, admin[1] === 'grant');
      if (!result) return fail(400, 'Give a uuid or a valid Minecraft username');
      return ok(result);
    }

    // Payment provider webhook (or a small relay in front of one). The raw body must be signed:
    //   X-Signature: sha256=<hex HMAC-SHA256 of the body with WEBHOOK_SECRET>
    // Body: { id, type: "grant" | "revoke", uuid | username, items: [...] }. The id makes retries safe.
    if (req.method === 'POST' && p === '/api/webhooks/payment') {
      if (!WEBHOOK_SECRET) return fail(404, 'Not found');
      const expected = 'sha256=' + (await hmacHex(WEBHOOK_SECRET, raw));
      if (!safeEqual(String(req.headers['x-signature'] || ''), expected)) return fail(401, 'Bad signature');
      const body = safeParse(text()) || {};
      if (typeof body.id !== 'string' || !body.id || body.id.length > 128) return fail(400, 'Missing event id');
      if (db.payments[body.id]) return ok({ ok: true, duplicate: true });
      const revoke = body.type === 'revoke';
      const result = await applyPurchase(body, body.items, !revoke);
      if (!result) return fail(400, 'Give a uuid or a valid Minecraft username');
      db.payments[body.id] = { type: revoke ? 'revoke' : 'grant', items: body.items, at: Date.now() };
      touch('payments', body.id);
      return ok(Object.assign({ ok: true }, result));
    }

    // Tebex (https://tebex.io) webhooks, straight from Tebex: no relay needed. Signature is
    // hex(HMAC-SHA256(secret, hex(SHA-256(raw body)))). Purchases grant, refunds and chargebacks
    // revoke, and an ended subscription revokes what its first payment granted.
    if (req.method === 'POST' && p === '/api/webhooks/tebex') {
      if (!TEBEX_SECRET) return fail(404, 'Not found');
      const expected = await hmacHex(TEBEX_SECRET, await sha256Hex(raw));
      if (!safeEqual(String(req.headers['x-signature'] || ''), expected)) return fail(401, 'Bad signature');
      const ev = safeParse(text()) || {};
      if (ev.type === 'validation.webhook') return ok({ id: ev.id });
      if (!ev.id) return fail(400, 'Missing event id');
      const eventId = 'tebex:' + String(ev.id);
      if (db.payments[eventId]) return ok({ ok: true, duplicate: true });
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
          const items = tebexItems(b.products, TEBEX_PACKAGES);
          if (items.length) results.push(await applyPurchase(b.target, items, grant));
        }
      }
      db.payments[eventId] = { type: ev.type, at: Date.now(), results: results.length };
      touch('payments', eventId);
      return ok({ ok: true, applied: results });
    }

    return fail(404, 'Not found');
  }

  async function handle(req) {
    const raw = req.body == null ? new Uint8Array(0) : typeof req.body === 'string' ? encoder.encode(req.body) : new Uint8Array(req.body);
    let res;
    if (raw.length > MAX_BODY) {
      res = fail(413, 'Body too large');
    } else {
      try {
        res = await route(Object.assign({ headers: {} }, req), raw);
      } catch (e) {
        console.error(req.method, req.url, e.message);
        res = fail(400, e.message);
      }
    }
    if (changed.size) {
      const c = new Set(changed);
      changed.clear();
      await persist(db, c);
    }
    return res;
  }

  return { handle, db, owns, visibleLoadout };
}

module.exports = { createApp, emptyDb, changedRecords, dbFromRecords, sanitizeLoadout, normUuid, tebexItems, HEADERS, COLLECTIONS, VERSION, MAX_BODY };
