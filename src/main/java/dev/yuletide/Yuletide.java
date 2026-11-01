package dev.yuletide;

import dev.yuletide.config.ConfigManager;
import dev.yuletide.text.Messages;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Yuletide implements ModInitializer {
	public static final String MOD_ID = "yuletide";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final ConfigManager CONFIG = new ConfigManager();
	public static final Messages MESSAGES = new Messages(CONFIG);

	@Override
	public void onInitialize() {
		CONFIG.load();
		LOGGER.info("Yuletide loaded");
	}
}
