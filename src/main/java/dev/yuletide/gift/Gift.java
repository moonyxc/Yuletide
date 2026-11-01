package dev.yuletide.gift;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record Gift(GiftType type, @Nullable ItemStack stack, @Nullable String command, @Nullable String permission) {
	public static Gift item(ItemStack stack) {
		return new Gift(GiftType.ITEM, stack, null, null);
	}

	public static Gift command(String command) {
		return new Gift(GiftType.COMMAND, null, command, null);
	}

	public static Gift permission(String permission) {
		return new Gift(GiftType.PERMISSION, null, null, permission);
	}
}
