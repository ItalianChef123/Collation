package com.astralsorceror.collation;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CollationNetworking {
    public static final Identifier REQUEST_CHEST_DATA =
        new Identifier("collation", "request_chest_data");
    public static final Identifier CHEST_DATA_SYNC =
        new Identifier("collation", "chest_data_sync");
    public static final Identifier APPLY_CHEST_SORT =
        new Identifier("collation", "apply_chest_sort");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_CHEST_DATA,
            (server, player, handler, buf, responseSender) -> {

                BlockPos pos = buf.readBlockPos();

                server.execute(() -> {
                    if (player.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity chest) {

                        Map<Item, Integer> chestItems = new HashMap<>();

                        for (int i = 0; i < chest.size(); i++) {
                            ItemStack stack = chest.getStack(i);

                            if (!stack.isEmpty()) {
                                chestItems.merge(stack.getItem(), stack.getCount(), Integer::sum);
                            }
                        }
                        PacketByteBuf responseBuf = PacketByteBufs.create();

                        responseBuf.writeBlockPos(pos);
                        responseBuf.writeInt(chestItems.size());

                        for (Map.Entry<Item, Integer> entry : chestItems.entrySet()) {
                            responseBuf.writeItemStack(new ItemStack(entry.getKey()));
                            responseBuf.writeInt(entry.getValue());
                        }

                        ServerPlayNetworking.send(player, CHEST_DATA_SYNC, responseBuf);
                    }
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(APPLY_CHEST_SORT,
            (server, player, handler, buf, responseSender) -> {

                BlockPos pos = buf.readBlockPos();

                int size = buf.readInt();
                Map<Item, Integer> sortedItems = new LinkedHashMap<>();

                for (int i = 0; i < size; i++) {
                    Item item = buf.readRegistryValue(Registries.ITEM);
                    int count = buf.readInt();
                    sortedItems.put(item, count);
                }

                server.execute(() -> {
                    if (player.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity chest) {

                        for (int i = 0; i < chest.size(); i++) {
                            chest.setStack(i, ItemStack.EMPTY);
                        }

                        int slot = 0;

                        for (Map.Entry<Item, Integer> entry : sortedItems.entrySet()) {
                            int remaining = entry.getValue();

                            while (remaining > 0 && slot < chest.size()) {
                                int stackSize = Math.min(remaining, entry.getKey().getMaxCount());

                                chest.setStack(slot, new ItemStack(entry.getKey(), stackSize));

                                remaining -= stackSize;
                                slot++;
                            }
                        }

                        chest.markDirty();
                    }
                });
            });
    }
}
