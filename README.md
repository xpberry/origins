# ServerRaces

`ServerRaces` is a Paper 1.20.6+ plugin for Java players and Bedrock players
connected through GeyserMC/Floodgate. Race selection is stored permanently on
the player with the namespaced PDC key `serverraces:selected_race`.

## Build and install

```sh
cd server-plugin
./gradlew build
```

The build targets Java 21 and Paper API 1.20.6. Copy
`server-plugin/build/libs/ServerRaces-1.0.0.jar` into the server `plugins/`
directory. Geyser and Floodgate remain soft dependencies; no Bedrock client mod
is required.

## Player controls

- `/race gui` opens the 27-slot chest race selector.
- `/race choose <race>` selects a race directly.
- `/race ability` or `/ability` activates the selected ability.
- Crouch and right-click with an empty main hand to activate the ability.

The ability cooldown is shown in the action bar. Selection and attributes are
server-authoritative, so Java and Geyser-connected Bedrock players use the same
logic and persistence.

## Races

All twelve races are implemented in `server-plugin/src/main/java/dev/serverraces`:
Dragonborn, Dwarf, Giant, Warlock, Witch, Mermaid, Fairy, Celestid, Shadowkin,
Arachnid, Ignis, and Golem.

`bedrock-pack` is available as a standalone Bedrock behavior pack for worlds that
do not use a Java server.