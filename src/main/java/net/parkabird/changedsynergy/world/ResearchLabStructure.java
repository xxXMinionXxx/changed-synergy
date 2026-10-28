package net.parkabird.changedsynergy.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.Optional;
import net.ltxprogrammer.changed.world.features.structures.HangingBlockFixerProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.parkabird.changedsynergy.ChangedSynergyMod;

/** Surface laboratories and a sea-level laboratory, using Changed's template cover support. */
public final class ResearchLabStructure extends Structure {
    public static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, ChangedSynergyMod.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, ChangedSynergyMod.MOD_ID);
    public static final RegistryObject<StructureType<ResearchLabStructure>> TYPE =
            TYPES.register("research_lab", () -> () -> ResearchLabStructure.CODEC);
    public static final RegistryObject<StructurePieceType> PIECE = PIECES.register("research_lab",
            () -> (StructurePieceType.StructureTemplateType) Piece::new);
    public static final Codec<ResearchLabStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            settingsCodec(instance),
            ResourceLocation.CODEC.fieldOf("template").forGetter(s -> s.template),
            Codec.STRING.optionalFieldOf("required_mod", "").forGetter(s -> s.requiredMod),
            Codec.intRange(-1, 64).optionalFieldOf("waterline", -1).forGetter(s -> s.waterline)
    ).apply(instance, ResearchLabStructure::new));

    private final ResourceLocation template;
    private final String requiredMod;
    private final int waterline;

    public ResearchLabStructure(StructureSettings settings, ResourceLocation template, String requiredMod, int waterline) {
        super(settings);
        this.template = template;
        this.requiredMod = requiredMod;
        this.waterline = waterline;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        // Check before reading the palette: missing optional blocks must never become air.
        if (!requiredMod.isEmpty() && !ModList.get().isLoaded(requiredMod)) return Optional.empty();
        var loaded = context.structureTemplateManager().get(template);
        if (loaded.isEmpty()) return Optional.empty();
        var size = loaded.get().getSize();
        var rotation = Rotation.getRandom(context.random());
        var anchor = context.chunkPos().getMiddleBlockPosition(0);
        var center = StructureTemplate.transform(new BlockPos(size.getX() / 2, 0, size.getZ() / 2),
                Mirror.NONE, rotation, BlockPos.ZERO);
        var origin = anchor.subtract(center);
        int[] ground = new int[9];
        int index = 0;
        for (int x : new int[]{0, size.getX() / 2, size.getX() - 1}) {
            for (int z : new int[]{0, size.getZ() / 2, size.getZ() - 1}) {
                var sample = StructureTemplate.transform(new BlockPos(x, 0, z), Mirror.NONE, rotation, BlockPos.ZERO).offset(origin);
                int floor = context.chunkGenerator().getBaseHeight(sample.getX(), sample.getZ(),
                        Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
                int surface = context.chunkGenerator().getBaseHeight(sample.getX(), sample.getZ(),
                        Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (waterline < 0 ? surface != floor : surface - floor < waterline - 2) return Optional.empty();
                ground[index++] = floor;
            }
        }
        Arrays.sort(ground);
        int y;
        if (waterline >= 0) {
            y = context.chunkGenerator().getSeaLevel() - 1 - waterline;
            if (ground[8] > y + 3 || ground[0] < y - 24) return Optional.empty();
        } else {
            if (ground[8] - ground[0] > 6) return Optional.empty();
            y = ground[4] - 1;
        }
        if (y <= context.heightAccessor().getMinBuildHeight() + 1
                || y + size.getY() >= context.heightAccessor().getMaxBuildHeight()) return Optional.empty();
        var position = new BlockPos(origin.getX(), y, origin.getZ());
        return Optional.of(new GenerationStub(anchor.atY(y), builder -> builder.addPiece(
                new Piece(context.structureTemplateManager(), template, position, rotation, waterline >= 0))));
    }

    @Override
    public StructureType<?> type() {
        return TYPE.get();
    }

    public static final class Piece extends TemplateStructurePiece {
        private final boolean ocean;

        public Piece(StructureTemplateManager manager, ResourceLocation template, BlockPos position, Rotation rotation, boolean ocean) {
            super(PIECE.get(), 0, manager, template, template.toString(), settings(rotation), position);
            this.ocean = ocean;
        }

        public Piece(StructureTemplateManager manager, CompoundTag tag) {
            super(PIECE.get(), tag, manager, location -> settings(Rotation.valueOf(tag.getString("Rot"))));
            this.ocean = tag.getBoolean("Ocean");
        }

        private static StructurePlaceSettings settings(Rotation rotation) {
            return new StructurePlaceSettings().setRotation(rotation).setMirror(Mirror.NONE)
                    .setKeepLiquids(false).setKnownShape(true).setIgnoreEntities(false)
                    .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                    .addProcessor(HangingBlockFixerProcessor.INSTANCE);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putString("Rot", placeSettings.getRotation().name());
            tag.putBoolean("Ocean", ocean);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
                                RandomSource random, BoundingBox clip, ChunkPos chunk, BlockPos pivot) {
            // Neighbor shapes must wait until adjoining chunks finish their multipart furniture.
            placeSettings.setBoundingBox(clip);
            template.placeInWorld(level, templatePosition, pivot, placeSettings, random,
                    Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            // Fill only beneath actual load-bearing base blocks, inside the current chunk.
            int base = templatePosition.getY() + (ocean ? 1 : 0);
            for (int x = Math.max(clip.minX(), boundingBox.minX()); x <= Math.min(clip.maxX(), boundingBox.maxX()); x++) {
                for (int z = Math.max(clip.minZ(), boundingBox.minZ()); z <= Math.min(clip.maxZ(), boundingBox.maxZ()); z++) {
                    var top = new BlockPos(x, base, z);
                    var state = level.getBlockState(top);
                    if (!state.isFaceSturdy(level, top, Direction.DOWN) || !state.getFluidState().isEmpty()) continue;
                    for (int depth = 1; depth <= (ocean ? 26 : 8); depth++) {
                        var below = top.below(depth);
                        if (below.getY() <= level.getMinBuildHeight() || !clip.isInside(below)) break;
                        var old = level.getBlockState(below);
                        if (!old.isAir() && old.getFluidState().isEmpty()) break;
                        level.setBlock(below, state, 2);
                    }
                }
            }
        }

        @Override
        protected void handleDataMarker(String marker, BlockPos position, ServerLevelAccessor level,
                                        RandomSource random, BoundingBox box) {
        }
    }
}
