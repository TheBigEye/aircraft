package minicraft.entity.furniture;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Timer;

import minicraft.core.Game;
import minicraft.core.io.Sound;
import minicraft.entity.Direction;
import minicraft.entity.Entity;
import minicraft.entity.mob.Mob;
import minicraft.entity.mob.Player;
import minicraft.gfx.Color;
import minicraft.gfx.Rectangle;
import minicraft.gfx.Screen;
import minicraft.gfx.Sprite;
import minicraft.item.Item;
import minicraft.level.Level;
import minicraft.level.tile.Tile;
import minicraft.level.tile.Tiles;
import minicraft.screen.AchievementsDisplay;

public class Tnt extends Furniture implements ActionListener {
	private static int FUSE_TIME = 90;
	private static int BLAST_RADIUS = 32;
	private static int BLAST_DAMAGE = 75;

	private int damage = 0;
	private int light;
	private int ftik = 0;
	private boolean fuseLit = false;
	private Timer explodeTimer;
	private Level levelSave;

	private final String[] explosionBlacklist = new String[] { "hard rock", "obsidian wall", "raw obsidian", "stairs up", "stairs down" };

	/**
	 * Creates a new tnt furniture.
	 */
	public Tnt() {
		super("Tnt", new Sprite(28, 24, 2, 2, 2), 3, 2);
		fuseLit = false;
		ftik = 0;

		explodeTimer = new Timer(300, this);
	}

	@Override
	public void tick() {
		super.tick();

		if (fuseLit) {
			ftik++;

			light = 2;

			if (ftik >= FUSE_TIME) {
				// blow up
				List<Entity> entitiesInRange = level.getEntitiesInRect(new Rectangle(x, y, BLAST_RADIUS * 2, BLAST_RADIUS * 2, Rectangle.CENTER_DIMS));

				for (Entity e : entitiesInRange) {
					float dist = (float) Math.hypot(e.x - x, e.y - y);
					damage = (int) (BLAST_DAMAGE * (1 - (dist / BLAST_RADIUS))) + 1;

					if (e instanceof Mob && damage > 0) {
						((Mob) e).onExploded(this, damage);
					}

					// Ignite other bombs in range.
					if (e instanceof Tnt) {
						Tnt tnt = (Tnt) e;

						if (!tnt.fuseLit) {
							tnt.fuseLit = true;
							Sound.Furniture_tnt_fuse.play();
							tnt.ftik = FUSE_TIME * 2 / 3;
						}

					}
				}

				int xt = x >> 4;
				int yt = (y - 2) >> 4;

				// Get the tiles that have been exploded.
				Tile[] affectedTiles = level.getAreaTiles(xt, yt, 1);

				// Call the onExplode() event.
				for (int i = 0; i < affectedTiles.length; i++) {
					// This assumes that range is 1.
					affectedTiles[i].hurt(level, xt, yt, damage);
					affectedTiles[i].onExplode(level, xt + i % 3 - 1, yt + i / 3 - 1);

				}

				// Random explode sound
				switch (random.nextInt(4)) {
				case 0: Sound.Furniture_tnt_explode.play(); break;
				case 1: Sound.Furniture_tnt_explode.play(); break;
				case 2: Sound.Furniture_tnt_explode_2.play(); break;
				case 3: Sound.Furniture_tnt_explode_3.play(); break;
				case 4: Sound.Furniture_tnt_explode_4.play(); break;
				default: Sound.Furniture_tnt_explode.play(); break;
				}

				level.setAreaTiles(xt, yt, 1, Tiles.get("explode"), 0, explosionBlacklist);

				levelSave = level;
				explodeTimer.start();
				super.remove();

				if (!Game.isMode("creative")) {
					AchievementsDisplay.setAchievement("minicraft.achievement.demolition", true);
				}

			}
		}
	}

	@Override
	public void render(Screen screen) {
		if (fuseLit) {
			int colFctr = 100 * ((ftik % 15) / 5) + 200;
			col = Color.get(-1, colFctr, colFctr + 100, 555);
		}
		super.render(screen);
	}

	/**
	 * Does the explosion.
	 */
	public void actionPerformed(ActionEvent e) {
		explodeTimer.stop();
		int xt = x >> 4;
				int yt = (y - 2) >> 4;

				if (levelSave.depth != 1 && levelSave.depth != 2) {
					levelSave.setAreaTiles(xt, yt, 1, Tiles.get("hole"), 0, explosionBlacklist);

				} else {
					levelSave.setAreaTiles(xt, yt, 1, Tiles.get("Infinite Fall"), 0, explosionBlacklist);
				}

				levelSave = null;
	}

	@Override
	public boolean interact(Player player, Item heldItem, Direction attackDir) {
		if (!fuseLit) {
			fuseLit = true;
			Sound.Furniture_tnt_fuse.play();
			return true;
		}
		return false;
	}

	@Override
	protected String getUpdateString() {
		String updates = super.getUpdateString() + ";";
		updates += "fuseLit," + fuseLit + ";ftik," + ftik;
		return updates;
	}

	@Override
	protected boolean updateField(String field, String val) {
		if (super.updateField(field, val)) {
			return true;
		}
		switch (field) {
		case "fuseLit": fuseLit = Boolean.parseBoolean(val); return true;
		case "ftik": ftik = Integer.parseInt(val); return true;
		}
		return false;
	}

	@Override
	public int getLightRadius() {
		return light;
	}

}
