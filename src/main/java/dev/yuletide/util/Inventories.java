package dev.yuletide.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class Inventories {
	private Inventories() {
	}

	/** Gibt das Item ins Inventar; was nicht passt, wird beim Spieler gedroppt. Große Mengen werden auf Stapel verteilt. */
	public static void give(ServerPlayer player, ItemStack stack) {
		ItemStack remaining = stack.copy();
		while (!remaining.isEmpty()) {
			ItemStack part = remaining.split(Math.min(remaining.getCount(), remaining.getMaxStackSize()));
			player.getInventory().add(part);
			if (!part.isEmpty()) {
				player.drop(part, false);
			}
		}
	}

	/** Prüft, ob {@code count} Stück des Items vollständig ins Hauptinventar passen (auch in Teil-Stapel). */
	public static boolean canFit(ServerPlayer player, ItemStack stack, int count) {
		int needed = count;
		int max = stack.getMaxStackSize();
		for (ItemStack slot : player.getInventory().getNonEquipmentItems()) {
			if (slot.isEmpty()) {
				needed -= max;
			} else if (ItemStack.isSameItemSameComponents(slot, stack)) {
				needed -= Math.max(0, max - slot.getCount());
			}
			if (needed <= 0) {
				return true;
			}
		}
		return false;
	}
}
