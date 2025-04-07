package minicraft.entity;

import minicraft.core.Game;
import minicraft.core.Updater;
import minicraft.core.io.Settings;
import minicraft.core.io.Sound;
import minicraft.entity.mob.Player;
import minicraft.entity.particle.SplashParticle;
import minicraft.graphic.Rectangle;
import minicraft.graphic.Screen;
import minicraft.graphic.Sprite;
import minicraft.item.BoatItem;
import minicraft.item.Item;
import minicraft.item.PowerGloveItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Boat extends Entity {
    private static Sprite boatSprite = new Sprite(4, 34, 2, 2, 2);

    public Player playerInBoat = null;

    private int exitTimer = 0;
    private int boatHeight = 0;
    private boolean boatAnim = false;
    protected int pushTime = 0;

    private int tickTime =  0;

    private Direction pushDir = Direction.NONE; // the direction to push the furniture

    public Boat() {
        super(1, 1);
    }

    public Boat(int xr, int yr) {
        super(xr, yr);
    }

    @Override
    public void render(Screen screen) {
        int xo = x - 4; // Horizontal
        int yo = (y - 8) - boatHeight; // Vertical

        if (Game.player.equals(playerInBoat)) {
        	switch (Game.player.dir) {
	        	case UP: // if currently attacking upwards...
	        		screen.render(xo + 4, yo, 2 + 34 * 32, 1, 2);
	        		screen.render(xo - 4, yo, 3 + 34 * 32, 1, 2);
	        		screen.render(xo + 4, yo + 8, 2 + 35 * 32, 1, 2);
	        		screen.render(xo - 4, yo + 8, 3 + 35 * 32, 1, 2);
	        		break;

	        	case LEFT: // Attacking to the left... (Same as above)
	        		screen.render(xo + 4, yo, 6 + 34 * 32, 1, 2);
	        		screen.render(xo - 4, yo, 7 + 34 * 32, 1, 2);
	        		screen.render(xo + 4, yo + 8, 6 + 35 * 32, 1, 2);
	        		screen.render(xo - 4, yo + 8, 7 + 35 * 32, 1, 2);
	        		break;

	        	case RIGHT: // Attacking to the right (Same as above)
	        		screen.render(xo + 4, yo, 0 + 34 * 32, 1, 2);
	        		screen.render(xo - 4, yo, 1 + 34 * 32, 1, 2);
	        		screen.render(xo + 4, yo + 8, 0 + 35 * 32, 1, 2);
	        		screen.render(xo - 4, yo + 8, 1 + 35 * 32, 1, 2);
	        		break;

	        	case DOWN: // Attacking downwards (Same as above)
	        		screen.render(xo + 4, yo, 4 + 34 * 32, 1, 2);
	        		screen.render(xo - 4, yo, 5 + 34 * 32, 1, 2);
	        		screen.render(xo + 4, yo + 8, 4 + 35 * 32, 1, 2);
	        		screen.render(xo - 4, yo + 8, 5 + 35 * 32, 1, 2);
	        		break;

	        	case NONE:
	        		break;
        	}
        }  else {
        	boatSprite.render(screen, x - 8, y - 8);
        }
    }


    @Override
    public void tick() {
    	tickTime++;

    	if (tickTime /2 %8 == 0) {
    		if (!boatAnim && boatHeight <= 3) boatHeight++;
    		if (boatHeight == 3) boatAnim = true;

    		if (boatAnim && boatHeight >= 0) boatHeight--;
    		if (boatHeight == 0) boatAnim = false;
    	}

        // moves the furniture in the correct direction.
        move(pushDir.getX(), pushDir.getY());
        pushDir = Direction.NONE;

        if (pushTime > 0) {
            pushTime--; // update pushTime by subtracting 1.
        }

        if (playerInBoat != null) {
            exitTimer--;

            if (exitTimer <= 0 && Game.input.getKey("SHIFT").down) {
                if (Game.player.equals(playerInBoat)) {
                    playerInBoat = null;
                    return;
                }
            }

            double ya = 0;
            double xa = 0;

            if (Game.input.getKey("move-up").down) ya -= 1;
            if (Game.input.getKey("move-down").down) ya += 1;
            if (Game.input.getKey("move-left").down) xa -= 1;
            if (Game.input.getKey("move-right").down) xa += 1;

            // Water particles
            if (Settings.get("particles").equals(true)) {
                int randX = random.nextInt(10);
                int randY = random.nextInt(9);
                level.add(new SplashParticle((x - 8) + randX, (y - 8) + randY));
            }

            move(xa, ya);
            playerInBoat.x = x;
            playerInBoat.y = y;
            playerInBoat.stamina = Player.maxStamina;
        }
    }

    @Override
    public boolean canSwim() {
        return true;
    }

    public boolean use(Player player) {
        if (playerInBoat == null) {
            playerInBoat = player;
            exitTimer = 10;
            return true;
        }
        return false;
    }

    @Override
    public boolean blocks(Entity entity) {
        return true;
    }

    @Override
    public boolean interact(Player player, @Nullable Item item, Direction attackDir) {
    	if (item instanceof PowerGloveItem) {
    		Sound.playAt("genericHurt", this.x, this.y);
    		remove();

    		// put whatever item the player is holding into their inventory
    		if (!Game.isMode("Creative") && player.activeItem != null && !(player.activeItem instanceof PowerGloveItem)) {
    			player.getInventory().add(0, player.activeItem);
    		}

    		// make this the player's current item.
    		player.activeItem = new BoatItem("Boat");
    		return true;
    	}
    	return false;
    }

    public boolean move(double xa, double ya) {
        if (Updater.saving || (xa == 0 && ya == 0)) {
            return true; // pretend that it kept moving
        }
        
		if (tickTime % 2 == 0) {
			return true;
		}

        // used to check if the entity has BEEN stopped, COMPLETELY; below checks for a lack of collision.
        boolean stopped = true;

        if (move2(xa, 0)) stopped = false; // becomes false if horizontal movement was successful.
        if (move2(0, ya)) stopped = false; // becomes false if vertical movement was successful.

        if (!stopped) {
            int xt = x >> 4; // the x tile coordinate that the entity is standing on.
            int yt = y >> 4; // the y tile coordinate that the entity is standing on.

            // Calls the steppedOn() method in a tile's class. (used for tiles like sand (footprints) or lava (burning))
            level.getTile(xt, yt).steppedOn(level, xt, yt, this);
        }

        return !stopped;
    }

    public boolean move2(double xa, double ya) {
        if (xa == 0 && ya == 0) {
            return true; // was not stopped
        }

        boolean interact = true;

        // gets the tile coordinate of each direction from the sprite...
        int xto0 = ((x) - 1) >> 4; // to the left
        int yto0 = ((y) - 1) >> 4; // above
        int xto1 = ((x) + 1) >> 4; // to the right
        int yto1 = ((y) + 1) >> 4; // below

        // gets same as above, but after movement.
        int xt0 = (int) ((x + xa) - 1) >> 4;
        int yt0 = (int) ((y + ya) - 1) >> 4;
        int xt1 = (int) ((x + xa) + 1) >> 4;
        int yt1 = (int) ((y + ya) + 1) >> 4;

        // boolean blocked = false; // if the next tile can block you.
        for (int yt = yt0; yt <= yt1; yt++) { // cycles through y's of tile after movement
            for (int xt = xt0; xt <= xt1; xt++) { // cycles through x's of tile after movement
                if (xt >= xto0 && xt <= xto1 && yt >= yto0 && yt <= yto1) {
                    continue; // skip this position if this entity's sprite is touching it
                }
                if (level.getTile(xt, yt).id != 6) {
                    return false;
                }
            }
        }

        // these lists are named as if the entity has already moved-- it hasn't, though.
        // gets all of the entities that are inside this entity (aka: colliding) before moving.
        List<Entity> wasInside = level.getEntitiesInRect(getBounds());

        int xr = 1;
        int yr = 1;

        // gets the entities that this entity will touch once moved.
        List<Entity> isInside = level.getEntitiesInRect(new Rectangle(x + (int) xa, y + (int) ya, xr * 2, yr * 2, Rectangle.CENTER_DIMS));
        for (int i = 0; interact && i < isInside.size(); i++) {
            /// cycles through entities about to be touched, and calls touchedBy(this) for each of them.
            Entity entity = isInside.get(i);
            if (entity == this) {
                continue; // touching yourself doesn't count XD
            }

            if (entity instanceof Player) {
                touchedBy(entity);
            } else {
                entity.touchedBy(this); // call the method. ("touch" the entity)
            }
        }

        isInside.removeAll(wasInside); // remove all the entities that this one is already touching before moving.
        for (Entity entity : isInside) {
            if (entity == this) {
                continue; // can't interact with yourself LOL
            }

            if (entity.blocks(this)) {
                return false; // if the entity prevents this one from movement, don't move.
            }
        }

        // finally, the entity moves!
        x += (int) xa;
        y += (int) ya;

        return true; // the move was successful.
    }

    /* @Override
    protected void touchedBy(Entity entity) {
        if (level.getTile(this.x, this.y).id != 6) {
            return;
        }
        if (entity instanceof Player) {
            tryPush((Player) entity);
        }
    }
    */

    /*

	public void tryPush(Player player) {
        if (level.getTile(this.x, this.y).id != 6) {
            return;
        }
		if (pushTime == 0) {
			pushDir = player.dir; // Set pushDir to the player's dir.
			pushTime = 10; // Set pushTime to 10.
		}
	}*/

    @Override
    public Boat clone() {
        return new Boat();
    }
}
