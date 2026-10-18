package org.codeberg.illusionclient;

//? if fabric {
import net.fabricmc.loader.api.FabricLoader;
//?} else {
/*import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.common.Loader;
*///?}

import java.nio.file.Path;

public final class Platform {
	private Platform() {
	}

	public static boolean isDevelopmentEnvironment() {
		//? if fabric {
		return FabricLoader.getInstance().isDevelopmentEnvironment();
		//?} else {
		/*return Boolean.TRUE.equals(Launch.blackboard.get("fml.deobfuscatedEnvironment"));
		*///?}
	}

	public static Path getConfigDirectory() {
		//? if fabric {
		return FabricLoader.getInstance().getConfigDir();
		//?} else {
		/*return Loader.instance().getConfigDir().toPath();
		*///?}
	}
}
