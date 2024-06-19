package io.github.dimaskama.stickynotes.mixin;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClickableWidget.class)
public interface ClickableWidgetInvoker {
    @Invoker("drawScrollableText")
    static void stickynotes_drawScrollableText(DrawContext context, TextRenderer textRenderer, Text text, int left, int top, int right, int bottom, int color) {}
}
