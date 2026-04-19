package com.astralsorceror.collation.client;

import com.astralsorceror.collation.CollationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;

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

                    List<ItemStack> keys = new ArrayList<>();
                    List<Integer> values = new ArrayList<>();

                    for (int i = 0; i < numOfStacks; i++) {

                        ItemStack stack = stacks.get(i);
                        int amount = amounts.get(i);

                        int index = -1;

                        for (int j = 0; j < keys.size(); j++) {
                            if (ItemStack.areEqual(keys.get(j), stack)) {
                                index = j;
                                break;
                            }
                        }

                        if (index != -1) {
                            values.set(index, values.get(index) + amount);
                        } else {
                            keys.add(stack.copy());
                            values.add(amount);
                        }
                    }

                    List<Integer> order = new ArrayList<>();
                    for (int i = 0; i < keys.size(); i++) {
                        order.add(i);
                    }

                    order.sort((a, b) -> Integer.compare(values.get(b), values.get(a)));

                    List<ItemStack> sortedItems = new ArrayList<>();
                    List<Integer> sortedAmounts = new ArrayList<>();

                    for (int i : order) {
                        sortedItems.add(keys.get(i));
                        sortedAmounts.add(values.get(i));
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
