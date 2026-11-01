package dev.yuletide.santa;

import dev.yuletide.Yuletide;
import dev.yuletide.config.Cfg;
import dev.yuletide.text.Text;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Das Weihnachtsmann-Event: höchstens eine laufende Instanz, getaktet über den Server-Tick. */
public final class SantaManager {
	public static final String TAG = "christmas_santa";
	public static final Pattern TIME_PATTERN = Pattern.compile("^\\d+[smh]$");

	private static final List<net.minecraft.world.item.Item> GIFTS = List.of(
		Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.GOLDEN_APPLE,
		Items.ENCHANTED_GOLDEN_APPLE, Items.COOKIE, Items.CAKE, Items.PUMPKIN_PIE);

	private record Trail(ResourceKey<Level> level, BlockPos pos, long expiresAt) {
	}

	private @Nullable MinecraftServer server;
	private @Nullable UUID santa;
	private @Nullable Entity santaRef;
	private int goneTicks;
	private @Nullable ResourceKey<Level> santaLevel;
	private @Nullable BlockPos forcedChunk;
	private int ticksLeft;
	private long tick;
	private final List<Trail> trail = new ArrayList<>();
	private final List<Entity> orphans = new ArrayList<>();

	public boolean isRunning() {
		return santa != null;
	}

	public void tick(MinecraftServer server) {
		this.server = server;
		cleanOrphans();
		if (santa == null) {
			return;
		}
		tick++;
		Entity entity = santaEntity();
		if (entity == null || !entity.isAlive()) {
			if (++goneTicks > 100) {
				Yuletide.LOGGER.info("Santa entity is gone - ending the event");
				stop(true);
			}
			return;
		}
		goneTicks = 0;
		ServerLevel level = (ServerLevel) entity.level();
		Cfg cfg = Yuletide.CONFIG.config();
		removeExpiredTrail(false);

		if (cfg.bool("santa-claus.gift-drop.enabled", true)) {
			int interval = Math.max(1, cfg.integer("santa-claus.gift-drop.interval", 5)) * 20;
			if (tick % interval == 0 && ThreadLocalRandom.current().nextDouble() * 100 < cfg.dbl("santa-claus.gift-drop.chance", 15.0)) {
				dropGift(level, entity);
			}
		}
		if (cfg.bool("santa-claus.snow-trail.enabled", true) && tick % 10 == 0) {
			snowTrail(level, entity, cfg);
		}
		if (--ticksLeft <= 0) {
			stop(true);
		}
	}

	private void dropGift(ServerLevel level, Entity entity) {
		ItemStack stack = new ItemStack(GIFTS.get(ThreadLocalRandom.current().nextInt(GIFTS.size())), 1);
		level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack));
		level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + 1, entity.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.5f);
	}

	private void snowTrail(ServerLevel level, Entity entity, Cfg cfg) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, entity.getX(), entity.getY() + 1, entity.getZ(),
			cfg.integer("santa-claus.snow-trail.particle-amount", 10), 0.5, 0.5, 0.5, 0.01);
		BlockPos at = entity.blockPosition();
		BlockPos below = at.below();
		if (level.getBlockState(below).isFaceSturdy(level, below, Direction.UP) && level.getBlockState(at).isAir()) {
			level.setBlockAndUpdate(at, Blocks.SNOW.defaultBlockState());
			trail.add(new Trail(level.dimension(), at.immutable(), tick + Math.max(0, cfg.integer("santa-claus.snow-trail.fade-delay", 100))));
		}
	}

	private void removeExpiredTrail(boolean all) {
		if (server == null) {
			trail.clear();
			return;
		}
		for (Iterator<Trail> it = trail.iterator(); it.hasNext(); ) {
			Trail t = it.next();
			if (!all && t.expiresAt() > tick) {
				continue;
			}
			it.remove();
			ServerLevel level = server.getLevel(t.level());
			if (level != null && level.getBlockState(t.pos()).is(Blocks.SNOW)) {
				level.setBlockAndUpdate(t.pos(), Blocks.AIR.defaultBlockState());
			}
		}
	}

	private @Nullable Entity santaEntity() {
		if (santaRef != null && !santaRef.isRemoved()) {
			return santaRef;
		}
		if (server == null || santa == null || santaLevel == null) {
			return null;
		}
		ServerLevel level = server.getLevel(santaLevel);
		Entity found = level == null ? null : level.getEntity(santa);
		if (found != null) {
			santaRef = found;
		}
		return found;
	}

	public @Nullable Vec3 position() {
		Entity entity = santaEntity();
		return entity == null ? null : entity.position();
	}

	public @Nullable ServerLevel level() {
		Entity entity = santaEntity();
		return entity == null ? null : (ServerLevel) entity.level();
	}

	/** Entfernt Santa-Wesen, die nach einem Absturz oder Neustart übrig geblieben sind (Fehler 13). */
	public void onEntityLoad(Entity entity) {
		if (entity.entityTags().contains(TAG) && (santa == null || !entity.getUUID().equals(santa))) {
			orphans.add(entity);
		}
	}

	private void cleanOrphans() {
		if (!orphans.isEmpty()) {
			orphans.forEach(Entity::discard);
			orphans.clear();
		}
	}

	public boolean allowDamage(Entity entity) {
		return santa == null || !entity.getUUID().equals(santa);
	}

	/** Startet das Event; liefert {@code false}, wenn bereits eines läuft oder Santa nicht gespawnt werden konnte. */
	public boolean start(ServerLevel level, Vec3 pos, int seconds) {
		if (santa != null) {
			return false;
		}
		Cfg cfg = Yuletide.CONFIG.config();
		Entity entity = createSanta(level, cfg);
		if (entity == null) {
			return false;
		}
		entity.snapTo(pos.x, pos.y, pos.z);
		entity.setInvulnerable(true);
		entity.addTag(TAG);
		entity.setCustomName(Text.legacy(cfg.str("santa-claus.name", "&c&lSanta Claus")));
		entity.setCustomNameVisible(cfg.bool("santa-claus.show-name-tag", true));
		if (entity instanceof Mob mob) {
			mob.setPersistenceRequired();
		}
		BlockPos block = BlockPos.containing(pos);
		level.setChunkForced(block.getX() >> 4, block.getZ() >> 4, true);
		level.getChunk(block.getX() >> 4, block.getZ() >> 4);
		server = level.getServer();
		santa = entity.getUUID();
		santaLevel = level.dimension();
		if (!level.addFreshEntity(entity)) {
			santa = null;
			santaLevel = null;
			level.setChunkForced(block.getX() >> 4, block.getZ() >> 4, false);
			return false;
		}
		ticksLeft = Math.max(1, seconds) * 20;
		santaRef = entity;
		goneTicks = 0;
		tick = 0;
		forcedChunk = block;
		String id = level.dimension().identifier().toString();
		broadcast(Yuletide.MESSAGES.get("santa.event-started", "world", id,
			"x", String.valueOf(block.getX()), "y", String.valueOf(block.getY()),
			"z", String.valueOf(block.getZ()), "time", formatTime(seconds)));
		return true;
	}

	private @Nullable Entity createSanta(ServerLevel level, Cfg cfg) {
		Identifier id = Identifier.tryParse(cfg.str("santa-claus.entity-type", "minecraft:villager").toLowerCase());
		Optional<EntityType<?>> type = id == null ? Optional.empty() : BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t -> (EntityType<?>) t);
		if (type.isEmpty()) {
			Yuletide.LOGGER.warn("Unknown santa-claus.entity-type - using villager");
		}
		Entity entity = type.orElse(EntityType.VILLAGER).create(level, EntitySpawnReason.COMMAND);
		if (entity == null) {
			entity = EntityType.VILLAGER.create(level, EntitySpawnReason.COMMAND);
		}
		if (entity instanceof Villager villager) {
			villager.setVillagerData(villager.getVillagerData()
				.withType(level.registryAccess(), VillagerType.SNOW)
				.withProfession(level.registryAccess(), VillagerProfession.NONE));
		}
		return entity;
	}

	/** Startet an einer zufälligen konfigurierten Position. */
	public boolean startRandom(MinecraftServer server, int seconds) {
		List<Cfg> locations = Yuletide.CONFIG.config().list("santa-claus.spawn-locations");
		List<Object[]> valid = new ArrayList<>();
		for (Cfg location : locations) {
			ServerLevel level = findLevel(server, location.str("world", "minecraft:overworld"));
			if (level != null) {
				valid.add(new Object[] {level, new Vec3(location.dbl("x", 0), location.dbl("y", 64), location.dbl("z", 0))});
			}
		}
		if (valid.isEmpty()) {
			Yuletide.LOGGER.warn("No valid santa-claus.spawn-locations configured for random spawn");
			return false;
		}
		Object[] pick = valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
		if (!start((ServerLevel) pick[0], (Vec3) pick[1], seconds)) {
			return false;
		}
		broadcast(Yuletide.MESSAGES.get("santa.event-started-random"));
		return true;
	}

	public void stop(boolean announce) {
		if (santa == null) {
			return;
		}
		Entity entity = santaEntity();
		removeExpiredTrail(true);
		if (server != null && santaLevel != null && forcedChunk != null) {
			ServerLevel level = server.getLevel(santaLevel);
			if (level != null) {
				level.setChunkForced(forcedChunk.getX() >> 4, forcedChunk.getZ() >> 4, false);
			}
		}
		if (entity != null) {
			entity.discard();
		}
		santa = null;
		santaRef = null;
		santaLevel = null;
		forcedChunk = null;
		if (announce) {
			broadcast(Yuletide.MESSAGES.get("santa.event-stopped"));
		}
	}

	public boolean teleportToSanta(ServerPlayer player) {
		Entity entity = santaEntity();
		if (entity == null) {
			return false;
		}
		player.teleportTo((ServerLevel) entity.level(), entity.getX(), entity.getY(), entity.getZ(),
			java.util.Set.<Relative>of(), player.getYRot(), player.getXRot(), true);
		return true;
	}

	private void broadcast(Component message) {
		if (server != null) {
			server.getPlayerList().broadcastSystemMessage(message, false);
		}
	}

	/** {@code 30s}, {@code 5m}, {@code 1h}; ungültig → 600 (Originalverhalten). */
	public static int parseTime(String input) {
		try {
			int value = Integer.parseInt(input.substring(0, input.length() - 1));
			return switch (input.charAt(input.length() - 1)) {
				case 's' -> value;
				case 'm' -> Math.multiplyExact(value, 60);
				case 'h' -> Math.multiplyExact(value, 3600);
				default -> 600;
			};
		} catch (RuntimeException e) {
			return 600;
		}
	}

	public static String formatTime(int seconds) {
		if (seconds < 60) {
			return seconds + "s";
		}
		return seconds < 3600 ? seconds / 60 + "m" : seconds / 3600 + "h";
	}

	/** Akzeptiert {@code overworld}, {@code minecraft:overworld}, {@code the_nether} usw. */
	public static @Nullable ServerLevel findLevel(MinecraftServer server, String name) {
		Identifier id = Identifier.tryParse(name.contains(":") ? name : "minecraft:" + name);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	public static List<String> worldNames(MinecraftServer server) {
		List<String> names = new ArrayList<>();
		for (ResourceKey<Level> key : server.levelKeys()) {
			names.add(key.identifier().toString());
		}
		return names;
	}
}
