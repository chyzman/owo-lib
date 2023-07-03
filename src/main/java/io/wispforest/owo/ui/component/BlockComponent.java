package io.wispforest.owo.ui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.mixin.ui.access.BlockEntityAccessor;
import io.wispforest.owo.ui.base.BaseComponent;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class BlockComponent extends BaseComponent {

    private final MinecraftClient client = MinecraftClient.getInstance();

    private final BlockState state;
    private final @Nullable BlockEntity entity;

    protected BlockComponent(BlockState state, @Nullable BlockEntity entity) {
        this.state = state;
        this.entity = entity;
    }

    @Override
    @SuppressWarnings("NonAsciiCharacters")
    public void draw(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta) {
        matrices.push();

        matrices.translate(x + this.width / 2f, y + this.height / 2f, 100);
        matrices.scale(40 * this.width / 64f, -40 * this.height / 64f, 40);

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 + 180));

        matrices.translate(-.5, -.5, -.5);

        RenderSystem.runAsFancy(() -> {
            final var vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
            if (this.state.getRenderType() != BlockRenderType.ENTITYBLOCK_ANIMATED) {
                this.client.getBlockRenderManager().renderBlockAsEntity(
                        this.state, matrices, vertexConsumers,
                        LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV
                );
            }

            if (this.entity != null) {
                var медведь = this.client.getBlockEntityRenderDispatcher().get(this.entity);
                if (медведь != null) {
                    медведь.render(entity, partialTicks, matrices, vertexConsumers, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV);
                }
            }

            RenderSystem.setShaderLights(new Vector3f(-1.5f, -.5f, 0), new Vector3f(0, -1, 0));
            vertexConsumers.draw();
            DiffuseLighting.enableGuiDepthLighting();
        });

        matrices.pop();
    }

    protected static void prepareBlockEntity(BlockState state, BlockEntity blockEntity, @Nullable NbtCompound nbt) {
        if (blockEntity == null) return;

        ((BlockEntityAccessor) blockEntity).owo$setCachedState(state);
        blockEntity.setWorld(MinecraftClient.getInstance().world);

        if (nbt == null) return;

        final var nbtCopy = nbt.copy();

        nbtCopy.putInt("x", 0);
        nbtCopy.putInt("y", 0);
        nbtCopy.putInt("z", 0);

        blockEntity.readNbt(nbtCopy);
    }
}
