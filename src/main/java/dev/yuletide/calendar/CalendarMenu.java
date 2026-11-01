package dev.yuletide.calendar;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Eigenes Menü-Objekt: Items sind nicht entnehmbar, der Tag wird aus dem Slot abgeleitet (nicht aus dem Titel). */
public final class CalendarMenu extends ChestMenu {
	public interface DayClickHandler {
		void onClick(ServerPlayer player, int day, CalendarMenu menu);
	}

	private final DayClickHandler handler;

	public CalendarMenu(MenuType<?> type, int containerId, Inventory inventory, SimpleContainer container, int rows, DayClickHandler handler) {
		super(type, containerId, inventory, container, rows);
		this.handler = handler;
	}

	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		if (slotIndex >= 0 && slotIndex < getRowCount() * 9 && player instanceof ServerPlayer serverPlayer) {
			int day = CalendarManager.dayForSlot(slotIndex);
			if (day > 0) {
				handler.onClick(serverPlayer, day, this);
			}
		}
		sendAllDataToRemote();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slotIndex) {
		return ItemStack.EMPTY;
	}

	public void refresh(ServerPlayer player, CalendarManager manager) {
		manager.fill((SimpleContainer) getContainer(), player);
		sendAllDataToRemote();
	}
}
