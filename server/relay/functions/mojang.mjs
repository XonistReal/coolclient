/**
 * Cool Client sign-in relay (Netlify Function).
 *
 * Mojang's session server refuses requests from Cloudflare's network, so the Cloudflare-hosted
 * API asks this function to make its two Mojang calls instead: the sign-in check (hasJoined, what
 * every Minecraft server does when a player joins) and the name-to-UUID lookup used for grants.
 * Only the API can use it: requests must carry the shared RELAY_KEY.
 *
 *   GET /mojang?kind=hasJoined&username=<name>&serverId=<hex>   X-Relay-Key: <key>
 *   GET /mojang?kind=profile&username=<name>                     X-Relay-Key: <key>
 *   -> 200 { status: <Mojang's status>, body: <Mojang's JSON or null> }
 */
const NAME_RE = /^[A-Za-z0-9_]{1,16}$/;
const SERVER_ID_RE = /^-?[0-9a-f]{1,40}$/;

function json(status, body) {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

function safeEqual(a, b) {
  const x = new TextEncoder().encode(String(a));
  const y = new TextEncoder().encode(String(b));
  if (x.length !== y.length) return false;
  let diff = 0;
  for (let i = 0; i < x.length; i++) diff |= x[i] ^ y[i];
  return diff === 0;
}

export default async (req) => {
  const key = process.env.RELAY_KEY || '';
  if (!key || !safeEqual(req.headers.get('x-relay-key') || '', key)) return json(401, { error: 'Bad relay key' });
  const q = new URL(req.url).searchParams;
  const username = q.get('username') || '';
  if (!NAME_RE.test(username)) return json(400, { error: 'Bad username' });
  let url;
  if (q.get('kind') === 'hasJoined') {
    const serverId = q.get('serverId') || '';
    if (!SERVER_ID_RE.test(serverId)) return json(400, { error: 'Bad serverId' });
    url = `https://sessionserver.mojang.com/session/minecraft/hasJoined?username=${username}&serverId=${serverId}`;
  } else if (q.get('kind') === 'profile') {
    url = `https://api.mojang.com/users/profiles/minecraft/${username}`;
  } else {
    return json(400, { error: 'Bad kind' });
  }
  try {
    const r = await fetch(url, {
      headers: { 'User-Agent': 'CoolClient-API/relay (+https://github.com/XonistReal/coolclient)' },
      signal: AbortSignal.timeout(5000),
    });
    const text = await r.text();
    let body = null;
    try {
      body = text ? JSON.parse(text) : null;
    } catch (e) {
      body = null;
    }
    return json(200, { status: r.status, body });
  } catch (e) {
    return json(502, { error: 'Could not reach Mojang' });
  }
};

export const config = { path: '/mojang' };
