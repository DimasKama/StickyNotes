package io.github.dimaskama.stickynotes.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractWidget.class)
public interface ClickableWidgetInvoker {
    @Invoker("renderScrollingString")
    static void stickynotes_drawScrollableText(GuiGraphics context, Font textRenderer, Component text, int left, int top, int right, int bottom, int color) {}
}
