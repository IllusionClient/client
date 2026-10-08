package org.example.illusion.mixin;

import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Screen.class)
public interface ScreenInvoker {
	@Invoker("mouseClicked")
	void illusion$mouseClicked(int mouseX, int mouseY, int button);

	@Invoker("mouseReleased")
	void illusion$mouseReleased(int mouseX, int mouseY, int button);
}
