package net.parkabird.changedsynergy.regression;

import com.mojang.authlib.GameProfile;
import java.util.Set;
import java.util.UUID;
import net.ltxprogrammer.changed.init.ChangedAbilities;
import net.ltxprogrammer.changed.init.ChangedRegistry;
import net.ltxprogrammer.changed.process.ProcessTransfur;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Read actual registered player ability instances, including universal abilities and optional-mod changes. */
@Mod.EventBusSubscriber(modid = "changed_synergy", value = Dist.DEDICATED_SERVER)
public final class PlayerHugAbilityAudit {
    @SubscribeEvent public static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            int checked = 0;
            for (var form : ChangedRegistry.TRANSFUR_VARIANT.get().getValues()) {
                var entityId = ForgeRegistries.ENTITY_TYPES.getKey(form.getEntityType());
                if (entityId == null || !(Set.of("milk_pudding", "latex_shark_feral", "headless_knight",
                        "dark_latex_wolf_pup", "pure_white_latex_wolf_pup").contains(entityId.getPath()) || entityId.getPath().endsWith("_pup")
                        || form.getFormId().getPath().contains("snep_feral"))) continue;
                var player = new ServerPlayer(server, server.overworld(), new GameProfile(UUID.randomUUID(), "HugAbilityAudit"));
                player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player);
                var instance = ProcessTransfur.setPlayerTransfurVariant(player, form, null, 1, false);
                if (instance == null) throw new AssertionError("form could not be instantiated: " + form.getFormId());
                boolean grab = instance.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get()) != null;
                System.out.println("PLAYER_HUG_ABILITY_AUDIT form=" + form.getFormId() + " entity=" + entityId + " grab=" + grab);
                checked++; player.discard();
            }
            for (String name : Set.of("milk_pudding", "headless_knight")) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("changed", name));
                var npc = (net.ltxprogrammer.changed.entity.ChangedEntity)type.create(server.overworld());
                System.out.println("PLAYER_HUG_ABILITY_AUDIT npc=" + name + " self_variant=" + npc.getSelfVariant()
                        + " native_grab=" + (npc.getAbilityInstance(ChangedAbilities.GRAB_ENTITY_ABILITY.get()) != null));
                checked++;
            }
            if (checked < 5) throw new AssertionError("missing expected core forms: " + checked);
            System.out.println("PLAYER_HUG_DESIGN_AUDIT_PASS: " + checked + " player forms and NPC-only entities checked");
        } catch (Throwable failure) {
            System.err.println("PLAYER_HUG_DESIGN_AUDIT_FAIL"); failure.printStackTrace();
        } finally { server.halt(false); }
    }
}
