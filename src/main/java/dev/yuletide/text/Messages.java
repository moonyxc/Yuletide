package dev.yuletide.text;

import dev.yuletide.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;

/** Nachrichten aus messages.json mit Prefix und Platzhaltern. */
public final class Messages {
	private final ConfigManager configs;

	public Messages(ConfigManager configs) {
		this.configs = configs;
	}

	/** @param placeholders abwechselnd Name und Wert, z. B. {@code "player", name} */
	public Component get(String key, String... placeholders) {
		String template = configs.messages().str(key, configs.fallbackMessages().str(key, null));
		if (template == null) {
			return Text.legacy("&cMessage not found: " + key);
		}
		String prefix = configs.messages().str("prefix", configs.fallbackMessages().str("prefix", ""));
		template = template.replace("{prefix}", prefix);
		return Text.legacy(template, map(placeholders));
	}

	/** Wie {@link #get}, aber {@code {markupName}} wird als Legacy-Text mit Farbcodes eingesetzt (nur für vertrauenswürdige Konfigurationswerte). */
	public Component getWithMarkup(String key, String markupName, String markup, String... placeholders) {
		String template = configs.messages().str(key, configs.fallbackMessages().str(key, null));
		if (template == null) {
			return Text.legacy("&cMessage not found: " + key);
		}
		String prefix = configs.messages().str("prefix", configs.fallbackMessages().str("prefix", ""));
		template = template.replace("{prefix}", prefix).replace("{" + markupName + "}", markup);
		return Text.legacy(template, map(placeholders));
	}

	public static Map<String, String> map(String... placeholders) {
		Map<String, String> map = new HashMap<>();
		for (int i = 0; i + 1 < placeholders.length; i += 2) {
			map.put(placeholders[i], placeholders[i + 1]);
		}
		return map;
	}
}
