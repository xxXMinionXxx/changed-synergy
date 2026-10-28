package net.parkabird.changedsynergy.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.parkabird.changedsynergy.ai.BondedRevivalData;
import net.parkabird.changedsynergy.ai.BondedRevivalService;

/** Explicit operator shortcut; the normal item-based revival remains available. */
final class BondReviveCommand {
    private BondReviveCommand() {}
    static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("revive").requires(source -> source.hasPermission(2))
                .executes(context -> choose(context.getSource()))
                .then(Commands.literal("list").executes(context -> list(context.getSource(),context.getSource().getPlayerOrException())))
                .then(Commands.argument("creature",UuidArgument.uuid())
                        .suggests((context,builder) -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player)
                                BondedRevivalData.get(player.server).availableTo(player.getUUID()).values().forEach(record ->
                                        builder.suggest(record.getUUID("OldEntity").toString(),Component.literal(record.getString("BondName"))));
                            return builder.buildFuture();
                        })
                        .executes(context -> revive(context.getSource(),context.getSource().getPlayerOrException(),UuidArgument.getUuid(context,"creature")))
                        .then(Commands.argument("player",EntityArgument.player())
                                .executes(context -> revive(context.getSource(),EntityArgument.getPlayer(context,"player"),UuidArgument.getUuid(context,"creature")))));
    }

    private static int choose(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        var records=BondedRevivalData.get(player.server).availableTo(player.getUUID());
        return records.size()==1 ? revive(source,player,records.values().iterator().next().getUUID("OldEntity")) : list(source,player);
    }

    private static int list(CommandSourceStack source,ServerPlayer player) {
        var records=BondedRevivalData.get(player.server).availableTo(player.getUUID());
        if (records.isEmpty()) {
            source.sendFailure(Component.translatable("command.changed_synergy.bond.revive.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.changed_synergy.bond.revive.choose"),false);
        records.values().forEach(record -> {
            String id=record.getUUID("OldEntity").toString();
            source.sendSuccess(() -> Component.literal(record.getString("BondName")+" — "+id).withStyle(style -> style
                    .withColor(net.minecraft.ChatFormatting.AQUA).withUnderlined(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/changedsynergy bond revive "+id))),false);
        });
        return records.size();
    }

    private static int revive(CommandSourceStack source,ServerPlayer player,UUID creature) {
        var revived=BondedRevivalService.reviveImmediately(player,creature);
        if (revived==null) {
            source.sendFailure(Component.translatable("command.changed_synergy.bond.revive.failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.changed_synergy.bond.revive.success",revived.getDisplayName(),player.getDisplayName()),true);
        return 1;
    }
}
