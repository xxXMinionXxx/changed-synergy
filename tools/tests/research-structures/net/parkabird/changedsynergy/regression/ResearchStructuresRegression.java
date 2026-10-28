package net.parkabird.changedsynergy.regression;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.parkabird.changedsynergy.world.ResearchLabStructure;

@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.DEDICATED_SERVER)
public final class ResearchStructuresRegression {
    private static final List<String> NAMES = List.of("water_tower_changed", "white_lab_tower", "red_lab_tower", "green_lab");
    private static int checks;

    @SubscribeEvent public static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            var level = server.overworld();
            level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
            var structures = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
            for (var name : NAMES) {
                if (Boolean.getBoolean("changed_synergy.greenLabOnly") && !name.equals("green_lab")) continue;
                var id = id(name);
                var structure = structures.get(id);
                check(structure instanceof ResearchLabStructure, "datapack structure decoded: " + name);
                if (name.equals("green_lab")) {
                    var biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
                    for (var biome : biomes.holders().toList())
                        check(structure.biomes().contains(biome) == biome.is(BiomeTags.IS_JUNGLE),
                                "green laboratory eligibility follows the jungle family exactly: " + biome.key().location());
                    check(structure.biomes().size() >= 3, "all three vanilla jungle variants eligible");
                }
                var raw = read(level, name);
                if (name.equals("red_lab_tower")) {
                    var residents = raw.getList("entities", Tag.TAG_COMPOUND);
                    check(residents.size() == 20, "Crossader's cleaned red tower retains eight wolves and twelve displays");
                    check(residents.stream().noneMatch(value -> ((CompoundTag)value).getCompound("nbt").getString("id").equals("minecraft:creeper")),
                            "red tower contains no authored creepers");
                    for(var repaired:List.of(new BlockPos(18,7,23),new BlockPos(20,7,5),new BlockPos(21,7,5),new BlockPos(27,7,17),new BlockPos(38,7,18),new BlockPos(38,7,19)))
                        check(raw.getList("blocks",Tag.TAG_COMPOUND).stream().map(value -> (CompoundTag)value)
                                .anyMatch(block -> pos(block.getList("pos",Tag.TAG_INT)).equals(repaired)
                                        &&raw.getList("palette",Tag.TAG_COMPOUND).getCompound(block.getInt("state")).getString("Name").equals("changed:dark_latex_block")),
                                "Crossader's revised ceiling retains its authored backing: "+repaired);
                    check(raw.getList("blocks", Tag.TAG_COMPOUND).stream().filter(value ->
                            raw.getList("palette", Tag.TAG_COMPOUND).getCompound(((CompoundTag)value).getInt("state")).getString("Name").equals("changed:dark_latex_puddle")).count() == 41,
                            "the nine replaced puddles and 32 existing puddles use Changed's native block");
                }
                if (name.equals("green_lab")) {
                    check(raw.getList("size", Tag.TAG_INT).getInt(0) == 64, "completed green template width is 64");
                    var residents = raw.getList("entities", Tag.TAG_COMPOUND);
                    check(residents.size() == 1 && residents.getCompound(0).getCompound("nbt").getString("id").equals("changed:gas_wolf_male"),
                            "revised green laboratory preserves its gas-wolf occupant without an invalid old-world seat");
                }
                validatePalette(raw, name);
                validateContents(level, raw);
                var template = server.getStructureManager().get(id).orElseThrow();
                check(template.getSize().getY() <= 48 && template.getSize().getX() > 30, "template dimensions retained: " + name);
                for (var rotation : Rotation.values()) {
                    var position = new BlockPos(1200 + NAMES.indexOf(name) * 200, 100, 1200 + rotation.ordinal() * 160);
                    if (name.equals("water_tower_changed")) position = position.atY(35);
                    var piece = new ResearchLabStructure.Piece(server.getStructureManager(), id, position, rotation, name.equals("water_tower_changed"));
                    var tag = piece.createTag(StructurePieceSerializationContext.fromLevel(level));
                    var restored = new ResearchLabStructure.Piece(server.getStructureManager(), tag);
                    check(restored.getBoundingBox().equals(piece.getBoundingBox()) && restored.getRotation() == rotation
                            && restored.templatePosition().equals(position), "piece reload preserves rotation and anchor: " + name + rotation);
                }
                var start = findNaturalStart(level, structure, name.equals("water_tower_changed"));
                check(start != null && start.isValid(), "seeded natural placement succeeds: " + name);
                var piece = (ResearchLabStructure.Piece) start.getPieces().get(0);
                System.out.println("RESEARCH_NATURAL_START " + name + " " + piece.templatePosition() + " " + piece.getRotation());
                placeAndCheck(level, piece, raw, name, true);
                if (name.equals("green_lab")) {
                    var anchor = start.getChunkPos().getMiddleBlockPosition(piece.templatePosition().getY());
                    var biome = level.getBiome(anchor);
                    check(biome.is(BiomeTags.IS_JUNGLE), "generated green laboratory is actually in a jungle biome");
                    System.out.println("GREEN_LAB_NATURAL_BIOME " + biome.unwrapKey().orElseThrow().location() + " anchor=" + anchor);
                }
            }
            // Rotation placement on the real chunk boundary path, not a single whole-template paste.
            if (!Boolean.getBoolean("changed_synergy.greenLabOnly")) {
            var white = read(level, "white_lab_tower");
            for (var rotation : Rotation.values()) {
                var piece = new ResearchLabStructure.Piece(server.getStructureManager(), id("white_lab_tower"),
                        new BlockPos(1600, 110, 1600 + 120 * rotation.ordinal()), rotation, false);
                placeAndCheck(level, piece, white, "white rotation " + rotation, false);
            }
            var red = read(level, "red_lab_tower");
            for (var rotation : Rotation.values()) {
                var piece = new ResearchLabStructure.Piece(server.getStructureManager(), id("red_lab_tower"),
                        new BlockPos(2100, 110, 1600 + 120 * rotation.ordinal()), rotation, false);
                placeAndCheck(level, piece, red, "red rotation " + rotation, false);
            }
            }
            System.out.println("RESEARCH_STRUCTURES_REGRESSION_PASS: " + checks + " registry, palette, loot, covers, rotation, clipping, reload and natural terrain checks; addon=" + ModList.get().isLoaded("changed_addon"));
        } catch (Throwable failure) {
            System.err.println("RESEARCH_STRUCTURES_REGRESSION_FAIL");
            failure.printStackTrace();
        } finally {
            server.halt(false);
        }
    }

    private static CompoundTag read(ServerLevel level, String name) throws Exception {
        try (var resource = level.getServer().getResourceManager().open(id("structures/" + name + ".nbt"))) {
            return NbtIo.readCompressed(resource);
        }
    }

    private static void validatePalette(CompoundTag raw, String name) {
        var palette = raw.getList("palette", Tag.TAG_COMPOUND);
        var invalid = new ArrayList<String>();
        for (var value : palette) {
            var tag = (CompoundTag) value;
            var id = new ResourceLocation(tag.getString("Name"));
            check(List.of("minecraft", "changed", "changed_synergy").contains(id.getNamespace()), "palette has no optional mod dependency");
            if (!BuiltInRegistries.BLOCK.containsKey(id)) { invalid.add("missing block: " + id); continue; }
            check(!id.getPath().equals("structure_block") && !id.getNamespace().equals("terramity"), "no editor/extra dependency");
            check(!id.getNamespace().equals("immersive_weathering"), "no new Weathering dependency");
            var block = BuiltInRegistries.BLOCK.get(id);
            var props = tag.getCompound("Properties");
            for (var property : props.getAllKeys()) {
                var definition = block.getStateDefinition().getProperty(property);
                if (definition == null || definition.getValue(props.getString(property)).isEmpty()) invalid.add(id + " " + property + "=" + props.getString(property));
            }
        }
        check(invalid.isEmpty(), "invalid template palette: " + invalid);
        var positions = new HashSet<BlockPos>();
        for (var value : raw.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) value;
            check(positions.add(pos(block.getList("pos", Tag.TAG_INT))), "unique block coordinates");
            var tag = block.getCompound("nbt");
            if (tag.contains("LootTable")) check(tag.getList("Items", Tag.TAG_COMPOUND).isEmpty(), "loot never overwrites authored inventory");
            if (name.equals("green_lab") && pos(block.getList("pos", Tag.TAG_INT)).equals(new BlockPos(54, 2, 53))) {
                check(palette.getCompound(block.getInt("state")).getString("Name").equals("minecraft:trapped_chest"), "green trap remains a trapped chest");
                check(tag.getString("LootTable").equals("changed:chests/low_tier_lab"), "green trap chest retains the author's revised loot");
            }
        }
        System.out.println("RESEARCH_PALETTE_VALID " + name + " blocks=" + positions.size());
    }

    private static void validateContents(ServerLevel level, Tag value) {
        if (value instanceof CompoundTag tag) {
            for (var key : List.of("id", "Name", "LootTable", "variant")) {
                if (tag.contains(key, Tag.TAG_STRING) && tag.getString(key).matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                    check(List.of("minecraft", "changed", "changed_synergy").contains(new ResourceLocation(tag.getString(key)).getNamespace()),
                            "nested content has no optional mod dependency: " + tag.getString(key));
            }
            if (tag.contains("LootTable"))
                check(level.getServer().getLootData().getLootTable(new ResourceLocation(tag.getString("LootTable")))
                        != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "referenced loot table resolves: " + tag.getString("LootTable"));
            if (tag.contains("Count") && tag.contains("id", Tag.TAG_STRING))
                check(BuiltInRegistries.ITEM.containsKey(new ResourceLocation(tag.getString("id"))), "authored item resolves: " + tag.getString("id"));
            if (tag.contains("Pos") && tag.contains("id", Tag.TAG_STRING)) {
                check(BuiltInRegistries.ENTITY_TYPE.containsKey(new ResourceLocation(tag.getString("id"))), "authored entity resolves");
                check(!tag.contains("UUID") && !tag.contains("Attributes") && !tag.contains("ForgeCaps"), "old-world occupant identity and compatibility snapshots removed");
            }
            for (var key : tag.getAllKeys()) validateContents(level, tag.get(key));
        } else if (value instanceof net.minecraft.nbt.ListTag list) for (var child : list) validateContents(level, child);
    }

    private static StructureStart findNaturalStart(ServerLevel level, Structure structure, boolean ocean) {
        var server = level.getServer();
        var generator = level.getChunkSource().getGenerator();
        var placement = (RandomSpreadStructurePlacement) server.registryAccess().registryOrThrow(Registries.STRUCTURE_SET)
                .get(id(ocean ? "ocean_laboratories" : "surface_laboratories")).placement();
        for (int ring = 0; ring <= 18; ring++) for (int x = -ring; x <= ring; x++) for (int z = -ring; z <= ring; z++) {
            if (Math.max(Math.abs(x), Math.abs(z)) != ring) continue;
            var candidate = placement.getPotentialStructureChunk(level.getSeed(), x * placement.spacing(), z * placement.spacing());
            if (!placement.isStructureChunk(level.getChunkSource().getGeneratorState(), candidate.x, candidate.z)) continue;
            var start = structure.generate(server.registryAccess(), generator, generator.getBiomeSource(),
                    level.getChunkSource().randomState(), server.getStructureManager(), level.getSeed(), candidate, 0, level, structure.biomes()::contains);
            if (start.isValid()) {
                var natural = level.getChunk(candidate.x, candidate.z, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(structure);
                if (natural != null && natural.isValid()) return natural;
            }
        }
        return null;
    }

    private static void placeAndCheck(ServerLevel level, ResearchLabStructure.Piece piece, CompoundTag raw, String label, boolean natural) {
        var box = piece.getBoundingBox();
        var generator = level.getChunkSource().getGenerator();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
            var chunk = new ChunkPos(x, z);
            level.getChunk(x, z);
            if (!natural) piece.postProcess(level, level.structureManager(), generator, RandomSource.create(4),
                    new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(), chunk.getMinBlockZ(),
                            chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ()), chunk, piece.templatePosition());
        }
        int loot = 0, code = 0, occupied = 0;
        var palette = raw.getList("palette", Tag.TAG_COMPOUND);
        for (var value : raw.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) value;
            var local = pos(block.getList("pos", Tag.TAG_INT));
            var world = StructureTemplate.transform(local, Mirror.NONE, piece.getRotation(), BlockPos.ZERO).offset(piece.templatePosition());
            var expected = palette.getCompound(block.getInt("state")).getString("Name");
            if (label.equals("green_lab") && local.getX() == 31 && local.getZ() == 12
                    && local.getY() >= 1 && local.getY() <= 4) {
                var placed = level.getBlockState(world);
                check(BuiltInRegistries.BLOCK.getKey(placed.getBlock()).toString().equals(expected), "crystal specimen and original glass cap placed");
                if (local.getY() == 2 || local.getY() == 3) {
                    check(placed.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF)
                                    == (local.getY() == 2 ? net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                                    : net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), "large dark crystal halves stay connected");
                    check(placed.canSurvive(level, world), "large dark crystal has valid support after structure placement");
                }
            }
            if (expected.equals("changed:wall_white") || expected.equals("changed:wall_green") || expected.equals("changed:wall_lightred")
                    || expected.contains("door") || expected.contains("scanner") || expected.contains("keypad")
                    || expected.equals("changed:iron_crate") || expected.equals("changed:white_pillow")) {
                check(BuiltInRegistries.BLOCK.getKey(level.getBlockState(world).getBlock()).toString().equals(expected), "structural block placed: " + label);
                occupied++;
            }
            var tag = block.getCompound("nbt");
            if (!tag.getString("id").isEmpty()) {
                var be = level.getBlockEntity(world);
                check(be != null, "authored block entity exists: " + tag.getString("id") + " " + world);
                var saved = be.saveWithFullMetadata();
                check(saved.getString("id").equals(tag.getString("id")), "authored block entity type preserved: " + world);
                if (!tag.getList("Items", Tag.TAG_COMPOUND).isEmpty())
                    check(saved.getList("Items", Tag.TAG_COMPOUND).size() == tag.getList("Items", Tag.TAG_COMPOUND).size(), "authored inventory entries retained: " + world);
                if (tag.contains("Text")) check(saved.getString("Text").equals(tag.getString("Text")), "authored note text retained");
            }
            if (tag.contains("LootTable")) {
                var be = level.getBlockEntity(world);
                check(be instanceof RandomizableContainerBlockEntity, "loot container exists: " + label);
                check(be.saveWithFullMetadata().getString("LootTable").equals(tag.getString("LootTable")), "loot retained: " + label);
                if (tag.getString("LootTable").equals("changed_synergy:chests/swordfish_research")) {
                    ((RandomizableContainerBlockEntity)be).unpackLootTable(null);
                    var inventory=(net.minecraft.world.Container)be;
                    int discs=0;
                    for (int slot=0;slot<inventory.getContainerSize();slot++) {
                        var item=inventory.getItem(slot);
                        if (item.is(net.ltxprogrammer.changed.init.ChangedItems.COMPACT_DISC.get()) && item.hasTag()
                                && item.getTag().getString("Text").contains("Latex Swordfish")) {
                            check(item.getTag().getString("Text").startsWith("# TOP SECRET // Swordfish Research\n\n"), "research disc retains reviewed Markdown and real paragraphs");
                            discs++;
                        }
                    }
                    check(discs==1,"research-room loot contains exactly one readable swordfish record");
                }
                loot++;
            }
            if (tag.contains("Code")) {
                check(level.getBlockEntity(world) != null && java.util.Arrays.equals(level.getBlockEntity(world).saveWithFullMetadata().getIntArray("Code"), tag.getIntArray("Code")), "numeric keypad password retained: " + label);
                code++;
            }
        }
        for (var value : raw.getList("latexCovers", Tag.TAG_COMPOUND)) {
            var local = pos(((CompoundTag) value).getList("pos", Tag.TAG_INT));
            var world = StructureTemplate.transform(local, Mirror.NONE, piece.getRotation(), BlockPos.ZERO).offset(piece.templatePosition());
            check(!net.ltxprogrammer.changed.world.LatexCoverState.getAt(level, world).isAir(), "Changed latex covering placed");
        }
        var occupants = level.getEntities(null, AABB.of(box).inflate(2));
        var uuids = new HashSet<java.util.UUID>();
        for (var entity : occupants) {
            check(uuids.add(entity.getUUID()), "no duplicate entity UUID");
            if (entity instanceof HangingEntity hanging) check(box.isInside(hanging.getPos()), "hanging entity local position transformed");
        }
        if (label.equals("red_lab_tower") || label.startsWith("red rotation ")) {
            var paintings = occupants.stream().filter(entity -> entity instanceof net.minecraft.world.entity.decoration.Painting)
                    .map(entity -> (net.minecraft.world.entity.decoration.Painting)entity)
                    .filter(painting -> painting.saveWithoutId(new CompoundTag()).getString("variant").equals("changed:kade_tail")).toList();
            check(paintings.size() == 1, "relocated painting spawns once: " + label);
            var painting = paintings.get(0);
            var expected = StructureTemplate.transform(new BlockPos(21, 20, 23), Mirror.NONE, piece.getRotation(), BlockPos.ZERO).offset(piece.templatePosition());
            check(painting.getPos().equals(expected), "painting anchor rotates with the structure: " + label);
            check(painting.getDirection() == piece.getRotation().rotate(net.minecraft.core.Direction.SOUTH), "painting facing rotates with its backing wall: " + label);
            check(painting.survives(), "painting has solid backing and no air-conditioner collision: " + label);
        }
        check(occupied > 500 && loot > 0, "building and loot present: " + label);
        System.out.println("RESEARCH_PLACED " + label + " blocks=" + occupied + " loot=" + loot + " keypads=" + code + " occupants=" + occupants.size());
    }

    private static BlockPos pos(net.minecraft.nbt.ListTag tag) { return new BlockPos(tag.getInt(0), tag.getInt(1), tag.getInt(2)); }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("changed_synergy", path); }
    private static void check(boolean condition, String message) { checks++; if (!condition) throw new AssertionError(message); }
}
