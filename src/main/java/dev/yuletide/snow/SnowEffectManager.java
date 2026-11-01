package dev.yuletide.snow;

import dev.yuletide.Yuletide;
import dev.yuletide.config.Cfg;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Schnee nur für Spieler, die ihn aktiviert haben (Zustand bleibt bis zum Serverneustart erhalten). */
public final class SnowEffectManager {
	public static final int MAX_LEVEL = 5;

	private final Map<UUID, Integer> levels = new HashMap<>();
	private final Map<Integer, int[]> levelValues = new HashMap<>();
	private final Set<String> disabledWorlds = new HashSet<>();
	private boolean enabled;

	public void reload() {
		Cfg cfg = Yuletide.CONFIG.config();
		enabled = cfg.bool("snow-effect.enabled", true);
		levelValues.clear();
		for (int level = 1; level <= MAX_LEVEL; level++) {
			levelValues.put(level, new int[] {
				cfg.integer("snow-effect.levels." + level + ".particles-per-tick", 3),
				cfg.integer("snow-effect.levels." + level + ".radius", 15)});
		}
		disabledWorlds.clear();
		for (String world : cfg.strings("snow-effect.disabled-worlds")) {
			disabledWorlds.add(world.contains(":") ? world : "minecraft:" + world);
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public int level(UUID player) {
		return levels.getOrDefault(player, 0);
	}

	/** Schaltet um und liefert den neuen Zustand. */
	public boolean toggle(UUID player) {
		if (levels.remove(player) != null) {
			return false;
		}
		levels.put(player, Math.max(1, Math.min(MAX_LEVEL, Yuletide.CONFIG.config().integer("snow-effect.default-intensity", 3))));
		return true;
	}

	/** Stufe 0 schaltet aus, 1–5 schaltet ein. */
	public void setLevel(UUID player, int level) {
		if (level <= 0) {
			levels.remove(player);
		} else {
			levels.put(player, Math.min(MAX_LEVEL, level));
		}
	}

	public void tick(MinecraftServer server) {
		if (!enabled || levels.isEmpty()) {
			return;
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			Integer level = levels.get(player.getUUID());
			if (level == null || disabledWorlds.contains(player.level().dimension().identifier().toString())) {
				continue;
			}
			int[] values = levelValues.getOrDefault(level, new int[] {3, 15});
			int radius = Math.max(1, values[1]);
			for (int i = 0; i < values[0]; i++) {
				double x = player.getX() + random.nextDouble(-radius, radius);
				double y = player.getY() + random.nextDouble(5, 15);
				double z = player.getZ() + random.nextDouble(-radius, radius);
				player.level().sendParticles(player, ParticleTypes.SNOWFLAKE, true, false, x, y, z, 1, 0, -0.5, 0, 0.02);
			}
		}
	}
}
