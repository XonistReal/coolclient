# Adding cosmetics without a mod update

Cosmetics come from two places:

- **Built in**: designed in code (`core/.../cosmetic/`). They work offline. Changing them needs a mod update.
- **From the server**: defined in `server/catalog.json` under `remote`, with their images and
  models in `server/public/cosmetics/`. Clients from 0.1.5 on download them, so new ones only
  need a server deploy.

Players need Online Features on to get server cosmetics. The client re-reads the catalog every 30
minutes and on every launch, and keeps a copy on disk, so cosmetics it has seen keep working
offline.

## Add one

1. Put the files in `server/public/cosmetics/` (lowercase names: letters, digits, `-` and `_`).
2. Add the definition to `remote` in `server/catalog.json`.
3. Give it a tier in `cosmetics` (`free`, `supporter`, `store`, `exclusive`, `media` or `owner`).
4. Optional: put it in a season under `seasonal`.
5. Check it: `cd server && node test.js`, then `./gradlew :core:test`. `CatalogSyncTest` runs every
   definition through the client's loader and checks that its files exist.
6. Deploy: `cd server/worker && npx wrangler deploy`.

To replace a file later, give the definition `"v": 2` (then 3, and so on), so clients download it
again instead of using their cached copy.

## Definitions

Every definition has `type` (`CAPE`, `HAT`, `HALO`, `BANDANA`, `WINGS` or `AURA`), `name`,
`description`, `rarity` (`COMMON`, `RARE`, `EPIC`, `LEGENDARY` or `MYTHIC`) and `color`
(`"#RRGGBB"`). Then it's one of three kinds.

### A cape from an image

```json
"cape_flannel": {
  "type": "CAPE", "name": "Cozy Flannel", "description": "Warm plaid for cold November nights.",
  "rarity": "RARE", "color": "#B3261E",
  "image": "capes/flannel.png", "frames": 12, "fps": 8
}
```

The image is either:

- **The cape's outer face** (what you see from behind), any size with a 10:16 shape, such as 40 x 64
  or 80 x 128. For an animated cape, stack the frames top to bottom and set `frames` and `fps`.
- **A whole cape in the vanilla 64 x 32 layout** (any 2:1 size, not animated). Only the outer face
  is used; the inside and the edges are made from it.

### A 3D model (Blockbench)

```json
"hat_pumpkin": {
  "type": "HAT", "name": "Pumpkin Hat", "description": "Fresh from the patch.",
  "rarity": "EPIC", "color": "#E8731E",
  "model": "models/pumpkin_hat.json", "texture": "models/pumpkin_hat.png", "sway": 3
}
```

Make it in Blockbench as a **Java Block/Item** model with one texture, then export the model
(`File > Export > Export Block/Item Model`) and save the texture as PNG.

- **Head slots** (hat, halo, bandana): the player's head is the cube from `[4, 0, 4]` to `[12, 8, 12]`.
  Build on and around it, then delete the reference cube before exporting.
- **Body slots** (wings, aura): the body is the cube from `[4, 0, 6]` to `[12, 12, 10]`.
- The front of the player is Blockbench's **north** side.
- Cubes can be rotated in Blockbench's 22.5 degree steps.

Optional motion: `spin` (degrees per second around the vertical axis), `bob` (pixels up and
down), `sway` (degrees side to side). `"glow": true` makes it ignore lighting.

### A recolour of a built-in design

```json
"halo_frost": { "type": "HALO", "kind": "halo", "name": "Frost Ring", "rarity": "EPIC", "color": "#AEE6FF" }
```

`kind` is one of:

| Slot | Kinds |
|---|---|
| Hat | `crown`, `tophat`, `witch`, `catears`, `horns`, `pilgrim`, `turkey` |
| Halo | `halo` |
| Bandana | `bandana` |
| Wings | `dragon`, `angel`, `demon`, `butterfly`, `energy` |
| Aura | `sparkle`, `flame`, `snow`, `hearts`, `orbit`, `leaves` |

Auras can also take `"colors": ["#FFEE88", "#FF2200"]`; particles pick from the list. Flames
fade from the first colour to the second.

## Seasons

Seasons live in `seasons` (`name`, `color`, `from` and `to` as `"MM-DD"`, UTC, and they can run
past New Year). Clients take new seasons and date changes from the server too. A season's
cosmetics are free in everyone's menu while it's on, and equipping one keeps it. See
`MONETIZATION.md` for the details.

## The title screen spotlight

The Showcase title screen shows the current season's drop. To feature something else, add
`"featured"` to `catalog.json`:

```json
"featured": { "kicker": "NEW", "title": "Winter Drop", "subtitle": "Out now in your locker.", "cosmetic": "cape_frost" }
```
