package dev.yuletide.cracker;

import dev.yuletide.Yuletide;
import dev.yuletide.config.Cfg;
import dev.yuletide.gift.ItemFactory;
import dev.yuletide.text.Text;
import dev.yuletide.util.Commands;
import dev.yuletide.util.Inventories;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Knallbonbons: normale Items mit der Komponente {@code cracker_type}; Erkennung erfolgt nie über den Namen. */
public final class CrackerManager {
	public static final String DATA_KEY = "cracker_type";

	public record Type(String id, String name, Item material, int modelData, List<String> lore, boolean glow, List<Cfg> rewards) {
	}

	private final Map<String, Type> types = new LinkedHashMap<>();
	private boolean enabled;

	public void reload() {
		Cfg cfg = Yuletide.CONFIG.config();
		enabled = cfg.bool("crackers.enabled", true);
		types.clear();
		for (String id : cfg.keys("crackers.types")) {
			Cfg section = cfg.section("crackers.types." + id);
			if (section == null) {
				continue;
			}
			Item material = ItemFactory.item(section.str("material", "minecraft:paper"));
			if (material == null) {
				Yuletide.LOGGER.warn("[cracker {}] Unknown material '{}' - using paper", id, section.str("material", ""));
				material = Items.PAPER;
			}
			List<Cfg> rewards = new ArrayList<>();
			for (Cfg reward : section.list("rewards")) {
				if (validReward(reward, id)) {
					rewards.add(reward);
				}
			}
			types.put(id, new Type(id, section.str("name", "&eCracker"), material, section.integer("custom-model-data", 0),
				section.strings("lore"), section.bool("glow", false), rewards));
		}
	}

	private static boolean validReward(Cfg reward, String id) {
		String type = reward.str("type", "").toUpperCase();
		boolean valid = switch (type) {
			case "ITEM" -> {
				if (ItemFactory.item(reward.str("item", "")) == null) {
					Yuletide.LOGGER.warn("[cracker {}] ITEM reward with unknown item '{}' - skipped", id, reward.str("item", ""));
					yield false;
				}
				yield true;
			}
			case "COMMAND" -> !reward.str("command", "").isBlank();
			case "PERMISSION" -> true;
			default -> false;
		};
		if (!valid && !type.equals("ITEM")) {
			Yuletide.LOGGER.warn("[cracker {}] Reward '{}' is invalid or has an unsupported type - skipped", id, type);
		}
		return valid;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Map<String, Type> types() {
		return types;
	}

	public @Nullable Type type(String id) {
		return types.get(id);
	}

	public ItemStack create(Type type, int amount) {
		ItemStack stack = new ItemStack(type.material(), amount);
		stack.set(DataComponents.CUSTOM_NAME, Text.legacy(type.name()).copy().withStyle(s -> s.withItalic(false)));
		if (!type.lore().isEmpty()) {
			stack.set(DataComponents.LORE, new ItemLore(type.lore().stream()
				.<Component>map(line -> Text.legacy(line).copy().withStyle(s -> s.withItalic(false))).toList()));
		}
		if (type.glow()) {
			stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		}
		if (type.modelData() > 0) {
			stack.set(DataComponents.CUSTOM_MODEL_DATA,
				new CustomModelData(List.of((float) type.modelData()), List.of(), List.of(), List.of()));
		}
		CompoundTag tag = new CompoundTag();
		tag.putString(DATA_KEY, type.id());
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	public @Nullable Type typeOf(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		return data.copyTag().getString(DATA_KEY).map(types::get).orElse(null);
	}

	/** Rechtsklick (Luft oder Block): betrifft genau die genutzte Hand, das Event wird nur bei einem Cracker abgebrochen. */
	public InteractionResult use(Player player, Level level, InteractionHand hand) {
		Type type = enabled ? typeOf(player.getItemInHand(hand)) : null;
		if (type == null) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			open(serverPlayer, hand, type);
		}
		return InteractionResult.SUCCESS;
	}

	private void open(ServerPlayer player, InteractionHand hand, Type type) {
		player.getItemInHand(hand).shrink(1);
		player.sendSystemMessage(Yuletide.MESSAGES.getWithMarkup("crackers.opened", "cracker", type.name()));
		for (Cfg reward : type.rewards()) {
			switch (reward.str("type", "").toUpperCase()) {
				case "ITEM" -> {
					ItemStack stack = ItemFactory.build(reward, "item", player.registryAccess(), "cracker " + type.id());
					if (stack != null) {
						Inventories.give(player, stack);
						player.sendSystemMessage(Yuletide.MESSAGES.get("crackers.received-item",
							"item", BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toUpperCase(),
							"amount", String.valueOf(stack.getCount())));
					}
				}
				case "COMMAND" -> {
					Commands.runAsConsole(player.level().getServer(), reward.str("command", ""), player);
					player.sendSystemMessage(Yuletide.MESSAGES.get("crackers.received-command-reward"));
				}
				default -> {
				}
			}
		}
	}
}
