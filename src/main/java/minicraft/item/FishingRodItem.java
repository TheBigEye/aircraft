package minicraft.item;

import minicraft.core.Game;
import minicraft.entity.Direction;
import minicraft.entity.mob.Player;
import minicraft.graphic.Sprite;
import minicraft.level.Level;
import minicraft.level.tile.Tile;
import minicraft.level.tile.Tiles;

import java.util.ArrayList;

public class FishingRodItem extends Item {

    protected static ArrayList<Item> getAllInstances() {
        ArrayList<Item> items = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            items.add(new FishingRodItem(i));
        }

        return items;
    }

    private int uses = 0; // the more uses, the higher the chance of breaking
    public int level; // the higher the level the lower the chance of breaking

    /*
     * these numbers are a bit confusing, so here's an explanation if you want to
     * know the percent chance of a category (let's say tool, which is third) you
     * have to subtract 1 + the "tool" number from the number before it (for the
     * first number subtract from 100)
     */
    private static final int[][] LEVEL_CHANCES = {
    		{ 44, 14, 9, 4 }, // they're in the order "fish", "junk", "tools", "rare"
            { 24, 14, 9, 4 }, // iron has very high chance of fish
            { 59, 49, 9, 4 }, // gold has very high chance of tools
            { 79, 69, 59, 4 } // gem has very high chance of rare items
    };

    private static final String[] LEVEL_NAMES = { "Wood", "Iron", "Gold", "Gem" };

    public FishingRodItem(int level) {
        super(LEVEL_NAMES[level] + " Fishing rod", new Sprite(level, 11, 0));
        this.level = level;
    }

    public static int getChance(int idx, int level) {
        return LEVEL_CHANCES[level][idx];
    }

    @Override
    public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, Direction attackDir) {
    	// Make sure not to use it if swimming
        if (tile == Tiles.get("Water") && !player.isSwimming()) {
            uses++;
            player.isFishing = true;
            player.fishingLevel = this.level;
            return true;
        }

        return false;
    }

    @Override
    public boolean canAttack() {
        return false;
    }

    @Override
    public boolean isDepleted() {
    	// breaking is random, the lower the level, and the more times you use it, the higher the chance
        if (random.nextInt(100) > 120 - uses + level * 6) {
            Game.notifications.add("Your Fishing rod broke.");
            return true;
        }
        return false;
    }

    @Override
    public Item clone() {
        FishingRodItem item = new FishingRodItem(this.level);
        item.uses = this.uses;
        return item;
    }
}
