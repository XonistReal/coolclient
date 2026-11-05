/**
 * The Cool Client API (../app.js) on Cloudflare Workers, free plan.
 *
 * One Durable Object holds all the data in memory, exactly like the Node server, and stores every
 * changed record (a user, a sign-in, an entitlement...) in its own SQLite-backed storage. The
 * Worker in front only drops junk paths and passes /api/* requests to it.
 */
import { DurableObject } from 'cloudflare:workers';
import api from '../app.js';

const { createApp, changedRecords, dbFromRecords, HEADERS, MAX_BODY } = api;
const BATCH = 128; // storage.put/delete take at most 128 keys at a time

function respond(status, body) {
  return new Response(status === 204 ? null : JSON.stringify(body), { status, headers: HEADERS });
}

export class CoolClientApi extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    ctx.blockConcurrencyWhile(async () => {
      const db = dbFromRecords(await ctx.storage.list());
      this.app = createApp({ env, db, persist: (data, changed) => this.persist(data, changed) });
    });
  }

  async persist(data, changed) {
    const { puts, deletes } = changedRecords(data, changed);
    const entries = Object.entries(puts);
    for (let i = 0; i < entries.length; i += BATCH) await this.ctx.storage.put(Object.fromEntries(entries.slice(i, i + BATCH)));
    for (let i = 0; i < deletes.length; i += BATCH) await this.ctx.storage.delete(deletes.slice(i, i + BATCH));
  }

  async fetch(request) {
    if (parseInt(request.headers.get('content-length') || '0', 10) > MAX_BODY) return respond(413, { error: 'Body too large' });
    const body = request.method === 'GET' || request.method === 'HEAD' ? null : new Uint8Array(await request.arrayBuffer());
    const headers = {};
    for (const [k, v] of request.headers) headers[k] = v;
    const ip = request.headers.get('cf-connecting-ip') || 'unknown';
    const r = await this.app.handle({ method: request.method, url: request.url, headers, ip, body });
    return respond(r.status, r.body);
  }
}

export default {
  async fetch(request, env) {
    if (!new URL(request.url).pathname.startsWith('/api/')) return respond(404, { error: 'Not found' });
    return env.API.get(env.API.idFromName('main')).fetch(request);
  },
};
