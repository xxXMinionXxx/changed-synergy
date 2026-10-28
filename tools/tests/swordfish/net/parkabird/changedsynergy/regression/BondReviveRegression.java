package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;

final class BondReviveRegression {
    @SuppressWarnings("unchecked")
    static void run(MinecraftServer server) throws Exception {
        var owner=player(server,"RecallOwner");var stranger=player(server,"RecallOther");
        var online=(java.util.List<ServerPlayer>)net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.server.players.PlayerList.class,server.getPlayerList(),"f_11196_");
        var players=(java.util.Map<UUID,ServerPlayer>)net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.server.players.PlayerList.class,server.getPlayerList(),"f_11197_");
        online.add(owner);players.put(owner.getUUID(),owner);
        try {
            for(var type:java.util.List.of(ChangedEntities.DARK_LATEX_WOLF_MALE.get(),ChangedSynergyEntities.LATEX_SWORDFISH.get(),ChangedEntities.GAS_WOLF_MALE.get())) {
                var creature=(ChangedEntity)type.create(server.overworld());creature.moveTo(80,84,80);server.overworld().addFreshEntity(creature);
                creature.setCustomName(Component.literal("Recalled friend"));
                LatexSocialMemory.addBond(creature,owner);CreaturePersonality.reconcileNativePetRelationship(creature);
                CreaturePersonality.setFamiliarity(creature,owner,48);
                creature.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
                UUID old=creature.getUUID();var data=BondedRevivalData.get(server);
                check(LatexSocialMemory.isBonded(creature,owner),"fixture bonded");
                creature.setHealth(0);boolean dropped=BondedRevivalService.dropBrokenMask(creature);
                check(dropped!=LatexSocialMemory.isOrganic(creature),"organic records do not change ordinary death drops");
                check(data.availableTo(owner.getUUID()).size()==1,"dead companion is listed");
                check(server.getCommands().performPrefixedCommand(owner.createCommandSourceStack().withPermission(2),"changedsynergy bond revive list")==1,"actual list command leaves the record available");
                var token=data.availableTo(owner.getUUID()).keySet().iterator().next();
                check(BondedRevivalService.reviveImmediately(stranger,old)==null,"another player's death record is inaccessible");
                var state=data.record(token).getString("State");
                data.setState(token,"wrapped");check(BondedRevivalService.reviveImmediately(owner,old)==null,"ongoing ritual cannot be duplicated");data.setState(token,state);
                LatexSocialMemory.breakBondsOnDeath(creature);creature.discard();
                var source=owner.createCommandSourceStack().withPermission(2);
                String command="changedsynergy bond revive "+old;
                if(type==ChangedSynergyEntities.LATEX_SWORDFISH.get()) command="changedsynergy bond revive";
                if(LatexSocialMemory.isOrganic(creature)) {source=server.createCommandSourceStack().withPermission(2);command+=" RecallOwner";}
                check(server.getCommands().performPrefixedCommand(source,command)==1,"actual command restores "+EntityType.getKey(type));
                UUID replacement=data.record(token).getUUID("NewEntity");
                var revived=(ChangedEntity)server.overworld().getEntity(replacement);
                check(revived!=null&&revived.getType()==type&&revived.getHealth()==revived.getMaxHealth(),"species and full health restored");
                check(revived.getName().getString().equals("Recalled friend")&&revived.position().equals(owner.position()),"name and owner position restored");
                check(LatexSocialMemory.isBonded(revived,owner)&&LatexSocialMemory.bondedCreatureUuids(owner).contains(replacement),"both bond references restored");
                check(CreaturePersonality.familiarity(revived,owner)==48,"command preserves familiarity without ritual memory damage or bonus");
                check(revived.getMainHandItem().isEmpty(),"equipment already dropped on death is not duplicated");
                check(!BondedRevivalService.isProvisional(revived)&&!revived.isInvulnerable(),"no provisional wrap or invulnerability remains");
                check(BondedRevivalService.reviveImmediately(owner,old)==null&&data.availableTo(owner.getUUID()).isEmpty(),"spent masks/samples cannot recall the identity twice");
                var load=BondedRevivalData.class.getDeclaredMethod("load",CompoundTag.class);load.setAccessible(true);
                var loaded=(BondedRevivalData)load.invoke(null,data.save(new CompoundTag()));
                check(loaded.availableTo(owner.getUUID()).isEmpty()&&loaded.record(token).getString("State").equals("complete"),"consumed state survives save/reload");
                revived.discard();
            }
            var node=server.getCommands().getDispatcher().getRoot().getChild("changedsynergy").getChild("bond").getChild("revive");
            check(!node.canUse(owner.createCommandSourceStack().withPermission(0)),"revival shortcut requires operator permission");
            System.out.println("BOND_REVIVE_REGRESSION_PASS: dark, aquatic and organic death records; command, ownership, permissions, memories, inventory and save/reload");
        } finally {online.remove(owner);players.remove(owner.getUUID());owner.discard();stranger.discard();}
    }
    private static ServerPlayer player(MinecraftServer server,String name) {
        var player=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),name));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        player.moveTo(80,84,80);return player;
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
