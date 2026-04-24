package com.astralsorceror.collation.client;

import com.astralsorceror.collation.CollationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class CollationClientNetworking {

    public static void registerS2CPackets() {
        ClientPlayNetworking.registerGlobalReceiver(
            CollationNetworking.CHEST_DATA_SYNC,
            (client, handler, buf, responseSender) -> {

                int numOfChests = buf.readInt();

                List<BlockPos> chestsPos = new ArrayList<>();
                for (int i = 0; i < numOfChests; i++) {
                    chestsPos.add(buf.readBlockPos());
                }

                int numOfStacks = buf.readInt();

                List<ItemStack> stacks = new ArrayList<>();
                List<Integer> amounts = new ArrayList<>();

                for (int i = 0; i < numOfStacks; i++) {
                    stacks.add(buf.readItemStack());
                }

                for (int i = 0; i < numOfStacks; i++) {
                    amounts.add(buf.readInt());
                }


                client.execute(() -> {

                    Map<Item, List<Pair<ItemStack, Integer>>> grouped = new HashMap<>();

                    for (int i = 0; i < numOfStacks; i++) {
                        ItemStack stack = stacks.get(i);
                        int amount = amounts.get(i);

                        Item item = stack.getItem();

                        grouped.computeIfAbsent(item, k -> new ArrayList<>())
                            .add(new Pair<>(stack.copy(), amount));
                    }

                    Map<Item, Integer> totals = new HashMap<>();

                    for (Map.Entry<Item, List<Pair<ItemStack, Integer>>> entry : grouped.entrySet()) {
                        int sum = entry.getValue().stream()
                            .mapToInt(Pair::getRight)
                            .sum();

                        totals.put(entry.getKey(), sum);
                    }

                    List<Item> order = new ArrayList<>(grouped.keySet());

                    order.sort((a, b) -> {
                        int quantityCompare = Integer.compare(totals.get(b), totals.get(a));
                        if (quantityCompare != 0) {
                            return quantityCompare;
                        }
                        String nameA = a.getName().getString();
                        String nameB = b.getName().getString();
                        return nameA.compareToIgnoreCase(nameB);
                    });

                    List<ItemStack> sortedItems = new ArrayList<>();
                    List<Integer> sortedAmounts = new ArrayList<>();

                    for (Item item : order) {
                        for (Pair<ItemStack, Integer> pair : grouped.get(item)) {
                            sortedItems.add(pair.getLeft());
                            sortedAmounts.add(pair.getRight());
                        }
                    }

                    PacketByteBuf buf2 = PacketByteBufs.create();

                    buf2.writeInt(numOfChests);

                    for (BlockPos chestPos : chestsPos) {
                        buf2.writeBlockPos(chestPos);
                    }

                    buf2.writeInt(sortedItems.size());

                    for (ItemStack stack : sortedItems) {
                        buf2.writeItemStack(stack);
                    }

                    for (int amount : sortedAmounts) {
                        buf2.writeInt(amount);
                    }

                    ClientPlayNetworking.send(CollationNetworking.APPLY_CHEST_SORT, buf2);
                });
            });
    }
}
