package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.ltxprogrammer.changed.entity.ChangedEntity;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedEntities;
import net.ltxprogrammer.changed.init.ChangedTags;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.parkabird.changedsynergy.ChangedSynergyConfig;
import net.parkabird.changedsynergy.ChangedSynergyMod;
import net.parkabird.changedsynergy.ai.CreatureSocialProfile;
import net.parkabird.changedsynergy.ai.HunterArchetype;
import net.parkabird.changedsynergy.ai.HunterFaction;
import net.parkabird.changedsynergy.entity.LatexSilkMoth;
import net.parkabird.changedsynergy.init.ChangedSynergyEntities;
import net.parkabird.changedsynergy.init.ChangedSynergyItems;
import net.parkabird.changedsynergy.init.ChangedSynergyTransfurVariants;
import net.parkabird.changedsynergy.origin.NativeOriginCatalog;

@Mod.EventBusSubscriber(modid = ChangedSynergyMod.MOD_ID)
public final class SilkMothServerRegression {
    @SubscribeEvent
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("silkmoth.server.regression")) return;
        try {
            var level = event.getServer().overworld();
            var type = ChangedSynergyEntities.LATEX_SILK_MOTH.get();
            var form = ChangedSynergyTransfurVariants.LATEX_SILK_MOTH.get();
            var moth = type.create(level);
            check(moth != null && moth.getSelfVariant() == form, "entity resolves its own transfur form");
            check(type != (Object) ChangedEntities.LATEX_MOTH.get(), "original moth remains a separate entity");
            check(type.is(ChangedTags.EntityTypes.LATEX), "Changed latex tag loaded");
            check(moth.getMaxHealth() > 0 && moth.getAttribute(Attributes.MOVEMENT_SPEED) != null,
                    "entity attributes registered on dedicated server");
            check(HunterFaction.of(moth) == HunterFaction.LIGHT
                    && HunterArchetype.of(moth) == HunterArchetype.INSECT
                    && CreatureSocialProfile.allowsSynergySystems(moth), "Synergy insect social profile");
            var egg = (SpawnEggItem) ChangedSynergyItems.LATEX_SILK_MOTH_SPAWN_EGG.get();
            check(egg.getType(null) == type, "spawn egg resolves the new entity");
            check(form.extraJumpCharges == 6, "original moth jump allowance retained");
            check(form.abilities.stream().anyMatch(ability -> ability.apply(type) == ChangedAbilities.SWITCH_HANDS.get()),
                    "extra hands ability registered");
            CompoundTag saved = new CompoundTag();
            moth.save(saved);
            var restored = EntityType.loadEntityRecursive(saved, level, entity -> entity);
            check(restored instanceof LatexSilkMoth && restored.getType() == type, "entity survives NBT roundtrip");
            ChangedSynergyConfig.COMMON.includeModdedOriginSpecies.set(false);
            check(NativeOriginCatalog.find(level.registryAccess(), form.getFormId()) != null,
                    "Synergy form is selectable without enabling third-party forms");
            var disabled = ChangedSynergyConfig.COMMON.disabledOriginForms;
            var previous = disabled.get();
            try {
                disabled.set(List.of(form.getFormId().toString()));
                check(NativeOriginCatalog.find(level.registryAccess(), form.getFormId()) == null, "origin denylist still applies");
            } finally {
                disabled.set(previous);
            }
            var player = new ServerPlayer(event.getServer(), level, new GameProfile(UUID.randomUUID(), "SilkMothTest"));
            player.connection = new ServerGamePacketListenerImpl(event.getServer(),
                    new Connection(PacketFlow.SERVERBOUND), player);
            var transformed = ProcessTransfur.setPlayerTransfurVariant(player, form);
            check(transformed != null && transformed.getChangedEntity() instanceof LatexSilkMoth,
                    "player transfur creates the new moth body");
            player.setOnGround(false);
            player.jumpFromGround();
            check(player.getDeltaMovement().y > 0, "air-jump event and tracking packet are safe on a dedicated server");
            var flapBuffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            try {
                var packet = new net.parkabird.changedsynergy.network.SilkMothWingFlapPacket(player.getId());
                net.parkabird.changedsynergy.network.SilkMothWingFlapPacket.encode(packet, flapBuffer);
                check(packet.equals(net.parkabird.changedsynergy.network.SilkMothWingFlapPacket.decode(flapBuffer)),
                        "wing flap packet codec loads without client classes on the dedicated server");
            } finally {
                flapBuffer.release();
            }
            check(ForgeRegistries.ENTITY_TYPES.getKey(type).equals(
                    ResourceLocation.parse("changed_synergy:latex_silk_moth")), "stable entity ID");
            System.out.println("SILK_MOTH_SERVER_REGRESSION_PASS");
        } catch (Throwable failure) {
            System.err.println("SILK_MOTH_SERVER_REGRESSION_FAIL");
            failure.printStackTrace();
            throw new RuntimeException(failure);
        } finally {
            event.getServer().halt(false);
        }
    }

    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
        System.out.println("SILK_MOTH_PASS: " + description);
    }
}
