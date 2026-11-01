package dev.yuletide.command;

import dev.yuletide.Yuletide;
import dev.yuletide.santa.SantaManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class ChristmasCommand extends CommandBase {
	@Override
	protected int execute(CommandSourceStack source, String[] args) {
		if (args.length == 0) {
			reply(source, "usage.christmas");
			return OK;
		}
		switch (args[0].toLowerCase()) {
			case "santa" -> santa(source, args);
			case "reload" -> {
				if (!isOp(source)) {
					reply(source, "general.no-permission");
				} else {
					Yuletide.reload(source.getServer());
					reply(source, "general.config-reloaded");
				}
			}
			default -> reply(source, "usage.christmas");
		}
		return OK;
	}

	private void santa(CommandSourceStack source, String[] args) {
		if (!isOp(source)) {
			reply(source, "general.no-permission");
			return;
		}
		SantaManager santa = Yuletide.SANTA;
		String sub = args.length > 1 ? args[1].toLowerCase() : "";
		switch (sub) {
			case "start" -> start(source, args);
			case "stop" -> {
				if (!santa.isRunning()) {
					reply(source, "santa.event-not-running");
				} else {
					santa.stop(true);
				}
			}
			case "teleport" -> {
				ServerPlayer player = source.getPlayer();
				if (player == null) {
					reply(source, "general.players-only");
				} else if (!santa.teleportToSanta(player)) {
					reply(source, "santa.event-not-running");
				} else {
					reply(source, "santa.teleported-to-santa");
				}
			}
			default -> reply(source, "usage.santa");
		}
	}

	private void start(CommandSourceStack source, String[] args) {
		SantaManager santa = Yuletide.SANTA;
		if (santa.isRunning()) {
			reply(source, "santa.event-already-running");
			return;
		}
		int next = 2;
		int seconds = Yuletide.CONFIG.config().integer("santa-claus.default-duration", 600);
		if (args.length > next && SantaManager.TIME_PATTERN.matcher(args[next]).matches()) {
			seconds = SantaManager.parseTime(args[next]);
			next++;
		}
		if (args.length > next && args[next].equalsIgnoreCase("random")) {
			if (!santa.startRandom(source.getServer(), seconds)) {
				reply(source, "santa.no-spawn-locations");
			}
			return;
		}
		ServerLevel level;
		Vec3 pos;
		if (args.length >= next + 4) {
			level = SantaManager.findLevel(source.getServer(), args[next]);
			if (level == null) {
				reply(source, "santa.invalid-world", "world", args[next]);
				return;
			}
			try {
				pos = new Vec3(Double.parseDouble(args[next + 1]), Double.parseDouble(args[next + 2]), Double.parseDouble(args[next + 3]));
			} catch (NumberFormatException e) {
				reply(source, "santa.invalid-coordinates", "input", String.join(" ", args[next + 1], args[next + 2], args[next + 3]));
				return;
			}
		} else {
			ServerPlayer player = source.getPlayer();
			if (player == null) {
				reply(source, "santa.console-needs-coordinates");
				reply(source, "usage.santa");
				return;
			}
			level = player.level();
			pos = player.position();
		}
		if (!santa.start(level, pos, seconds)) {
			reply(source, "santa.spawn-failed");
		}
	}

	@Override
	protected List<String> suggest(CommandSourceStack source, String[] args) {
		List<String> out = new ArrayList<>();
		boolean op = isOp(source);
		if (args.length == 1) {
			out.add("snow");
			if (op) {
				out.add("santa");
				out.add("reload");
			}
		} else if (op && args[0].equalsIgnoreCase("santa")) {
			if (args.length == 2) {
				out.addAll(List.of("start", "stop", "teleport"));
			} else if (args[1].equalsIgnoreCase("start")) {
				if (args.length == 3) {
					out.addAll(List.of("5m", "10m", "30m", "1h", "random"));
					out.addAll(SantaManager.worldNames(source.getServer()));
				} else if (args.length == 4 && SantaManager.TIME_PATTERN.matcher(args[2]).matches()) {
					out.add("random");
					out.addAll(SantaManager.worldNames(source.getServer()));
				}
			}
		}
		return out;
	}
}
