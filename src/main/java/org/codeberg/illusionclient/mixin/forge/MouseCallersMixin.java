package org.codeberg.illusionclient.mixin.forge;

//? if forge {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.game.ChatScreen;
import net.minecraft.client.gui.screen.game.inventory.CreativeInventoryScreen;
import net.minecraft.client.gui.widget.ListWidget;
import net.minecraft.realms.RealmsClickableScrolledSelectionListProxy;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.client.GuiScrollingList;
import org.codeberg.illusionclient.util.MouseUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({
	Minecraft.class,
	Screen.class,
	ListWidget.class,
	ChatScreen.class,
	CreativeInventoryScreen.class,
	RealmsClickableScrolledSelectionListProxy.class,
	MouseEvent.class,
	GuiScrollingList.class
})
public class MouseCallersMixin {
	@ModifyExpressionValue(
		method = {"*", "<init>"},
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;next()Z", remap = false),
		require = 0
	)
	private boolean illusion$next(boolean hasEvent) {
		return MouseUtils.next(hasEvent);
	}

	@ModifyExpressionValue(
		method = {"*", "<init>"},
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false),
		require = 0
	)
	private int illusion$eventButton(int button) {
		return MouseUtils.synthetic() ? MouseUtils.eventButton() : button;
	}

	@ModifyExpressionValue(
		method = {"*", "<init>"},
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButtonState()Z", remap = false),
		require = 0
	)
	private boolean illusion$eventButtonState(boolean state) {
		return MouseUtils.synthetic() ? MouseUtils.eventState() : state;
	}

	@ModifyExpressionValue(
		method = {"*", "<init>"},
		at = @At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventDWheel()I", remap = false),
		require = 0
	)
	private int illusion$eventDWheel(int wheel) {
		return MouseUtils.synthetic() ? 0 : wheel;
	}
}
*///?}
