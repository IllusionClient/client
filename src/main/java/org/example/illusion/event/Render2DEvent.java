package org.example.illusion.event;

import net.minecraft.client.render.Window;

public record Render2DEvent(float partialTicks, Window window) implements Event {
}
