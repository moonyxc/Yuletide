package dev.yuletide;

import dev.yuletide.calendar.CalendarManager;
import dev.yuletide.command.AdventCalendarCommand;
import dev.yuletide.command.ChristmasCommand;
import dev.yuletide.santa.SantaManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import dev.yuletide.config.ConfigManager;
import dev.yuletide.gift.GiftManager;
import dev.yuletide.text.Messages;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Yuletide implements ModInitializer {
	public static final String MOD_ID = "yuletide";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ConfigManager CONFIG = new ConfigManager();
	public static final Messages MESSAGES = new Messages(CONFIG);
	public static final GiftManager GIFTS = new GiftManager();
	public static final CalendarManager CALENDAR = new CalendarManager(CONFIG, MESSAGES, GIFTS);

	public static final SantaManager SANTA = new SantaManager();

	@Override
	public void onInitialize() {
		CONFIG.load();

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			new AdventCalendarCommand().register(dispatcher, "adventcalendar", "ac", "calendar", "advent");
			new ChristmasCommand().register(dispatcher, "christmas", "xmas");
		});

		ServerTickEvents.END_SERVER_TICK.register(SANTA::tick);
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> SANTA.onEntityLoad(entity));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> SANTA.allowDamage(entity));

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			GIFTS.load(server, CONFIG.gifts(), CONFIG.config());
			CALENDAR.start(server);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			SANTA.stop(false);
			CALENDAR.stop();
		});
		LOGGER.info("Yuletide loaded");
	}

	/** Lädt Konfiguration, Geschenke und Kalenderdaten neu (Santa-Event bleibt unberührt). */
	public static void reload(MinecraftServer server) {
		CONFIG.load();
		GIFTS.load(server, CONFIG.gifts(), CONFIG.config());
		CALENDAR.reload();
	}
}
