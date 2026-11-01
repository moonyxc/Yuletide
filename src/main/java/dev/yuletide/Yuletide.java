package dev.yuletide;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Yuletide implements ModInitializer {
	public static final String MOD_ID = "yuletide";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Yuletide loading");
	}
}
