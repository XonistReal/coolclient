# Monetizing Cool Client (while staying open source)

Cool Client's code is public, so anything enforced only in the client can be patched out.
The model that works for open-source clients is the same one Lunar and Badlion use for
cosmetics: **sell what other players see, not what the client can do.**

- Everyone can equip any cosmetic locally. That's fine: it's a preview.
- The API server decides what *other* players see. With enforcement on, it only shows a
  cosmetic on you if your account owns it. A modified client can't change that, because the
  server holds the ownership list and other players fetch your loadout from the server.
- Every gameplay module stays free. Selling gameplay features would break Mojang's rules (below).

## What's built

| Piece | Where |
|---|---|
| Unlock tier per cosmetic: Free, Supporter, Store, Exclusive, Media, Owner | `core/.../cosmetic/Unlock.java`, `server/catalog.json` |
| Ownership from the server, "Unlock" button and tier chips in the locker | `core/.../cosmetic/Entitlements.java`, `CosmeticsScreen` |
| Nametag badges instead of the aqua one: gold star (supporters), red play (Media), purple crown (Owner) | `PlayerRendererMixin` (1.21.4), `EntityRendererMixin` (1.8.9) |
| Server: enforcement, store info, admin grants, Tebex webhook, generic signed webhook | `server/app.js` (Node: `server.js`, Cloudflare: `worker/`) |
| Command-line grants for donors, giveaways and creators | `server/admin.js` |
| Test that the client and server tier lists match | `core/src/test/.../CatalogSyncTest.java` |

Default tiers come from rarity: common and rare are free, epic and legendary come with
**Supporter**, mythic items are sold individually or come with **Supporter+**. To change one,
edit `Unlock.forRarity` (or give a cosmetic its own tier) and `server/catalog.json` together;
`./gradlew :core:test` fails if they drift apart.

Nothing changes for players until you turn enforcement on, so the beta keeps "everything is free".

Three tiers are never sold and are checked even with enforcement off:

- **Exclusive**: the Founder cape, given to the first sign-ins (`FOUNDER_LIMIT`).
- **Media**: for creators. The `media` rank shows the Media capes (Media, On Air, Premiere) and a
  red play badge, and covers everything Supporter+ does. Grant it by hand with `admin.js`; a store
  package can't grant it.
- **Owner**: the project owner's rank. Shows the Owner cape and a purple crown badge, and covers
  every cosmetic. Only an admin grant gives it.

## Server settings

| Variable | Meaning |
|---|---|
| `ENFORCE_ENTITLEMENTS=1` | Hide unowned paid cosmetics from other players, and show locks in the client. |
| `STORE_URL=https://...` | Opened by the client's Unlock button (`?username=<name>` is appended). |
| `ADMIN_TOKEN=<long random string>` | Enables `/api/admin/grant`, `/api/admin/revoke` and `admin.js`. |
| `TEBEX_SECRET=<from Tebex>` | Enables `POST /api/webhooks/tebex` (Tebex directly, no relay). |
| `TEBEX_PACKAGES={"123":["supporter"]}` | Optional: which Tebex package grants what. Without it, a package named `Supporter`, `Supporter+` or a cosmetic id (`cape_aurora`) grants that. |
| `WEBHOOK_SECRET=<long random string>` | Enables `POST /api/webhooks/payment` (any other provider, through a relay). |

Grant by hand (donors, support tickets, giveaways, creators):

```bash
API_URL=https://api.example.com ADMIN_TOKEN=... node server/admin.js grant Notch supporter
```

Items are cosmetic ids from `catalog.json` or the ranks `supporter`, `supporter_plus`, `media` and `owner`. A name
resolves through Mojang; if that fails (or in dev mode) the grant waits until the player first signs in.

## Payment webhooks

**Tebex**, directly (no relay):

1. Name the packages `Supporter` and `Supporter+`, or after a cosmetic id (`cape_aurora`), or map
   package ids with `TEBEX_PACKAGES`.
2. In Tebex, Developers, then Webhooks: add `https://<your api>/api/webhooks/tebex`, put the secret
   in `TEBEX_SECRET`, restart the server, then press **Validate**.
3. Set `STORE_URL` to your Tebex webstore address.

The server grants on `payment.completed` (and `payment.dispute.won`), revokes on refunds and
chargebacks, revokes a subscription's items when `recurring-payment.ended` arrives, ignores
replays, and holds purchases for players who haven't launched the client yet.

**Any other provider** through the generic signed webhook. It takes one provider-neutral shape, so
a ~30-line relay can adapt any provider:

```
POST /api/webhooks/payment
X-Signature: sha256=<hex HMAC-SHA256 of the raw body, keyed with WEBHOOK_SECRET>

{ "id": "unique-event-id", "type": "grant" | "revoke", "username": "Notch", "items": ["supporter"] }
```

The `id` makes retries safe: a repeated id is acknowledged and ignored. Use `revoke` for refunds
and chargebacks. For Stripe, put the username in a checkout custom field and relay
`checkout.session.completed` and `charge.refunded`. Keep relays and provider keys out of this repo.

## Staying within Mojang's rules

Mojang's [Minecraft Usage Guidelines](https://www.minecraft.net/en-us/usage-guidelines) and EULA
allow selling cosmetics in a client as long as they don't affect gameplay. Practical rules:

- Never sell modules, performance features or anything that changes how the game plays.
- Don't use Mojang or Minecraft branding in the store, and say you're not affiliated with Mojang.
- Don't sell access to the game itself, or accounts.

This is a summary, not legal advice; read the guidelines before you launch.

## Licensing

The code is GPL-3.0 ([LICENSE](LICENSE)): forks must stay open source, so nobody can close it up
and resell it. The name and logo are kept separately ([TRADEMARKS.md](TRADEMARKS.md)), so the
money stays with whoever runs the official API server and owns the brand.
