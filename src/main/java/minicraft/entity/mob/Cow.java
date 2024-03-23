package minicraft.entity.mob;

import minicraft.core.io.Settings;
import minicraft.graphic.MobSprite;
import minicraft.item.Item;
import minicraft.item.Items;

public class Cow extends PassiveMob {
    private static final MobSprite[][] sprites = MobSprite.compileMobSpriteAnimations(0, 40);
    private static final String[] sounds = new String[] {"cowSay1", "cowSay2", "cowSay3"};

    /**
     * Creates the cow with the right sprites and color.
     */
    public Cow() {
        super(sprites, 5);
    }

    public void tick() {
        super.tick();

		// follows to the player if holds wheat
		followOnHold(Items.get("Wheat"), 3);
        
		// Cow sounds
		if ((tickTime % (random.nextInt(100) + 120) == 0)) {
			doPlaySound(sounds, 8);
		}
    }

    @Override
    public void die() {
        int min = 0, max = 0;      
		String difficulty = (String) Settings.get("diff");

        if (difficulty == "Peaceful" || difficulty == "Easy") { min = 1; max = 3; }
        if (difficulty == "Normal") { min = 1; max = 2; }
        if (difficulty == "Hard") { min = 0; max = 1; }

        dropItem(min, max, new Item[] {
        	Items.get("leather"), Items.get("raw beef") 
        });

        super.die();
    }
}