package io.wispforest.owo.ui.component;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIModel;
import io.wispforest.owo.ui.parsing.UIModelParsingException;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.util.pond.OwoEntityRenderDispatcherExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerModelPart;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.w3c.dom.Element;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

public class EntityComponent<E extends Entity> extends BaseComponent {

    protected final EntityRenderDispatcher dispatcher;
    protected final VertexConsumerProvider.Immediate entityBuffers;
    protected final E entity;

    protected float mouseRotation = 0;
    protected float scale = 1;
    protected boolean lookAtCursor = false;
    protected boolean allowMouseRotation = false;
    protected boolean scaleToFit = false;
    protected boolean showNametag = false;
    protected Consumer<MatrixStack> transform = matrixStack -> {};

    protected EntityComponent(Sizing sizing, E entity) {
        final var client = MinecraftClient.getInstance();
        this.dispatcher = client.getEntityRenderDispatcher();
        this.entityBuffers = client.getBufferBuilders().getEntityVertexConsumers();

        this.entity = entity;

        this.sizing(sizing);
    }

    @SuppressWarnings("DataFlowIssue")
    protected EntityComponent(Sizing sizing, EntityType<E> type, @Nullable NbtCompound nbt) {
        final var client = MinecraftClient.getInstance();
        this.dispatcher = client.getEntityRenderDispatcher();
        this.entityBuffers = client.getBufferBuilders().getEntityVertexConsumers();

        this.entity = type.create(client.world);
        if (nbt != null) entity.readNbt(nbt);
        entity.updatePosition(client.player.getX(), client.player.getY(), client.player.getZ());

        this.sizing(sizing);
    }

    @Override
    public void draw(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta) {
        matrices.push();

        matrices.translate(x + this.width / 2f, y + this.height / 2f, 100);
        matrices.scale(75 * this.scale * this.width / 64f, -75 * this.scale * this.height / 64f, 75 * this.scale);

        matrices.translate(0, entity.getHeight() / -2f, 0);

        this.transform.accept(matrices);

        if (this.lookAtCursor) {
            float xRotation = (float) Math.toDegrees(Math.atan((mouseY - this.y - this.height / 2f) / 40f));
            float yRotation = (float) Math.toDegrees(Math.atan((mouseX - this.x - this.width / 2f) / 40f));

            if (this.entity instanceof LivingEntity living) {
                living.prevHeadYaw = -yRotation;
            }

            this.entity.prevYaw = -yRotation;
            this.entity.prevPitch = xRotation * .65f;

            // We make sure the xRotation never becomes 0, as the lighting otherwise becomes very unhappy
            if (xRotation == 0) xRotation = .1f;
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(xRotation * .15f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yRotation * .15f));
        } else {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(35));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45 + this.mouseRotation));
        }

        var dispatcher = (OwoEntityRenderDispatcherExtension) this.dispatcher;
        dispatcher.owo$setCounterRotate(true);
        dispatcher.owo$setShowNametag(this.showNametag);

        RenderSystem.setShaderLights(new Vector3f(.15f, 1, 0), new Vector3f(.15f, -1, 0));
        this.dispatcher.setRenderShadows(false);
        this.dispatcher.render(this.entity, 0, 0, 0, 0, 0, matrices, this.entityBuffers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        this.dispatcher.setRenderShadows(true);
        this.entityBuffers.draw();
        DiffuseLighting.enableGuiDepthLighting();

        matrices.pop();

        dispatcher.owo$setCounterRotate(false);
        dispatcher.owo$setShowNametag(true);
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (this.allowMouseRotation && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.mouseRotation += deltaX;

            super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
            return true;
        } else {
            return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
        }
    }

    public E entity() {
        return this.entity;
    }

    public EntityComponent<E> allowMouseRotation(boolean allowMouseRotation) {
        this.allowMouseRotation = allowMouseRotation;
        return this;
    }

    public boolean allowMouseRotation() {
        return this.allowMouseRotation;
    }

    public EntityComponent<E> lookAtCursor(boolean lookAtCursor) {
        this.lookAtCursor = lookAtCursor;
        return this;
    }

    public boolean lookAtCursor() {
        return this.lookAtCursor;
    }

    public EntityComponent<E> scale(float scale) {
        this.scale = scale;
        return this;
    }

    public float scale() {
        return this.scale;
    }

    public EntityComponent<E> scaleToFit(boolean scaleToFit) {
        this.scaleToFit = scaleToFit;

        if (scaleToFit) {
            float xScale = .5f / entity.getWidth();
            float yScale = .5f / entity.getHeight();

            this.scale(Math.min(xScale, yScale));
        }

        return this;
    }

    public boolean scaleToFit() {
        return this.scaleToFit;
    }

    public EntityComponent<E> transform(Consumer<MatrixStack> transform) {
        this.transform = transform;
        return this;
    }

    public Consumer<MatrixStack> transform() {
        return transform;
    }

    public EntityComponent<E> showNametag(boolean showNametag) {
        this.showNametag = showNametag;
        return this;
    }

    public boolean showNametag() {
        return showNametag;
    }

    @Override
    public boolean canFocus(FocusSource source) {
        return source == FocusSource.MOUSE_CLICK;
    }

    public static RenderablePlayerEntity createRenderablePlayer(GameProfile profile) {
        return new RenderablePlayerEntity(profile);
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);

        UIParsing.apply(children, "scale", UIParsing::parseFloat, this::scale);
        UIParsing.apply(children, "look-at-cursor", UIParsing::parseBool, this::lookAtCursor);
        UIParsing.apply(children, "mouse-rotation", UIParsing::parseBool, this::allowMouseRotation);
        UIParsing.apply(children, "scale-to-fit", UIParsing::parseBool, this::scaleToFit);
    }

    public static EntityComponent<?> parse(Element element) {
        UIParsing.expectAttributes(element, "type");
        var entityId = UIParsing.parseIdentifier(element.getAttributeNode("type"));
        var entityType = Registries.ENTITY_TYPE.getOrEmpty(entityId).orElseThrow(() -> new UIModelParsingException("Unknown entity type " + entityId));

        return new EntityComponent<>(Sizing.content(), entityType, null);
    }

    public static class RenderablePlayerEntity extends ClientPlayerEntity {

        protected Identifier skinTextureId = null;
        protected String model = null;

        protected RenderablePlayerEntity(GameProfile profile) {
            super(MinecraftClient.getInstance(),
                    MinecraftClient.getInstance().world,
                    new ClientPlayNetworkHandler(MinecraftClient.getInstance(),
                            null,
                            new ClientConnection(NetworkSide.CLIENTBOUND),
                            null,
                            profile,
                            MinecraftClient.getInstance().getTelemetryManager().createWorldSession(false, Duration.ZERO)
                    ),
                    null, null, false, false
            );

            this.client.getSkinProvider().loadSkin(this.getGameProfile(), (type, identifier, texture) -> {
                if (type != MinecraftProfileTexture.Type.SKIN) return;

                this.skinTextureId = identifier;
                this.model = texture.getMetadata("model");
                if (this.model == null) this.model = "default";

            }, true);
        }

        @Override
        public boolean hasSkinTexture() {
            return skinTextureId != null;
        }

        @Override
        public Identifier getSkinTexture() {
            return this.skinTextureId != null ? this.skinTextureId : super.getSkinTexture();
        }

        @Override
        public boolean isPartVisible(PlayerModelPart modelPart) {
            return true;
        }

        @Override
        public String getModel() {
            return this.model != null ? this.model : super.getModel();
        }

        @Nullable
        @Override
        protected PlayerListEntry getPlayerListEntry() {
            return null;
        }
    }
}
