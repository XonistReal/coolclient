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
| Unlock tier per cosmetic: Free, Supporter, Store | `core/.../cosmetic/Unlock.java`, `server/catalog.json` |
| Ownership from the server, "Unlock" button and tier chips in the locker | `core/.../cosmetic/Entitlements.java`, `CosmeticsScreen` |
| Gold nametag badge for supporters (instead of the aqua one) | `PlayerRendererMixin` (1.21.4), `EntityRendererMixin` (1.8.9) |
| Server: enforcement, store info, admin grants, signed payment webhook | `server/server.js` |
| Test that the client and server tier lists match | `core/src/test/.../CatalogSyncTest.java` |

Default tiers come from rarity: common and rare are free, epic and legendary come with
**Supporter**, mythic items are sold individually or come with **Supporter+**. To change one,
edit `Unlock.forRarity` (or give a cosmetic its own tier) and `server/catalog.json` together;
`./gradlew :core:test` fails if they drift apart.

Nothing changes for players until you turn enforcement on, so the beta keeps "everything is free".

## Turning it on

Run the API server with:

| Variable | Meaning |
|---|---|
| `ENFORCE_ENTITLEMENTS=1` | Hide unowned paid cosmetics from other players, and show locks in the client. |
| `STORE_URL=https://...` | Opened by the client's Unlock button (`?username=<name>` is appended). |
| `ADMIN_TOKEN=<long random string>` | Enables `POST /api/admin/grant` and `/api/admin/revoke`. |
| `WEBHOOK_SECRET=<long random string>` | Enables `POST /api/webhooks/payment`. |

Grant by hand (support tickets, giveaways, creator codes):

```bash
curl -X POST https://api.example.com/api/admin/grant \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{"username":"Notch","items":["supporter"]}'
```

`items` takes cosmetic ids from `catalog.json` and the ranks `supporter` and `supporter_plus`.
A `username` only resolves once that player has signed in to the API; a `uuid` always works.

## Connecting a payment provider

The webhook takes one simple, provider-neutral shape, so the store can be anything:

```
POST /api/webhooks/payment
X-Signature: sha256=<hex HMAC-SHA256 of the raw body, keyed with WEBHOOK_SECRET>

{ "id": "unique-event-id", "type": "grant" | "revoke", "username": "Notch", "items": ["supporter"] }
```

The `id` makes retries safe: a repeated id is acknowledged and ignored. Use `revoke` for refunds
and chargebacks.

Keep the relay (and its provider keys) out of this repo, or in a private one.

## Staying within Mojang's rules

Mojang's [Minecraft Usage Guidelines](https://www.minecraft.net/en-us/usage-guidelines) and EULA
allow selling cosmetics in a client as long as they don't affect gameplay. Practical rules:

- Never sell modules, performance features or anything that changes how the game plays.
- Don't use Mojang or Minecraft branding in the store, and say you're not affiliated with Mojang.
- Don't sell access to the game itself, or accounts.

This is a summary, not legal advice; read the guidelines before you launch.

## Licensing

There is no LICENSE file yet, which means the code isn't actually open source until one is
added. Two common setups for a project like this:

- **GPL-3.0** for the code: forks must stay open source, which stops someone closing it up
  and reselling it. Add a trademark note that "Cool Client", the logo and cosmetic designs
  aren't covered.
- **MIT or Apache-2.0** for the code: most permissive; forks can be closed. Same trademark note.

Either way, the money stays with whoever runs the official API server and owns the brand.
