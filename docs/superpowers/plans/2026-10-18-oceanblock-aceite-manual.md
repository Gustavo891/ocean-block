# OceanBlock — Aceite manual (fork base + raft)

Checks that need a player in-game. Agents could not run them. Server: `server/`, port 25566. Boot by running `java -jar paper.jar --nogui` directly; `start.bat` restarts in a loop.

## 1. Preparação
- [ ] Save the raft schematic. Anywhere in the ocean, place a 3×3 of `OAK_PLANKS` at Y 62 and a chest on top of the corner plank (+1, 63, +1).
  - `//pos1` (-1, 62, -1) and `//pos2` (+1, 63, +1), relative to the center.
  - Stand on top of the center plank, with your feet at Y 63, then run `//copy` and `//schem save raft`.
  - Copy `plugins/FastAsyncWorldEdit/schematics/raft.schem` to `server/plugins/ocean-block/templates/raft.schem`.

## 2. Mundo
- [ ] Fly about 2000 blocks in two directions: only water, no land.
- [ ] Dive at (0, 0): the floor is sand/gravel at roughly Y 41–44.
- [ ] At X=5000, Z=5000, build on the surface and dig into the floor. Then select 100×100 at full height and run `//regen`: water and floor come back.

## 3. Raft
- [ ] A new player joins (or runs `/raft`): they land on the raft, level with the water, with the chest (or the 3×3 fallback if there is no schematic).
- [ ] Temporarily rename `raft.schem`, then create a raft with another new player: you get the 3×3 of planks plus a `[raft]` severe log. Restore the name.
- [ ] Break the center plank: `OAK_PLANKS` drops, it does not grow back, and no mob spawns.
- [ ] The HUD shows `Raft • 30x30`. After a border upgrade it shows `Raft • 50x50` right away.
- [ ] The scoreboard shows the title `OCEAN BLOCK` and no raw `%placeholder%`.
- [ ] `/raft`, `/balsa`, `/ilha`, `/island` and `/is` all open the command. `/is phases` and `/is setphase` no longer exist.
- [ ] Messages and menus say "raft" with the right gender ("seu raft").

## 4. Construção e membros
- [ ] Build outward from the raft's edge with dirt, cobblestone and planks. Outside the border it is blocked.
- [ ] `/raft member <outro>` lets the other player build. Without member or trust, they cannot.
- [ ] `/raft sethome` outside your own raft is refused. Inside it works, and survives a restart.

## 5. Reset
- [ ] Build outward, dig into the floor, drop an item, spawn a mob, and set a home elsewhere. Then run `/raft reset terrain <dono> -confirm`.
  - Water and floor are restored and the raft is pasted again.
  - The item and the mob are removed.
  - The player is back on top of the raft, and the home is back in the center.
- [ ] Place a custom block in the lot before the reset: it is no longer tracked afterwards.

## 6. Geral
- [ ] Log out and back in: you return to the raft.
- [ ] `/locate structure minecraft:shipwreck` finds nothing.
- [ ] `/oceanblock reload`, `/shop`, `/sell` and `/tops` respond.
