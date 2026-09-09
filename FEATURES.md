# UltiBot — Feature Inventory

This document catalogues every operator- or player-visible function, command, content item and
configuration key in this repository, as read directly from source. It is an internal reference
for UAT execution and issue reconciliation — the public description of these features lives on
<https://doc.ultikits.com/>. Update this file in the same pull request as any feature change.

## Conventions

- **ID grammar:** `<repo-slug>.<area>.<action>`, dot-separated, every segment lowercase ASCII
  drawn from `[a-z0-9-]`. `<repo-slug>` is the repository name lowercased with no separators —
  `ultibot` here, `ultitools`, `ultichat`, `ultieconomy`, and `ultitools-example` for
  `UltiTools-External-Example`. `<area>` is the feature section's slug. `<action>` is the verb.
  A `config` row is the one shape that exceeds three segments and is exempt from the
  lowercase-ASCII rule for its key-path suffix:
  `<repo-slug>.config.<file-stem>.<yml key path>`. An ID changes only when the feature's identity
  changes, never on rewording. IDs are unique within a repository.
- **Kind**, exactly these eight values: `command`, `config`, `event`, `gui`, `scheduled`,
  `placeholder`, `persistence`, `gate`. This module has no `gui` rows (no GUI page class), no
  `placeholder` rows (no PlaceholderAPI dependency at all), no `gate` rows (0
  `@ConditionalOnConfig` sites), and — despite the public documentation describing repeating
  actions and physics ticking as ongoing behaviour — no `scheduled` rows either: this module has
  0 `@Scheduled` sites, and (see `## Actions` below) nothing else drives either mechanism.
- **Tier**, exactly three: `player`, `admin`, `internal`. Judged from what the feature is for, not
  from whether it carries a permission string.
- **Manual**, exactly three: `detailed`, `brief`, `none`.
- **Target**, exactly four: `player`, `console`, `both`, or `n/a`. `BotCommands` carries class-level
  `@CmdTarget(BOTH)`; three of its 22 mappings (`spawn`, `tp`) narrow to `PLAYER` at the method
  level (the sender must be an in-world player to have a location to spawn at or teleport toward)
  — every other mapping stays at the class-level `BOTH`.
- **Permission:** the literal node string, `none`, or `n/a`. No `@CmdExecutor` in this module sets
  `requireOp = true`.
- **Source:** `ClassName#member` — the class and member that actually reads or applies the
  feature — for every Kind, `config` included.
- **Row order:** by section, then by ID ascending within the section.
- **No manual prose:** no troubleshooting column, no explanatory paragraphs, no draft page text.
  Where a feature's actual runtime behaviour genuinely diverges from what the public doc page
  describes it as doing, that fact is itself part of "what the feature does" and is stated here as
  a plain, sourced observation, with the filed issue number.
- **Fake-player precondition (plan 10-16 scheduling note):** every row below that spawns or drives
  a bot creates a real `ServerPlayer`-backed entity that other modules' own UAT rows could observe
  as an unexpected "extra player" (join/quit event floods, player-count-sensitive checks, etc.).
  Per this plan's own instruction, this repository's real-machine dispatch is scheduled ALONE,
  never interleaved with another module's batch — stated once here rather than repeated on every
  affected row's own Preconditions cell, since it is a scheduling property of the whole repository,
  not of any individual feature.

### Reconciliation command family

The canonical form for counting an annotation site across this repository's real sources:

```bash
find <repo-root> -path '*/src/main/java/*' -name '*.java' -not -path '*/target/*' \
  -not -path '*/.worktrees/*' -print0 | xargs -0 grep -nE '^[[:space:]]*@AnnotationName\b' | wc -l
```

**This repository's own trap: three source roots, no `<repo-root>/src/main/java` at all.** UltiBot
has no single-module source tree — `ultibot-api/`, `ultibot-core/`, and `ultibot-v1_21_R1/` are
three independent Maven modules, each with its own `src/main/java`, with `ultibot-dist/` shading
all three together at package time (confirmed by reading `ultibot-dist/pom.xml`: its
`<dependencies>` list exactly `ultibot-core` and `ultibot-v1_21_R1` — `ultibot-api` is a transitive
dependency of `ultibot-core` — and its `maven-shade-plugin` execution's `<includes>` names all
three artifact coordinates explicitly). A command written against a literal
`<repo-root>/src/main/java` path glob returns **0** for this repository and does **not error** —
worse than a wrong number, since a silent 0 reads as a balanced reconciliation line rather than an
obviously broken one. The canonical `find <repo-root> -path '*/src/main/java/*'` form self-adapts
to all three roots with no per-module special-casing, and is the only form used anywhere in this
document.

**Positive control:** the canonical (multi-root) form returns `@CmdExecutor` = 3, `@CmdMapping` =
38, `@EventListener` = 1 (class, 2 handler methods), `@Scheduled` = 0, `@ConfigEntity` = 1,
`@ConfigEntry` = 8, `@ConditionalOnConfig` = 0, `@Table` = 0 — confirmed by reading
`BotCommands.java` directly (22 of the 38 `@CmdMapping` sites, all in `ultibot-core`) and by the
single-root form's own literal 0, run side by side with the canonical form on the same tree.

**A second, more serious trap this repository's own layout produces: `@CmdExecutor` = 3, but only
ONE of the three classes is ever actually registered.** `ActionCommands` and `UtilityCommands`
both carry `manualRegister = true` — the framework's own documented convention for "this class is
NOT auto-registered by component scan; something must call `getCommandManager().register(...)`
for it explicitly" (see the monorepo framework's own Common Gotcha #4: "`manualRegister = true`
commands need `getCommandManager().register()`"). **`getCommandManager()` is called ZERO times
anywhere in this module's source** (`grep -rn getCommandManager` across all three `src/main/java`
roots returns nothing), so neither class is EVER registered by anything — both are permanently
dead code. Every one of their 16 combined `@CmdMapping` sites (`ActionCommands`: `action`, `stop`;
`UtilityCommands`: `chat`, `cmd`, `skin`, `messages`, `clearmsg`, `op`, `deop`, `click`,
`closeinv`, `inv`, `macro record`, `macro stop`, `macro play`, `macro list`) duplicates a
functionally-identical mapping already live in `BotCommands` (the one class with no
`manualRegister` flag, auto-registered normally) — this reads as a superseded intermediate
refactor state, not two intentionally-alternate command sets. **This document still catalogues
all 38 sites** — `BotCommands`'s 22 live mappings as 23 rows (`remove` split into
`remove`/`remove-all`, see `## Bot management` below) plus `ActionCommands`'s and
`UtilityCommands`'s 16 combined dead mappings as their own 16 rows (`## Dead command classes`
below, Tier `internal`, each stating plainly that it is unreachable) — so the reconciliation line
balances 38-against-39 exactly (39 = 23 live + 16 dead), rather than omitting the unreachable
sites from the count entirely. Filed as UltiKits/UltiBot#15.

## Bot management

`BotCommands` — the module's ONLY live `@CmdExecutor` (`permission = "ultibot.use"`, `alias =
{"bot"}`), class-level `@CmdTarget(BOTH)`.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultibot.bot.list | List every currently active bot with its name and its owner's UUID, truncated to 8 characters (not resolved to a player name) | command | `/bot list` | ultibot.use | both | admin | brief | ultibot-core: BotCommands#onList |
| ultibot.bot.remove | Remove a single named bot, disconnecting its `ServerPlayer` and releasing it from both the name/UUID lookup maps and its owner's tracked set | command | `/bot remove <name>` | ultibot.use | both | admin | brief | ultibot-core: BotCommands#onRemove |
| ultibot.bot.remove-all | Remove every currently active bot in one command — a materially different, independently-observable behaviour from the single-name case sharing the same `@CmdMapping` site, split into its own row the same way the framework's own `FEATURES.md` splits `/upm update` from `/upm update all` | command | `/bot remove all` | ultibot.use | both | admin | brief | ultibot-core: BotCommands#onRemove |
| ultibot.bot.reload | Claims to reload this module's configuration. Reads NO config anywhere in its own body and calls no config-reload method at all — the command is purely cosmetic, always reporting success regardless of whether `config.yml` was ever actually re-read (UltiKits/UltiBot#16). Also separately checks `sender.hasPermission("ultibot.admin")` in the method body itself — a SECOND, hardcoded permission check layered on top of the class-level `@CmdExecutor` permission, not declared via any annotation | command | `/bot reload` | ultibot.use | both | admin | detailed | ultibot-core: BotCommands#onReload |
| ultibot.bot.spawn | Spawn a new bot at the sender's own location, refusing if the name is taken or either the per-player or server-wide bot limit is reached, with the default skin and no chat-prefix applied at spawn time (`default-skin`/`bot-prefix` are declared config keys with zero readers anywhere in this module — UltiKits/UltiBot#19). The "limit reached" refusal message always cites the PER-PLAYER limit even when the server-wide `max-total-bots` cap was the actual cause — `BotManagerImpl#spawnBot` returns a bare `null` for either refusal, and `BotCommands#onSpawn` cannot tell them apart; a `bot_limit_server` message key exists in both lang files and is never referenced by any code path (UltiKits/UltiBot#19) | command | `/bot spawn <name>` | ultibot.use | player | player | detailed | ultibot-core: BotCommands#onSpawn, BotManagerImpl#spawnBot |
| ultibot.bot.spawn-at | Spawn a new bot at the FIRST loaded world's spawn location (`Bukkit.getWorlds().get(0)`), reachable from console (unlike `spawn`, which needs a player location); owner-less — the bot created this way is not tracked in any player's owner set, so `auto-remove-on-quit` can never remove it and no player can be identified as "responsible" for it via `/bot list` | command | `/bot spawnat <name>` | ultibot.use | both | admin | brief | ultibot-core: BotCommands#onSpawnAt |
| ultibot.bot.teleport | Teleport a named bot to the sender's own location | command | `/bot tp <name>` | ultibot.use | player | player | brief | ultibot-core: BotCommands#onTeleport |
| ultibot.bot.action | Start a bot performing a named action on a repeating tick interval. Claims to actually repeat the action — it does not: see `## Actions` below (UltiKits/UltiBot#17) | command | `/bot action <name> <action> <interval>` | ultibot.use | both | player | detailed | ultibot-core: BotCommands#onAction |
| ultibot.bot.stop | Stop every repeating action currently registered for a named bot | command | `/bot stop <name>` | ultibot.use | both | player | brief | ultibot-core: BotCommands#onStop |
| ultibot.bot.chat | Make a named bot send a chat message, using the real `AsyncPlayerChatEvent` pipeline (`getBukkitPlayer().chat(...)`) — other plugins' chat listeners (formatters, filters, loggers) see this exactly as a real player's message | command | `/bot chat <name> <words...>` | ultibot.use | both | player | brief | ultibot-core: BotCommands#onChat |
| ultibot.bot.cmd | Make a named bot execute a server command as itself (no leading `/`) | command | `/bot cmd <name> <args...>` | ultibot.use | both | admin | brief | ultibot-core: BotCommands#onCmd |
| ultibot.bot.skin | Fetch a real player's skin from Mojang's API (two synchronous, BLOCKING HTTP calls, up to 5s timeout each) and apply it to a named bot, caching the result by player name. This runs on whatever thread invoked the command — for an in-game player, that is the SERVER MAIN THREAD, which the framework's own Common Gotcha #1 states must never block on I/O; a cold (uncached) skin fetch can freeze the entire server for up to ~10 seconds. Filed as UltiKits/UltiBot#18 | command | `/bot skin <name> <skinName>` | ultibot.use | both | admin | detailed | ultibot-core: BotCommands#onSkin |
| ultibot.bot.messages | List every chat/system message this bot's fake client connection has captured since the last clear, via a Netty outbound handler intercepting `ClientboundSystemChatPacket` (action-bar overlays excluded) | command | `/bot messages <name>` | ultibot.use | both | internal | brief | ultibot-core: BotCommands#onMessages |
| ultibot.bot.clearmsg | Clear a named bot's captured-message history | command | `/bot clearmsg <name>` | ultibot.use | both | internal | none | ultibot-core: BotCommands#onClearMessages |
| ultibot.bot.op | Grant a named bot server-operator status via the real `Player#setOp(true)` path | command | `/bot op <name>` | ultibot.use | both | internal | brief | ultibot-core: BotCommands#onOp |
| ultibot.bot.deop | Remove a named bot's operator status | command | `/bot deop <name>` | ultibot.use | both | internal | brief | ultibot-core: BotCommands#onDeop |
| ultibot.bot.click | Simulate a left-click PICKUP_ONE action on a numbered slot of a bot's currently open inventory view, by firing a real `InventoryClickEvent` — any plugin's own `InventoryClickEvent` listener reacts to this identically to a genuine player click | command | `/bot click <name> <slot>` | ultibot.use | both | internal | detailed | ultibot-core: BotCommands#onClick |
| ultibot.bot.closeinv | Close a named bot's currently open inventory | command | `/bot closeinv <name>` | ultibot.use | both | internal | none | ultibot-core: BotCommands#onCloseInventory |
| ultibot.bot.inv | Print the contents of a named bot's currently open inventory (slot index, display name or material, stack size) to the sender | command | `/bot inv <name>` | ultibot.use | both | internal | brief | ultibot-core: BotCommands#onInventory |
| ultibot.bot.macro-list | List every macro name saved so far, across all bots | command | `/bot macro list` | ultibot.use | both | player | brief | ultibot-core: BotCommands#onMacroList |
| ultibot.bot.macro-play | Look up a saved macro by name and confirm it exists — this command reports success but never actually replays any recorded action; see `## Actions` below (the macro-playback path shares the same never-invoked action-execution machinery `/bot action` does, UltiKits/UltiBot#17) | command | `/bot macro play <name> <macroName>` | ultibot.use | both | player | detailed | ultibot-core: BotCommands#onMacroPlay |
| ultibot.bot.macro-record | Claims to start recording every `/bot action` invocation issued against a named bot into a named, in-memory macro. `MacroServiceImpl#recordAction` — the only method that would ever append an entry to a `RecordingSession` — has **zero callers anywhere in this module's source**; `BotCommands#onAction`/`ActionServiceImpl#startRepeatingAction` never call it. A "recording" session accepts the start/stop commands and reports success but never actually captures a single action, regardless of how many `/bot action` commands run while it is nominally active. Filed together with UltiKits/UltiBot#17 (the same incomplete action-recording/playback subsystem) | command | `/bot macro record <name> <macroName>` | ultibot.use | both | player | detailed | ultibot-core: BotCommands#onMacroRecord, MacroServiceImpl#recordAction (declared, never called) |
| ultibot.bot.macro-stop | Stop the active recording session for a named bot, persisting its entries in-memory under the recording's macro name | command | `/bot macro stop <name>` | ultibot.use | both | player | brief | ultibot-core: BotCommands#onMacroStop |

## Actions

`ActionServiceImpl` (`@Service`) and `SimpleActionTicker` implement the repeating-action machinery
`ultibot.bot.action` claims to drive.

**Confirmed defect, filed as UltiKits/UltiBot#17: repeating actions never actually repeat, and
`tick-bots` physics ticking never runs at all, on any server.** `SimpleActionTicker#tick()` — the
method that actually re-invokes `ActionServiceImpl#performAction` once the configured interval has
elapsed — is only ever called from `ActionServiceImpl#tickAll()`, and `tickAll()` has **zero
callers anywhere in this module's three source roots** (grepped across all three, worktree N/A for
this repository). No `@Scheduled` method, no `Bukkit.getScheduler()`/`BukkitRunnable`/
`runTaskTimer` call exists anywhere in the source. Separately, `BotPlayerV1_21_R1#tick()` (which
calls `nmsPlayer.doTick()` — real gravity, fall damage, hunger, etc.) is likewise never called from
anywhere in `ultibot-core`; the `tick-bots` config key gates nothing that would ever run in the
first place. `/bot action` itself succeeds and reports `bot_action_started`, and the ticker's own
`start()` sets `running = true`, but nothing external ever drives its clock forward — the bot
performs the named action exactly ZERO times after the command returns, on any interval, forever.

Separately, three of the ten declared `ActionType` values are silent no-ops even if the ticking
machinery above were fixed: `ATTACK`, `MINE`, and `LOOK_AT_NEAREST` each fall through
`ActionServiceImpl#performAction`'s `switch` with an empty case body and an inline comment
("no-op swing" / "no-op" / "Requires entity scan — handled at higher level") — none of the three
was ever implemented past the enum declaration. Filed together with the scheduling defect above,
since both roots in the same incomplete-implementation area.

## Dead command classes (unreachable)

`ActionCommands` (`permission = "ultibot.action"`, `manualRegister = true`) and `UtilityCommands`
(`permission = "ultibot.use"`, `manualRegister = true`) — both carry a `format`-identical
`@CmdMapping` to a mapping already live in `BotCommands` above, and neither is EVER registered
(UltiKits/UltiBot#15: `getCommandManager()` is called zero times anywhere in this module). Every
row below is Tier `internal` for that reason — not because the feature is meant to stay hidden
from ordinary users, but because no user, ordinary or otherwise, can reach it at all. Each row's
own behavior (if it COULD be reached) is otherwise identical to its `BotCommands` counterpart
documented above, so the Feature text here states only the unreachability, not a re-description.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultibot.dead.action | UNREACHABLE — `ActionCommands` is never registered (UltiKits/UltiBot#15); identical body to `ultibot.bot.action` | command | none — `manualRegister = true`, never registered | ultibot.action | both | internal | none | ultibot-core: ActionCommands#onAction (unreachable) |
| ultibot.dead.chat | UNREACHABLE — `UtilityCommands` is never registered (UltiKits/UltiBot#15); identical body to `ultibot.bot.chat` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onChat (unreachable) |
| ultibot.dead.clearmsg | UNREACHABLE; identical body to `ultibot.bot.clearmsg` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onClearMessages (unreachable) |
| ultibot.dead.click | UNREACHABLE; identical body to `ultibot.bot.click` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onClick (unreachable) |
| ultibot.dead.closeinv | UNREACHABLE; identical body to `ultibot.bot.closeinv` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onCloseInventory (unreachable) |
| ultibot.dead.cmd | UNREACHABLE; identical body to `ultibot.bot.cmd` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onCmd (unreachable) |
| ultibot.dead.deop | UNREACHABLE; identical body to `ultibot.bot.deop` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onDeop (unreachable) |
| ultibot.dead.inv | UNREACHABLE; identical body to `ultibot.bot.inv` — note this dead copy uses direct Bukkit `InventoryView` typing rather than `BotCommands#onInventory`'s reflection workaround, so even if it WERE reachable it would behave slightly differently on the Paper 1.21+ API this module targets | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onInventory (unreachable) |
| ultibot.dead.macro-list | UNREACHABLE; identical body to `ultibot.bot.macro-list` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onMacroList (unreachable) |
| ultibot.dead.macro-play | UNREACHABLE; identical body to `ultibot.bot.macro-play` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onMacroPlay (unreachable) |
| ultibot.dead.macro-record | UNREACHABLE; identical body to `ultibot.bot.macro-record` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onMacroRecord (unreachable) |
| ultibot.dead.macro-stop | UNREACHABLE; identical body to `ultibot.bot.macro-stop` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onMacroStop (unreachable) |
| ultibot.dead.messages | UNREACHABLE; identical body to `ultibot.bot.messages` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onMessages (unreachable) |
| ultibot.dead.op | UNREACHABLE; identical body to `ultibot.bot.op` | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onOp (unreachable) |
| ultibot.dead.skin | UNREACHABLE; identical body to `ultibot.bot.skin` (including the same blocking-main-thread defect, UltiKits/UltiBot#18, that this dead copy would also exhibit if it were ever reachable) | command | none — `manualRegister = true`, never registered | ultibot.use | both | internal | none | ultibot-core: UtilityCommands#onSkin (unreachable) |
| ultibot.dead.stop | UNREACHABLE — `ActionCommands` is never registered (UltiKits/UltiBot#15); identical body to `ultibot.bot.stop` | command | none — `manualRegister = true`, never registered | ultibot.action | both | internal | none | ultibot-core: ActionCommands#onStop (unreachable) |

## Player events

`BotEventListener` — the module's only `@EventListener`, two `@EventHandler` methods.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultibot.event.death-respawn | On a bot's death, respawn it automatically if `auto-respawn` is enabled | event | a bot dies (fall damage, combat, etc.) with `auto-respawn: true` | n/a | n/a | internal | brief | ultibot-core: BotEventListener#onPlayerDeath |
| ultibot.event.owner-quit-cleanup | On a REAL player's quit (bots' own quit events are explicitly filtered out first), remove every bot that player owns, if `auto-remove-on-quit` is enabled. A bot spawned via `/bot spawnat` (owner-less) is never removed by this handler regardless of this setting, since it has no entry in any player's owned-bot set to begin with | event | a real player who owns at least one bot quits, with `auto-remove-on-quit: true` | n/a | n/a | internal | brief | ultibot-core: BotEventListener#onPlayerQuit |

## Data persistence

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultibot.persistence.no-persistence | Bots (`BotManagerImpl`'s three in-memory maps) and macros (`MacroServiceImpl`'s in-memory map) have NO `@Table`/`DataOperator` binding anywhere in this module's source (`@Table` reconciliation count: 0) — every active bot and every saved macro is lost, unconditionally, on a full server restart. `/ul reload` does NOT lose them (the bean instances survive a config reload; only their own internal state does, or does not, get flushed to disk — here, nothing is ever flushed at all) | persistence | spawn a bot and/or record a macro, then restart the server | n/a | n/a | admin | detailed | ultibot-core: BotManagerImpl#BotManagerImpl, MacroServiceImpl#MacroServiceImpl |

## Configuration

Every leaf key in `ultibot-core/src/main/resources/config.yml` (8 keys, matching the
`@ConfigEntry` reconciliation count of 8 exactly — confirmed by reading `BotConfig.java` field by
field), all bound through the module's one `@ConfigEntity` class, `BotConfig`. Unlike UltiEconomy's
`config.yml`, every one of these 8 keys IS present in the packaged default resource — none is
migrated onto disk after the fact.

| ID | Feature | Kind | How to reach | Permission | Target | Tier | Manual | Source |
|---|---|---|---|---|---|---|---|---|
| ultibot.config.config.allow-chunk-loading | Whether bots keep their chunks loaded (ships disabled — recommended to leave off for performance, per the public doc's own FAQ) | config | `config.yml: allow-chunk-loading (default: false)` | n/a | n/a | admin | brief | ultibot-core: BotConfig#isAllowChunkLoading |
| ultibot.config.config.auto-remove-on-quit | Whether a real player's owned bots are removed automatically on their quit — see `ultibot.event.owner-quit-cleanup` above | config | `config.yml: auto-remove-on-quit (default: true)` | n/a | n/a | admin | brief | ultibot-core: BotEventListener#onPlayerQuit |
| ultibot.config.config.auto-respawn | Whether a bot respawns automatically after death — see `ultibot.event.death-respawn` above | config | `config.yml: auto-respawn (default: true)` | n/a | n/a | admin | brief | ultibot-core: BotEventListener#onPlayerDeath |
| ultibot.config.config.bot-prefix | Declared as the prefix shown in chat/tab for bots (`@ConfigEntry` comment: "Prefix shown in chat/tab for bots"); has **zero readers anywhere in this module's source** outside its own field declaration — no command, listener, or NMS bridge class ever calls `BotConfig#getBotPrefix()`. A spawned bot's display name is whatever `GameProfile#getName()` (the raw name argument to `/bot spawn`) returns, with no prefix ever applied. Filed as UltiKits/UltiBot#19 | config | `config.yml: bot-prefix (default: "[Bot] ", no effect, see UltiKits/UltiBot#19)` | n/a | n/a | admin | none | BotConfig#getBotPrefix (declared, never read outside this class) |
| ultibot.config.config.default-skin | Declared as the default skin name applied to a bot when none is specified (`@ConfigEntry` comment: "Default skin name for bots"); has **zero readers anywhere in this module's source** outside its own field declaration — `BotCommands#onSpawn`/`#onSpawnAt` never call `BotConfig#getDefaultSkin()` at all, and a freshly-spawned bot has no skin applied unless `/bot skin` is run against it afterward, contradicting the public doc's own "The bot appears at your position with the default skin (Steve)" Quick-Start claim. Filed as UltiKits/UltiBot#19 | config | `config.yml: default-skin (default: "Steve", no effect, see UltiKits/UltiBot#19)` | n/a | n/a | admin | none | BotConfig#getDefaultSkin (declared, never read outside this class) |
| ultibot.config.config.max-bots-per-player | Maximum bots a single player may own concurrently, checked in `BotManagerImpl#spawnBot` (the OWNED path only — `spawnBotNoOwner`, backing `/bot spawnat`, never checks this limit at all, so an owner-less bot spawned via `spawnat` can push the server arbitrarily far past this per-player cap) | config | `config.yml: max-bots-per-player (default: 5, @Range 1-100)` | n/a | n/a | admin | detailed | ultibot-core: BotManagerImpl#spawnBot |
| ultibot.config.config.max-total-bots | Server-wide bot cap, checked in BOTH `spawnBot` and `spawnBotNoOwner` — the one limit that does apply uniformly regardless of ownership | config | `config.yml: max-total-bots (default: 20, @Range 1-200)` | n/a | n/a | admin | brief | ultibot-core: BotManagerImpl#spawnBot, BotManagerImpl#spawnBotNoOwner |
| ultibot.config.config.tick-bots | Declared as the switch for bot physics ticking (gravity, collisions); has no effect regardless of its value — nothing anywhere in this module's source ever calls `BotPlayer#tick()` at all, gated or otherwise. Filed together with UltiKits/UltiBot#17 (the same "javadoc-promised periodic behaviour with zero wiring" root cause as the repeating-action defect) | config | `config.yml: tick-bots (default: true, no effect, see UltiKits/UltiBot#17)` | n/a | n/a | admin | detailed | ultibot-v1_21_R1: BotPlayerV1_21_R1#tick (declared, never called) |
