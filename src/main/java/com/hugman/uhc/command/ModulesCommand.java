package com.hugman.uhc.command;

import com.hugman.uhc.config.UHCConfig;
import com.hugman.uhc.module.UHCModule;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import xyz.nucleoid.plasmid.game.manager.GameSpaceManager;
import xyz.nucleoid.plasmid.game.manager.ManagedGameSpace;

import java.util.List;
import java.util.Objects;

public class ModulesCommand {
	public static final SimpleCommandExceptionType NO_MODULES_ACTIVATED = new SimpleCommandExceptionType(new TranslatableText("command.uhc.modules.no_modules_activated"));

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("modules").requires(ModulesCommand::isSourceUHC).executes(ModulesCommand::displayModules));
	}

	public static boolean isSourceUHC(ServerCommandSource source) {
		ManagedGameSpace gameSpace = GameSpaceManager.get().byWorld(source.getWorld());
		if(gameSpace != null) {
			return gameSpace.getSourceConfig().getConfig() instanceof UHCConfig;
		}
		return false;
	}

	private static int displayModules(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ManagedGameSpace gameSpace = GameSpaceManager.get().byWorld(source.getWorld());
		ServerPlayerEntity player = source.getPlayer();
		UHCConfig config = (UHCConfig) Objects.requireNonNull(gameSpace).getSourceConfig().getConfig();
		List<UHCModule> modules = config.modules();
		if(!modules.isEmpty()) {
			ScreenHandlerType<?> type = Registry.SCREEN_HANDLER.get(new Identifier("generic_9x" + MathHelper.clamp(1, MathHelper.ceil((float) modules.size() / 9), 6)));
			SimpleGui gui = new SimpleGui(type, player, false);
			gui.setTitle(new TranslatableText("ui.uhc.modules.title"));
			for(UHCModule module : modules) {
				GuiElementBuilder elementBuilder = new GuiElementBuilder(module.icon())
						.setName(new TranslatableText(module.translation()).formatted(Formatting.BOLD).setStyle(Style.EMPTY.withColor(module.color())))
						.hideFlags();
				for(String s : module.getDescriptionLines()) {
					elementBuilder.addLoreLine(new LiteralText("- ").append(new TranslatableText(s)).formatted(Formatting.GRAY));
				}
				gui.setSlot(modules.indexOf(module), elementBuilder);
			}
			gui.open();
			return Command.SINGLE_SUCCESS;
		}
		else {
			throw NO_MODULES_ACTIVATED.create();
		}
	}
}