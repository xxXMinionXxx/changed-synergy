package net.parkabird.changedsynergy.regression;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ai.PlayerOutpostData;
import net.parkabird.changedsynergy.ai.PlayerOutpostGoal;

final class OutpostStorageRegression {
    private static int checks;
    static void run(MinecraftServer server) throws Exception {
        var level=server.overworld();var pos=new BlockPos(1024,160,1024);level.getChunkAt(pos);
        try {
            var huge=new net.minecraft.world.SimpleContainer() {
                @Override public int getContainerSize(){return 2;}
                @Override public ItemStack getItem(int slot){return new ItemStack(Items.WHEAT,Integer.MAX_VALUE);}
            };
            for(var pair:java.util.List.of(
                    new Object[]{net.parkabird.changedsynergy.ai.PlayerOutpostMealGoal.class,"storedCount"},
                    new Object[]{net.parkabird.changedsynergy.ai.PlayerOutpostWorkGoal.class,"count"})) {
                var count=((Class<?>)pair[0]).getDeclaredMethod((String)pair[1],net.minecraft.world.Container.class,ItemStack.class);count.setAccessible(true);
                check((int)count.invoke(null,huge,new ItemStack(Items.WHEAT))==Integer.MAX_VALUE,"Very large stock totals cannot overflow into negative reserves");
            }
            level.setBlock(pos,Blocks.CHEST.defaultBlockState(),3);
            var vanilla=PlayerOutpostData.container(level,pos);
            check(vanilla!=null&&PlayerOutpostGoal.insert(vanilla,new ItemStack(Items.WHEAT,65)).isEmpty(),"Vanilla chest delivery");
            check(vanilla.getItem(0).getCount()==64&&vanilla.getItem(1).getCount()==1,"Vanilla stack limits unchanged");
            level.removeBlock(pos,false);
            if(!ModList.get().isLoaded("sophisticatedstorage")) {
                System.out.println("OUTPOST_STORAGE_OPTIONAL_ABSENT_PASS: vanilla containers still work without Sophisticated Storage");return;
            }
            for(String type:new String[]{"diamond_chest","diamond_barrel"}) {
                var block=ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("sophisticatedstorage:"+type));
                check(block!=null&&block!=Blocks.AIR,"Actual installed storage block exists: "+type);
                level.setBlock(pos,block.defaultBlockState(),3);
                var entity=level.getBlockEntity(pos);
                var wrapper=entity.getClass().getMethod("getStorageWrapper").invoke(entity);
                var upgrades=(IItemHandlerModifiable)wrapper.getClass().getMethod("getUpgradeHandler").invoke(wrapper);
                var upgrade=ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("sophisticatedstorage:stack_upgrade_tier_1"));
                check(upgrade!=null&&upgrade!=Items.AIR,"Real stack upgrade exists");
                upgrades.setStackInSlot(0,new ItemStack(upgrade));
                var storage=PlayerOutpostData.container(level,pos);
                var handler=entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new AssertionError("No automation handler"));
                check(storage!=null&&storage.getContainerSize()>27,"Upgraded storage recognised with every inventory slot");
                var source=new ItemStack(Items.WHEAT,128);
                check(PlayerOutpostGoal.insert(storage,source).isEmpty()&&source.getCount()==128,"Deposit preserves source and returns the exact remainder");
                check(handler.getStackInSlot(0).getCount()==128,"Stack upgrade fills beyond the vanilla 64 limit");
                var snapshot=storage.getItem(0);snapshot.shrink(100);
                check(handler.getStackInSlot(0).getCount()==128,"Planning reads cannot mutate capability contents");
                check(storage.removeItem(0,1).getCount()==1&&handler.getStackInSlot(0).getCount()==127,"Meal and seed extraction moves a real item");
                entity.getClass().getMethod("toggleLock").invoke(entity);
                storage=PlayerOutpostData.container(level,pos);
                check(PlayerOutpostGoal.insert(storage,new ItemStack(Items.GOLD_INGOT,16)).getCount()==16,"Locked storage keeps its contents filter");
                entity.getClass().getMethod("toggleLock").invoke(entity);
                storage=PlayerOutpostData.container(level,pos);
                handler=entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new AssertionError("No refreshed automation handler"));
                int moved=0;
                for(int i=0;i<storage.getContainerSize()+2;i++) {
                    int count=handler.getSlotLimit(0);
                    var batch=new ItemStack(Items.WHEAT,count);var left=PlayerOutpostGoal.insert(storage,batch);moved+=count-left.getCount();
                }
                int stored=0;for(int i=0;i<handler.getSlots();i++)stored+=handler.getStackInSlot(i).getCount();
                check(stored==127+moved,"Full storage retains exact item conservation");
                check(!PlayerOutpostGoal.insert(storage,new ItemStack(Items.WHEAT,64)).isEmpty(),"Full-capacity deposit leaves cargo on the worker");
                var saved=entity.saveWithFullMetadata();
                level.removeBlock(pos,false);level.setBlock(pos,block.defaultBlockState(),3);
                level.getBlockEntity(pos).load(saved);
                var reopened=PlayerOutpostData.container(level,pos);int reloaded=0;
                for(int i=0;i<reopened.getContainerSize();i++)reloaded+=reopened.getItem(i).getCount();
                check(reloaded==stored,"Contents and upgraded capacity survive storage reload");
                level.removeBlock(pos,false);
            }
            System.out.println("OUTPOST_STORAGE_REGRESSION_PASS: "+checks+" actual Sophisticated Storage chest/barrel, stack upgrade, capacity, extraction, conservation and reload checks");
        } finally {level.removeBlock(pos,false);}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
}
