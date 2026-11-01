package dev.yuletide.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.yuletide.Yuletide;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

/** Gemeinsame Basis: Argumente werden wie im Original-Plugin positionsbasiert ausgewertet (inkl. Rechteprüfung mit eigener Meldung). */
public abstract class CommandBase {
	public static final int OK = 1;

	/** Ausführung; {@code args} sind die durch Leerzeichen getrennten Argumente nach dem Befehlsnamen. */
	protected abstract int execute(CommandSourceStack source, String[] args);

	/** Vorschläge für das zuletzt getippte Argument ({@code args} enthält es als letztes Element, ggf. leer). */
	protected abstract List<String> suggest(CommandSourceStack source, String[] args);

	public final void register(CommandDispatcher<CommandSourceStack> dispatcher, String... names) {
		for (String name : names) {
			LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name)
				.executes(ctx -> execute(ctx.getSource(), new String[0]))
				.then(Commands.argument("args", StringArgumentType.greedyString())
					.suggests((ctx, builder) -> suggestions(ctx.getSource(), builder))
					.executes(ctx -> execute(ctx.getSource(), split(StringArgumentType.getString(ctx, "args")))));
			dispatcher.register(root);
		}
	}

	private CompletableFuture<Suggestions> suggestions(CommandSourceStack source, SuggestionsBuilder builder) {
		String remaining = builder.getRemaining();
		int lastSpace = remaining.lastIndexOf(' ');
		String prefix = remaining.substring(lastSpace + 1).toLowerCase();
		String[] args = remaining.split(" ", -1);
		SuggestionsBuilder offset = builder.createOffset(builder.getStart() + lastSpace + 1);
		for (String suggestion : suggest(source, args)) {
			if (suggestion.toLowerCase().startsWith(prefix)) {
				offset.suggest(suggestion);
			}
		}
		return offset.buildFuture();
	}

	private static String[] split(String raw) {
		String trimmed = raw.trim();
		return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
	}

	/** Entspricht Op-Level 2. */
	protected static boolean isOp(CommandSourceStack source) {
		return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
	}

	protected static void reply(CommandSourceStack source, Component message) {
		source.sendSystemMessage(message);
	}

	protected static void reply(CommandSourceStack source, String key, String... placeholders) {
		source.sendSystemMessage(Yuletide.MESSAGES.get(key, placeholders));
	}

	protected static ServerPlayer onlinePlayer(CommandSourceStack source, String name) {
		return source.getServer().getPlayerList().getPlayerByName(name);
	}

	protected static List<String> onlineNames(CommandSourceStack source) {
		return new ArrayList<>(source.getOnlinePlayerNames());
	}
}
