package com.astralsorceror.collation;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public class CollationNetworking {
    public static final Identifier REQUEST_CHEST_DATA = new Identifier("collation", "request_chest_data");
    public static final Identifier CHEST_DATA_SYNC = new Identifier("collation", "chest_data_sync");
    public static final Identifier APPLY_CHEST_SORT = new Identifier("collation", "apply_chest_sort");

    public static void registerC2SPackets() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_CHEST_DATA,
            (server, player, handler, buf, responseSender) -> {

                List<BlockPos> chestsPos = new ArrayList<>();
                int numOfChests = buf.readInt();

                for (int i = 0; i < numOfChests; i++) {
                    chestsPos.add(buf.readBlockPos());
                }

                server.execute(() -> {

                    List<ItemStack> stacks = new ArrayList<>();
                    List<Integer> amounts = new ArrayList<>();
                    List<BlockPos> validChests = new ArrayList<>();

                    PacketByteBuf responseBuf = PacketByteBufs.create();

                    for (BlockPos pos : chestsPos) {

                        if (player.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity chest) {

                            validChests.add(pos);

                            for (int i = 0; i < chest.size(); i++) {

                                ItemStack chestStack = chest.getStack(i);
                                if (chestStack.isEmpty()) continue;

                                int index = -1;

                                for (int j = 0; j < stacks.size(); j++) {
                                    if (ItemStack.areEqual(stacks.get(j), chestStack)) {
                                        index = j;
                                        break;
                                    }
                                }

                                if (index != -1) {
                                    amounts.set(index,
                                        amounts.get(index) + chestStack.getCount());
                                } else {
                                    stacks.add(chestStack.copy());
                                    amounts.add(chestStack.getCount());
                                }
                            }
                        }
                    }

                    responseBuf.writeInt(validChests.size());

                    for (BlockPos pos : validChests) {
                        responseBuf.writeBlockPos(pos);
                    }

                    responseBuf.writeInt(stacks.size());

                    for (ItemStack stack : stacks) {
                        responseBuf.writeItemStack(stack);
                    }

                    for (int amount : amounts) {
                        responseBuf.writeInt(amount);
                    }

                    ServerPlayNetworking.send(player, CHEST_DATA_SYNC, responseBuf);
                });
            });

        ServerPlayNetworking.registerGlobalReceiver(APPLY_CHEST_SORT,
            (server, player, handler, buf, responseSender) -> {

                int numOfChests = buf.readInt();

                List<BlockPos> chestsPos = new ArrayList<>();
                for (int i = 0; i < numOfChests; i++) {
                    chestsPos.add(buf.readBlockPos());
                }

                int numOfItems = buf.readInt();

                List<ItemStack> items = new ArrayList<>();
                List<Integer> amounts = new ArrayList<>();

                for (int i = 0; i < numOfItems; i++) {
                    items.add(buf.readItemStack());
                }

                for (int i = 0; i < numOfItems; i++) {
                    amounts.add(buf.readInt());
                }

                server.execute(() -> {

                    for (BlockPos pos : chestsPos) {
                        if (player.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                            for (int i = 0; i < chest.size(); i++) {
                                chest.setStack(i, ItemStack.EMPTY);
                            }
                            chest.markDirty();
                        }
                    }

                    int chestIndex = 0;
                    int slotIndex = 0;

                    ItemStack current = null;
                    int remaining = 0;

                    int itemIndex = 0;

                    if (numOfItems > 0) {
                        current = items.get(0).copy();
                        remaining = amounts.get(0);
                    }

                    while (current != null && chestIndex < chestsPos.size()) {

                        BlockPos pos = chestsPos.get(chestIndex);

                        if (!(player.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
                            chestIndex++;
                            continue;
                        }

                        while (slotIndex < chest.size() && current != null) {

                            int maxStack = current.getMaxCount();
                            int toPlace = Math.min(remaining, maxStack);

                            ItemStack placed = current.copy();
                            placed.setCount(toPlace);

                            chest.setStack(slotIndex, placed);

                            remaining -= toPlace;
                            slotIndex++;

                            if (remaining <= 0) {
                                itemIndex++;

                                if (itemIndex < numOfItems) {
                                    current = items.get(itemIndex).copy();
                                    remaining = amounts.get(itemIndex);
                                } else {
                                    current = null;
                                }
                            }
                        }

                        chest.markDirty();

                        chestIndex++;
                        slotIndex = 0;
                    }
                });
            });
    }
}