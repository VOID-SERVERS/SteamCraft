# SteamCraft

**Play Minecraft with your Steam friends — no IP addresses, no port forwarding, no VPN.**

SteamCraft connects Minecraft to Steam. Open your singleplayer world to your Steam friends in two clicks, see who is online right from the pause menu, wear your own skin and cape, and protect your offline-mode server with Steam sign-in.

- **Co-op through Steam** — friends join your world through Steam's network, wherever they are.
- **Friends in the pause menu** — see who is online, invite them, manage players in your world.
- **Skins and capes** — use any PNG you like and switch it in seconds.
- **Server protection** — every player on your offline-mode server signs in with their Steam account.

## Requirements

- Minecraft **1.21 – 1.21.11** or **26.1 – 26.3** with **Fabric** (plus [Fabric API](https://modrinth.com/mod/fabric-api)), or **NeoForge** for 1.21.1, 1.21.3, 1.21.4, 1.21.5, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3
- **Steam** running and signed in while you play

## Installation

1. Download the SteamCraft file for your Minecraft version and mod loader. One file covers several versions: for example, the `1.21.8` file works on 1.21.6, 1.21.7 and 1.21.8 with Fabric.
2. Put it into the `mods` folder of your game.
3. Start Steam, then start Minecraft.

Everyone who wants to play together needs SteamCraft installed. Fabric and NeoForge players can play together.

---

## Playing with friends

### Opening your world

1. Open your singleplayer world and press **Esc**.
2. In the **Steam friends** panel on the right, click **Open through Steam…**
3. Choose the settings and click **Open**:

| Setting | What it does |
|---|---|
| **Who may join** | **Everyone** — anyone who finds you in Steam<br>**Steam friends** — only your friends (default)<br>**Invited only** — only friends you invite |
| **Cheats** | On: you and your operators can use every command.<br>Off: you and your operators can use `/kick` and `/say` only. |
| **Guests' game mode** | Survival, Creative, Adventure or Spectator for players who join |
| **Difficulty** | The world's difficulty |

You can change these at any time with the ⚙ button, or stop new players from joining with ✕ — players already in your world stay.

### Inviting friends

In the **Steam friends** panel, click **Invite** next to a friend. They get a Steam invite and can join with one click. An invite is valid for 10 minutes.

In an **Invited only** world, friends who try to join without an invite show up under **Want to join**, and you get a notification. Click **Let in** next to their name, and they can join.

### Joining a friend

Your friend must have their world open first. Then do any of these:

- Click **Join game** on your friend in the Steam friends list, or accept their invite.
- In Minecraft, go to **Multiplayer → SteamCraft → Play** and click **Join** next to your friend.

If you are already in a world, SteamCraft asks before taking you to your friend's.

### Managing players in your world

Right-click a player in the **In this world** list:

- **Make operator** / **Remove operator** — operators get the same commands as you (see *Cheats* above).
- **Kick** — removes the player from your world.

Players are shown with their Steam name and, in brackets, the name they play under. Operators are saved with each world: a friend you trust in one world has no extra rights in another.

---

## Friends and their status

The **Steam friends** panel (pause menu) and **Multiplayer → SteamCraft → Play** show your Steam friends:

- **Playing SteamCraft** friends are at the top, with what they are doing: in their own world, visiting someone, on a server, the game mode and the in-game day. Hover over a friend for more: dimension and number of players.
- **Online** friends come next.
- **Offline** friends are in a collapsed section at the bottom.

Your own world name and server address are **hidden** from friends by default. You can show them in **SteamCraft → Settings → Show friends my world and server**.

---

## Skins and capes

Your skin and cape are visible to everyone with SteamCraft in co-op worlds and on SteamCraft servers.

### Choosing where your skin comes from

Go to **Multiplayer → SteamCraft → Skin**:

| Skin | |
|---|---|
| **From Minecraft account** | the skin of the account your launcher is signed in with |
| **From a file** | any PNG skin from your `skins` folder |
| **Default** | the standard Minecraft skin |

Capes work the same way: **From Minecraft account**, **From a file** (from your `capes` folder) or **None**.

### Adding your own skins and capes

- Put skin PNGs (64×64) into the **`skins`** folder and capes (64×32) into the **`capes`** folder of your game. The **Skins folder** and **Capes folder** buttons open them for you.
- Or simply **drag PNG files onto the Skin tab**: drop on the **left half** of the window for a skin, on the **right half** for a cape.

The model on the left shows how you will look — drag it to turn it around. Arm width (classic or slim) is detected automatically and can be changed.

A cape file can also contain elytra. If yours does not, you get the standard elytra.

### Quick change in the pause menu

When your skin comes **from a file**, the pause menu has a small **Skin** box next to the buttons: pick a skin and a cape, check them on the model and click **Apply**. No need to leave the game.

---

## Servers: extra protection for offline mode

If your server runs with `online-mode=false`, SteamCraft adds a layer of protection: **every player has to sign in with their Steam account**.

- Nobody can join under someone else's name — a name belongs to the Steam account that first used it.
- `/whitelist`, `/op`, `/deop`, `/ban` and `/pardon` work with Steam accounts. Use a player's name, a name they used before, or their SteamID64:
  `/whitelist add 76561199092349621`
- Players keep their inventory and progress when they change their name.
- These lists are kept separately in the `steamcraft/` folder of your server.

**Setting it up**

1. Install SteamCraft on the server.
2. Set `online-mode=false` in `server.properties`.
3. Start the server. It signs in to Steam by itself — no Steam account is needed on the server, only the Steam client library (from an installed Steam or from SteamCMD).

Players need SteamCraft and a running Steam to join. Players without the mod get a message asking them to install it.

With `online-mode=true`, SteamCraft stays out of the way and the server works exactly as usual. On other servers without SteamCraft, players with the mod join as usual too.

> **Switching an existing world?** Players get new IDs, so their old inventories are not picked up automatically. To give a player their items back, stop the server and rename their old file in `world/playerdata/` to the ID shown by `/steamcraft whois <name>`. The server reminds you about this in its log.

---

## Commands

Everything in the menus is also available as commands.

**Co-op** (host, in your own world)

| Command | What it does |
|---|---|
| `/steam host [everyone\|friends\|invite] [cheats]` | open your world through Steam |
| `/steam unhost` | stop new players from joining |
| `/steam invite <friend>` | send a Steam invite (valid for 10 minutes) |
| `/steam op <player>` / `/steam deop <player>` | add or remove an operator of this world |
| `/steam ops` | list the operators of this world |
| `/steam id` | show your Steam name and SteamID64 |

**Skin and cape**

| Command | What it does |
|---|---|
| `/steam skin` / `/steam cape` | show your current choice |
| `/steam skin file <file>` | use a skin from a file (suggestions come from `skins/`) |
| `/steam skin mojang` / `reset` | skin from your Minecraft account / default skin |
| `/steam skin model classic\|slim` | arm width for a skin from a file |
| `/steam cape file <file>` / `mojang` / `reset` | cape from a file (suggestions from `capes/`), from your account, or none |

**Server** (operators level 3+, or the host of a co-op world)

| Command | What it does |
|---|---|
| `/steamcraft status` | sign-in mode, Steam connection, co-op state |
| `/steamcraft whois <name>` | a player's ID and SteamID64 |

---

## FAQ

**Why does Steam show me as playing "Spacewar"?**
SteamCraft uses Spacewar (AppID 480), Valve's free Steamworks test application. It lets everyone use Steam's networking without buying anything on Steam. Your friends with SteamCraft still see what you are really doing in Minecraft.

**Do I need to forward ports or know my IP?**
No. Connections go through Steam.

**Does SteamCraft change how I join normal servers?**
No. On servers without SteamCraft, and on servers with `online-mode=true`, you join exactly as before.

**The Steam overlay (Shift+Tab) does not open on Linux.**
SteamCraft works without it. If you want the overlay, see [docs.md](docs.md#steam-overlay-on-linux).

**I found a bug.**
Please report it here: **https://issues.encdev.uk/p/steamcraft**

For developers: building, the config file and technical details are in [docs.md](docs.md).

---

## Disclaimer

*SteamCraft is an independent open-source project and is not affiliated with, endorsed by, or authorized by Valve Corporation, Mojang Studios or Microsoft.*

*"Steam", "Steamworks" and "Spacewar" are trademarks of Valve Corporation. "Minecraft" is a trademark of Mojang Studios.*

*The project redistributes the required Steamworks runtime (`steam_api64.dll`, `libsteam_api.so`, `libsteam_api.dylib`, bundled through [steamworks4j](https://github.com/code-disaster/steamworks4j)) under the Steamworks SDK license applicable to Steam developers.*
