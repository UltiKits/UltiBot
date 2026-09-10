# UltiBot — UAT Checklist

This document is the executable companion to `FEATURES.md`: one row per feature stating the
steps to exercise it and the observable truth that proves it works. It is an internal reference
for real-machine verification, not user-facing documentation.

> Batches are dispatched at 60 rows or fewer, and a batch never spans two repositories. There are
> exactly two legitimate exits to `human-uat-pending`: a row needing the pixel layer while the
> real-client harness is not ready, and a row needing personal credentials. Every other row must
> reach `pass`, `fail`, or `blocked`.

## Conventions

- **Columns:** `ID`, `Preconditions`, `Steps`, `Expected`, `Layer`, `Covers`.
- **ID:** cites its `FEATURES.md` ID verbatim. A negative case suffixes the checklist ID only, as
  `.neg-<slug>` — a negative case still tests the same feature, so the base ID is unchanged.
- **Layer**, copied verbatim from Laojun's own `ultitools-real-client-uat` skill: `protocol`,
  `java-client`, `os-input`, `pixel`, `server`, `human`. This module has no panel-facing surface,
  so no row below carries `human`.
- **Covers** back-references a Phase 9 GUI-excluded class name; left blank when no such class
  applies. UltiBot is not one of the nine modules in Phase 9's GUI-exclusion register (no
  `UltiBot.md` file exists under `.planning/phases/09-module-ecosystem-readiness-and-test-coverage/gui-exclusions/`,
  and this module has no GUI page class at all), so every row below leaves `Covers` blank.
- **This module's i18n dictionary is clean** — every `plugin.i18n(...)` key literal used in
  `src/main/java` has an exact match in both `lang/en.yml` and `lang/zh.yml` (confirmed: 39 used
  keys, 0 missing from either file). Every Expected line below quoting chat text is therefore the
  real, reachable English translation under `language: en` — no UltiEconomy-style raw-source-
  literal caveat applies anywhere in this document.
- `{0}`/`{1}` placeholder substitution is a literal `String#replace(...)` call chain in this
  module (`plugin.i18n("key").replace("{0}", ...)`), not `String.format`'s `%s` — every quoted
  Expected line below reflects that substitution mechanism exactly.
- A row whose Preconditions cite a prior row's checklist ID must appear after that row in file
  order — asserted mechanically: for every row, every checklist ID literally cited in its
  Preconditions cell must have a strictly smaller line number in this file than the row citing it
  (sweep class 8, D-27a).
- **Expected** must name an observable truth — an exact chat line, a log line, a database row —
  and never the words "it works".
- **Fake player precondition:** every row below spawns or drives a real `ServerPlayer`-backed fake player
  entity. This repository's real-machine dispatch runs ALONE, never interleaved with another
  module's batch (see `FEATURES.md`'s own Conventions note) — restated here so a batch-composition
  tool reading only this file still sees the constraint.
- **Config-per-file rule (D-06):** one checklist row per `@ConfigEntity`-annotated class, never one
  row per key. This module ships exactly one yml resource (`plugins/UltiTools/pluginConfig/UltiBot/config.yml`, all 8 declared keys
  present in the packaged default — no migrated-on-first-boot keys the way UltiEconomy's
  `tax.*` block is), so this rule produces exactly one config-per-file row here.

## Bot management

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultibot.bot.list | At least one active bot, spawned by a name known in advance (positive control) | Run `/bot list` | Chat starts with `===== Active Bots =====` (gold), then one line per active bot reading `<name> - Owner: <first-8-chars-of-owner-uuid>` (yellow name, gray text) — including a line for the known bot named in the precondition | server | |
| ultibot.bot.list.neg-empty | No active bots | Run `/bot list` | Chat line reads `No active bots` (gray) — no header line precedes it, unlike the populated case above (`onList` returns before printing the header when the collection is empty) | server | |
| ultibot.bot.remove | A named bot currently active | Run `/bot remove <name>` | Chat line reads `Bot <name> removed` (red); `/bot list` immediately afterward no longer lists it | server | |
| ultibot.bot.remove.neg-not-found | No bot named `does-not-exist` | Run `/bot remove does-not-exist` | Chat line reads `Bot not found: does-not-exist` (red) | server | |
| ultibot.bot.remove-all | At least two active bots | Run `/bot remove all` | Chat line reads `All bots removed` (red); `/bot list` immediately afterward reports `No active bots` — confirming EVERY bot was removed, not just one named `all` | server | |
| ultibot.bot.reload | An OP sender or one holding `ultibot.admin`; `plugins/UltiTools/pluginConfig/UltiBot/config.yml` hand-edited to a distinguishable value (e.g. `max-total-bots`) BEFORE running this row's Steps | Run `/bot reload`, then attempt to observe the edited value's effect (e.g. spawn bots up to the new `max-total-bots` and confirm the OLD limit, not the new one, is what actually applies) | Chat line reads `Configuration reloaded` (green) — but the edited config value's effect does NOT change: `onReload` never re-reads `plugins/UltiTools/pluginConfig/UltiBot/config.yml` at all (UltiKits/UltiBot#16), so this row's whole point is confirming the success message is emitted despite nothing being reloaded | server | |
| ultibot.bot.reload.neg-no-permission | A sender holding neither OP nor `ultibot.admin` (but holding `ultibot.use`, since the class-level `@CmdExecutor` permission still gates entry to the command) | Run `/bot reload` | Chat line reads `You don't have permission to do this` (red) — from the method body's own hardcoded `sender.hasPermission("ultibot.admin")` check, not the class-level `@CmdExecutor` permission validator | server | |
| ultibot.bot.spawn | Sender is a player; no bot named `TestBot` exists; sender is under both the per-player and server-wide bot limits | Run `/bot spawn TestBot` as a player | Chat line reads `Bot TestBot spawned` (green); `/bot list` immediately afterward includes `TestBot`; the spawned bot has NO custom skin applied (still whatever the server's own default player skin rendering is) and no `[Bot] `-style prefix on its name anywhere — confirming `default-skin`/`bot-prefix` have no effect (UltiKits/UltiBot#19) | server | |
| ultibot.bot.spawn.neg-name-taken | A bot named `TestBot` already active (spawned via the row above) | Run `/bot spawn TestBot` again | Chat line reads `Name TestBot is already taken` (red); no second bot is created | server | |
| ultibot.bot.spawn.neg-limit-server | `max-total-bots` set to a small number (e.g. 1) already reached by OTHER players' bots, well BELOW the sender's own `max-bots-per-player` limit — isolating the server-wide cap as the true cause | Run `/bot spawn NewBot` as a player who has spawned zero bots themselves | Chat line reads `Reached personal bot limit (<max-bots-per-player value>)` (red) — quoting the PER-PLAYER limit even though the actual refusal cause was the SERVER-WIDE cap; the already-declared `bot_limit_server` message is never shown, confirming UltiKits/UltiBot#19 | server | |
| ultibot.bot.spawn-at | No bot named `SpawnedAtSpawn` exists; runnable from console | Run `/bot spawnat SpawnedAtSpawn` from console | Chat/console line reads `Bot SpawnedAtSpawn spawned` (green); the bot appears at the FIRST loaded world's spawn location, not any player's location; `/bot list` shows it with owner truncated to `unknown` (this bot has no tracked owner UUID at all) | server | |
| ultibot.bot.spawn-at.neg-owner-untracked | A bot spawned via `ultibot.bot.spawn-at` above | With `auto-remove-on-quit: true`, have ANY real player quit the server | The `spawnat`-created bot is NOT removed — `BotEventListener#onPlayerQuit` only removes bots tracked in the QUITTING player's own owner-set, and an owner-less bot has no entry in any player's set at all | server | |
| ultibot.bot.teleport | Sender is a player; a named bot exists elsewhere on the map | Run `/bot tp <name>` | Chat line reads `Bot <name> teleported` (green); the bot's location now matches the sender's | server | |
| ultibot.bot.teleport.neg-not-found | No bot named `does-not-exist` | Run `/bot tp does-not-exist` | Chat line reads `Bot not found: does-not-exist` (red) | server | |

## Actions

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultibot.bot.action | A named bot active | Run `/bot action <name> JUMP 20` | Chat line reads `Bot <name> started action: JUMP` (green) — but the bot NEVER ACTUALLY JUMPS, not once, regardless of how long the observer waits: `ActionServiceImpl#tickAll()` (the only method that would ever call the ticker's own `tick()`) has zero callers anywhere in this module (UltiKits/UltiBot#17). Observe for at least 60 real seconds (well past the 20-tick/1-second interval requested) to confirm no jump ever occurs | server | |
| ultibot.bot.action.neg-invalid-type | A named bot active | Run `/bot action <name> not-a-real-action 20` | Chat line reads `Invalid action type: not-a-real-action` (red); no ticker is created | server | |
| ultibot.bot.action.neg-no-op-types | A named bot active | Run `/bot action <name> ATTACK 20` (also try `MINE` and `LOOK_AT_NEAREST`) | The command reports `Bot <name> started action: ATTACK` (green, since these are valid enum values) — this row deliberately does NOT wait to observe an in-game effect, because with UltiKits/UltiBot#17's scheduling gap in place, waiting would only re-confirm that defect (the ticker never fires at all), not this row's own distinct claim. This row's own finding is a STATIC source read, not a live observation: `ActionServiceImpl#performAction`'s `switch` falls through the `ATTACK`/`MINE`/`LOOK_AT_NEAREST` cases with an empty body and an inline "no-op" comment (`ActionServiceImpl.java:38-46`) — read those three lines directly to confirm no implementation exists, rather than waiting on a ticker that will never run either way | protocol | |
| ultibot.bot.stop | A named bot with at least one active repeating action started via `ultibot.bot.action` above | Run `/bot stop <name>` | Chat line reads `Bot <name> stopped all actions` (green). This row deliberately makes NO claim about the ticker's internal `running`/`isRunning()` state actually flipping — `SimpleActionTicker#stop()` is not observable through this command's own server-layer output, and per UltiKits/UltiBot#17 the ticker was never actually ticking regardless, so any live behavioral difference before/after this command would only re-confirm that pre-existing defect, not this command's own distinct effect. `ActionServiceImpl#stopAllActions`'s own source (read directly, not observed live) confirms it removes the bot's tracker map entry and calls `stop()` on each ticker found there | server | |
| ultibot.bot.stop.neg-not-found | No bot named `does-not-exist` | Run `/bot stop does-not-exist` | Chat line reads `Bot not found: does-not-exist` (red) | server | |

## Interaction

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultibot.bot.chat | A named bot active; another real player online to observe chat | Run `/bot chat <name> Hello world` | The bot's message `Hello world` appears in chat exactly as a real player's message would (through the genuine `AsyncPlayerChatEvent` pipeline — any installed chat-formatting plugin applies its own formatting to it); the command sender separately sees `Bot <name> sent message` (green) | server | |
| ultibot.bot.chat.neg-not-found | No bot named `does-not-exist` | Run `/bot chat does-not-exist hi` | Chat line reads `Bot not found: does-not-exist` (red) | server | |
| ultibot.bot.cmd | A named bot active; OP granted to the bot beforehand (e.g. via `/bot op <name>`) if the target command needs it | Run `/bot cmd <name> say hello` | The bot executes `/say hello` as itself (visible as the bot's own name issuing the command, if the target command surfaces attribution); the sender sees `Bot <name> executed command` (green) | server | |
| ultibot.bot.cmd.neg-not-found | No bot named `does-not-exist` | Run `/bot cmd does-not-exist say hi` | Chat line reads `Bot not found: does-not-exist` (red) | server | |
| ultibot.bot.skin | A named bot active; a real, existing Mojang player name (e.g. `Notch`) NOT already cached by a prior fetch in this session; a way to actually OBSERVE the bot's rendered appearance (a second client's screen, or a screenshot/pixel check) — the chat line alone is not sufficient evidence, since it is emitted unconditionally on a non-null fetch result regardless of whether anything was applied | Run `/bot skin <name> Notch`, wait for the fetch to complete, then visually inspect the bot's actual in-game skin/appearance; separately, independently confirm the server's tick rate / other players' actions during the call | Chat line reads `Bot <name> skin changed to Notch` (green) once the fetch completes — but the bot's ACTUAL rendered appearance does NOT change to Notch's skin (or any skin at all): nothing in this module's source ever writes the fetched value/signature to the bot's `GameProfile` (UltiKits/UltiBot#21). This row's real finding is the mismatch between the message and the visual: a `pass` on the chat line alone, without the visual check, is a vacuous pass that conceals the defect. Separately, observe that OTHER players' movement/interactions freeze for up to several seconds while the two synchronous Mojang HTTP calls are in flight — confirming UltiKits/UltiBot#18 | pixel | |
| ultibot.bot.skin.neg-not-found | A named bot active; a name that does not exist on Mojang (e.g. a string with invalid characters) | Run `/bot skin <name> not_a_real_mojang_name!!!` | Chat line reads `Failed to fetch skin for not_a_real_mojang_name!!!` (red) | server | |
| ultibot.bot.skin.neg-bot-not-found | No bot named `does-not-exist` | Run `/bot skin does-not-exist Notch` | Chat line reads `Bot not found: does-not-exist` (red) | server | |

## Testing/debug commands

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultibot.bot.clearmsg | A named bot with at least one captured message (any other player's chat message the bot's fake connection would intercept) | Run `/bot clearmsg <name>` | Chat line reads `Messages cleared for <name>` (green); `/bot messages <name>` immediately afterward reports the empty case | server | |
| ultibot.bot.click | A named bot with a currently open inventory view (e.g. spawn it near a chest and have it interact, or use `/bot cmd` to open one) | Run `/bot click <name> 0` | Chat line reads `Bot <name> clicked slot 0` (green); a real `InventoryClickEvent` fires for slot 0 of the bot's open inventory — observable by any plugin's own inventory-click listener | server | |
| ultibot.bot.click.neg-invalid-slot | A named bot with an open inventory | Run `/bot click <name> not-a-number` | Chat line reads `Invalid slot number: not-a-number` (red) | server | |
| ultibot.bot.click.neg-no-inventory | A named bot online, with no container GUI open (only its own player inventory, the normal state) | Read `BotPlayerV1_21_R1.java:249-257` and `BotCommands.java:335-364` directly, rather than running the command live | `getOpenInventoryView()` delegates via reflection to the real `Player#getOpenInventory()`, whose Bukkit/Paper contract guarantees a non-null return for any online player (falling back to that player's own inventory view when no container is open) — so `BotCommands#onClick`'s `view == null` branch, and the `bot_no_inventory` message it would emit, can never actually fire against a live bot. This is a static source-level confirmation, not a live command run: attempting `/bot click <name> 0` with "no open inventory" as a precondition is not achievable in the first place (UltiKits/UltiBot#22) | protocol | |
| ultibot.bot.closeinv | A named bot with a currently open inventory | Run `/bot closeinv <name>` | Chat line reads `Bot <name> inventory closed` (green); the bot's open inventory view is closed | server | |
| ultibot.bot.deop | A named bot currently OP (granted via `/bot op <name>` beforehand) | Run `/bot deop <name>` | Chat line reads `Bot <name> is no longer OP` (green); the bot's own `isOp()` now reports `false` | server | |
| ultibot.bot.inv | A named bot with a currently open inventory containing at least one non-air item | Run `/bot inv <name>` | Chat starts with `===== Inventory of <name> (<inventory title>) =====` (gold), then one `  [<slot>] <display name or material> x<amount>` line per non-empty slot | server | |
| ultibot.bot.inv.neg-empty | A named bot with a currently open, but entirely empty, inventory | Run `/bot inv <name>` | The header line appears as above, followed by `Inventory is empty` (gray) instead of any item lines | server | |
| ultibot.bot.inv.neg-no-inventory | A named bot online, with no container GUI open (only its own player inventory, the normal state) | Read `BotPlayerV1_21_R1.java:249-257` and `BotCommands.java:382-427` directly, rather than running the command live | Same static-source confirmation as `ultibot.bot.click.neg-no-inventory` above — `onInventory`'s own `view == null` branch and its `bot_no_inventory` message can never fire against a live bot, for the identical reason (UltiKits/UltiBot#22); `/bot inv <name>` against any live bot always prints at least that bot's own inventory contents | protocol | |
| ultibot.bot.messages | A named bot that has received at least one chat message while online (e.g. another player sends a message the bot's fake connection would capture, or the bot is sent a private message) | Run `/bot messages <name>` | Chat starts with `===== Messages for <name> (<count>) =====` (gold), then one `[<index>] <message text>` line per captured message, in receipt order | server | |
| ultibot.bot.messages.neg-empty | A named bot with no captured messages (or freshly cleared via `ultibot.bot.clearmsg`) | Run `/bot messages <name>` | Chat line reads `No messages captured for <name>` (gray) | server | |
| ultibot.bot.op | A named bot active, not currently OP | Run `/bot op <name>` | Chat line reads `Bot <name> is now OP` (green); the bot's own `isOp()` now reports `true` | server | |

## Macros

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultibot.bot.macro-list | At least one macro previously saved (start a recording via `/bot macro record` against any bot, then end it with `/bot macro stop`) | Run `/bot macro list` | Chat starts with `===== Saved Macros =====` (gold), then one `  - <macro name>` line per saved macro (hardcoded literal formatting, not run through i18n) | server | |
| ultibot.bot.macro-list.neg-empty | No macros saved this session (macros are in-memory only and never persisted — a fresh server start alone satisfies this) | Run `/bot macro list` | Chat line reads `No saved macros` (gray) | server | |
| ultibot.bot.macro-play | A macro named `test-macro` previously saved (start a recording via `/bot macro record` against any bot, then end it with `/bot macro stop`); a named bot active | Run `/bot macro play <name> test-macro` | Chat line reads `Playing macro: test-macro` (green). This row does NOT attempt to observe an in-game replay effect live — the macro saved via `/bot macro record`/`/bot macro stop` is ALREADY established (by `ultibot.bot.macro-record` below) to always save an empty entry list regardless of what actions ran during "recording", so a live pass/fail here would only re-confirm that upstream defect, not test `onMacroPlay`'s own separate incompleteness. `onMacroPlay`'s own source (`BotCommands.java:471-489`, read directly) confirms it only calls `macroService.getMacro(macroName)` to check existence and prints this one confirmation line — it never iterates the returned `List<MacroEntry>` to re-invoke any action, for a saved macro of ANY size, empty or not | protocol | |
| ultibot.bot.macro-play.neg-not-found | No macro named `does-not-exist` | Run `/bot macro play <name> does-not-exist` | Chat line reads `Macro not found: does-not-exist` (red) | server | |
| ultibot.bot.macro-record | A named bot active, not currently recording a macro | Run `/bot macro record <name> test-macro`, then run one or more `/bot action` commands against the SAME bot while recording is active, then run `/bot macro stop <name>` | Chat line reads `Started recording macro for bot <name>: test-macro` (green) on the record command, then `Recording stopped` (green) on the stop command. This module exposes NO command that prints a saved macro's own entry COUNT (`/bot macro list` names macros only), so the "captures nothing" claim is NOT verified live by this row's own Steps — it is a static source fact, confirmed by reading `MacroServiceImpl#recordAction` (`MacroServiceImpl.java:38-44`) directly: it is the only method that appends to a `RecordingSession`'s entries, and it has ZERO callers anywhere in this module's source (`BotCommands#onAction`/`ActionServiceImpl#startRepeatingAction` never call it), so the SUBSEQUENT `/bot action` invocations during this row's Steps are never actually captured regardless of what this row observes live | protocol | |
| ultibot.bot.macro-record.neg-already-recording | The same named bot already recording (from the row above, with no stop command run against it yet) | Run `/bot macro record <name> another-name` | Chat line reads `Started recording macro for bot <name>: already recording` (green) — the literal English word "already recording" is substituted into the SAME success-shaped message template as the row above, not a distinct red refusal line; `MacroServiceImpl#startRecording` returned `false`, but `BotCommands#onMacroRecord` reuses `bot_macro_recording` with a different second argument rather than a dedicated refusal key | server | |
| ultibot.bot.macro-stop | The named bot from `ultibot.bot.macro-record` above, currently recording | Run `/bot macro stop <name>` | Chat line reads `Recording stopped` (green); `/bot macro list` immediately afterward includes `test-macro` — but per the row above, its entry list is empty regardless of how many actions were issued during the recording window | server | |

## Player events

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultibot.event.death-respawn | `auto-respawn: true` (shipped default); a named bot with `isOp()` false (so it takes real fall/combat damage) put in a lethal situation (e.g. teleported into lava, or attacked to zero health) | Cause the bot to die | The bot is automatically respawned (its `ServerPlayer` re-enters the world via `PlayerList#respawn`) without any operator command; `/bot list` still shows it as active afterward | server | |
| ultibot.event.death-respawn.neg-disabled | `auto-respawn: false`; same lethal setup as the row above; a way to observe whether a `PlayerRespawnEvent` fires for the bot's UUID (a debug listener, or a plugin that logs respawns) | Cause the bot to die, then wait at least 10 seconds | NO `PlayerRespawnEvent` ever fires for the bot — `BotEventListener#onPlayerDeath`'s own body is a single `if (config.isAutoRespawn())` guard around the one and only `bot.respawn()` call in this module's source, so with the flag false, `PlayerList#respawn` is never invoked at all. `/bot list` is NOT a reliable observation for this row (its `botsByName`/`botsByUuid` map entries are untouched by either branch of this handler — the bot is neither added nor removed from those maps by dying) | server | |
| ultibot.event.owner-quit-cleanup | `auto-remove-on-quit: true` (shipped default); a REAL player who owns at least one bot (spawned via `ultibot.bot.spawn`, not `spawnat`) | The owning player quits the server | Every bot that player owned is removed — `/bot list` no longer shows any of them | server | |
| ultibot.event.owner-quit-cleanup.neg-disabled | `auto-remove-on-quit: false`; a real player who owns at least one bot | The owning player quits the server | The owned bot(s) remain active — `/bot list` still shows them after the owner has quit | server | |

## Data persistence

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultibot.persistence.no-persistence | At least one active bot AND at least one saved macro, both noted by name before the restart | Stop the server completely (not `/ul reload`), start it again, then run `/bot list` and `/bot macro list` | BOTH commands report the empty case (`No active bots`, `No saved macros`) — neither the bot nor the macro survived the restart, confirming this module has zero `@Table`/`DataOperator` persistence anywhere | server | |

## Configuration

One row per `@ConfigEntity`-annotated class (D-06's config-per-file rule) — this module has
exactly one, `BotConfig`, bound to `plugins/UltiTools/pluginConfig/UltiBot/config.yml`.

| ID | Preconditions | Steps | Expected | Layer | Covers |
|---|---|---|---|---|---|
| ultibot.config.config-yml | Fresh `plugins/UltiTools/pluginConfig/UltiBot/config.yml` at its shipped default (not hand-edited) | Confirm each of the 8 keys listed under this document's companion `FEATURES.md` `## Configuration` section is present at its documented default; then exercise the two keys that DO have a live effect, each in its own restart so neither masks the other: (a) set `max-total-bots: 2` (default 20), restart, and confirm a third `/bot spawn` attempt (across any combination of players) refuses with the per-player-limit message per UltiKits/UltiBot#19's own finding (the server-wide cap IS enforced — only its OWN dedicated message never shows); (b) set `max-bots-per-player: 1` (default 5), restart, and confirm a single player's second `/bot spawn` attempt refuses with `Reached personal bot limit (1)`. Do NOT attempt to observe an effect from `tick-bots`, `default-skin`, `bot-prefix`, `auto-remove-on-quit`, or `auto-respawn` in THIS row — the first three have no reader anywhere in the source (UltiKits/UltiBot#17, #19) and the latter two are already exercised by their own dedicated `ultibot.event.*` rows above, which need a real player-quit/bot-death event this row's Steps do not produce | All 8 keys present at their documented defaults before either change; after lowering `max-total-bots`, the third spawn attempt refuses; after lowering `max-bots-per-player`, the same single player's second spawn attempt refuses with the value `1` quoted in the message | server | |
