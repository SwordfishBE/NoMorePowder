package com.nomorepowder;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoMorePowder implements ModInitializer {

    public static final String MOD_ID = "nomorepowder";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[NoMorePowder] Active — naturally-generated powder snow will be replaced with snow blocks via surface rule intercept.");
    }
}

