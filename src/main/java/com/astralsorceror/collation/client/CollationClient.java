package com.astralsorceror.collation.client;

import com.astralsorceror.collation.event.KeyInputHandler;
import net.fabricmc.api.ClientModInitializer;

public class CollationClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyInputHandler.register();
        CollationClientNetworking.registerS2CPackets();
    }
}
