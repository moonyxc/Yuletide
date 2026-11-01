package dev.yuletide.command;

import dev.yuletide.Yuletide;
import dev.yuletide.calendar.CalendarManager;
import dev.yuletide.gift.GiftManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public final class AdventCalendarCommand extends CommandBase {
	@Override
	protected int execute(CommandSourceStack source, String[] args) {
		CalendarManager calendar = Yuletide.CALENDAR;
		if (args.length == 0) {
			ServerPlayer player = source.getPlayer();
			if (player == null) {
				reply(source, "general.players-only");
			} else if (!calendar.isEnabled()) {
				reply(source, "advent-calendar.disabled");
			} else {
				calendar.open(player);
			}
			return OK;
		}
		switch (args[0].toLowerCase()) {
			case "reset" -> {
				if (!isOp(source)) {
					reply(source, "general.no-permission");
				} else if (args.length < 3) {
					reply(source, "usage.advent-calendar");
				} else {
					reset(source, args[1], args[2]);
				}
			}
			case "test" -> {
				ServerPlayer player = source.getPlayer();
				if (!isOp(source)) {
					reply(source, "general.no-permission");
				} else if (player == null) {
					reply(source, "general.players-only");
				} else {
					reply(source, calendar.toggleTestMode(player) ? "advent-calendar.test-mode-enabled" : "advent-calendar.test-mode-disabled");
				}
			}
			default -> reply(source, "usage.advent-calendar");
		}
		return OK;
	}

	private void reset(CommandSourceStack source, String playerName, String dayArg) {
		ServerPlayer target = onlinePlayer(source, playerName);
		if (target == null) {
			reply(source, "general.player-not-found", "player", playerName);
			return;
		}
		if (dayArg.equalsIgnoreCase("all")) {
			Yuletide.CALENDAR.resetAll(target.getUUID());
			reply(source, "advent-calendar.reset-all-success", "player", target.getGameProfile().name());
			return;
		}
		int day;
		try {
			day = Integer.parseInt(dayArg);
		} catch (NumberFormatException e) {
			reply(source, "general.invalid-number", "input", dayArg);
			return;
		}
		if (day < 1 || day > GiftManager.DAYS) {
			reply(source, "general.day-out-of-range");
			return;
		}
		Yuletide.CALENDAR.resetDay(target.getUUID(), day);
		reply(source, "advent-calendar.reset-success", "day", String.valueOf(day), "player", target.getGameProfile().name());
	}

	@Override
	protected List<String> suggest(CommandSourceStack source, String[] args) {
		List<String> out = new ArrayList<>();
		if (!isOp(source)) {
			return out;
		}
		if (args.length == 1) {
			out.add("reset");
			out.add("test");
		} else if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
			out.addAll(onlineNames(source));
		} else if (args.length == 3 && args[0].equalsIgnoreCase("reset")) {
			out.add("all");
			for (int i = 1; i <= GiftManager.DAYS; i++) {
				out.add(String.valueOf(i));
			}
		}
		return out;
	}
}
