package minicraft.level.tile;

import minicraft.core.Game;
import minicraft.core.Updater;
import minicraft.entity.Arrow;
import minicraft.entity.Entity;
import minicraft.entity.Fireball;
import minicraft.entity.mob.AirWizard;
import minicraft.entity.mob.Player;
import minicraft.graphic.Sprite;
import minicraft.level.Level;

public class InfiniteFallTile extends Tile {

    protected InfiniteFallTile(String name) {
        super(name, (Sprite) null);
    }

    
    @Override
    public boolean tick(Level level, int xt, int yt) {
        int data = level.getData(xt, yt);
        
        // If the datavalue is zero, or we dont need tick this ...
        if (data <= 0 || random.nextInt(4) != 0) {
        	return false;
        }
        
        // If the datavalue is 1, we place a Cloud tile
        // If the datavalue is 2, we place Ferrosite tile
        level.setTile(xt, yt, Tiles.get(data == 1 ? "Cloud" : "Ferrosite"), 0);
        return true;
    }

 
    @Override
    public boolean mayPass(Level level, int x, int y, Entity entity) {
        if (entity instanceof AirWizard || entity instanceof Arrow || entity instanceof Fireball) {
            return true;
        }

        if (entity instanceof Player) {
            Player player = (Player) entity;
            return player.suitOn || Game.isMode("Creative") || !Game.isMode("Creative") && player.fallWarn;
        }

        return false;
    }

    
    @Override
    public void bumpedInto(Level level, int x, int y, Entity entity) {
        if (entity instanceof Player) {
	        Player player = (Player) entity;
	        if (!player.fallWarn && !Game.isMode("Creative")) {
	        	Updater.notifyAll("¡Watch out so you won't slip and fall!");
	        	player.hurt(this, x, y, 1);
	        	player.fallWarn = true;
	        }
        }
    }
}
