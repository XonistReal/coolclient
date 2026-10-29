# Cool Client — Master Plan

> **Cool Client**: a Lunar-style PvP client, not an anarchy/hacked client.
> Every feature is visual, informational, or quality-of-life. Nothing gives an unfair
> mechanical advantage (no reach, no aim assist, no ESP through walls on other players, no
> auto-clicking, no "fast crystal"/packet tricks). "Hand chams" only ever affects **your own
> first-person hand**.

---

## 1. Goals

| Goal | What "done" looks like |
|---|---|
| Lunar-class launcher | One app: log in, pick a version, press Launch. It downloads Minecraft, Java, the loader and the client and keeps them up to date. |
| Multi-version client | **1.8.9** (classic PvP) and **1.21.4** (crystal PvP / modern), sharing ~80% of the code through a version-independent core. |
| Big legit module set | 50+ modules: HUD, PvP visuals, utility, performance, 1.8.9-only, and 1.21-only/cPvP. |
| Its own look | Smooth custom fonts, animated aurora theme, rounded glass panels, Lunar-style mod menu and HUD editor, custom main menu. |
| Cosmetics | Animated procedural capes, wings, halos, hats, bandanas and auras, visible to other Cool Client users through a cosmetics server. |
| Beta release | Built jars, a runnable launcher (dev + Windows installer), a self-hostable API server, and docs. |

## 2. Architecture

```
coolclient/
├─ core/                 Pure Java 8. Zero Minecraft imports. Compiled into every version jar.
│  └─ dev.coolclient.core
│     ├─ CoolClient          bootstrap/singleton; owns every manager below
│     ├─ api/                Platform contracts implemented per MC version
│     │   Platform, GameState, GuiRenderer, Textures, ItemRef, ModelRenderer, Feature …
│     ├─ event/              tiny typed event bus + events (tick, render, attack, chat, totem pop …)
│     ├─ setting/            Boolean/Number/Mode/Color(chroma)/Keybind/Text settings
│     ├─ module/             Module, Category, ModuleManager (feature-gated per version)
│     ├─ modules/            ALL module logic (hud/, pvp/, visual/, utility/, perf/, legacy/, cpvp/)
│     ├─ hud/                HudElement (anchor+offset+scale), HudManager
│     ├─ gui/                widget toolkit, ModMenu, HudEditor, Cosmetics locker, Main menu, Settings
│     ├─ render/             Draw (rounded rects, circles, gradients, shadows), Colors, Easing, Animation
│     ├─ font/               TTF → glyph atlas (AWT) → smooth text with § colour codes
│     ├─ cosmetic/           catalog, procedural textures (capes), meshes (wings/halo/hats), sync
│     ├─ config/             profiles, JSON persistence (own tiny JSON lib, no Gson version issues)
│     ├─ net/                API client: auth handshake, cosmetics lookup, presence
│     └─ util/
├─ platform/
│  ├─ v1_8_9/            Legacy Fabric + Mixin (legacy yarn). Java 8 target.
│  └─ v1_21_4/           Fabric + Mixin (official Mojang mappings). Java 21.
├─ launcher/             Electron app (main process does all Minecraft work; renderer = UI)
├─ server/               Node (zero dependencies) cosmetics + presence + news API
└─ tools/                build/dist scripts
```

**Why this split:** the platform layer is deliberately thin. It implements a handful of
interfaces: draw a triangle batch, draw a textured quad, draw an item, read the player
state, upload a texture, and render a mesh in model space. It also contains the mixins
that feed events and read module settings. Every module, every screen and every cosmetic
is written once in `core`. Adding 1.21.11/26.x later is a new `platform/` folder.

### Key technical decisions
- **Build:** single Gradle 9 multi-project (`core`, `platform:v1_8_9`, `platform:v1_21_4`), Loom 1.16 + legacy-looming. Core sources are compiled into each version jar (no jar-in-jar for core).
- **Rendering:** the UI is shader-drawn. Rounded rects, outlines, shadows, glows, circles and arcs are one SDF uber-shader (`shape.fsh`); menus are frosted glass (the frame is copied to a mipmapped texture once per frame and blurred in the shader); menu backgrounds and skyboxes are procedural GLSL. Sources are written once and get a generated GLSL 120 (1.8.9) or 150 (1.21.4, launcher) header. Platforms implement only the small `Gpu` interface; the CPU triangle path in `Draw` stays as the fallback (`Shader Rendering` off or `-Dcoolclient.noshaders`).
- **Themes:** `Theme` holds the active palette (10 presets incl. one light theme); the Client module switches it. Every screen reads `Theme.*`, so new themes need no screen changes.
- **Monetization:** open-core cosmetics. Anyone can equip anything locally; the API server only shows paid cosmetics to other players when the wearer owns them (see MONETIZATION.md).
- **Fonts:** glyph atlases rasterised with `java.awt` (headless-safe) at runtime from system fonts (Segoe UI → Inter → SansSerif fallback). No binary font assets are needed.
- **Keys:** core uses GLFW key codes everywhere; 1.8.9 translates its LWJGL2 codes.
- **Cosmetics:** textures are generated procedurally (ARGB arrays) and animated by re-uploading. Mesh cosmetics use a `ModelRenderer` abstraction (pose stack + quads), so wings and halos look identical on both versions. Capes go through the vanilla cape pipeline (physics for free) via an overridden cape texture.
- **Self-contained jars:** the 1.8.9 jar uses mixins only (no Legacy Fabric API). The 1.21.4 jar jar-in-jars only the Fabric API modules it uses.
- **Launcher:** Electron. It downloads from Mojang piston-meta, uses Mojang's own Java runtimes (Java 8 for 1.8.9, 21 for 1.21.4) and Fabric/Legacy Fabric meta profiles, and loads the client via `-Dfabric.addMods`. Accounts: Microsoft device-code login (client ID configurable) plus offline/dev accounts. Optional performance pack (Sodium, Lithium, etc. from Modrinth) for 1.21.4.
- **Server:** Node `http` only, JSON-file storage. Identity is proven through Mojang's `joinServer`/`hasJoined` handshake (same trick as server auth), with an insecure dev mode for offline testing.

## 3. Module catalogue

Legend: **B** = both versions, **8** = 1.8.9 only, **21** = 1.21.4 only.

### HUD (all draggable, scalable, themable: background, text colour/chroma, shadow, font)
| Module | Ver | Notes |
|---|---|---|
| FPS | B | |
| CPS | B | left/right, optional combined |
| Keystrokes | B | WASD, LMB/RMB with CPS, spacebar, press-fade animation |
| Ping | B | |
| Coordinates | B | XYZ, facing, biome, nether-coords conversion |
| Direction / Compass | B | scrolling compass strip |
| Armor Status | B | icons + durability (value/percent/bar), colour by damage |
| Potion Effects | B | icon, name, timer, blink when low |
| Clock | B | 12/24h |
| Memory | B | |
| Server Address | B | |
| Reach Display | B | distance of your last hit |
| Combo Counter | B | |
| Speed | B | blocks/sec |
| Saturation | B | |
| Item Counter | B | arrows, pots, gapples, pearls (+ totems, crystals, obsidian, anchors, glowstone, XP on 21) |
| Totem Counter | 21 | |
| Cooldowns | 21 | pearl, chorus, wind charge, shield, mace etc. item cooldowns |
| Pack Display | B | active resource pack |
| Session Time / Playtime | B | |
| Day Counter | B | |
| Stopwatch | B | hotkey start/stop |
| Custom Text | B | |
| CPS Graph | B | rolling click graph |
| Durability Alerts | B | warning when your armour/held item gets low |

### PvP and visuals
| Module | Ver | Notes |
|---|---|---|
| **Hand Chams** | B | own hand/held item only: tint, flat, glow, wireframe, chroma, alpha; separate arm/item colours |
| View Model | B | hand position/rotation/scale, swing speed (visual) |
| 1.7 Animations | 8 | block-hit, rod, bow, eat/drink, armour red flash, smooth sneak, swing-while-using |
| Hit Color | B | custom hurt overlay colour |
| Hurt Cam | B | strength or disable |
| Block Outline | B | colour, width, fill, chroma |
| Crosshair | B | full pixel crosshair editor + presets, colour, dynamic gap |
| Hitboxes | B | colours/line width for F3+B hitboxes |
| Particles | B | multiplier, always-crit/sharpness |
| Nametags | B | own nametag in F5, background opacity, shadow, client-user badge |
| Item Physics | B | dropped items lie flat |
| Low Fire | B | |
| Low Shield / Shield tweaks | 21 | |
| Totem Pop Animation | 21 | scale/duration/disable of the pop overlay |
| Crystal Tweaks | 21 | visual crystal scale/spin/bounce (rendering only) |
| Totem Pop Counter | 21 | per-player pop counts in chat and on nametags, reset on death |
| Motion Blur | B | |
| Zoom | B | scroll-to-zoom, smooth zoom, cinematic option |
| Freelook | B | hold to look around (auto-off on servers that block it) |
| Fullbright | B | |
| Time Changer | B | client-side time |
| Weather Changer | B | client-side clear weather |
| FOV Changer | B | disable dynamic FOV (speed/bow) |
| No Bobbing tweaks | B | view bobbing tweaks (hand only / camera only) |

### Utility
| Module | Ver | Notes |
|---|---|---|
| Toggle Sprint / Sneak | B | with HUD indicator |
| Auto GG | B | configurable message, Hypixel and generic triggers |
| Auto Text Hotkeys | B | one message per keypress |
| Chat | B | background opacity, timestamps, stack spam (x2), no close fade, scroll, copy |
| Scoreboard | B | hide red numbers, background, scale, position |
| Tab List | B | numeric ping, colours |
| Boss Bar | B | hide/scale |
| Nick Hider | B | replace your name client-side |
| Waypoints | B | world waypoints with beacon beam (your own markers) |
| Screenshot Helper | B | copy to clipboard / open folder toast |
| Borderless Fullscreen | 8 | |
| Mouse Delay Fix | 8 | fixes the vanilla 1.8 look-vector bug |

### Performance
| Module | Ver | Notes |
|---|---|---|
| Entity Culling (distance) | B | entity/tile entity render distance |
| Particle Limit | B | |
| Disable Texture Animations | 8 | |
| Smart FPS limiter | B | unfocused/AFK frame cap |
| Launcher perf pack | 21 | optional Sodium/Lithium/FerriteCore/EntityCulling installed by the launcher |

### Cosmetics (all procedural, all animated where it makes sense)
- **Capes:** Aurora Borealis, Cool Emblem, Galaxy, Inferno, Ocean, Rainbow, Matrix, Sunset, Void, Sakura, Lightning, Gradient (custom colours), plus matching elytra on 21.
- **Wings:** Dragon, Angel, Demon, Butterfly (flapping, tintable).
- **Head:** Halo (glowing ring), Crown, Top Hat, Witch Hat, Cat Ears, Horns.
- **Body:** Bandana, Aura particles (sparkles/hearts/flames/snow).
- **Badges:** client-user icon next to nametags and in the tab list.

## 4. Phases and acceptance criteria

**Priority (from the owner):** ship and polish the client as a plain Fabric mod first. The launcher (Phase 4) waits until the client is good enough.

**Status (2026-10-29):** Phases 0 to 3 are done. Both release jars pass the automated screenshot tour in dev
and in production mode (`prodClient`, real intermediary mappings), and 1.21.4 runs alongside Fabric API.
Phase 4 (launcher) is built in Java + OpenGL (no Electron). It installs and launches both
versions end to end (offline accounts verified; Microsoft sign-in needs an approved Azure app ID).
UI overhaul (2026-10-29): shader-drawn UI with glass panels, 10 themes and a Themes page, 8 animated
menu backgrounds, a Skybox module with 8 shader skies plus custom images, glass HUD backgrounds,
and monetization groundwork (unlock tiers, server entitlements, store webhook, supporter badges).


**Phase 0 — Scaffolding.** Repo, git, Gradle wrapper, both platform skeletons compile, both `runClient` dev runs launch.
**Phase 1 — Core.** Settings, modules, events, config/profiles, JSON, render primitives, smooth font, widget toolkit, HUD system, every HUD module, the mod menu, the HUD editor and the cosmetics catalogue. `core` compiles alone on Java 8.
**Phase 2 — 1.8.9 platform.** Renderer, game-state bridge, input translation, mixins for every module, cosmetics rendering, hand chams, 1.7 animations, custom main menu. Passes when `runClient` starts with no mixin errors and the HUD and menus work in game.
**Phase 3 — 1.21.4 platform.** Same as Phase 2, plus the cPvP modules. No Fabric API dependency (every hook is a mixin).
**Phase 4 — Launcher.** Java + LWJGL/OpenGL, drawn with the core UI toolkit, packaged with jpackage. Done: accounts (MSA device code + offline), version manifest, library/asset/natives download with SHA-1 verification, Mojang Java runtimes, Fabric/Legacy Fabric profiles, launch with log console, settings, cosmetics locker. Still to do: 3D player preview in the locker, server list with quick-join, optional perf pack (Sodium etc., after a compatibility pass), auto-update.
**Phase 5 — Server.** Auth handshake, cosmetics get/put, batch lookup, presence, news; dev mode.
**Phase 6 — Beta.** End-to-end: launcher installs and launches both versions with the client loaded. Build scripts produce `dist/` (2 jars + launcher installer + server). README, CHANGELOG, `v0.1.0-beta` tag.

## 5. Beta definition (v0.1.0-beta)
- `tools/build-all` builds both client jars and the launcher.
- Launcher boots 1.8.9 and 1.21.4 with the client loaded (offline account at minimum; Microsoft login once a client ID is configured).
- All modules above that are marked B/8/21 are present and toggleable. Known gaps are listed in `CHANGELOG.md`.
- Cosmetics work locally, and between players when the server is running.

## 6. Risks and mitigations
| Risk | Mitigation |
|---|---|
| Microsoft login needs an Azure app approved for Minecraft APIs | Client ID is a launcher setting; README explains registration; offline accounts for dev. |
| Mixin targets wrong for legacy yarn names | Every target is verified against the remapped game jar with `javap`, then confirmed by the `runClient` and `prodClient` self-tests. |
| Server rules (freelook etc.) | Per-server blocklist that turns sensitive modules off automatically. |
| Sodium/Iris compatibility on 21 | Hooks use vanilla entry points that Sodium keeps. Hand chams wrap the buffer source rather than replacing renderers. |
| Cosmetics need hosting | Zero-dependency server plus Dockerfile. The client degrades gracefully to local-only cosmetics. |

## 7. Post-beta roadmap
Emotes, pets, 1.21.11/26.x platform (Mojang names, so a small port), Discord RPC, friends and
party system, in-launcher mod browser, auto-updater, replay-lite clips, more cosmetics.
Store: pick a payment provider and write the webhook relay, add a LICENSE, theme editor
(custom palettes), shader-based cosmetic previews, more skies.
