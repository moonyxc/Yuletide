package dev.yuletide.calendar;

import dev.yuletide.Yuletide;
import dev.yuletide.config.Cfg;
import dev.yuletide.config.ConfigManager;
import dev.yuletide.gift.Gift;
import dev.yuletide.gift.GiftManager;
import dev.yuletide.gift.ItemFactory;
import dev.yuletide.text.Messages;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.storage.LevelResource;

public final class CalendarManager {
	/** Slot je Tag (Index 0 = Tag 1). */
	private static final int[] DAY_SLOTS = {
		10, 11, 12, 13, 14, 15, 16,
		19, 20, 21, 22, 23, 24, 25,
		28, 29, 30, 31, 32, 33, 34,
		37, 38, 39, 40
	};

	private final ConfigManager configs;
	private final Messages messages;
	private final GiftManager gifts;
	private final CalendarData data = new CalendarData();
	private final Set<UUID> testMode = new HashSet<>();

	private MinecraftServer server;
	private Item claimedItem = Items.YELLOW_WOOL;
	private Item availableItem = Items.GREEN_WOOL;
	private Item lockedItem = Items.RED_WOOL;
	private Item fillItem = Items.RED_STAINED_GLASS_PANE;

	public CalendarManager(ConfigManager configs, Messages messages, GiftManager gifts) {
		this.configs = configs;
		this.messages = messages;
		this.gifts = gifts;
	}

	public static int dayForSlot(int slot) {
		for (int i = 0; i < DAY_SLOTS.length; i++) {
			if (DAY_SLOTS[i] == slot) {
				return i + 1;
			}
		}
		return 0;
	}

	public void start(MinecraftServer server) {
		this.server = server;
		data.load(server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("yuletide_calendar.json"));
		reload();
	}

	public void reload() {
		if (server == null) {
			return;
		}
		data.load(server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("yuletide_calendar.json"));
		Cfg config = configs.config();
		claimedItem = material(config, "advent-calendar.gui.day-item.claimed", Items.YELLOW_WOOL);
		availableItem = material(config, "advent-calendar.gui.day-item.available", Items.GREEN_WOOL);
		lockedItem = material(config, "advent-calendar.gui.day-item.locked", Items.RED_WOOL);
		fillItem = material(config, "advent-calendar.gui.fill-material", Items.RED_STAINED_GLASS_PANE);
	}

	public void stop() {
		data.save();
	}

	private static Item material(Cfg config, String path, Item fallback) {
		return ItemFactory.fallbackStack(config.str(path, ""), "config.json " + path, fallback, 1).getItem();
	}

	private ZoneId zone() {
		String id = configs.config().str("advent-calendar.timezone", "");
		if (id.isBlank()) {
			return ZoneId.systemDefault();
		}
		try {
			return ZoneId.of(id);
		} catch (DateTimeException e) {
			Yuletide.LOGGER.warn("Unknown timezone '{}' - using server time", id);
			return ZoneId.systemDefault();
		}
	}

	public LocalDate today() {
		return LocalDate.now(zone());
	}

	public boolean isEnabled() {
		return configs.config().bool("advent-calendar.enabled", true);
	}

	public boolean isTestMode(ServerPlayer player) {
		return testMode.contains(player.getUUID());
	}

	/** @return neuer Zustand des Testmodus */
	public boolean toggleTestMode(ServerPlayer player) {
		if (!testMode.remove(player.getUUID())) {
			testMode.add(player.getUUID());
			return true;
		}
		return false;
	}

	public boolean isClaimed(ServerPlayer player, int day) {
		return data.isClaimed(today().getYear(), player.getUUID(), day);
	}

	public boolean canClaim(ServerPlayer player, int day) {
		if (isTestMode(player)) {
			return true;
		}
		if (isClaimed(player, day)) {
			return false;
		}
		LocalDate today = today();
		if (today.getMonth() != Month.DECEMBER) {
			return false;
		}
		return configs.config().bool("advent-calendar.allow-previous-days", true) ? day <= today.getDayOfMonth() : day == today.getDayOfMonth();
	}

	public enum ClaimResult { SUCCESS, NOT_AVAILABLE, NO_GIFTS }

	public ClaimResult claim(ServerPlayer player, int day) {
		if (!canClaim(player, day)) {
			return ClaimResult.NOT_AVAILABLE;
		}
		List<Gift> list = gifts.getGiftsForDay(day, player);
		if (list.isEmpty()) {
			Yuletide.LOGGER.warn("No gifts configured for day {} (player {})", day, player.getGameProfile().name());
			return ClaimResult.NO_GIFTS;
		}
		gifts.give(server, player, list);
		data.claim(today().getYear(), player.getUUID(), day);
		return ClaimResult.SUCCESS;
	}

	public int giftCount(int day, ServerPlayer player) {
		return gifts.getGiftsForDay(day, player).size();
	}

	public void resetDay(UUID player, int day) {
		data.reset(today().getYear(), player, day);
	}

	public void resetAll(UUID player) {
		data.resetAll(today().getYear(), player);
	}

	public void open(ServerPlayer player) {
		int rows = Math.clamp(configs.config().integer("advent-calendar.gui.size", 54) / 9, 5, 6);
		MenuType<?> type = rows == 5 ? MenuType.GENERIC_9x5 : MenuType.GENERIC_9x6;
		Component title = dev.yuletide.text.Text.legacy(configs.config().str("advent-calendar.gui.title", "&c&lAdvent Calendar"));
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> {
			SimpleContainer container = new SimpleContainer(rows * 9);
			fill(container, player);
			return new CalendarMenu(type, id, inventory, container, rows, this::onClick);
		}, title));
		player.sendSystemMessage(messages.get("advent-calendar.gui-opened"));
	}

	private void onClick(ServerPlayer player, int day, CalendarMenu menu) {
		if (!isEnabled()) {
			player.sendSystemMessage(messages.get("advent-calendar.disabled"));
			return;
		}
		if (!canClaim(player, day)) {
			player.sendSystemMessage(isClaimed(player, day)
				? messages.get("advent-calendar.already-claimed")
				: messages.get("advent-calendar.not-available-yet", "day", String.valueOf(day)));
			return;
		}
		switch (claim(player, day)) {
			case SUCCESS -> player.sendSystemMessage(messages.get("advent-calendar.claimed-success",
				"day", String.valueOf(day), "amount", String.valueOf(giftCount(day, player))));
			case NO_GIFTS -> player.sendSystemMessage(messages.get("advent-calendar.no-gifts", "day", String.valueOf(day)));
			case NOT_AVAILABLE -> player.sendSystemMessage(messages.get("advent-calendar.not-available-yet", "day", String.valueOf(day)));
		}
		menu.refresh(player, this);
	}

	public void fill(SimpleContainer container, ServerPlayer player) {
		container.clearContent();
		if (configs.config().bool("advent-calendar.gui.fill-empty-slots", true)) {
			for (int i = 0; i < container.getContainerSize(); i++) {
				ItemStack pane = new ItemStack(fillItem);
				pane.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
				container.setItem(i, pane);
			}
		}
		for (int day = 1; day <= GiftManager.DAYS; day++) {
			container.setItem(DAY_SLOTS[day - 1], dayItem(player, day));
		}
	}

	private ItemStack dayItem(ServerPlayer player, int day) {
		String d = String.valueOf(day);
		String status;
		Item item;
		if (isClaimed(player, day)) {
			status = "claimed";
			item = claimedItem;
		} else if (canClaim(player, day)) {
			status = "available";
			item = availableItem;
		} else {
			status = "locked";
			item = lockedItem;
		}
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.CUSTOM_NAME, plain(messages.get("gui.name-" + status, "day", d)));
		stack.set(DataComponents.LORE, new ItemLore(List.of(
			plain(messages.get("gui.status-" + status, "day", d)),
			Component.empty(),
			plain(messages.get("gui.hint-" + status, "day", d))
		)));
		return stack;
	}

	private static Component plain(Component component) {
		return component.copy().withStyle(s -> s.withItalic(false));
	}
}
