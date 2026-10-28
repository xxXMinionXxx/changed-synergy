# Changed: Synergy 1.1.5-Beta.1

### Interface and controls

- Added spring animations to interface transitions, radial wheels and the ability HUD, with a client setting to reduce motion.
- The ability wheel supports tap-to-open/click-to-select and hold-to-aim/release-to-select. Both player wheels have mouse-driven tilt, animated highlights and paging. Players can move, jump, crouch and sprint while holding a wheel, and switch between ability and relationship wheels with the scroll wheel.
- Redesigned relationship and social panels with species-themed artwork, readable text and faction-colored labels. Panels fade in and out while the wheel moves between its central and side positions.
- Updated outpost management and creature inventories with vanilla-style frames, member lists, item slots and a clearer Work Cargo section.
- Notes, clipboards and disc-writing screens resize to fit their content, retaining readable text and visible buttons. Long documents can scroll without text spilling outside the panel.
- Updated the mod icon to the Silk Moth artwork.

### New species

- Added **Latex Silk Moth**, a four-armed forest species with natural spawns, a playable origin, a spawn egg and matching transfur syringes, flasks and arrows. **Spin Silk** consumes food energy to produce four string. Idle moths prefer safe light sources while avoiding fire, lava and lit campfires. Their origin dossier includes a Crisscross log note.
- Added **Latex Swordfish**, an open-ocean aquatic species with natural deep-ocean spawns, a playable origin, a spawn egg and matching transfur items. **Swordfish Rush** works underwater, costs food energy and launches at Riptide II speed, with a straight swimming pose, body rotation, particles and Riptide sound. Contact damage ignores half the target's armor. NPCs can repeat the rush on a four-second cooldown, and fatal rushes have their own death message. Corrected natural spawning without making Swordfish disproportionately common.
- Added male and female **Latex Muntjac**, belonging to the jungle population, with natural jungle spawns, playable origins, spawn eggs and matching transfur items. The synthesizer selects the sex of Muntjac transfur items.
- Milk Puddings and Feral Sharks remain wanderers rather than receiving provider, scout or guard jobs.

### Social interaction and games

- Idle latex creatures can interact with one another, exchange pats and emotes, and invite nearby non-hostile creatures to play. Personality affects their preferences, and familiar playmates are favored. Combat, work, rescue and player commands take priority.
- **Play together** can start a chase while retaining its hypnosis-practice and random friendly-hug outcomes. Crouch to chase or jump to run; the runner has a three-second head start. Players see a countdown, distance bar and remaining time. A player chaser catches their friend by getting close and right-clicking. Starting early ends the round; quickly pressing crouch three times leaves it.
- Chase games support land and aquatic creatures, including NPC pairs and underwater player games. Catches can end in a harmless short hug; unsuccessful chases lead to encouragement. Added separate settings for automatic interactions, chase games, catch hugs, game duration and automatic chase cooldown.
- Added **Friendly hug** mode to the player's existing grab ability, toggled by right-clicking its wheel option. It can hold humans, humanoid monsters, latex creatures and transformed players without damage, transfur, absorption or wrapping. Manual release and player escape remain available, and hostile targets retain their hostility. Forms without grabbing do not gain the ability.
- Friendly hugs include two pup carry styles and tailored poses for small feral forms, Feral Sharks, Milk Puddings and Headless Knights. Added matching arm movement for Swordfish and Muntjac forms.
- Fixed missing arm-grab escape QTEs, lingering held poses and released targets snapping back into a hug. Friendly releases end the held-key action and apply a two-second recapture cooldown.
- Fixed holding the human pat key repeatedly triggering dialogue and social reactions, including Addon Plus's pat key.
- Partial Dark Latex Wolves, Partial Snow Leopards and Latex Humans can speak without translation or telepathy. Hypno Cats' direct understanding applies during hypnosis and its result dialogue.

### Bonds, factions and companions

- Established acquaintances can offer confirmed voluntary transfur without requiring Close friendship or a bond token. The first successful voluntary transfur grants a substantial affection bonus and has corresponding creature dialogue.
- Restored Addon Plus latex-totem protection when Keep Conscious is disabled. A totem can prevent fatal punitive takeover and let the player claim the carrier's body, with corresponding reactions.
- Non-bonded takeover offers **Give up completely**, followed by a separate confirmation. The carrier reacts and says farewell. Fatal consciousness loss gradually covers the screen in the carrier's theme color, with time for the dialogue to finish before death.
- Bonded companions reliably intervene after excessive witnessed killings of their kin. Human players receive confinement takeover; transformed players are carried away without changing species. Organic bonds use the same safe carry. Escaping fatal punitive takeover no longer grants a temporary ceasefire, and tolerated bare-handed interruptions no longer provoke nearby compatriots.
- Restored peaceful relations between Dark and White latex, including transformed players and Instinct bodies. Separate settings control faction peace and protection from the opposite faction's environmental latex hazards.
- Injured bonded and friendly creatures can eat food from their own inventory to recover. Food choice considers injury severity, nutrition, saturation and useful effects, reserving golden apples for serious injuries. Critically injured bonds still prioritize wrapping their player. Added an independent auto-eating setting.
- Added `/changedsynergy bond revive` for operators to restore a deceased companion beside its player.
- Added companion boarding for boats, minecarts and modded seats, including a multi-seat **Call aboard** action, generic passenger support and optional Superb Warfare compatibility. Social wheels offer a dismount action, and **Travel together** recalls nearby seated friends.
- Fixed player-controlled bonded wraps producing wandering chatter or taking items intended for the player. Restored Pure White eye adaptation while wrapped, and corrected bonded pets addressing their player as a partner.
- Fixed villagers fleeing transformed players and iron golems attacking them solely for their species when peaceful village relations are enabled, including the nearby-player detection crash.
- Improved navigation around upright trapdoors and limited door-crouching to low facility doorways. Fixed piglins repeatedly losing their target around wrapped players and toggle crouch preventing taur riding.
- Form changes, including native gender switching, preserve transfur negotiation. Leaving initial origin selection with Esc chooses human; cancelling optional reselection keeps the existing identity.
- Added client settings to hide untranslated animal-call text and native TrueTransfur players' overhead nameplates. Understood dialogue remains available.

### Outposts and structures

- Restored access to wild-outpost storage at every reputation level, retaining witnessed warnings, reputation costs and guard reactions.
- Displaced workers and guards no longer repeatedly attempt expensive return routes. Player-camp assignments remain separate from wild-community jobs, and blocked deliveries retain their cargo.
- Added naturally generated aquatic laboratories and white, red and green laboratory towers, **built by Crossader**. Green laboratories generate in jungle, sparse jungle and bamboo jungle. The structures include loot, inhabitants, traps, books, notes, discs and password clues.
- Added advancements for chase games, companion boarding, silk production, Swordfish Rush, laboratory exploration, voluntary transfur and latex-totem takeover reversal.

### Mod compatibility

- Player-outpost workers support **Sophisticated Storage**, including upgraded capacities and storage filters. Both halves of wild-community double chests share ownership and witnessed-access reactions. Includes contributions from **Kodyris**.
- Added **3D Skin Layers** support for Latex Human, Partial Dark Latex Wolf and Addon Plus's Partial Snow Leopard, including classic and slim skins.
- Fixed **TaCZ Additions** cancelling gunfire while the player controls a bonded wrap, and Addon Plus's **Latex Solvent** enchantment failing on weapons wielded by latex creatures.
- With **Changed Vanilla**, creatures with a trusted Familiar relationship stop assimilating animals, including while their friend is offline.
- TrueTransfur integration targets **2.x**; compatibility with **1.0.1** is no longer maintained.
