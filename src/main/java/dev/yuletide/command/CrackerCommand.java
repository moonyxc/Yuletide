package dev.yuletide.command;

import dev.yuletide.Yuletide;
import dev.yuletide.cracker.CrackerManager;
import dev.yuletide.util.Inventories;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CrackerCommand extends CommandBase {
	private static final List<String> AMOUNTS = List.of("1", "5", "10", "32", "64");

	@Override
	protected int execute(CommandSourceStack source, String[] args) {
		String sub = args.length > 0 ? args[0].toLowerCase() : "";
		switch (sub) {
			case "list" -> list(source);
			case "give" -> {
				if (!isOp(source)) {
					reply(source, "general.no-permission");
				} else if (args.length < 3) {
					reply(source, "usage.cracker");
				} else {
					give(source, source.getPlayer(), args[1], args[2], args.length > 3 ? args[3] : null);
				}
			}
			case "get" -> {
				ServerPlayer player = source.getPlayer();
				if (!isOp(source)) {
					reply(source, "general.no-permission");
				} else if (player == null) {
					reply(source, "general.players-only");
				} else if (args.length < 2) {
					reply(source, "usage.cracker");
				} else {
					give(source, player, player.getGameProfile().name(), args[1], args.length > 2 ? args[2] : null);
				}
			}
			default -> reply(source, "usage.cracker");
		}
		return OK;
	}

	private void list(CommandSourceStack source) {
		reply(source, "crackers.list-header");
		for (String id : Yuletide.CRACKERS.types().keySet()) {
			reply(source, "crackers.list-entry", "cracker", id);
		}
	}

	private void give(CommandSourceStack source, ServerPlayer sender, String targetName, String typeId, String amountText) {
		ServerPlayer target = onlinePlayer(source, targetName);
		if (target == null) {
			reply(source, "general.player-not-found", "player", targetName);
			return;
		}
		CrackerManager.Type type = Yuletide.CRACKERS.type(typeId);
		if (type == null) {
			reply(source, "crackers.cracker-not-found", "cracker", typeId);
			return;
		}
		int amount = 1;
		if (amountText != null) {
			try {
				amount = Integer.parseInt(amountText);
			} catch (NumberFormatException e) {
				reply(source, "general.invalid-number", "input", amountText);
				return;
			}
			if (amount < 1 || amount > 64) {
				reply(source, "crackers.invalid-amount");
				return;
			}
		}
		ItemStack stack = Yuletide.CRACKERS.create(type, amount);
		if (!Inventories.canFit(target, stack, amount)) {
			reply(source, "crackers.inventory-full");
			return;
		}
		Inventories.give(target, stack);
		String senderName = sender == null ? source.getTextName() : sender.getGameProfile().name();
		if (target != sender) {
			reply(source, "crackers.given-to-player", "amount", String.valueOf(amount), "cracker", type.id(), "player", target.getGameProfile().name());
		}
		target.sendSystemMessage(Yuletide.MESSAGES.get("crackers.received-from-player",
			"amount", String.valueOf(amount), "cracker", type.id(), "player", senderName));
	}

	@Override
	protected List<String> suggest(CommandSourceStack source, String[] args) {
		List<String> out = new ArrayList<>();
		boolean op = isOp(source);
		if (args.length == 1) {
			out.add("list");
			if (op) {
				out.add("give");
				out.add("get");
			}
		} else if (op) {
			String sub = args[0].toLowerCase();
			if (sub.equals("give")) {
				if (args.length == 2) {
					out.addAll(onlineNames(source));
				} else if (args.length == 3) {
					out.addAll(Yuletide.CRACKERS.types().keySet());
				} else if (args.length == 4) {
					out.addAll(AMOUNTS);
				}
			} else if (sub.equals("get")) {
				if (args.length == 2) {
					out.addAll(Yuletide.CRACKERS.types().keySet());
				} else if (args.length == 3) {
					out.addAll(AMOUNTS);
				}
			}
		}
		return out;
	}
}
