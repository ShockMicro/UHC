package com.hugman.uhc.game.phase;

import com.hugman.uhc.config.UHCConfig;
import com.hugman.uhc.game.UHCBar;
import com.hugman.uhc.game.UHCLogic;
import com.hugman.uhc.game.UHCSpawner;
import com.hugman.uhc.game.map.UHCMap;
import com.hugman.uhc.module.piece.BlockLootModulePiece;
import com.hugman.uhc.module.piece.BucketBreakModulePiece;
import com.hugman.uhc.module.piece.EntityLootModulePiece;
import com.hugman.uhc.module.piece.ModulePieceManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.WorldBorderS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.*;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.player.PlayerSet;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

import java.util.ArrayList;
import java.util.List;

public class UHCActive {
	public final GameSpace gameSpace;
	private final UHCMap map;
	private final UHCConfig config;
	private final ModulePieceManager modulePieceManager;

	private final PlayerSet participants;

	private final UHCLogic logic;
	private final UHCSpawner spawnLogic;
	private final UHCBar bar;

	private long cagesEndTick;
	private long invulnerabilityEndTick;
	private long peacefulEndTick;
	private long wildEndTick;
	private long shrinkingEndTick;
	private long deathmatchEndTick;
	private long gameCloseTick;

	private boolean invulnerable;

	private UHCActive(GameSpace gameSpace, UHCMap map, UHCConfig config, PlayerSet participants, GlobalWidgets widgets) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.modulePieceManager = new ModulePieceManager(config);

		this.participants = participants;

		this.logic = new UHCLogic(config, participants.size());
		this.spawnLogic = new UHCSpawner(gameSpace, modulePieceManager);
		this.bar = UHCBar.create(widgets);
	}

	public static void open(GameSpace gameSpace, UHCMap map, UHCConfig config) {
		gameSpace.openGame(game -> {
			GlobalWidgets widgets = new GlobalWidgets(game);
			UHCActive active = new UHCActive(gameSpace, map, config, gameSpace.getPlayers(), widgets);

			game.setRule(GameRule.CRAFTING, RuleResult.ALLOW);
			game.setRule(GameRule.PORTALS, RuleResult.DENY);
			game.setRule(GameRule.PVP, RuleResult.ALLOW);
			game.setRule(GameRule.BLOCK_DROPS, RuleResult.ALLOW);
			game.setRule(GameRule.FALL_DAMAGE, RuleResult.ALLOW);
			game.setRule(GameRule.HUNGER, RuleResult.ALLOW);

			game.on(GameOpenListener.EVENT, active::open);
			game.on(GameCloseListener.EVENT, active::close);

			game.on(OfferPlayerListener.EVENT, player -> JoinResult.ok());
			game.on(PlayerAddListener.EVENT, active::addPlayer);
			game.on(PlayerRemoveListener.EVENT, active::removePlayer);

			game.on(GameTickListener.EVENT, active::tick);

			game.on(PlayerDamageListener.EVENT, active::onPlayerDamage);
			game.on(PlayerDeathListener.EVENT, active::onPlayerDeath);

			game.on(EntityDropLootListener.EVENT, active::onMobLoot);
			game.on(BreakBlockListener.EVENT, active::onBlockBroken);
			game.on(ExplosionListener.EVENT, active::onExplosion);
		});
	}

	private void open() {
		ServerWorld world = this.gameSpace.getWorld();

		world.getWorldBorder().setCenter(0, 0);
		world.getWorldBorder().setSize(this.logic.getStartMapSize());
		world.getWorldBorder().setDamagePerBlock(0.5);

		this.cagesEndTick = world.getTime() + this.logic.getInCagesTime();
		this.invulnerabilityEndTick = this.cagesEndTick + this.logic.getInvulnerabilityTime();
		this.peacefulEndTick = this.invulnerabilityEndTick + this.logic.getPeacefulTime();
		this.wildEndTick = this.peacefulEndTick + this.logic.getWildTime();
		this.shrinkingEndTick = this.wildEndTick + this.logic.getShrinkingTime();
		this.deathmatchEndTick = this.shrinkingEndTick + this.logic.getDeathmatchTime();
		this.gameCloseTick = this.deathmatchEndTick + 200;

		this.invulnerable = true;

		int index = 0;
		for(ServerPlayerEntity player : this.participants) {
			player.networkHandler.sendPacket(new WorldBorderS2CPacket(world.getWorldBorder(), WorldBorderS2CPacket.Type.INITIALIZE));

			double theta = ((double) index++ / this.participants.size()) * 2 * Math.PI;

			int x = MathHelper.floor(Math.cos(theta) * (this.logic.getStartMapSize() / 2 - this.config.getMapConfig().getSpawnOffset()));
			int z = MathHelper.floor(Math.sin(theta) * (this.logic.getStartMapSize() / 2 - this.config.getMapConfig().getSpawnOffset()));

			this.spawnLogic.resetPlayer(player, GameMode.ADVENTURE);

			this.spawnLogic.summonPlayerInCageAt(player, x, z);
		}
	}

	private void close() {
		for(ServerPlayerEntity player : this.gameSpace.getPlayers()) {
			player.setGameMode(GameMode.ADVENTURE);
		}
	}

	private void tick() {
		ServerWorld world = this.gameSpace.getWorld();

		// Cage chapter
		if(world.getTime() < this.cagesEndTick) {
			this.bar.tickCages(this.cagesEndTick - world.getTime(), this.logic.getInCagesTime());
			if(world.getTime() == this.cagesEndTick - (logic.getInCagesTime() * 0.8)) {
				this.sendModuleListToChat();
			}
		}
		// Cage chapter ends
		else if(world.getTime() == this.cagesEndTick) {
			this.gameSpace.getPlayers().sendMessage(new TranslatableText("text.uhc.cages_end").formatted(Formatting.AQUA));
			this.spawnLogic.clearCages();
			this.participants.forEach(player -> {
				this.spawnLogic.resetPlayer(player, GameMode.SURVIVAL);
				this.spawnLogic.applyEffects(player, (int) this.shrinkingEndTick);
			});
		}
		// Invulnerable chapter
		else if(world.getTime() < this.invulnerabilityEndTick) {
			this.bar.tickInvulnerable(this.invulnerabilityEndTick - world.getTime(), this.logic.getInvulnerabilityTime());
		}
		// Invulnerable chapter ends
		else if(world.getTime() == this.invulnerabilityEndTick) {
			this.gameSpace.getPlayers().sendMessage(new TranslatableText("text.uhc.invulnerability_end").formatted(Formatting.RED));
			this.invulnerable = false;
		}
		// Peaceful chapter
		else if(world.getTime() < this.peacefulEndTick) {
			this.bar.tickPeaceful(this.peacefulEndTick - world.getTime(), this.logic.getPeacefulTime());
		}
		// Peaceful chapter ends
		else if(world.getTime() == this.peacefulEndTick) {
			this.gameSpace.getPlayers().sendMessage(new TranslatableText("text.uhc.peaceful_end").formatted(Formatting.RED));
			this.invulnerable = false;
		}
		// Wild chapter
		else if(world.getTime() < this.wildEndTick) {
			this.bar.tickWild(this.wildEndTick - world.getTime(), this.logic.getWildTime());
		}
		// Wild chapter ends
		else if(world.getTime() == this.wildEndTick) {
			this.gameSpace.getPlayers().sendMessage(new TranslatableText("text.uhc.wild_end").formatted(Formatting.RED));

			world.getWorldBorder().interpolateSize(this.logic.getStartMapSize(), this.logic.getEndMapSize(), this.logic.getShrinkingTime() * 50L);
			this.gameSpace.getPlayers().forEach(player -> player.networkHandler.sendPacket(new WorldBorderS2CPacket(world.getWorldBorder(), WorldBorderS2CPacket.Type.LERP_SIZE)));
		}
		// Shrinking chapter
		else if(world.getTime() < this.shrinkingEndTick) {
			this.bar.tickShrinking(this.shrinkingEndTick - world.getTime(), this.logic.getShrinkingTime());
		}
		// Shrinking chapter ends
		else if(world.getTime() == this.shrinkingEndTick) {
			this.gameSpace.getPlayers().sendMessage(new TranslatableText("text.uhc.shrinking_end").formatted(Formatting.AQUA));
			world.getWorldBorder().setDamagePerBlock(2.5);
			world.getWorldBorder().setBuffer(0.125);
			this.bar.setDeathmatch();
			this.participants.forEach(player -> this.spawnLogic.applyEffects(player, (int) this.deathmatchEndTick));
		}
		// Game ends
		if(world.getTime() > this.gameCloseTick) {
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	private void addPlayer(ServerPlayerEntity player) {
		player.networkHandler.sendPacket(new WorldBorderS2CPacket(this.gameSpace.getWorld().getWorldBorder(), WorldBorderS2CPacket.Type.INITIALIZE));
		this.setSpectator(player, true);
	}

	private void removePlayer(ServerPlayerEntity player) {
		this.eliminatePlayer(player);
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		this.eliminatePlayer(player);
		return ActionResult.FAIL;
	}

	private void eliminatePlayer(ServerPlayerEntity player) {
		PlayerSet players = this.gameSpace.getPlayers();
		players.sendMessage(new LiteralText("\n").append(new TranslatableText("text.uhc.player_eliminated", player.getDisplayName()).formatted(Formatting.BOLD, Formatting.DARK_RED)).append(new LiteralText("\n")));
		players.sendSound(SoundEvents.ENTITY_WITHER_SPAWN);

		ItemScatterer.spawn(this.gameSpace.getWorld(), player.getBlockPos(), player.inventory);

		this.setSpectator(player, false);

		int survival = 0;
		for(ServerPlayerEntity participant : this.participants) {
			if(participant.interactionManager.getGameMode().isSurvivalLike()) {
				survival++;
			}
		}

		if(survival == 1) {
			for(ServerPlayerEntity participant : this.participants) {
				if(participant.interactionManager.getGameMode().isSurvivalLike()) {
					players.sendMessage(new TranslatableText("text.uhc.player_win", participant.getEntityName()).formatted(Formatting.GOLD));
					this.gameCloseTick = this.gameSpace.getWorld().getTime() + 200;
					break;
				}
			}
		}
	}

	private void setSpectator(ServerPlayerEntity player, boolean moveToCenter) {
		this.spawnLogic.resetPlayer(player, GameMode.SPECTATOR);
		if(moveToCenter) this.spawnLogic.spawnPlayerAtCenter(player);
	}

	private void sendModuleListToChat() {
		if(!this.modulePieceManager.getModules().isEmpty()) {
			MutableText text = new LiteralText("\n").append(new TranslatableText("text.uhc.modules_enabled").formatted(Formatting.GOLD));
			this.modulePieceManager.getModules().forEach(module -> {
				Style style = Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableText(module.getTranslation() + ".description")));
				text.append(new LiteralText("\n  - ").formatted(Formatting.WHITE)).append(Texts.bracketed(new TranslatableText(module.getTranslation()).formatted(Formatting.GREEN)).setStyle(style));
			});
			text.append("\n");
			this.gameSpace.getPlayers().sendMessage(text);
			this.gameSpace.getPlayers().sendSound(SoundEvents.ENTITY_ITEM_PICKUP);
		}
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity entity, DamageSource damageSource, float v) {
		if(this.invulnerable) {
			return ActionResult.FAIL;
		}
		else {
			return ActionResult.SUCCESS;
		}
	}

	public boolean breakIndividualBlock(@Nullable ServerPlayerEntity player, BlockPos pos) {
		if(!this.modulePieceManager.getModules().isEmpty()) {
			for(BlockLootModulePiece piece : this.modulePieceManager.blockLootModulePieces) {
				if(piece.breakBlock(this, player, pos)) return true;
			}
		}
		return false;
	}

	public ActionResult onBlockBroken(@Nullable ServerPlayerEntity player, BlockPos origin) {
		if(!this.modulePieceManager.getModules().isEmpty()) {
			for(BucketBreakModulePiece piece : this.modulePieceManager.bucketBreakModulePieces) {
				if(piece.breakBlock(this, player, origin)) return ActionResult.PASS;
			}
			if(breakIndividualBlock(player, origin)) return ActionResult.PASS;
		}
		return ActionResult.SUCCESS;
	}

	private void onExplosion(List<BlockPos> positions) {
		positions.forEach(pos -> onBlockBroken(null, pos));
	}

	private TypedActionResult<List<ItemStack>> onMobLoot(LivingEntity livingEntity, List<ItemStack> itemStacks) {
		if(!this.modulePieceManager.getModules().isEmpty()) {
			boolean hasCustomDrop = false;
			List<ItemStack> stacks = new ArrayList<>();
			for(EntityLootModulePiece piece : this.modulePieceManager.entityLootModulePieces) {
				if(piece.test(livingEntity)) {
					hasCustomDrop = true;
					stacks.addAll(piece.getLoots(this.gameSpace.getWorld(), livingEntity));
				}
			}
			if(hasCustomDrop) {
				return TypedActionResult.pass(stacks);
			}
		}
		return TypedActionResult.success(itemStacks);
	}
}
