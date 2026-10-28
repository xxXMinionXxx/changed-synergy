package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.Map;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;

/** Calls the installed Additions muzzle ray, tracker and server fire-cancellation handler. */
final class TaczSuitShootingRegression {
    private static int checks;
    @SuppressWarnings("unchecked")
    static void run(MinecraftServer server) throws Exception {
        if(!ModList.get().isLoaded("taczadditions"))return;
        var level=server.overworld();
        var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"SuitShooter"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        player.setGameMode(GameType.SURVIVAL);player.setPos(50.5,181,50.9);player.setYRot(0);player.setXRot(0);
        level.getChunkAt(player.blockPosition());
        var players=(Map<UUID,ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_")
                .get(server.getPlayerList());
        players.put(player.getUUID(),player);level.addNewPlayer(player);
        var pet=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);
        pet.setPos(player.position());pet.setNoAi(true);level.addFreshEntity(pet);
        var other=ChangedEntities.WHITE_LATEX_WOLF_MALE.get().create(level);other.setNoAi(true);other.setPos(54,181,54);level.addFreshEntity(other);
        BlockPos wall=new BlockPos(50,182,51);
        Class<?> builderType=Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder");
        Object builder=builderType.getMethod("create").invoke(null);
        builderType.getMethod("setId",ResourceLocation.class).invoke(builder,ResourceLocation.fromNamespaceAndPath("tacz","ak47"));
        builderType.getMethod("setAmmoCount",int.class).invoke(builder,30);
        ItemStack gun=(ItemStack)builderType.getMethod("forceBuild").invoke(builder);
        check(!gun.isEmpty(),"The installed TACZ builder supplies a real gun");player.setItemSlot(EquipmentSlot.MAINHAND,gun);
        try {
            for(int x=49;x<=52;x++)for(int z=49;z<=53;z++)for(int y=181;y<=184;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
            check(target(player)>0.9F,"An independent overlapping creature is a genuine muzzle obstruction");
            check(blocked(player,gun),"Additions normally blocks server fire at an obstructing entity");
            LatexSocialMemory.addBond(pet,player);
            check(BondedSuitService.suitOwner(pet,player,BondedSuitService.SuitReason.MANUAL),"The public active bonded-wrap route succeeds");
            check(target(player)==0,"The player's own controlled wrapper is ignored on the dedicated server");
            check(!blocked(player,gun),"Additions allows server fire during active bonded wrapping");
            level.setBlock(wall,Blocks.STONE.defaultBlockState(),3);
            check(target(player)>0.5F&&blocked(player,gun),"A real wall still blocks fire while wrapped");
            level.setBlock(wall,Blocks.AIR.defaultBlockState(),3);other.setPos(player.getX(),player.getY(),player.getZ()+.3);
            check(target(player)>0.5F&&blocked(player,gun),"Other creatures remain real muzzle obstructions");
            other.setPos(54,181,54);
            check(!blocked(player,gun),"Removing the real obstacle restores shooting without disabling gun tuck");
            player.setOnGround(true);
            check(BondedSuitService.releaseOwner(pet,player,BondedSuitService.ReleaseReason.MANUAL),"A grounded player can release the active wrap");
            pet.setPos(player.position());
            check(target(player)>0.9F&&blocked(player,gun),"A released creature immediately becomes a normal obstacle again");
            System.out.println("TACZ_SUIT_SHOOTING_REGRESSION_PASS: "+checks+" installed TACZ/Additions ray, server tracker, active bonded wrapping, fire cancellation, wall/entity and release checks");
        } finally {
            level.setBlock(wall,Blocks.AIR.defaultBlockState(),3);
            var grab=BondedSuitService.ability(pet);if(grab!=null)grab.releaseEntity(false);
            pet.discard();other.discard();ProcessTransfur.removePlayerTransfurVariant(player);players.remove(player.getUUID());player.discard();
        }
    }
    private static float target(ServerPlayer player)throws Exception {
        return (float)Class.forName("com.raiiiden.taczadditions.util.GunTuckMath").getMethod("calculateTarget",LivingEntity.class,float.class,float.class)
                .invoke(null,player,1F,1F);
    }
    private static boolean blocked(ServerPlayer player,ItemStack gun)throws Exception {
        Class<?> tracker=Class.forName("com.raiiiden.taczadditions.server.ServerGunTuckTracker");
        for(int i=0;i<24;i++)tracker.getMethod("onLivingTick",LivingEvent.LivingTickEvent.class).invoke(null,new LivingEvent.LivingTickEvent(player));
        Class<?> eventType=Class.forName("com.tacz.guns.api.event.common.GunShootEvent");
        Event shot=(Event)eventType.getConstructor(LivingEntity.class,ItemStack.class,LogicalSide.class).newInstance(player,gun,LogicalSide.SERVER);
        Class.forName("com.raiiiden.taczadditions.server.GunTuckFireHandler").getMethod("onGunShoot",eventType).invoke(null,shot);
        return shot.isCanceled();
    }
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
