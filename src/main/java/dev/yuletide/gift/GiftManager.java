package dev.yuletide.gift;

import dev.yuletide.Yuletide;
import dev.yuletide.config.Cfg;
import dev.yuletide.util.Commands;
import dev.yuletide.util.Inventories;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Kalender-Geschenke aus gifts.json: Gruppenwahl und Ausgabe. */
public final class GiftManager {
	public static final int DAYS = 25;

	private final Map<Integer, Map<String, List<Gift>>> gifts = new HashMap<>();
	private final Map<String, Integer> priorities = new HashMap<>();
	private final Map<String, String> tags = new HashMap<>();

	public void load(MinecraftServer server, Cfg giftsConfig, Cfg config) {
		gifts.clear();
		priorities.clear();
		tags.clear();
		for (String group : config.keys("gift-groups")) {
			priorities.put(group, config.integer("gift-groups." + group + ".priority", 0));
			String tag = config.str("gift-groups." + group + ".tag", "");
			if (!tag.isEmpty()) {
				tags.put(group, tag);
			}
		}
		priorities.putIfAbsent("default", 0);

		int total = 0;
		for (int day = 1; day <= DAYS; day++) {
			Cfg dayCfg = giftsConfig.section("day-" + day + ".groups");
			if (dayCfg == null) {
				continue;
			}
			Map<String, List<Gift>> groups = new HashMap<>();
			for (String group : dayCfg.keys()) {
				List<Gift> list = new ArrayList<>();
				int index = 0;
				for (Cfg entry : dayCfg.list(group + ".items")) {
					Gift gift = parse(server, entry, "gifts.json day-" + day + "." + group + "[" + index++ + "]");
					if (gift != null) {
						list.add(gift);
					}
				}
				groups.put(group, list);
				total += list.size();
			}
			gifts.put(day, groups);
		}
		Yuletide.LOGGER.info("Loaded {} calendar gifts", total);
	}

	private @Nullable Gift parse(MinecraftServer server, Cfg entry, String context) {
		String type = entry.str("type", "ITEM").toUpperCase();
		switch (type) {
			case "ITEM" -> {
				ItemStack stack = ItemFactory.build(entry, entry.has("item") ? "item" : "material", server.registryAccess(), context);
				return stack == null ? null : Gift.item(stack);
			}
			case "COMMAND" -> {
				String command = entry.str("command", "");
				if (command.isBlank()) {
					Yuletide.LOGGER.warn("[{}] COMMAND gift without 'command' - skipped", context);
					return null;
				}
				return Gift.command(command);
			}
			case "PERMISSION" -> {
				String permission = entry.str("permission", "");
				if (permission.isBlank()) {
					Yuletide.LOGGER.warn("[{}] PERMISSION gift without 'permission' - skipped", context);
					return null;
				}
				return Gift.permission(permission);
			}
			default -> {
				Yuletide.LOGGER.warn("[{}] Unknown gift type '{}' - skipped", context, type);
				return null;
			}
		}
	}

	/** Gültige Gruppe mit der höchsten Priorität; Gruppen ohne Tag gelten für alle. */
	public String getPlayerGroup(ServerPlayer player) {
		String best = "default";
		int bestPriority = Integer.MIN_VALUE;
		for (Map.Entry<String, Integer> entry : priorities.entrySet()) {
			String tag = tags.get(entry.getKey());
			boolean allowed = tag == null || player.entityTags().contains(tag);
			if (allowed && entry.getValue() > bestPriority) {
				best = entry.getKey();
				bestPriority = entry.getValue();
			}
		}
		return best;
	}

	/** Gruppe des Spielers, Fallback {@code default}, sonst leer (keine Vererbung). */
	public List<Gift> getGiftsForDay(int day, ServerPlayer player) {
		Map<String, List<Gift>> groups = gifts.get(day);
		if (groups == null) {
			return List.of();
		}
		List<Gift> list = groups.get(getPlayerGroup(player));
		if (list == null) {
			list = groups.get("default");
		}
		return list == null ? List.of() : list;
	}

	public void give(MinecraftServer server, ServerPlayer player, List<Gift> list) {
		for (Gift gift : list) {
			switch (gift.type()) {
				case ITEM -> Inventories.give(player, gift.stack().copy());
				case COMMAND -> Commands.runAsConsole(server, gift.command(), player);
				case PERMISSION -> player.addTag(gift.permission());
			}
		}
	}
}
