package com.astralsorceror.collation.client;

import com.astralsorceror.collation.CollationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class CollationClientNetworking {

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(CollationNetworking.CHEST_DATA_SYNC,
            (client, handler, buf, responseSender) -> {

                BlockPos pos = buf.readBlockPos();
                int size = buf.readInt();
                Map<Item, Integer> chestItems = new HashMap<>();

                for (int i = 0; i < size; i++) {
                    ItemStack stack = buf.readItemStack();
                    int count = buf.readInt();

                    chestItems.merge(stack.getItem(), count, Integer::sum);
                }

                client.execute(() -> {

                    Map<Item, Integer> sorted = sortChestItems(chestItems);

                    PacketByteBuf buf2 = PacketByteBufs.create();
                    buf2.writeBlockPos(pos);

                    buf2.writeInt(sorted.size());

                    for (Map.Entry<Item, Integer> entry : sorted.entrySet()) {
                        buf2.writeRegistryValue(Registries.ITEM, entry.getKey());
                        buf2.writeInt(entry.getValue());
                    }

                    ClientPlayNetworking.send(CollationNetworking.APPLY_CHEST_SORT, buf2);
                });
            });
    }

    private static Map<Item, Integer> sortChestItems(Map<Item, Integer> chestItems) {

        List<Map.Entry<Item, Integer>> entries = new ArrayList<>(chestItems.entrySet());

        entries.sort(Comparator.comparingInt(Map.Entry::getValue));
        entries = entries.reversed();

        Map<Item, Integer> sorted = new LinkedHashMap<>();

        for (Map.Entry<Item, Integer> entry : entries) {
            sorted.put(entry.getKey(), entry.getValue());
        }

        return sorted;
    }
}
