# SteamCraft — technical notes

## Building

Gradle needs JDK 25 or newer; the mod itself is compiled with JDK 21, which Gradle downloads by itself through the toolchain.

One source tree is built for every supported Minecraft version with [Stonecutter](https://stonecutter.kikugie.dev).

```sh
./gradlew build                          # every jar, collected in build/libs, plus the core tests
./gradlew :fabric:1.21.4:build           # one loader and version
./gradlew :core:test                     # core tests only
./gradlew :fabric:1.21.1:runClient       # development client (:neoforge:1.21.1:runClient for NeoForge)
./gradlew :fabric:1.21.1:runServer       # development server
```

`-Pjoin=<address>` makes `runClient` join a server right away:

```sh
./gradlew :neoforge:1.21.1:runClient -Pjoin=127.0.0.1:25565
```

### Versions

Each build covers a range of Minecraft versions:

| Build | Fabric | NeoForge |
|---|---|---|
| 1.21.1 | 1.21 – 1.21.1 | 1.21.1 |
| 1.21.3 | 1.21.2 – 1.21.3 | 1.21.3 |
| 1.21.4 | 1.21.4 | 1.21.4 |
| 1.21.5 | 1.21.5 | 1.21.5 |
| 1.21.8 | 1.21.6 – 1.21.8 | 1.21.8 |
| 1.21.10 | 1.21.9 – 1.21.10 | 1.21.10 |
| 1.21.11 | 1.21.11 | 1.21.11 |
| 26.1.2 | 26.1 – 26.1.2 | 26.1.2 |
| 26.2 | 26.2 | 26.2 |
| 26.3 | 26.3 | 26.3 |

Minecraft 26.x needs Java 25; its builds are compiled for Java 25, the 1.21.x builds for Java 21. Fabric 26.x is unobfuscated and uses a different Loom; `loom-back-compat` picks the right one for each version.

Version numbers of dependencies are in `versions/<version>/gradle.properties` (NeoForm), `fabric/versions/<version>/gradle.properties` (Fabric API, Minecraft range) and `neoforge/versions/<version>/gradle.properties` (NeoForge, ranges).

Code that differs between versions is marked with Stonecutter conditions:

```java
//? if >=1.21.6 {
/*graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
*///?} else {
graphics.blitSprite(sprite, x, y, width, height);
//?}
```

The sources are written for the active version, set in `stonecutter.gradle` (`stonecutter.active`); the other versions are generated from them. Renamed classes and methods (for example `GuiGraphics` → `GuiGraphicsExtractor` in 26.1) are handled by replacements in `stonecutter.gradle` instead of conditions. Replacements work in both directions, so each one must match only what it means to rename: `graphics.drawString(`, not `.drawString(`. Most version differences live in a few helpers: `Gfx` (drawing), `Skins`, `Profiles`, `ChatEvents`.

### Project layout

| Folder | Contents |
|---|---|
| `core/` | Java 8 code without Minecraft classes: config, Steam, sign-in decisions, names, co-op access, the P2P tunnel, skin checks. All unit tests live here. |
| `src/` | Minecraft code shared by both loaders, compiled into each loader's jar. `src/main` is the server side, `src/client` the client side. |
| `fabric/` | Fabric adapter |
| `neoforge/` | NeoForge adapter |
| `tools/login-probe/` | goes through a server login like a client with or without SteamCraft |
| `tools/server-bundle/` | files for a ready-to-run test server (`./gradlew :fabric:1.21.1:serverBundle`) |

## Configuration

`config/steamcraft.json`, the same file for games and servers:

```json
{
  "steamAppId": 480,
  "auth": { "ticketTimeoutSeconds": 10 },
  "coop": { "access": "FRIENDS" },
  "skin": { "source": "FILE", "model": "CLASSIC", "cape": "FILE", "file": "my_skin.png", "capeFile": "my_cape.png" },
  "presence": { "showWorldName": false }
}
```

| Key | Meaning |
|---|---|
| `steamAppId` | Steam AppID, 480 (Spacewar) |
| `auth.ticketTimeoutSeconds` | how long a server waits for Steam to confirm a player |
| `coop.access` | who may join when you open your world: `EVERYONE`, `FRIENDS`, `INVITE` |
| `skin.source` | `MOJANG`, `FILE` or `NONE` |
| `skin.model` | arm width of a skin from a file: `CLASSIC` or `SLIM` |
| `skin.cape` | `MOJANG`, `FILE` or `NONE` |
| `skin.file`, `skin.capeFile` | names of the picked files in `skins/` and `capes/` |
| `presence.showWorldName` | whether friends see your world name and server address |

A copy of the picked skin and cape is kept in `config/steamcraft/`. The `skin` and `presence` sections are not used by servers.

### Files in worlds and on servers

| File | Contents |
|---|---|
| `<world>/steamcraft/ops.json` | operators of a co-op world |
| `<world>/steamcraft/names.json` | names Steam players used in a co-op world |
| `<server>/steamcraft/whitelist.json`, `ops.json`, `banned-players.json`, `names.json` | lists of a server with `online-mode=false` |

## How it works

### Co-op

```
Minecraft client ─TCP─> 127.0.0.1 (tunnel client) ═Steam P2P═> tunnel host ─TCP─> LAN port of the host's world
```

The host's world is opened to LAN on a free port, and a tunnel host forwards Steam P2P connections to it. A guest's game turns the address `<SteamID64>.steam` into a local tunnel. Who may connect is decided before Minecraft receives any data, and a refused guest sees the reason.

While the world is open through Steam, SteamCraft decides permission levels: the host and operators get level 4 with cheats on, otherwise level 0 plus `/kick` and `/say`. Everyone else gets level 0.

### Server sign-in

With `online-mode=false`, the server sends a login query (`steamcraft:auth`). The client answers with a Steam session ticket, and the server checks it with Steam while the login waits. A player's UUID is derived from their SteamID, so it stays the same when they change their name.

The server logs on to Steam anonymously as a game server. It needs the Steam client library: `steamclient.so` / `steamclient.dll` from an installed Steam, or from SteamCMD (`~/.steam/sdk64/steamclient.so` on Linux).

### Skins

A skin is sent when the player joins and whenever they change it, at most once every 2 seconds. The server checks and redraws the image and sends it to every player with SteamCraft.

### Friends' status

Each game publishes what it is doing in its own Steam rich presence keys (`steamcraft`, `sc_*`), which only SteamCraft reads. Steam itself always shows Spacewar.

## Steam overlay on Linux

SteamCraft does not need the overlay, but Shift+Tab does not work by default on Linux: Steam only loads its overlay (`gameoverlayrenderer.so` through `LD_PRELOAD`) into games it starts itself, and Minecraft is started by a launcher.

To turn it on, set these environment variables for the game. In Prism Launcher: instance settings → Environment variables.

```
LD_PRELOAD=/home/<user>/.local/share/Steam/ubuntu12_64/gameoverlayrenderer.so
SteamAppId=480
SteamGameId=480
```

For the development client: `./gradlew :fabric:1.21.1:runClient -PsteamOverlay`.

The game window has to run through X11 or XWayland, which is the default. The overlay does not work on native Wayland.
