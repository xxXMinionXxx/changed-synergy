package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.CreatureCacheGuardService;
import net.parkabird.changedsynergy.ai.CreatureCommunityData;
import net.parkabird.changedsynergy.ai.CreatureLifeMemory;
import net.parkabird.changedsynergy.ai.CreatureSettlementService;
import net.parkabird.changedsynergy.ai.FactionReputation;
import net.parkabird.changedsynergy.ai.PlayerOutpostData;
import net.parkabird.changedsynergy.ai.PlayerOutpostGoal;
import net.parkabird.changedsynergy.init.ChangedSynergyGameRules;

@Mod.EventBusSubscriber(modid = "changed_synergy")
public final class StorageCompatRegression {
    private static int checks;
    @SubscribeEvent public static void run(ServerStartedEvent event) {
        try {
            ChangedSynergyConfig.SPEC.setConfig(com.electronwill.nightconfig.core.CommentedConfig.inMemory());
            var server = event.getServer(); var level = server.overworld();
            level.getGameRules().getRule(ChangedSynergyGameRules.NATIVE_ORIGINS).set(false, server);
            level.getGameRules().getRule(ChangedSynergyGameRules.CREATURE_LIFE).set(true, server);
            level.getGameRules().getRule(ChangedSynergyGameRules.FACTION_REPUTATION).set(true, server);
            server.getWorldData().overworldData().setGameTime(100);
            check(ModList.get().isLoaded("sophisticatedstorage") == Boolean.getBoolean("changed_synergy.expectStorage"), "optional storage mode matches the requested test");
            capabilityChest(level);
            doubleCache(level);
            OutpostStorageRegression.run(server);
            System.out.println("STORAGE_COMPAT_REGRESSION_PASS: " + checks + " focused chest-type, filter, extraction, conservation and double-cache ownership/guard checks; storage=" + ModList.get().isLoaded("sophisticatedstorage"));
        } catch (Throwable failure) {
            System.err.println("STORAGE_COMPAT_REGRESSION_FAIL"); failure.printStackTrace();
        } finally { event.getServer().halt(false); }
    }

    private static void capabilityChest(ServerLevel level) {
        var pos = new BlockPos(1040, 160, 1040); level.getChunkAt(pos);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 18);
        var entity = new CapabilityChest(pos, level.getBlockState(pos)); level.setBlockEntity(entity);
        try {
            check(level.getBlockEntity(pos) == entity, "chest-shaped modded block entity installed");
            var storage = PlayerOutpostData.container(level, pos);
            check(storage != null, "chest subclass with a non-vanilla block entity falls through to its capability");
            var source = new ItemStack(Items.WHEAT, 128);
            check(PlayerOutpostGoal.insert(storage, source).isEmpty() && source.getCount() == 128, "capability insertion preserves source and accepts upgraded stack capacity");
            check(entity.items.getStackInSlot(0).getCount() == 128, "all deposited items are stored");
            entity.items.locked = true;
            check(!storage.canPlaceItem(1, source) && PlayerOutpostGoal.insert(storage, source).getCount() == 128, "automation refusal is checked by simulation and preserves the full remainder");
            entity.items.locked = false; entity.items.blockExtraction = true;
            check(storage.getItem(0).isEmpty() && storage.removeItem(0, 1).isEmpty(), "work planners do not reserve stock blocked by extraction rules");
            entity.items.blockExtraction = false;
            var copy = storage.getItem(0); copy.shrink(127);
            check(entity.items.getStackInSlot(0).getCount() == 128, "planning reads never mutate handler stock");
            boolean rejected = false;
            try { storage.setItem(0, new ItemStack(Items.GOLD_INGOT, 64)); } catch (UnsupportedOperationException expected) { rejected = true; }
            check(rejected && entity.items.getStackInSlot(0).getCount() == 128, "raw overwrite cannot silently discard rejected items");
            check(storage.removeItem(0, 1).getCount() == 1 && entity.items.getStackInSlot(0).getCount() == 127, "worker extraction conserves items");
            level.removeBlock(pos, false);
            var viewer = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "StorageValidity"));
            viewer.setPos(pos.getX(), pos.getY(), pos.getZ());
            check(!storage.stillValid(viewer), "removed block entities do not remain valid storage");
        } finally { level.removeBlock(pos, false); }
    }

    private static void doubleCache(ServerLevel level) {
        var left = new BlockPos(8, 160, 8); level.getChunkAt(left);
        var leftState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT);
        var right = left.relative(ChestBlock.getConnectedDirection(leftState));
        var rightState = leftState.setValue(ChestBlock.TYPE, ChestType.RIGHT);
        level.setBlock(left, leftState, 18); level.setBlock(right, rightState, 18);
        var guard = ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        guard.moveTo(left.getX() + .5, left.getY(), left.getZ() + 2.5); level.addFreshEntity(guard);
        CreatureLifeMemory.setRole(guard, CreatureLifeMemory.GroupRole.GUARD);
        CreatureCommunityData.markCache(guard, left);
        var visitor = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "DoubleCache"));
        visitor.connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), visitor);
        visitor.setGameMode(GameType.SURVIVAL); visitor.moveTo(left.getX() + .5, left.getY(), left.getZ() + 1.5);
        try {
            var primary = CreatureCommunityData.snapshotAtCache(level, left).orElseThrow();
            check(CreatureCommunityData.snapshotAtCache(level, right).orElseThrow().id().equals(primary.id()), "both physical halves resolve to the same wild cache owner");
            check(PlayerOutpostData.container(level, left).getContainerSize() == 54, "vanilla double chest keeps all 54 slots");
            FactionReputation.set(guard, visitor, 0);
            CreatureCacheGuardService.handleCacheAccess(level, visitor, right);
            check(FactionReputation.score(guard, visitor) == -2 && CreatureCacheGuardService.isDefendingAgainst(guard, visitor),
                    "opening the other half preserves witnessed warnings and guard reaction; score=" + FactionReputation.score(guard, visitor)
                            + " patrol=" + CreatureCacheGuardService.patrolCache(guard) + " visible=" + level.getEntitiesOfClass(net.ltxprogrammer.changed.entity.ChangedEntity.class, guard.getBoundingBox().inflate(1)).contains(guard));
            CreatureCacheGuardService.handleCacheAccess(level, visitor, left);
            check(FactionReputation.score(guard, visitor) == -2, "both halves share one reputation cooldown");
            CreatureCacheGuardService.handleCacheDestroyed(level, visitor, right);
            check(FactionReputation.score(guard, visitor) == -12, "breaking either half retains the destruction consequence");
            level.setBlock(right, Blocks.BARREL.defaultBlockState(), 18);
            check(CreatureCommunityData.snapshotAtCache(level, right).isEmpty(), "adjacent unrelated storage is not claimed");
            level.setBlock(right, rightState.setValue(ChestBlock.FACING, Direction.SOUTH), 18);
            check(CreatureCommunityData.snapshotAtCache(level, right).isEmpty(), "mismatched chest facing cannot impersonate the other half");
            level.setBlock(right, rightState.setValue(ChestBlock.TYPE, ChestType.SINGLE), 18);
            check(CreatureCommunityData.snapshotAtCache(level, right).isEmpty(), "an independent adjacent chest remains player-claimable");
        } finally {
            CreatureCacheGuardService.forget(guard); guard.discard(); visitor.discard();
            level.removeBlock(left, false); level.removeBlock(right, false);
        }
    }

    private static final class TestHandler extends ItemStackHandler {
        boolean locked, blockExtraction;
        TestHandler() { super(2); }
        @Override public int getSlotLimit(int slot) { return 1024; }
        @Override protected int getStackLimit(int slot, ItemStack stack) { return getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return stack.is(Items.WHEAT); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return locked ? stack : super.insertItem(slot, stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return blockExtraction ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate); }
    }

    private static final BlockEntityType<CapabilityChest> CAP_TYPE = BlockEntityType.Builder.of(CapabilityChest::new, Blocks.CHEST).build(null);
    private static final class CapabilityChest extends BlockEntity {
        final TestHandler items = new TestHandler();
        final LazyOptional<IItemHandler> capability = LazyOptional.of(() -> items);
        CapabilityChest(BlockPos pos, BlockState state) { super(CAP_TYPE, pos, state); }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            return cap == ForgeCapabilities.ITEM_HANDLER ? capability.cast() : super.getCapability(cap, side);
        }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); checks++; }
}
