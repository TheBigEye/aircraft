package minicraft.entity.mob;

import minicraft.core.io.Settings;
import minicraft.entity.particle.HeartParticle;
import minicraft.gfx.MobSprite;
import minicraft.item.Items;

public class Cat extends PassiveMob {
	private static MobSprite[][] sprites = MobSprite.compileMobSpriteAnimations(30, 6);

	/**
	 * Creates a Cat.
	 */
	public Cat() {
		super(sprites);
	}

	public void tick() {
	    super.tick();

	    Player player = getClosestPlayer();
	    boolean holdingRawFish = player != null && player.activeItem != null && player.activeItem.name.equals("Raw Fish");

	    // Render heart particles
	    if (Settings.get("particles").equals(true) && holdingRawFish && random.nextInt(6) == 0) {
	        int randX = random.nextInt(10);
	        int randY = random.nextInt(9);
	        level.add(new HeartParticle(x - 9 + randX, y - 12 + randY));
	    }

	    if (holdingRawFish) {
	        int xd = player.x - x;
	        int yd = player.y - y;

	        /// if player is less than 6.25 tiles away, then set move dir towards player
	        int sig0 = 1; // this prevents too precise estimates, preventing mobs from bobbing up and down.
	        xa = ya = 0;

	        if (xd < sig0) xa = -1;
	        else if (xd > sig0) xa = 1;
	        if (yd < sig0) ya = -1;
	        else if (yd > sig0) ya = 1;
	    } else {
	        randomizeWalkDir(false);
	    }
	}

	public void die() {
		int min = 0;
		int max = 0;

		if (Settings.get("diff").equals("Peaceful")) { min = 1; max = 3; }
		if (Settings.get("diff").equals("Easy")) { min = 1; max = 2; }
		if (Settings.get("diff").equals("Normal")) { min = 1; max = 2; }
		if (Settings.get("diff").equals("Hard")) { min = 0; max = 1; }

		dropItem(min, max, Items.get("Raw Fish"));

		super.die();
	}
}