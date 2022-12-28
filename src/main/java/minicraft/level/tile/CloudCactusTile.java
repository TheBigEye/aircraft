package minicraft.level.tile;

import minicraft.core.Game;
import minicraft.core.io.Settings;
import minicraft.core.io.Sound;
import minicraft.entity.Direction;
import minicraft.entity.Entity;
import minicraft.entity.mob.Mob;
import minicraft.entity.mob.Player;
import minicraft.entity.mob.boss.AirWizard;
import minicraft.entity.particle.SmashParticle;
import minicraft.entity.particle.TextParticle;
import minicraft.gfx.Color;
import minicraft.gfx.Screen;
import minicraft.gfx.Sprite;
import minicraft.item.Item;
import minicraft.item.ToolItem;
import minicraft.item.ToolType;
import minicraft.level.Level;

public class CloudCactusTile extends Tile {
    private static Sprite sprite = new Sprite(27, 24, 2, 2, 1);

    protected CloudCactusTile(String name) {
        super(name, sprite);
    }

    private final String baseTile = "Ferrosite";
    @Override
    public boolean mayPass(Level level, int x, int y, Entity entity) {
        return entity instanceof AirWizard;
    }

    @Override
    public boolean hurt(Level level, int x, int y, Mob source, int dmg, Direction attackDir) {
        hurt(level, x, y, 0);
        return true;
    }

    @Override
    public boolean interact(Level level, int xt, int yt, Player player, Item item, Direction attackDir) {
        if (Game.isMode("Creative"))
            return false; // go directly to hurt method
        if (item instanceof ToolItem) {
            ToolItem tool = (ToolItem) item;
            if (tool.type == ToolType.Pickaxe) {
                if (player.payStamina(6 - tool.level) && tool.payDurability()) {
                    hurt(level, xt, yt, 1);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void hurt(Level level, int x, int y, int dmg) {
        int damage = level.getData(x, y) + dmg;
        int health = 10;
        if (Game.isMode("Creative"))
            dmg = damage = health;
        level.add(new SmashParticle(x * 16, y * 16));
        Sound.genericHurt.playOnGui();

        level.add(new TextParticle("" + dmg, x * 16 + 8, y * 16 + 8, Color.RED));
        if (damage >= health) {
            level.setTile(x, y, Tiles.get(baseTile));
        } else {
            level.setData(x, y, damage);
        }
    }

    @Override
    public void render(Screen screen, Level level, int x, int y) {
        Tiles.get(baseTile).render(screen, level, x, y);
        sprite.render(screen, x << 4, y << 4);
    }

    @Override
    public void bumpedInto(Level level, int x, int y, Entity entity) {
        if (entity instanceof AirWizard || Settings.get("diff").equals("Peaceful")) {
            return; // Cannot do damage
        }

        if (entity instanceof Mob) {
            ((Mob) entity).hurt(this, x, y, random.nextInt(2) + Settings.getIdx("diff"));
        }
    }
}
