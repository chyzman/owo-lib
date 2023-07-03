package io.wispforest.owo.util.pond;

import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.layers.Layer;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;

public interface OwoScreenExtension {
    List<Layer<?, ?>.Instance> owo$getInstancesView();
    <S extends Screen, R extends ParentComponent> Layer<S, R>.Instance owo$getInstance(Layer<S, R> layer);

    void owo$updateLayers();
}
