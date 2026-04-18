package com.astralsorceror.collation;

import net.fabricmc.api.ModInitializer;

public class Collation implements ModInitializer {

    @Override
    public void onInitialize() {
        CollationNetworking.registerC2SPackets();
    }
}
