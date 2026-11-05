#!/usr/bin/env node
'use strict';
/**
 * Grant or revoke cosmetics and ranks by hand, e.g. for Ko-fi donors, giveaways or creators.
 *
 *   API_URL=https://api.example.com ADMIN_TOKEN=... node admin.js grant Notch supporter
 *   node admin.js revoke Notch cape_aurora
 *   node admin.js grant 069a79f4-44e9-4726-a5be-fca90e38aaf5 supporter_plus
 *
 * The player is a Minecraft name or UUID; items are ranks (supporter, supporter_plus, media, owner)
 * or cosmetic ids from catalog.json. media is for creators: Supporter+ plus the Media capes; owner is
 * the project owner's (everything, plus the Owner cape). Neither is ever sold.
 * A name that has never signed in is resolved through Mojang, or held until
 * that player first signs in.
 */
const [, , action, player, ...items] = process.argv;
const api = (process.env.API_URL || 'http://localhost:8787').replace(/\/+$/, '');
const token = process.env.ADMIN_TOKEN;

if (!['grant', 'revoke'].includes(action) || !player || !items.length || !token) {
  console.error('Usage: API_URL=<url> ADMIN_TOKEN=<token> node admin.js grant|revoke <name|uuid> <item...>');
  process.exit(2);
}

const isUuid = /^[0-9a-f]{8}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{4}-?[0-9a-f]{12}$/i.test(player);
fetch(`${api}/api/admin/${action}`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
  body: JSON.stringify(Object.assign(isUuid ? { uuid: player } : { username: player }, { items })),
})
  .then(async (res) => {
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.error || `HTTP ${res.status}`);
    if (body.pending) console.log(`${player} hasn't signed in yet; it applies on their first sign-in.`);
    else console.log(`${body.uuid}: ${body.entitlements.join(', ') || '(nothing)'}`);
  })
  .catch((e) => {
    console.error('Failed:', e.message);
    process.exit(1);
  });
