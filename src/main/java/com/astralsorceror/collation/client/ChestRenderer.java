package com.astralsorceror.collation.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class ChestRenderer {
    private static final List<BlockPos> CHESTS = new ArrayList<>();

    public static void init() {
        WorldRenderEvents.END.register(context -> {
            for (BlockPos pos : new ArrayList<>(CHESTS)) {
                if (context.world().getBlockState(pos).getBlock() == Blocks.CHEST) {
                    renderCube(context, pos);
                } else {
                    removeChest(pos);
                }
            }
        });
    }

    private static void drawLine(WorldRenderContext context, Vec3d start, Vec3d end) {
        MatrixStack matrices = context.matrixStack();
        Vec3d camera = context.camera().getPos();

        double x1 = start.x - camera.x;
        double y1 = start.y - camera.y;
        double z1 = start.z - camera.z;

        double x2 = end.x - camera.x;
        double y2 = end.y - camera.y;
        double z2 = end.z - camera.z;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        buffer.vertex(matrix, (float)x1, (float)y1, (float)z1)
            .color(0f, 1f, 0f, 1f)
            .next();

        buffer.vertex(matrix, (float)x2, (float)y2, (float)z2)
            .color(0f, 1f, 0f, 1f)
            .next();

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void renderCube(WorldRenderContext context, BlockPos chestPos) {

        Vec3d c1 = new Vec3d(chestPos.getX(), chestPos.getY(), chestPos.getZ());

        Vec3d c2 = c1.add(1, 0, 0);
        Vec3d c3 = c1.add(0, 0, 1);
        Vec3d c4 = c1.add(1, 0, 1);
        Vec3d c5 = c1.add(0, 1, 0);
        Vec3d c6 = c1.add(1, 1, 0);
        Vec3d c7 = c1.add(0, 1, 1);
        Vec3d c8 = c1.add(1, 1, 1);

        drawLine(context, c1, c2);
        drawLine(context, c1, c3);
        drawLine(context, c1, c5);

        drawLine(context, c2, c4);
        drawLine(context, c2, c6);

        drawLine(context, c3, c4);
        drawLine(context, c3, c7);

        drawLine(context, c4, c8);

        drawLine(context, c5, c6);
        drawLine(context, c5, c7);

        drawLine(context, c6, c8);

        drawLine(context, c7, c8);
    }

    public static void addChest(BlockPos pos) {
        if (!CHESTS.contains(pos)) {
            CHESTS.add(pos);
        }
    }

    public static void removeChest(BlockPos pos) {
        CHESTS.remove(pos);
    }

    public static void clearChests() {
        CHESTS.clear();
    }
}
