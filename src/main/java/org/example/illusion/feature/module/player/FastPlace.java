package org.example.illusion.feature.module.player;

import io.github.nevalackin.radbus.Listen;
import net.minecraft.client.Minecraft;
import org.example.illusion.event.PlayerTickEvent;
import org.example.illusion.event.UseDelayEvent;
import org.example.illusion.feature.config.api.Slider;
import org.example.illusion.feature.module.api.Category;
import org.example.illusion.feature.module.api.Description;
import org.example.illusion.feature.module.api.Module;
import org.example.illusion.feature.module.api.ModuleInfo;

@ModuleInfo(name = "Fast Place", category = Category.PLAYER)
@Description("Removes the delay between block placements while holding the use key.")
public class FastPlace extends Module {
    private static final float TICKS_PER_SECOND = 20.0F;

    @Slider(value = "Maximum CPS", min = 5.0F, max = 20.0F)
    public float maximumCps = 20.0F;

    @Slider(value = "Activate Delay", min = 0.0F, max = 4.0F)
    public float activateDelay = 0.0F;

    private final Minecraft minecraft = Minecraft.getInstance();

    private int heldTicks;
    private float remainder;

    @Override
    public String details() {
        return number(maximumCps) + "cps";
    }

    @Override
    public void onDisable() {
        heldTicks = 0;
        remainder = 0.0F;
    }

    @Listen
    public void onPlayerTick(PlayerTickEvent event) {
        if (minecraft.options.useKey.isPressed()) {
            heldTicks++;
        } else {
            heldTicks = 0;
            remainder = 0.0F;
        }
    }

    @Listen
    public void onUseDelay(UseDelayEvent event) {
        float ticks = TICKS_PER_SECOND / maximumCps + remainder;
        int delay = (int) ticks;
        remainder = ticks - delay;

        int untilActive = Math.round(activateDelay) - heldTicks;
        if (untilActive > 0) {
            remainder = 0.0F;
            delay = Math.max(delay, untilActive);
        }
        event.delay = Math.min(event.delay, delay);
    }
}
