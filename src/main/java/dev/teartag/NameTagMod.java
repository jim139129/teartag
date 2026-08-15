package dev.teartag;

import dev.teartag.command.NameTagCommands;
import dev.teartag.config.ConfigManager;
import dev.teartag.game.NametagService;
import dev.teartag.integration.AccessoryIntegrations;
import dev.teartag.integration.TrinketsIntegration;
import dev.teartag.item.NametagItems;
import dev.teartag.network.NetworkHandler;
import dev.teartag.rule.AttackRuleManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NameTagMod implements ModInitializer {
    public static final String MOD_ID = "teartag";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        var configErrors = ConfigManager.reload();
        if (!configErrors.isEmpty()) {
            configErrors.forEach(error -> LOGGER.error("Invalid configuration: {}", error));
        }
        NametagItems.register();
        NetworkHandler.register();
        if (FabricLoader.getInstance().isModLoaded(TrinketsIntegration.MOD_ID)) {
            AccessoryIntegrations.install(TrinketsIntegration.initialize());
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> NameTagCommands.register(dispatcher, registryAccess));
        ServerTickEvents.END_SERVER_TICK.register(NametagService::tick);
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> NametagService.allowParticipantDamage(entity, source, amount));
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(id("attack_rules"), new AttackRuleManager());
        LOGGER.info("TearTag initialized");
    }

    public static net.minecraft.resources.Identifier id(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
