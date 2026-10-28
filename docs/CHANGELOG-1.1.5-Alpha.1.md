# Changed: Synergy 1.1.5-Alpha.1

### Interface and controls

- Added spring motion to UI transitions, radial opening, switching and paging, the ability HUD, archives and dialogue screens, with a client-only Reduced UI Motion setting.
- The ability wheel supports both tap-to-open/click-to-select and hold-to-aim/release-to-select, with mouse-driven 3D tilt. Players can walk, jump, crouch and sprint while holding either player wheel, switch between them with the scroll wheel, and keep moving as the ability wheel closes. Control hints use plain text and show the hold controls from the first frame.
- Relationship panels have soft, breathing theme-colored shadows, staggered entrances and exit fades. They keep a stable layout while the full-size wheel moves between the center and right-side positions.

### Creature interactions and chase games

- Added companion seating: designate boats, minecarts or chairs, or call following companions aboard a multi-seat vehicle. Includes safe dismounts available on the first social-wheel page, a generic passenger fallback, optional Superb Warfare support and separate boarding settings. Travel together also recalls nearby seated friends, even if they were already following.
- Idle latex creatures can interact with one another, exchange pats and emotes, and invite nearby non-hostile creatures to play. Personality affects their preferences, familiar playmates are preferred, and work, combat, rescue and player commands take priority.
- Play together can now start a ground chase while retaining its existing hypnosis-practice and random friendly-hug outcomes. Crouch to chase or jump to run. The runner starts immediately with three seconds of head start. Both participants see the chaser's countdown, then the chaser starts as it ends. A distance bar and remaining time appear when pursuit begins.
- Player chasers catch their friend by getting close and right-clicking. Starting early voids the round. Catches can end in a harmless two-second hug with a grab sound; unsuccessful chases lead to encouragement. Quickly press crouch three times to leave, with both hold and toggle crouch supported.
- Added separate settings for automatic creature interactions, chase games, catch hugs, chase duration and automatic chase cooldown.
- Bonded interventions and chase games use creature dialogue for spoken reactions, with essential controls shown in the HUD.

### New species and native origins

- Added Latex Muntjac, a jungle-population species with natural jungle spawns, a playable origin, spawn egg and matching transfur items. Uses its supplied model, texture and ear animations, with attributes matching Latex Deer.
- Added Latex Swordfish, an open-ocean aquatic species with a playable origin, natural deep-ocean spawns, spawn egg and matching transfur items. Its swimming speed is increased, and NPC rushes aim at their target with sword-tip contact damage. Corrected its natural spawning in open deep water during both day and night. Swordfish Rush works only in water, costs half a hunger shank and launches at Riptide II speed with contact damage. It blends from swimming into a straight, rotating dash, recovering promptly when movement ends, with Riptide effects and sound without the throwing intro.
- Idle NPC Silk Moths prefer resting near safe light sources, including on glass floors, with more reliable attraction during idle wandering. Their approach and resting spots avoid fire, lava and lit campfires, while work, following, combat and social interactions take priority.
- Added Latex Silk Moth, a four-armed species belonging to the forest variant population. It spawns naturally in forests and has a playable transfur form, spawn egg, latex syringes, flasks and tipped arrows. The original Latex Moth remains available.
- Silk Moth can Spin Silk, spending food energy to produce four string per use. Its antennae, wings and extra jumps have matching animations.
- Added a Crisscross log-note slip to Silk Moth's native-origin dossier. Its transfur items appear with the corresponding item groups in the creative tab, and Synergy's own species appear in origin selection without enabling third-party forms.

### Friendship, factions and companions

- Partial Dark Latex Wolves, Partial Snow Leopards and Latex Humans can speak without translation or telepathy. Hypno Cats' temporary understanding is limited to their hypnosis interaction and its result dialogue.
- Restored access to wild-outpost storage at every reputation level, while retaining witnessed warnings, reputation costs and guard reactions.
- Reduced repeated failed pathfinding by displaced workers and guards. Camp residents remain separate from wild-community jobs when camp features are paused, and blocked deliveries retain their cargo.
- Native gender switching now preserves transfur negotiation, including its original creature, progress and cooldown.
- Milk Puddings and Feral Sharks remain wanderers; existing provider, scout and guard assignments are corrected.
- Injured bonded and friendly creatures can eat food from their own inventory to recover, choosing by injury severity and useful healing while reserving golden apples for critical health. Recovery depends on nutrition and saturation, with native food effects, eating times and bowl/bottle returns preserved. Critically injured bonds still prioritize wrapping their player. Added a separate auto-eating setting.
- Added the operator command /changedsynergy bond revive to restore a deceased companion beside its player, preserving its saved identity and bond without duplicating dropped equipment.
- Established acquaintances can offer confirmed voluntary transfur without requiring Close friendship or a bond token. The first successful voluntary transfur grants a substantial affection bonus, with new faction dialogue.
- Restored Addon Plus latex-totem protection when Keep Conscious is disabled. Fatal punitive takeover remains possible; a totem can prevent the loss of consciousness and let the player claim the carrier's body, with corresponding creature reactions.
- Restored peaceful competition between Dark and White latex, including transformed players and TrueTransfur Instinct bodies. Added separate settings for peaceful relations and protection from the opposite faction's environmental latex hazards. Genuine aggression and hostile standing still have consequences.
- Player-outpost workers can use Sophisticated Storage containers, including upgraded capacities and storage filters, for deliveries and work supplies. Improved recognition of modded chest block entities. Both halves of wild-community double chests retain the same ownership and witnessed-access reactions.
- Fixed bonded companions repeatedly pressing against their owner instead of intervening after witnessed kin killings. Human owners receive confinement takeover; transformed owners are carried away without changing species. Organic bonds use the same safe carry.
- Fixed player-controlled bonded wraps producing idle chatter in wandering mode and taking equipment the player was trying to collect. Restored Pure White regional eye adaptation during bonded wrapping.
- Fixed bonded native pets calling their player "master" instead of using a partner's address.
- Escaping fatal punitive takeover no longer grants a temporary ceasefire. Tolerated empty-hand interruptions of greetings or transfur approaches no longer turn nearby compatriots hostile.
- Fixed villagers fleeing transformed players and iron golems attacking them solely for their species with Peaceful Village Relations enabled. Also fixed a crash during nearby-player scare detection.
- Fixed creatures getting stuck beside upright trapdoors, piglins repeatedly losing their combat target around wrapped players, and toggle crouch preventing taur riding.
- Added client settings to hide untranslated animal-call text and native TrueTransfur players' overhead nameplates. Understood dialogue remains available.

### Mod compatibility

- Added 3D Skin Layers support for Latex Human, Partial Dark Latex Wolf and Addon Plus's Partial Snow Leopard, including classic and slim skins.
- Fixed TaCZ Additions cancelling gunfire while the player controls a bonded latex wrap.
- Fixed Addon Plus's Latex Solvent weapon enchantment failing to give armed latex creatures its damage bonus against latex targets.
- With Changed Vanilla installed, creatures with a trusted Familiar relationship stop assimilating animals, including while their friend is offline.
- Added support for Sophisticated Storage, contributed by Kodyris.
