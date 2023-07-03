package io.wispforest.owo.ui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.ui.util.Drawer;
import io.wispforest.owo.ui.util.SpriteUtilInvoker;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import org.w3c.dom.Element;

public class SpriteComponent extends BaseComponent {

    protected final Sprite sprite;

    protected SpriteComponent(Sprite sprite) {
        this.sprite = sprite;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return this.sprite.getContents().getWidth();
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return this.sprite.getContents().getHeight();
    }

    @Override
    public void draw(MatrixStack matrices, int mouseX, int mouseY, float partialTicks, float delta) {
        SpriteUtilInvoker.markSpriteActive(this.sprite);

        RenderSystem.setShaderTexture(0, this.sprite.getAtlasId());
        Drawer.drawSprite(matrices, this.x, this.y, 0, this.width, this.height, this.sprite);
    }

    public static SpriteComponent parse(Element element) {
        UIParsing.expectAttributes(element, "atlas", "sprite");

        var atlas = UIParsing.parseIdentifier(element.getAttributeNode("atlas"));
        var sprite = UIParsing.parseIdentifier(element.getAttributeNode("sprite"));

        return Components.sprite(new SpriteIdentifier(atlas, sprite));
    }
}
