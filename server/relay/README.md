# Cool Client sign-in relay (Netlify)

Mojang's session server refuses requests from Cloudflare's network (HTTP 403), so the API on
Cloudflare Workers (`../worker/`) can't check sign-ins itself. This Netlify Function makes those two
Mojang calls for it: the sign-in check (`hasJoined`) and the name-to-UUID lookup used by grants.
It needs a shared key, so only the API can use it. A self-hosted Node/Docker server doesn't need it.

## Deploy

```bash
cd server/relay
npx netlify-cli login
npx netlify-cli sites:create --name <site-name>
npx netlify-cli env:set RELAY_KEY <a long random string>
npx netlify-cli deploy --prod
```

Then tell the API where it is and give it the same key:

```bash
cd ../worker
# in wrangler.toml, [vars]: MOJANG_RELAY = "https://<site-name>.netlify.app/mojang"
npx wrangler secret put RELAY_KEY
npx wrangler deploy
```

Check it with `curl -H "X-Relay-Key: <key>" "https://<site-name>.netlify.app/mojang?kind=profile&username=Notch"`:
it should answer `{"status":200,...}`.
