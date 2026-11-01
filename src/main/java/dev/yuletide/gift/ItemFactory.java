package dev.yuletide.gift;

import dev.yuletide.Yuletide;
import dev.yuletide.config.Cfg;
import dev.yuletide.text.Text;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

/** Baut ItemStacks aus Konfigurationseinträgen; Fehler werden mit Kontext geloggt statt still zu scheitern. */
public final class ItemFactory {
	private ItemFactory() {
	}

	/** Akzeptiert {@code DIAMOND}, {@code diamond} und {@code minecraft:diamond}. */
	public static @Nullable Item item(@Nullable String id) {
		Identifier identifier = identifier(id);
		if (identifier == null) {
			return null;
		}
		Optional<Item> item = BuiltInRegistries.ITEM.getOptional(identifier);
		return item.isPresent() && item.get() != Items.AIR ? item.get() : null;
	}

	public static @Nullable Identifier identifier(@Nullable String id) {
		if (id == null || id.isBlank()) {
			return null;
		}
		return Identifier.tryParse(id.trim().toLowerCase().replace(' ', '_'));
	}

	public static ItemStack fallbackStack(String id, String context, Item fallback, int amount) {
		Item item = item(id);
		if (item == null) {
			Yuletide.LOGGER.warn("[{}] Unknown item '{}' - using {}", context, id, BuiltInRegistries.ITEM.getKey(fallback));
			item = fallback;
		}
		return new ItemStack(item, Math.max(1, amount));
	}

	/** @return null, wenn das Item unbekannt ist (wird geloggt) */
	public static @Nullable ItemStack build(Cfg entry, String itemKey, HolderLookup.Provider registries, String context) {
		String id = entry.str(itemKey, "minecraft:stone");
		Item item = item(id);
		if (item == null) {
			Yuletide.LOGGER.warn("[{}] Unknown item '{}' - entry skipped", context, id);
			return null;
		}
		ItemStack stack = new ItemStack(item, Math.max(1, entry.integer("amount", 1)));
		String name = entry.str("name", "");
		if (!name.isEmpty()) {
			stack.set(DataComponents.CUSTOM_NAME, Text.legacy(name).copy().withStyle(style -> style.withItalic(false)));
		}
		List<String> lore = entry.strings("lore");
		if (!lore.isEmpty()) {
			stack.set(DataComponents.LORE, new ItemLore(lore.stream()
				.<Component>map(line -> Text.legacy(line).copy().withStyle(style -> style.withItalic(false)))
				.toList()));
		}
		ItemEnchantments enchants = enchantments(entry.strings("enchantments"), registries, context);
		if (!enchants.isEmpty()) {
			stack.set(DataComponents.ENCHANTMENTS, enchants);
		}
		ItemEnchantments stored = enchantments(entry.strings("stored-enchantments"), registries, context);
		if (!stored.isEmpty()) {
			stack.set(DataComponents.STORED_ENCHANTMENTS, stored);
		}
		return stack;
	}

	/** Format {@code name:level} bzw. {@code namespace:name:level}; Level 1, wenn nicht angegeben. */
	public static ItemEnchantments enchantments(List<String> specs, HolderLookup.Provider registries, String context) {
		ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
		for (String spec : specs) {
			String idPart = spec;
			int level = 1;
			int colon = spec.lastIndexOf(':');
			if (colon >= 0) {
				try {
					level = Integer.parseInt(spec.substring(colon + 1).trim());
					idPart = spec.substring(0, colon);
				} catch (NumberFormatException ignored) {
				}
			}
			Identifier id = identifier(idPart);
			Optional<Holder.Reference<Enchantment>> holder = id == null ? Optional.empty() : lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id));
			if (holder.isEmpty()) {
				Yuletide.LOGGER.warn("[{}] Unknown enchantment '{}' - ignored", context, spec);
				continue;
			}
			result.set(holder.get(), Math.max(1, level));
		}
		return result.toImmutable();
	}
}
