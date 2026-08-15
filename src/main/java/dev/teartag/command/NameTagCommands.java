package dev.teartag.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.teartag.config.ConfigManager;
import dev.teartag.game.DatapackEvents;
import dev.teartag.game.NametagService;
import dev.teartag.network.NetworkHandler;
import dev.teartag.rule.AttackRuleManager;
import dev.teartag.state.NametagSavedData;
import dev.teartag.state.PlayerNametag;
import java.util.Collection;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public final class NameTagCommands {
    private NameTagCommands() {
    }

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("teartag");
        root.then(Commands.literal("status")
            .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
            .then(Commands.argument("target", EntityArgument.player()).requires(NameTagCommands::isAdmin)
                .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))));

        root.then(Commands.literal("enable").requires(NameTagCommands::isAdmin)
            .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> forPlayers(EntityArgument.getPlayers(ctx, "targets"), NametagService::enable))));
        root.then(Commands.literal("disable").requires(NameTagCommands::isAdmin)
            .then(Commands.argument("targets", EntityArgument.players()).executes(ctx -> forPlayers(EntityArgument.getPlayers(ctx, "targets"), NametagService::disable))));

        root.then(Commands.literal("text").requires(NameTagCommands::isAdmin)
            .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("text", ComponentArgument.textComponent(buildContext)).executes(ctx -> {
                    Component component = ComponentArgument.getResolvedComponent(ctx, "text");
                    return forPlayers(EntityArgument.getPlayers(ctx, "targets"), player -> NametagService.setText(player, component));
                }))))
            .then(Commands.literal("plain").then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                    Component text = Component.literal(StringArgumentType.getString(ctx, "text"));
                    return forPlayers(EntityArgument.getPlayers(ctx, "targets"), player -> NametagService.setText(player, text));
                }))))
            .then(Commands.literal("reset").then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> forPlayers(EntityArgument.getPlayers(ctx, "targets"), NametagService::resetText)))));

        root.then(Commands.literal("reset").requires(NameTagCommands::isAdmin)
            .then(Commands.literal("round").then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> forPlayers(EntityArgument.getPlayers(ctx, "targets"), NametagService::resetRound))))
            .then(Commands.literal("all").then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> forPlayers(EntityArgument.getPlayers(ctx, "targets"), NametagService::resetAll)))));

        root.then(Commands.literal("rule").requires(NameTagCommands::isAdmin)
            .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("rule", IdentifierArgument.id()).executes(ctx -> {
                    var id = IdentifierArgument.getId(ctx, "rule");
                    if (!AttackRuleManager.exists(id)) {
                        ctx.getSource().sendFailure(Component.translatable("command.teartag.rule_missing", id.toString()));
                        return 0;
                    }
                    return forPlayers(EntityArgument.getPlayers(ctx, "targets"), player -> NametagService.setRule(player, id));
                })))));

        root.then(Commands.literal("config").requires(NameTagCommands::isAdmin)
            .then(Commands.literal("reload").executes(ctx -> reload(ctx.getSource()))));
        root.then(Commands.literal("event").requires(NameTagCommands::isAdmin)
            .then(Commands.literal("claim").executes(ctx -> DatapackEvents.claim(ctx.getSource().getPlayerOrException()) ? 1 : 0)));
        dispatcher.register(root);
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        PlayerNametag state = NametagSavedData.get(source.getServer()).get(player.getUUID());
        if (state == null) {
            source.sendSuccess(() -> Component.translatable("command.teartag.status_disabled", player.getDisplayName()), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("command.teartag.status", player.getDisplayName(), state.enabled,
            state.tears, ConfigManager.get().requiredTears(), state.eliminated, state.ruleId.toString()).append(" ").append(state.text.copy()), false);
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        var errors = ConfigManager.reload();
        if (!errors.isEmpty()) {
            errors.forEach(error -> source.sendFailure(Component.literal("[TearTag] " + error)));
            return 0;
        }
        for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
            PlayerNametag state = NametagSavedData.get(source.getServer()).get(player.getUUID());
            if (state != null && state.enabled) NetworkHandler.syncToAll(player, state);
        }
        source.sendSuccess(() -> Component.translatable("command.teartag.config_reloaded"), true);
        return 1;
    }

    private static int forPlayers(Collection<ServerPlayer> players, java.util.function.Consumer<ServerPlayer> action) {
        players.forEach(action);
        return players.size();
    }

    private static boolean isAdmin(CommandSourceStack source) {
        PermissionLevel level = PermissionLevel.byId(ConfigManager.get().commandPermissionLevel());
        return source.permissions().hasPermission(new Permission.HasCommandLevel(level));
    }
}
