package minicraft.level.tile;

import minicraft.core.Game;
import minicraft.core.io.Sound;
import minicraft.entity.Direction;
import minicraft.entity.Entity;
import minicraft.entity.mob.Player;
import minicraft.graphic.Color;
import minicraft.graphic.ConnectorSprite;
import minicraft.graphic.Screen;
import minicraft.graphic.Sprite;
import minicraft.item.Item;
import minicraft.item.Items;
import minicraft.item.ToolItem;
import minicraft.item.ToolType;
import minicraft.level.Level;

public class CloudTile extends Tile {
	private static final ConnectorSprite sprite = new ConnectorSprite(CloudTile.class, new Sprite(0, 21, 3, 3, 1), new Sprite(5, 21, 2, 2, 1), new Sprite(3, 21, 2, 2, 1)) {

		@Override
		public boolean connectsTo(Tile tile, boolean isSide) { // Cloud tile cannot connect with these tiles
			return tile != Tiles.get("Infinite fall") && tile != Tiles.get("Ferrosite") && tile != Tiles.get("Cloud cactus") && tile != Tiles.get("Goldroot Tree");
		}
	};

	public static int cloudColor(int depth) {
        return Color.get(1, 201, 201, 201);
	}

	protected CloudTile(String name) {
        super(name, sprite);
	}

	@Override
	public void render(Screen screen, Level level, int x, int y) {
		Tiles.get("Ferrosite").render(screen, level, x, y);
		sprite.render(screen, level, x, y);
	}

    @Override
	public void steppedOn(Level level, int x, int y, Entity entity) {
	    /*if (tickCount / 8 % 2 == 0 && Settings.get("particles").equals(true)) {
	        if (entity instanceof Mob && random.nextBoolean()) {
	            int spawnX  = (entity.x - 4) + random.nextInt(8) - random.nextInt(8);
	            int spawnY = (entity.y - 4) + random.nextInt(8) - random.nextInt(8);

	            for (Direction dir : Direction.values()) {
	                Tile neighbour = level.getTile(x + dir.getX(), y + dir.getY());
	                if (neighbour != null && !(neighbour instanceof CloudTile)) {
	                    // Offsets
	                    if (dir.getX() < 0) if ((entity.x % 16) < 8) spawnX += 8 - entity.x % 16;
	                    if (dir.getX() > 0) if ((entity.x % 16) > 7) spawnX -= entity.x % 16 - 8;
	                    if (dir.getY() < 0) if ((entity.y % 16) < 8) spawnY += 8 - entity.y % 16;
	                    if (dir.getY() > 0) if ((entity.y % 16) > 7) spawnY -= entity.y % 16 - 8;
	                }
	            }
	            level.add(new CloudParticle(spawnX, spawnY));
	        }
	    }*/
	}

	@Override
	public boolean interact(Level level, int xt, int yt, Player player, Item item, Direction attackDir) {
		// we don't want the tile to break when attacked with just anything, even in creative mode
		if (item instanceof ToolItem) {
			ToolItem tool = (ToolItem) item;
			if (tool.type == ToolType.Shovel && player.payStamina(5)) {

				Sound.playAt("genericHurt", xt << 4, yt << 4);

				/*
                 If the current level is the sky then when breaking the
                 cloud tile ferrosite appears, if not, hole will appear
				 */
				if (Game.currentLevel == 4) {
					level.setTile(xt, yt, Tiles.get("Infinite fall"), 1);
				} else {
					level.setTile(xt, yt, Tiles.get("Hole"));
				}

				level.dropItem((xt << 4) + 8, (yt << 4) + 8, 1, 2, Items.get("Cloud"));
				return true;
			}
		}
		return false;
	}

}
