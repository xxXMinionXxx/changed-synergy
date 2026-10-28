package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.ltxprogrammer.changed.ability.IAbstractChangedEntity;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ai.*;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;
import net.parkabird.changedsynergy.world.inventory.BondedInventoryService;

final class CompanionFoodRegression {
    private static int checks;
    @SuppressWarnings("unchecked") static void run(MinecraftServer server)throws Exception {
        var owner=new ServerPlayer(server,server.overworld(),new GameProfile(UUID.randomUUID(),"MealOwner"));
        owner.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),owner);
        owner.moveTo(96.5,180,96.5);
        var players=(java.util.Map<UUID,ServerPlayer>)net.minecraftforge.fml.util.ObfuscationReflectionHelper.getPrivateValue(
                net.minecraft.server.players.PlayerList.class,server.getPlayerList(),"f_11197_");
        players.put(owner.getUUID(),owner);server.overworld().addNewPlayer(owner);
        boolean rescue=ChangedSynergyConfig.COMMON.bondedEmergencyRescue.get();
        try {
            ChangedSynergyConfig.COMMON.bondedEmergencyRescue.set(false);
            for(boolean bonded:new boolean[]{true,false}) for(var food:java.util.List.of(Items.APPLE,Items.BREAD,Items.COOKED_BEEF,Items.COOKIE,Items.MUSHROOM_STEW,Items.GOLDEN_APPLE,Items.HONEY_BOTTLE,net.ltxprogrammer.changed.init.ChangedItems.ORANGE.get())) {
                var mob=create(server,owner,bonded);
                try {
                    var inv=BondedInventoryService.inventory(mob,owner);inv.setItem(2,new ItemStack(food,2));
                    mob.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.SHIELD));
                    mob.setHealth(2);float health=mob.getHealth();var props=new ItemStack(food).getFoodProperties(mob);
                    var goal=new CompanionAutoEatGoal(mob);check(goal.canUse(),"injured "+(bonded?"bond":"friend")+" chooses "+food);
                    goal.start();check(mob.isUsingItem()&&count(mob,food)==1,"one real food transferred out of storage");
                    int ticks=0;while(goal.canContinueToUse()&&ticks++<100){goal.tick();mob.tick();}
                    goal.stop();
                    check(ticks>=new ItemStack(food).getUseDuration()-2,"native eating duration honoured: "+food);
                    check(Math.abs(mob.getHealth()-Math.min(mob.getMaxHealth(),health+props.getNutrition()*(.5F+props.getSaturationModifier())))<.001F,"nutrition and saturation determine healing: "+food+" health="+mob.getHealth());
                    check(count(mob,food)==1&&mob.getOffhandItem().is(Items.SHIELD),"one item consumed and shield restored");
                    if(food==Items.MUSHROOM_STEW)check(count(mob,Items.BOWL)==1,"native stew bowl returned");
                    if(food==Items.HONEY_BOTTLE)check(count(mob,Items.GLASS_BOTTLE)==1,"native honey bottle returned");
                    if(food==Items.GOLDEN_APPLE)check(mob.hasEffect(MobEffects.REGENERATION)&&mob.hasEffect(MobEffects.ABSORPTION),"golden apple effects applied by native food code");
                    check(!new CompanionAutoEatGoal(mob).canUse(),"post-meal cooldown prevents immediate repeat");
                } finally {mob.discard();}
            }
            var mob=create(server,owner,false);
            try {
                var inv=BondedInventoryService.inventory(mob,owner);inv.setItem(0,new ItemStack(Items.ROTTEN_FLESH));mob.setHealth(2);
                check(!new CompanionAutoEatGoal(mob).canUse(),"harmful food skipped");
                inv.setItem(0,new ItemStack(Items.BREAD));mob.setHealth(mob.getMaxHealth());
                check(!new CompanionAutoEatGoal(mob).canUse(),"healthy friend does not waste food");
                mob.setHealth(mob.getMaxHealth()*.5F);mob.setTarget(net.minecraft.world.entity.EntityType.ZOMBIE.create(mob.level()));
                check(!new CompanionAutoEatGoal(mob).canUse(),"combat uses the lower critical-health threshold");mob.setTarget(null);
                mob.setHealth(2);mob.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));
                var goal=new CompanionAutoEatGoal(mob);check(goal.canUse(),"interruption fixture ready");goal.start();goal.stop();
                check(count(mob,Items.BREAD)==1&&mob.getOffhandItem().is(Items.TORCH)&&mob.getHealth()==2,"interruption returns uneaten food without healing");
                mob.getPersistentData().remove("ChangedSynergyNextCompanionMeal");
                goal=new CompanionAutoEatGoal(mob);check(goal.canUse(),"reload fixture ready");goal.start();
                var snapshot=mob.saveWithoutId(new CompoundTag());
                var reloaded=ChangedSynergyEntities.LATEX_SILK_MOTH.get().create(mob.level());reloaded.load(snapshot);reloaded.setUUID(UUID.randomUUID());
                server.overworld().addFreshEntity(reloaded);
                check(!CompanionAutoEatGoal.active(reloaded)&&count(reloaded,Items.BREAD)==1&&reloaded.getOffhandItem().is(Items.TORCH),"chunk reload restores the real reserved food and offhand");reloaded.discard();goal.stop();
                ChangedSynergyConfig.COMMON.companionAutoEat.set(false);mob.getPersistentData().remove("ChangedSynergyNextCompanionMeal");
                check(!new CompanionAutoEatGoal(mob).canUse(),"config disables automatic meals");ChangedSynergyConfig.COMMON.companionAutoEat.set(true);
            } finally {mob.discard();}
            var bonded=create(server,owner,true);
            try {
                var inv=BondedInventoryService.inventory(bonded,owner);inv.setItem(1,new ItemStack(Items.BREAD,2));bonded.setHealth(2);
                bonded.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TORCH));
                bonded.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));
                var goal=new CompanionAutoEatGoal(bonded);check(goal.canUse(),"morph fixture ready");goal.start();
                var abstracted=IAbstractChangedEntity.forEntity(bonded);abstracted.replaceVariant(ChangedSynergyTransfurVariants.LATEX_SWORDFISH.get());
                var replacement=abstracted.getChangedEntity();
                check(!CompanionAutoEatGoal.active(replacement)&&count(replacement,Items.BREAD)==2&&replacement.getOffhandItem().is(Items.TORCH),"form replacement preserves both food and original offhand without duplicating the meal");
                check(count(replacement,Items.DIAMOND_SWORD)+(replacement.getMainHandItem().is(Items.DIAMOND_SWORD)?replacement.getMainHandItem().getCount():0)==1,"native selected weapon is not duplicated into the fallback backpack");replacement.discard();
            } finally {if(!bonded.isRemoved())bonded.discard();}
            if(net.minecraftforge.fml.ModList.get().isLoaded("changed_addon")) {
                var addon=(ChangedEntity)net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("changed_addon","latex_cheetah_male")).create(server.overworld());
                addon.moveTo(96.5,180,97.5);addon.setNoGravity(true);server.overworld().addFreshEntity(addon);
                try {
                    LatexSocialMemory.addBond(addon,owner);addon.goalSelector.removeAllGoals(goal->true);addon.targetSelector.removeAllGoals(goal->true);
                    var inventory=BondedInventoryService.inventory(addon,owner);inventory.setItem(3,new ItemStack(Items.BREAD));addon.setHealth(2);
                    check(net.parkabird.changedsynergy.compat.ChangedAddonCompat.bondedInventory(addon)==inventory,"generic companion uses Addon's real native backpack");
                    players.remove(owner.getUUID());
                    var food=new CompanionAutoEatGoal(addon);check(food.canUse(),"offline owner does not hide Addon inventory food");food.start();
                    int ticks=0;while(food.canContinueToUse()&&ticks++<100){food.tick();addon.tick();}food.stop();
                    check(count(addon,Items.BREAD)==0&&addon.getHealth()>2,"offline Addon companion consumes its actual food");
                } finally {players.put(owner.getUUID(),owner);addon.discard();}
            }
            choices(server,owner);
            priority(server,owner);
            System.out.println("COMPANION_FOOD_REGRESSION_PASS: "+checks+" native/fallback inventory, food-specific healing, effects, bowls, cooldown, interrupt, reload, morph and emergency-wrap priority checks");
        } finally {ChangedSynergyConfig.COMMON.companionAutoEat.set(true);ChangedSynergyConfig.COMMON.bondedEmergencyRescue.set(rescue);players.remove(owner.getUUID());owner.discard();}
    }
    private static ChangedEntity create(MinecraftServer server,ServerPlayer owner,boolean bonded) {
        ChangedEntity mob=bonded?ChangedEntities.DARK_LATEX_WOLF_MALE.get().create(server.overworld()):ChangedSynergyEntities.LATEX_SILK_MOTH.get().create(server.overworld());
        mob.moveTo(96.5,180,97.5);mob.setNoGravity(true);server.overworld().addFreshEntity(mob);
        if(bonded)LatexSocialMemory.addBond(mob,owner);else CreaturePersonality.establishRelationship(mob,owner);
        mob.goalSelector.removeAllGoals(goal->true);mob.targetSelector.removeAllGoals(goal->true);
        return mob;
    }
    private static int count(ChangedEntity mob,net.minecraft.world.item.Item item) {
        var inventory=BondedInventoryService.inventory(mob,LatexSocialMemory.getPetOwner(mob));int count=0;
        for(int i=0;i<24;i++)if(inventory.getItem(i).is(item))count+=inventory.getItem(i).getCount();return count;
    }
    private static void choices(MinecraftServer server,ServerPlayer owner) {
        var mob=create(server,owner,false);
        try {
            var inventory=BondedInventoryService.inventory(mob,owner);
            inventory.setItem(1,new ItemStack(Items.GOLDEN_APPLE));
            inventory.setItem(2,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
            mob.setHealth(mob.getMaxHealth()*.6F);
            check(!new CompanionAutoEatGoal(mob).canUse(),"moderate injury reserves both golden apples even when no ordinary food is available");
            inventory.setItem(3,new ItemStack(Items.APPLE));mob.setHealth(mob.getMaxHealth()*.34F);
            choose(mob,Items.APPLE,"at the critical threshold ordinary food outweighs golden apples");
            inventory=BondedInventoryService.inventory(mob,owner);
            inventory.clearContent();inventory.setItem(1,new ItemStack(Items.GOLDEN_APPLE));inventory.setItem(2,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
            inventory.setItem(3,new ItemStack(Items.BREAD));mob.setHealth(mob.getMaxHealth()*.19F);
            choose(mob,Items.BREAD,"serious injury still prefers sufficient ordinary food over an enchanted golden apple");
            inventory=BondedInventoryService.inventory(mob,owner);
            inventory.clearContent();inventory.setItem(1,new ItemStack(Items.GOLDEN_APPLE));inventory.setItem(2,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));mob.setHealth(mob.getMaxHealth()*.05F);
            choose(mob,Items.GOLDEN_APPLE,"near death uses a normal golden apple before the rarer enchanted one");
            inventory=BondedInventoryService.inventory(mob,owner);
            inventory.clearContent();inventory.setItem(2,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
            choose(mob,Items.ENCHANTED_GOLDEN_APPLE,"near death permits the last available enchanted golden apple");
            inventory=BondedInventoryService.inventory(mob,owner);
            inventory.clearContent();inventory.setItem(3,new ItemStack(Items.COOKED_BEEF));inventory.setItem(4,new ItemStack(Items.RABBIT_STEW));
            mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(20);mob.setHealth(12);
            choose(mob,Items.COOKED_BEEF,"selection accounts for wasted healing rather than always choosing the richest food");
        } finally {mob.discard();}
    }
    private static void choose(ChangedEntity mob,net.minecraft.world.item.Item expected,String message) {
        mob.getPersistentData().remove("ChangedSynergyNextCompanionMeal");
        var goal=new CompanionAutoEatGoal(mob);check(goal.canUse(),"weighted selection fixture ready");goal.start();
        check(mob.getOffhandItem().is(expected),message+": expected="+expected+", selected="+mob.getOffhandItem());goal.stop();
    }
    private static void priority(MinecraftServer server,ServerPlayer owner) {
        ChangedSynergyConfig.COMMON.bondedEmergencyRescue.set(true);
        var mob=create(server,owner,true);
        try {
            BondedInventoryService.inventory(mob,owner).setItem(1,new ItemStack(Items.BREAD));mob.setHealth(2);
            var food=new CompanionAutoEatGoal(mob);var wrap=new BondedEmergencySuitGoal(mob);
            check(food.canUse()&&wrap.canUse(),"both self-healing actions are eligible at critical health");
            mob.goalSelector.addGoal(-2,food);mob.goalSelector.addGoal(-3,wrap);mob.goalSelector.tick();
            check(mob.goalSelector.getRunningGoals().anyMatch(goal->goal.getGoal()==wrap)
                    &&mob.goalSelector.getRunningGoals().noneMatch(goal->goal.getGoal()==food),"critical bond prioritizes wrapping its player over eating");
        } finally {BondedSuitService.releaseForBondEnd(mob,owner);mob.discard();}
        owner.getPersistentData().remove("ChangedSynergyRescueGraceUntil");
        owner.getPersistentData().remove("ChangedSynergyRescueReleaseSuppressed");
        mob=create(server,owner,true);
        try {
            ChangedSynergyConfig.COMMON.bondedEmergencyRescue.set(false);
            BondedInventoryService.inventory(mob,owner).setItem(1,new ItemStack(Items.BREAD));mob.setHealth(mob.getMaxHealth()*.5F);
            var food=new CompanionAutoEatGoal(mob);var wrap=new BondedEmergencySuitGoal(mob);
            mob.goalSelector.addGoal(-2,food);mob.goalSelector.addGoal(-3,wrap);mob.goalSelector.tick();
            check(CompanionAutoEatGoal.active(mob),"ordinary injury begins a real meal");
            ChangedSynergyConfig.COMMON.bondedEmergencyRescue.set(true);mob.setHealth(2);
            for(int tick=0;tick<4;tick++)mob.goalSelector.tick();
            check(!CompanionAutoEatGoal.active(mob)&&count(mob,Items.BREAD)==1
                    &&(BondedSuitService.isSuitingOwner(mob,owner)||mob.goalSelector.getRunningGoals().anyMatch(goal->goal.getGoal()==wrap)),
                    "critical wrapping interrupts an ongoing meal and returns the uneaten food: eating="+CompanionAutoEatGoal.active(mob)+", food="+count(mob,Items.BREAD)+", suited="+BondedSuitService.isSuitingOwner(mob,owner));
        } finally {ChangedSynergyConfig.COMMON.bondedEmergencyRescue.set(true);BondedSuitService.releaseForBondEnd(mob,owner);mob.discard();}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
