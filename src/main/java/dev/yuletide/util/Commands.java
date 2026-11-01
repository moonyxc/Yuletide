package dev.yuletide.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class Commands {
	private Commands() {
	}

	/** Führt den Befehl als Konsole aus; {@code {player}} wird durch den Spielernamen ersetzt. */
	public static void runAsConsole(MinecraftServer server, String command, ServerPlayer player) {
		String resolved = command.replace("{player}", player.getGameProfile().name()).trim();
		if (resolved.startsWith("/")) {
			resolved = resolved.substring(1);
		}
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), resolved);
	}
}
