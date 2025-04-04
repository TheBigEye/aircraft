package minicraft.core;

import minicraft.core.io.Settings;
import minicraft.entity.furniture.Bed;
import minicraft.entity.mob.Player;
import minicraft.level.Level;
import minicraft.saveload.Load;
import minicraft.screen.*;
import minicraft.util.Action;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

import java.util.Random;

public class World extends Game {
	private World() {}

    // This is to map the level depths to each level's index in Game's levels array.
	// This must ALWAYS be the same length as the levels array, of course.
	public static final int[] indexToDepth = {-3, -2, -1, 0, 1, 2, -4};
	public static final int minLevelDepth, maxLevelDepth;

    static int worldSize = 128;
	public static int worldWidth = worldSize;
	public static int worldHeight = worldSize;

	static int playerDeadTime; // The time after you die before the dead menu shows up.
	static int pendingLevelChange; // Used to determine if the player should change levels or not.

	public static String currentMusicTheme;

	private static long lastWorldExitTime = 0; // When the world exited.
	private static long lastWorldEnterTime = 0; // When the world entered.

    private static long seed;
	private static Random random;

	@Nullable
	public static Action onChangeAction; // Allows action to be stored during a change schedule that should only occur once the screen is blacked out.

	static {
		int min = indexToDepth[0];
		int max = indexToDepth[0];
		for (int depth: indexToDepth) {
			if (depth < min) {
				min = depth;
            }
			if (depth > max) {
				max = depth;
            }
		}
		minLevelDepth = min;
		maxLevelDepth = max;
	}

	/// SCORE MODE

	/** This is for a contained way to find the index in the levels array of a level, based on it's depth. This is also helpful because add a new level in the future could change this. */
	public static int levelIndex(int depth) {
		if (depth > maxLevelDepth) return levelIndex(minLevelDepth);
		if (depth < minLevelDepth) return levelIndex(maxLevelDepth);
		if (depth < -3) return Math.abs(depth) + maxLevelDepth;

		return depth + 3;
	}

	/** This method is used when respawning, and by initWorld to reset the vars. It does not generate any new terrain. */
	public static void resetGame() {
		resetGame(true);
	}

	public static void resetGame(boolean keepPlayer) {
		Logger.debug("Resetting player game info ...");
		playerDeadTime = 0;
		currentLevel = 3;
		Updater.asTick = 0;
		Updater.notifications.clear();

		// Adds a new player
		if (keepPlayer) {
			player = new Player(player, input);
		} else {
			player = new Player(null, input);
        }

		if (levels[currentLevel] == null) return;

		// "shouldRespawn" is false on hardcore, or when making a new world.
		if (PlayerDeathDisplay.shouldRespawn) { // respawn, don't regenerate level.
			Level level = levels[currentLevel];
			player.respawn(level);
			level.add(player); // Adds the player to the current level (always surface here)
		}
	}

	/** This method is used to create a brand new world, or to load an existing one from a file.
	 * For the loading screen updates to work, it it assumed that *this* is called by a thread *other* than the one rendering the current *menu*.
	 **/
	public static void initWorld() { // This is a full reset; everything.
		Logger.debug("Resetting world game info ...");

		PlayerDeathDisplay.shouldRespawn = false;
		resetGame();
		player = new Player(null, input);
		Bed.removePlayers();
		Updater.gameTime = 0;
		Updater.gameSpeed = 1.00f;

		Updater.changeTimeOfDay(Updater.Time.Morning); // Resets tickCount; game starts in the day, so that it's nice and bright.
		gameOver = false;

		levels = new Level[7];

		Updater.scoreTime = (((int) Settings.get("scoretime")) * 60) * Updater.normalSpeed;
		LoadingDisplay.setPercentage(0); // This actually isn't necessary, I think; it's just in case.
		Logger.trace("Initializing world non-client...");

		if (WorldSelectDisplay.hasLoadedWorld()) {
			new Load(WorldSelectDisplay.getWorldName());
		} else {

			worldSize = (int) Settings.get("size");

            seed = WorldGenDisplay.getSeed().orElse(new Random().nextLong());
			random = new Random(seed);

			float loadingIncrement = 100f / (maxLevelDepth - minLevelDepth + 1); // The .002 is for floating point errors, in case they occur.
			for (int i = maxLevelDepth; i >= minLevelDepth; i--) {
				// i = level depth; the array starts from the top because the parent level is used as a reference, so it should be constructed first. It is expected that the highest level will have a null parent.

				Logger.trace("Loading level {} ..." , i);

				LoadingDisplay.setProgressType(Level.getDepthString(i));
				if (i > 0) {
					levels[levelIndex(i)] = new Level(worldSize, worldSize, random.nextLong(), i, null, !WorldSelectDisplay.hasLoadedWorld());
				} else {
					levels[levelIndex(i)] = new Level(worldSize, worldSize, random.nextLong(), i, levels[levelIndex(i + 1)], !WorldSelectDisplay.hasLoadedWorld());
				}

				LoadingDisplay.progress(loadingIncrement);
			}

			Logger.trace("Level loading complete.");

			Level level = levels[currentLevel]; // Sets level to the current level (3; surface)
			Updater.pastFirstDay = false;
			player.findStartPos(level, seed); // Finds the start level for the player
			level.add(player);
		}

		Renderer.readyToRenderGameplay = true;
		PlayerDeathDisplay.shouldRespawn = true;
		Logger.trace("World initialized.");
	}

    public static long getWorldSeed() {
        return seed;
    }

	public static void setWorldSeed(long seed) {
        World.seed = seed;
    }

	/** This method is called when you interact with stairs, this will give you the transition effect. While changeLevel(int) just changes the level. */
	public static void scheduleLevelChange(int dir) {
		scheduleLevelChange(dir, null);
	}

	public static void scheduleLevelChange(int dir, @Nullable Action changeAction) {
		onChangeAction = changeAction;
		pendingLevelChange = dir;
	}

	/** This method changes the level that the player is currently on.
	 * It takes 1 integer variable, which is used to tell the game which direction to go.
	 * For example, 'changeLevel(1)' will make you go up a level,
	 while 'changeLevel(-1)' will make you go down a level. */
	public static void changeLevel(int dir) {
		if (onChangeAction != null) {
			onChangeAction.act();
			onChangeAction = null;
		}

		levels[currentLevel].remove(player); // Removes the player from the current level.

		int nextLevel = currentLevel + dir;
		if (nextLevel <= -1) nextLevel = levels.length-1; // Fix accidental level underflow
		if (nextLevel >= levels.length) nextLevel = 0; // Fix accidental level overflow
		Logger.trace("Setting level from {} to {}", currentLevel, nextLevel);
		currentLevel = nextLevel;

		player.x = ((player.x >> 4) << 4) + 8; // Sets the player's x coord (to center yourself on the stairs)
		player.y = ((player.y >> 4) << 4) + 8; // Sets the player's y coord (to center yourself on the stairs)

		levels[currentLevel].add(player); // Adds the player to the level.

		if (currentLevel == 0) {
			AchievementsDisplay.setAchievement("minicraft.achievement.lowest_caves", true);
		} else if (currentLevel == 6) {
			AchievementsDisplay.setAchievement("minicraft.achievement.obsidian_dungeon", true);
		}
	}

	public static void onWorldExits() {
		lastWorldExitTime = System.currentTimeMillis();
	}

	public static long getLastWorldExitTime() {
		return lastWorldExitTime;
	}

	public static long getLastWorldEnterTime() {
		return lastWorldEnterTime;
	}
}
