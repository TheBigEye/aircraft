package minicraft.item;

import java.util.Random;

import minicraft.core.Updater;
import minicraft.core.io.Sound;
import minicraft.entity.Direction;
import minicraft.entity.Entity;
import minicraft.entity.mob.Player;
import minicraft.entity.mob.boss.EyeQueen;
import minicraft.gfx.Sprite;
import minicraft.level.Level;
import minicraft.level.tile.Tile;

public class AmuletItem extends Item {

	private static Random random = new Random();

	public AmuletItem() {
		super("Eye Amulet", new Sprite(11, 38, 0));
	}

	@Override
	public Item clone() {
		return new AmuletItem();
	}

	@Override
	public boolean interactOn(Tile tile, Level level, int xt, int yt, Player player, Direction attackDir) {

		if (random.nextInt(3) == 0) {
			Sound.Amulet_locked.playOnGui();
		}

		if (random.nextInt(3) == 1) {
			Sound.Amulet_locked_2.playOnGui();
		}

		if (random.nextInt(3) == 2) {
			Sound.Amulet_sucess.playOnGui();
			level.add(new EyeQueen(1), player.x, player.y);
			Updater.notifyAll("The eye queen has awakened!");
			Updater.setTime(600000);
		}

		return interact(player, (Entity) null, attackDir);
	}

	@Override
	public boolean interactsWithWorld() {
		return false;
	}

	@Override
	public boolean canAttack() {
		return true;
	}
}
