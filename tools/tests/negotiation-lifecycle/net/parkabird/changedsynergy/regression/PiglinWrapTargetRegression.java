package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.parkabird.changedsynergy.ai.BondedSuitService;
import net.parkabird.changedsynergy.ai.LatexSocialMemory;

final class PiglinWrapTargetRegression {
    @SuppressWarnings("unchecked")
    static void run(MinecraftServer server)throws Exception {
        var level=server.overworld();var player=new ServerPlayer(server,level,new GameProfile(UUID.randomUUID(),"PiglinWrap"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class,player,0,"f_8921_");
        var players=(java.util.Map<UUID,ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(server.getPlayerList());
        player.setPos(1100.5,160,1100.5);level.getChunkAt(player.blockPosition());players.put(player.getUUID(),player);level.addNewPlayer(player);
        var carrier=ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(level);carrier.setPos(player.position());carrier.tame(player);carrier.setNoAi(true);level.addFreshEntity(carrier);
        var piglin=EntityType.PIGLIN.create(level);piglin.setPos(1102.5,160,1100.5);piglin.setImmuneToZombification(true);level.addFreshEntity(piglin);
        var nearest=ObfuscationReflectionHelper.findMethod(PiglinAi.class,"m_35000_",Piglin.class);
        var valid=ObfuscationReflectionHelper.findMethod(PiglinAi.class,"m_34900_",Piglin.class,net.minecraft.world.entity.LivingEntity.class);
        try {
            piglin.getBrain().setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,new NearestVisibleLivingEntities(piglin,List.of(player,carrier)));
            piglin.getBrain().setMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD,player);
            piglin.getBrain().setMemory(MemoryModuleType.ANGRY_AT,player.getUUID());
            piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,player);
            check(((Optional<?>)nearest.invoke(null,piglin)).orElse(null)==player,"Unwrapped player targeting is unchanged");
            LatexSocialMemory.addBond(carrier,player);
            check(BondedSuitService.suitOwner(carrier,player,BondedSuitService.SuitReason.MANUAL),"Real friendly wrapping starts");
            check(BondedSuitService.getWrappingPet(player)==carrier,"Wrapped player resolves to the authoritative carrier");
            check(((Optional<?>)nearest.invoke(null,piglin)).orElse(null)==carrier,"Piglin selects the same visible body as Forge target redirection");
            piglin.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,carrier);
            for(int tick=0;tick<40;tick++) {
                check((boolean)valid.invoke(null,piglin,carrier),"Native fight validity keeps the carrier target: "+tick);
            }
            LatexSocialMemory.endFriendlySuit(carrier,player);BondedSuitService.ability(carrier).releaseEntity(false);
            check(((Optional<?>)nearest.invoke(null,piglin)).orElse(null)!=carrier,"Release removes carrier targeting");
            piglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            piglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            piglin.getBrain().eraseMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
            player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(Items.GOLDEN_CHESTPLATE));
            check(((Optional<?>)nearest.invoke(null,piglin)).isEmpty(),"Peaceful gold-wearing and barter state does not acquire a target");
            System.out.println("PIGLIN_WRAP_TARGET_REGRESSION_PASS: actual friendly suit, 40 stable native fight validations, release and unchanged peaceful gold state");
        } finally {
            LatexSocialMemory.endFriendlySuit(carrier,player);var ability=BondedSuitService.ability(carrier);if(ability!=null)ability.releaseEntity(false);
            carrier.discard();piglin.discard();ProcessTransfur.removePlayerTransfurVariant(player);players.remove(player.getUUID());player.discard();
        }
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
