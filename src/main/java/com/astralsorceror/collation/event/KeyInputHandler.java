package com.astralsorceror.collation.event;

import com.astralsorceror.collation.CollationNetworking;
import com.astralsorceror.collation.client.ChestRenderer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

public class KeyInputHandler {
    public static final String KEY_CATEGORY_COLLATION = "key.collation.category.collation";
    public static final String KEY_SELECT_CHEST = "key.collation.select_chest";
    public static final String KEY_SORT_CHESTS = "key.collation.sort_chests";

    private static List<BlockPos> chestsPos = new ArrayList<>();
    public static KeyBinding selectChestKey;
    public static KeyBinding sortChestsKey;

    public static void registerKeyInputs() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (selectChestKey.wasPressed()) {
                assert client.player != null;

                HitResult hit = client.player.raycast(4.5, 1.0F, false);

                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult blockHit = (BlockHitResult) hit;
                    BlockPos pos = blockHit.getBlockPos();

                    if (client.player.getWorld().getBlockState(pos).getBlock() == Blocks.CHEST) {
                        if (chestsPos.contains(pos)) {
                            chestsPos.remove(pos);
                            ChestRenderer.removeChest(pos);
                        } else {
                            chestsPos.add(pos);
                            ChestRenderer.addChest(pos);
                        }
                    }
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (sortChestsKey.wasPressed()) {

                assert client.world != null;

                List<BlockPos> validChests = new ArrayList<>();

                for (BlockPos chestPos : chestsPos) {
                    if (client.world.getBlockState(chestPos).getBlock() == Blocks.CHEST) {
                        validChests.add(chestPos);
                    }
                }

                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeInt(validChests.size());

                for (BlockPos chestPos : validChests) {
                    buf.writeBlockPos(chestPos);
                }

                ClientPlayNetworking.send(CollationNetworking.REQUEST_CHEST_DATA, buf);

                chestsPos = new ArrayList<>();
                ChestRenderer.clearChests();
            }
        });
    }

    public static void register() {
        selectChestKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding( KEY_SELECT_CHEST,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_BACKSLASH,
                KEY_CATEGORY_COLLATION
            ));
        sortChestsKey = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                KEY_SORT_CHESTS,
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                KEY_CATEGORY_COLLATION
            ));
        registerKeyInputs();
    }
}