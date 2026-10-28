# Origin archive assets

Imported from the author's `archive_ui_minecraft_1_20_1_forge_prepare_v3_5_20261002.zip`, supplied with the Godot 4.7.2 Archive Book V3.5 showroom. The paper texture below is copied without modification into Synergy's namespace.

| Synergy resource | Source within `archive_ui_migration` | SHA-256 |
| --- | --- | --- |
| `textures/gui/origin_archive/paper_surface.png` | `resources/assets/archive_ui/textures/gui/archive/paper_surface.png` | `d17accc125e6865f46e5346be38ea26e9a0a03b25f54aff04f56f55bdd839777` |

The paper image is the supplied native bake of the showroom's procedural fiber shader (774 × 1095).

As requested for the Minecraft adaptation, audio uses the built-in UI button click, book placement, and book page-turn sound events instead of the showroom's exported samples. Hover is quieter than selection; a root transition clicks at its start, folder movement settles once on arrival, and paper navigation plays one page-turn sound. Minecraft's sound settings and resource packs control playback.

The showroom's example character pictures and Windows system font files are not imported. Portraits and paper dolls use the corresponding Minecraft player or Changed entity renderer, and text uses the game's current font resources.
