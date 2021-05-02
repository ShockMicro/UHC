package com.hugman.uhc.util;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.rule.RuleTest;
import net.minecraft.tag.Tag;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;
import java.util.function.Predicate;

// TODO: remove this when plasmid PR gets merged
public final class BucketScanner {
	private BucketScanner() {
	}

	/**
	 * Finds any 6-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin   the position of the first block
	 * @param depth    the amount of maximum blocks to find
	 * @param ruleTest the rule test for the blocks that can be accepted in the set
	 */
	public static LongSet findSix(BlockPos origin, int depth, RuleTest ruleTest, ServerWorld world, Random random) {
		return findSix(origin, depth, pos -> ruleTest.test(world.getBlockState(pos), random));
	}

	/**
	 * Finds any 6-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin the position of the first block
	 * @param depth  the amount of maximum blocks to find
	 * @param tag    the tag for the blocks that can be accepted in the set
	 */
	public static LongSet findSix(BlockPos origin, int depth, Tag<Block> tag, ServerWorld world) {
		return findSix(origin, depth, pos -> world.getBlockState(pos).isIn(tag));
	}

	/**
	 * Finds any 6-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin the position of the first block
	 * @param depth  the amount of maximum blocks to find
	 * @param block  the block type that can be accepted in the set
	 */
	public static LongSet findSix(BlockPos origin, int depth, Block block, ServerWorld world) {
		return findSix(origin, depth, pos -> world.getBlockState(pos).isOf(block));
	}

	/**
	 * Finds any 6-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin    the position of the first block
	 * @param depth     the amount of maximum blocks to find
	 * @param predicate the predicate for the blocks that can be accepted in the set
	 */
	public static LongSet findSix(BlockPos origin, int depth, Predicate<BlockPos> predicate) {
		LongSet set = new LongArraySet();
		Deque<BlockPos> ends = new ArrayDeque<>();
		ends.push(origin);
		BlockPos.Mutable local = origin.mutableCopy();
		while(depth > 0) {
			if(ends.isEmpty()) {
				return set;
			}
			BlockPos pos = ends.pollLast();
			for(Direction direction : Direction.values()) {
				local.set(pos.offset(direction));
				if(predicate.test(local) && !set.contains(local.asLong()) && !ends.contains(local)) {
					ends.push(local.toImmutable());
				}
			}
			set.add(pos.asLong());
			depth--;
		}
		return set;
	}

	/**
	 * Finds any 18-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin   the position of the first block
	 * @param depth    the amount of maximum blocks to find
	 * @param ruleTest the rule test for the blocks that can be accepted in the set
	 */
	public static LongSet findEighteen(BlockPos origin, int depth, RuleTest ruleTest, ServerWorld world, Random random) {
		return findEighteen(origin, depth, pos -> ruleTest.test(world.getBlockState(pos), random));
	}

	/**
	 * Finds any 18-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin the position of the first block
	 * @param depth  the amount of maximum blocks to find
	 * @param tag    the tag for the blocks that can be accepted in the set
	 */
	public static LongSet findEighteen(BlockPos origin, int depth, Tag<Block> tag, ServerWorld world) {
		return findEighteen(origin, depth, pos -> world.getBlockState(pos).isIn(tag));
	}

	/**
	 * Finds any 18-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin the position of the first block
	 * @param depth  the amount of maximum blocks to find
	 * @param block  the block type that can be accepted in the set
	 */
	public static LongSet findEighteen(BlockPos origin, int depth, Block block, ServerWorld world) {
		return findEighteen(origin, depth, pos -> world.getBlockState(pos).isOf(block));
	}

	/**
	 * Finds any 18-connected blocks and puts them in a {@link LongSet}..
	 *
	 * @param origin    the position of the first block
	 * @param depth     the amount of maximum blocks to find
	 * @param predicate the predicate for the blocks that can be accepted in the set
	 */
	public static LongSet findEighteen(BlockPos origin, int depth, Predicate<BlockPos> predicate) {
		LongSet set = new LongArraySet();
		Deque<BlockPos> ends = new ArrayDeque<>();
		ends.push(origin);
		BlockPos.Mutable local = origin.mutableCopy();
		while(depth > 0) {
			if(ends.isEmpty()) {
				return set;
			}
			BlockPos pos = ends.pollLast();
			for(Direction direction : Direction.values()) {
				local.set(pos.offset(direction));
				if(predicate.test(local) && !set.contains(local.asLong()) && !ends.contains(local)) {
					ends.push(local.toImmutable());
				}
			}
			for(int i = -1; i <= 1; i += 2) {
				for(int j = -1; j <= 1; j += 2) {
					local.set(pos.add(i, j, 0));
					if(predicate.test(local) && !set.contains(local.asLong()) && !ends.contains(local)) {
						ends.push(local.toImmutable());
					}
					local.set(pos.add(0, i, j));
					if(predicate.test(local) && !set.contains(local.asLong()) && !ends.contains(local)) {
						ends.push(local.toImmutable());
					}
					local.set(pos.add(j, 0, i));
					if(predicate.test(local) && !set.contains(local.asLong()) && !ends.contains(local)) {
						ends.push(local.toImmutable());
					}
				}
			}
			set.add(pos.asLong());
			depth--;
		}
		return set;
	}

	/**
	 * Finds any 26-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin   the position of the first block
	 * @param depth    the amount of maximum blocks to find
	 * @param ruleTest the rule test for the blocks that can be accepted in the set
	 */
	public static LongSet findTwentySix(BlockPos origin, int depth, RuleTest ruleTest, ServerWorld world, Random random) {
		return findTwentySix(origin, depth, pos -> ruleTest.test(world.getBlockState(pos), random));
	}

	/**
	 * Finds any 26-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin the position of the first block
	 * @param depth  the amount of maximum blocks to find
	 * @param tag    the tag for the blocks that can be accepted in the set
	 */
	public static LongSet findTwentySix(BlockPos origin, int depth, Tag<Block> tag, ServerWorld world) {
		return findTwentySix(origin, depth, pos -> world.getBlockState(pos).isIn(tag));
	}

	/**
	 * Finds any 26-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin the position of the first block
	 * @param depth  the amount of maximum blocks to find
	 * @param block  the block type that can be accepted in the set
	 */
	public static LongSet findTwentySix(BlockPos origin, int depth, Block block, ServerWorld world) {
		return findTwentySix(origin, depth, pos -> world.getBlockState(pos).isOf(block));
	}

	/**
	 * Finds any 26-connected blocks and puts them in a {@link LongSet}.
	 *
	 * @param origin    the position of the first block
	 * @param depth     the amount of maximum blocks to find
	 * @param predicate the predicate for the blocks that can be accepted in the set
	 */
	public static LongSet findTwentySix(BlockPos origin, int depth, Predicate<BlockPos> predicate) {
		LongSet set = new LongArraySet();
		Deque<BlockPos> ends = new ArrayDeque<>();
		ends.push(origin);
		BlockPos.Mutable local = origin.mutableCopy();
		while(depth > 0) {
			if(ends.isEmpty()) {
				return set;
			}
			BlockPos pos = ends.pollLast();
			for(int x = -1; x <= 1; x++) {
				for(int y = -1; y <= 1; y++) {
					for(int z = -1; z <= 1; z++) {
						if(x == 0 && y == 0 && z == 0) continue;
						local.set(pos.add(x, y, z));
						if(predicate.test(local) && !set.contains(local.asLong()) && !ends.contains(local)) {
							ends.push(local.toImmutable());
						}
					}
				}
			}
			set.add(pos.asLong());
			depth--;
		}
		return set;
	}
}