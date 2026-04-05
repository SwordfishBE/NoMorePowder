package net.nomorepowder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.nomorepowder.util.ModrinthUpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoMorePowder implements ModInitializer {

    public static final String MOD_ID = "nomorepowder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final String DEFAULT_MOD_NAME = "NoMorePowder";

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ModrinthUpdateChecker.checkOnceAsync());
        LOGGER.info("[{}] Mod initialized. Version: {}", modName(), modVersion());
    }

    public static String modName() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getName())
                .filter(name -> !name.isBlank())
                .orElse(DEFAULT_MOD_NAME);
    }

    public static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
