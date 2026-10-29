# Privacy

Short version: Cool Client has no ads, no analytics and no telemetry. It only talks to the
Cool Client server if you turn on online cosmetics, and even then it only sends what's needed
to show cosmetics.

## The game client (the mod)

With **Online Features off** (Settings), the client never contacts any Cool Client server.

With **Online Features on** (the welcome screen asks you on first launch), it talks to the
Cool Client API server to show cosmetics:

| Sent | Why |
|---|---|
| Your Minecraft name and UUID | To sign you in, so other players see your cosmetics. |
| A Mojang "join server" check | Proves you own the account, the same check every Minecraft server does. It goes to Mojang's session server with the game's own session; **your Microsoft/Minecraft token is never sent to Cool Client.** |
| Your equipped cosmetics and their colours | So other players can see them. |
| A heartbeat about once a minute while in a world | Shows you as online. |
| The UUIDs of players loaded around you | To fetch their cosmetics. |

Your IP address reaches the server as with any web request. It is used in memory for rate
limiting and is not stored.

## The launcher

The launcher downloads Minecraft, Java and the mod loaders from Mojang, Fabric and Legacy
Fabric, and checks the Minecraft files against the hashes Mojang publishes. Microsoft sign-in uses
Microsoft's device-code flow: you type your password on microsoft.com, never in the launcher.

Your accounts (including the Microsoft refresh token that keeps you signed in) are saved in
`%APPDATA%\.coolclient`, like other launchers do, readable only by your Windows user. They are
only ever sent to Microsoft and Mojang.

## The Cool Client server

It stores, per player: name, UUID, equipped cosmetics, what you've bought (cosmetic ids and
ranks), and when you were last online. Payments are handled entirely by the store provider
(Tebex). The server receives a purchase notice from Tebex but only keeps which items were
granted, not your name, email or payment details.

To have your data deleted, open an issue titled "Data deletion" with just your Minecraft name, or
send a private report through the repository's Security tab.

## Verify it yourself

All of this is checkable in the source: network code lives in `core/.../net/ApiClient.java`
(client), `launcher/.../core/` (launcher) and `server/server.js` (server).
