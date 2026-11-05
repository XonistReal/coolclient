# Cool Client API on Cloudflare Workers

Runs the same API as `../server.js` on Cloudflare's free Workers plan: no server to look after, no
card needed, HTTPS included. One Durable Object keeps the data (players, sign-ins, entitlements) in
its built-in SQLite storage.

Mojang refuses requests from Cloudflare's network, so the API's two Mojang calls (the sign-in
check and name lookups for grants) go through a small relay on Netlify's free plan:
[`../relay/`](../relay/README.md). `MOJANG_RELAY` in `wrangler.toml` points at it and the
`RELAY_KEY` secret must match the relay's.

The free plan allows 100,000 requests a day. A player in game sends about one request a minute,
plus one lookup every few seconds while new players are in view, so that's plenty for a start.
When it isn't, move to Workers Paid ($5/month) or to the Docker setup on a VPS (no relay needed).

## First deploy

Deploy the relay first (see `../relay/README.md`), then:

```bash
cd server/worker
npm install
npx wrangler login          # opens Cloudflare in the browser: click Allow
npx wrangler deploy         # prints https://coolclient-api.<your-subdomain>.workers.dev
```

The `workers.dev` subdomain is the account's; change it in the dashboard under Workers & Pages
(the client has the full address built in, so pick it before a release).

Then the secrets (each command asks for the value):

```bash
npx wrangler secret put ADMIN_TOKEN       # a long random string; needed for admin.js grants
npx wrangler secret put RELAY_KEY         # the same key the relay has
npx wrangler secret put TEBEX_SECRET      # later, when the Tebex store is approved
```

Public settings (`FOUNDER_LIMIT`, `ENFORCE_ENTITLEMENTS`, `STORE_URL`, `MOJANG_RELAY`, ...) are in
`wrangler.toml` under `[vars]`; change them there and deploy again. Every setting is described at
the top of `../app.js`.

Check it: `https://coolclient-api.<subdomain>.workers.dev/api/health`.

## Day to day

- **Grant something:** `API_URL=https://coolclient-api.<subdomain>.workers.dev ADMIN_TOKEN=... node ../admin.js grant <name> media`
- **Logs:** `npx wrangler tail`, or the Worker's Logs tab in the dashboard.
- **Update:** `npx wrangler deploy` after changing `../app.js` or `../catalog.json`.

## Test locally

```bash
npx wrangler dev --port 8799 --var AURORA_INSECURE:1 --var ENFORCE_ENTITLEMENTS:1 --var ADMIN_TOKEN:test-admin-token \
  --var WEBHOOK_SECRET:test-webhook-secret --var TEBEX_SECRET:test-tebex-secret --var FOUNDER_LIMIT:1 \
  --var STORE_URL:https://store.example.test/ --var MOJANG_RELAY: --persist-to /tmp/coolclient-wrangler
API_BASE=http://127.0.0.1:8799 node ../test.js   # in another terminal, on fresh storage
```
