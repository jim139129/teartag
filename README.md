# TearTag

Fabric mod for a Minecraft 26.3 nametag-tearing minigame. TearTag renders a dynamic paper nametag on each enabled player's back and validates every tear attempt on the logical server.

Both the server and every client must install the mod. No generated per-player resource pack, chest-slot item, or following interaction entity is used.

## Requirements

- Minecraft 26.3
- Fabric Loader 0.19.3 or newer
- Fabric API 0.161.0+26.3 or newer compatible build
- Java 25

Optional accessory storage:

- TearTag always registers the single-stack `teartag:nametag` item. [Trinkets Updated 4.2.1+26.3](https://modrinth.com/mod/trinkets-updated) adds its dedicated `teartag/nametag` slot.
- When Trinkets is installed, `/teartag enable` automatically fills that slot if configured. Dynamic back rendering continues to use TearTag's server-authoritative state.
- Trinkets settings are under `[trinkets]` in `config/teartag.toml`. They control automatic equip, enabling on equip, and disabling on unequip. The core mod remains usable without Trinkets.

## Persistence and Replay Compatibility

Nametag state is stored in two complementary places:

- The authoritative text, tear count, eliminated state, and enabled state are attached to the player entity and saved in the player's `teartag_nametag` NBT compound.
- The `teartag:nametag` item stores only the display text in item custom data. Its item name is kept synchronized with the displayed nametag text.

This keeps state across server restarts, player saves, and player replacement. Trinkets is only an optional equipment integration; it is not a second source of gameplay state, and TearTag does not require a Trinkets slot to render or restore a nametag.

The same state is exposed through vanilla entity metadata. Replay tools that record player entity data can therefore restore the nametag without recording Trinkets inventory contents. On the client, rendering first uses synchronized player state and then falls back to the nametag item, including an item that only has a custom display name. This allows recorded scenes to render without requiring a live server update or a successfully replayed TearTag payload. If the recording contains no enabled nametag state, the client does not render one.

Build on Windows:

```powershell
.\gradlew.bat build
```


## Commands

```text
/teartag enable <targets>
/teartag disable <targets>
/teartag text set <targets> <json_component>
/teartag text plain <targets> <text>
/teartag text reset <targets>
/teartag reset round <targets>
/teartag reset all <targets>
/teartag rule set <targets> <namespace:rule>
/teartag config reload
/teartag status [target]
/teartag event claim
```

Enabling a player for the first time snapshots their current display name. `text reset` takes a new snapshot. State is stored by UUID in the world and survives restarts.

Changing text through `/teartag text ...` updates the active player state and the stored nametag item's display name together. Tearing, recovery, elimination, reset, enable, and disable update the player state only; they never compete with item data.

## Mechanics

- A normal tear requires the server ray to hit the finite rectangle on the target's back without hitting a block or the target body first.
- If the plate is directly against collision geometry, a front ray may pass through the target body and continue toward the plate. The server applies `wall.success_chance` (25% by default). A failed chance consumes the separate wall attempt cooldown.
- Enabled, non-eliminated participants deny ordinary player damage by default. A target's attack rule can allow or deny that damage directionally; other damage sources are unchanged.
- The default limit is five tears. After one second without a successful tear, one layer recovers every second until zero.
- On elimination, the client renders a temporary torn-off plate, while the server emits particles. The datapack may claim the elimination; otherwise the player is teleported and changed to spectator after ten seconds.

## Configuration

The server creates `config/teartag.toml` on first start. It documents ranges for combat distance, cooldowns, packet rate limiting, wall probability, recovery, elimination destination, text limits, rendering, HUD, permission level, and diagnostics.

`/teartag config reload` parses and validates a complete replacement before applying it. Invalid input leaves the previous live configuration unchanged. Authoritative rendering and hitbox values are sent to clients.

## Datapack Extension

See the complete Chinese guide at [`docs/DATAPACK.zh-CN.md`](docs/DATAPACK.zh-CN.md). A directly installable example is available under [`docs/datapack-example/tear_demo`](docs/datapack-example/tear_demo).

Attack rules live at `data/<namespace>/teartag_attack_rules/<path>.json`:

```json
{
  "allow_self": false,
  "any_of": [
    {
      "attacker_requires": ["game.hunter"],
      "attacker_forbids": ["game.paused"],
      "target_requires": ["game.target"],
      "target_forbids": ["game.immune"],
      "team": "different"
    }
  ],
  "participant_damage": "deny",
  "damage_rules": [
    {
      "attacker_requires": ["game.team_a"],
      "target_requires": ["game.team_b"],
      "policy": "allow"
    }
  ]
}
```

Entries in `any_of` are OR clauses; fields inside a clause are AND conditions. `team` is `any`, `same`, or `different`. Assign a loaded rule to the protected target with `/teartag rule set`.

`participant_damage` is the default ordinary player-damage policy for this target and is `deny` when omitted. `damage_rules` are checked in file order; the first matching rule overrides that default. They use the same tag and team conditions as `any_of`, but do not change whether the target's nametag can be torn. This makes directional damage possible without coupling it to tear permission: assign a rule with an `allow` damage rule to team B, while team A's rule can keep the default `deny`.

Rule conditions read vanilla entity tags created with `/tag`, not scoreboard objectives. Run `/reload` after changing rules or callback functions; `/teartag config reload` only reloads the TOML configuration.

Optional function tags:

- `#teartag:on_enable`
- `#teartag:on_disable`
- `#teartag:on_tear_success`
- `#teartag:on_participant_damage`
- `#teartag:on_wall_attempt_failed`
- `#teartag:on_recover`
- `#teartag:on_eliminate`
- `#teartag:before_default_teleport`
- `#teartag:on_reset`

During callbacks, `@s` is the subject and has `teartag.event_target`; an attacker, when present, has `teartag.event_attacker`. Storage `teartag:event` contains `event`, target/attacker UUID and name, `wall_attempt`, `tear_count`, `required_tears`, `rule`, and the serialized `text` component. For `on_participant_damage`, it additionally contains `damage_amount` and `damage_allowed`; this event fires for active participant pairs before the damage result is applied, including when the rule denies the damage. Temporary tags and storage are restored after the synchronous callback, so scheduled work must first copy data to its own storage.

Run `/teartag event claim` as the target from `#teartag:on_eliminate` to suppress the default delayed teleport. The command is rejected outside that callback.

## Java API

`dev.teartag.api.NametagApi` exposes queries and server-side mutations, including optional accessory availability/equipped-state queries and `participantDamagePolicy(attacker, target)`. `NametagEvents` exposes enable, disable, before/after tear, participant-damage, recovery, and elimination listeners. A participant-damage listener receives a mutable `DamageDecision`, so another mod can allow or deny the final result. `dev.teartag.item.NametagItems.NAMETAG` exposes the registered item. A before-tear listener can reject a valid tear; it cannot bypass server geometry validation.

When Trinkets Updated is present, `teartag:nametag` is a single-stack item accepted only by the `teartag/nametag` accessory slot. The server-side TearTag state remains authoritative, so datapacks and Java integrations can still enable, disable, reset, or replace text independently of the item.

## Repository Notes

The local legacy `nametag/` datapack and `nametag-resources-new/` resource pack are intentionally ignored by Git and are not build inputs. They can be removed manually after migration review.

Licensed under MIT.
