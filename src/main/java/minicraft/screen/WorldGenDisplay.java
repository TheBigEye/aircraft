package minicraft.screen;

import minicraft.core.Game;
import minicraft.core.io.Localization;
import minicraft.core.io.Settings;
import minicraft.graphic.Color;
import minicraft.graphic.Font;
import minicraft.graphic.Screen;
import minicraft.screen.entry.InputEntry;
import minicraft.screen.entry.SelectEntry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalLong;
import java.util.regex.Pattern;

public class WorldGenDisplay extends Display {

	private static final String worldNameRegex = "[a-zA-Z0-9 ]+";

	private static InputEntry worldSeed = new InputEntry("World Seed", "[-!\"#%/()=+,a-zA-Z0-9]+", 20, true);

	private static final String worldNameHelp =
			"it seems you've set letters as the controls to move the cursor up and down, which is probably annoying." +
			"This can be changed in the key binding menu as the \"cursor-XXX\" keys. For now, to type the letter" +
			"instead of moving the cursor, hold the shift key while typing.";

	public static OptionalLong getSeed() {
		String seedString = worldSeed.getUserInput();

		if (seedString.isEmpty()) {
			return OptionalLong.empty();
		}

		// If the seed is only numbers, just use numbers
		if (Pattern.matches("[-]?[0-9]*", seedString)) {
			return OptionalLong.of(Long.parseLong(seedString));
		} else {
			// If the seed is some combination of numbers/letters, hash them into a floating point number
			long seed = 1125899906842597L; // rather large prime number
			int len = seedString.length();

			for (int i = 0; i < len; i++) {
				seed = 31 * seed + seedString.charAt(i);
			}

			return OptionalLong.of(seed);
		}
	}

	public static InputEntry makeWorldNameInput(String prompt, List<String> takenNames, String initValue) {
		return new InputEntry(prompt, worldNameRegex, 28, initValue, false) {
			@Override
			public boolean isValid() {
				if (!super.isValid()){
					return false;
                }
				String name = getUserInput();
				for (String other : takenNames) {
					if (other.equalsIgnoreCase(name)) {
						return false;
                    }
                }
				return true;
			}

			@Override
			public String getUserInput() {
				return super.getUserInput();
			}
		};
	}

	public WorldGenDisplay() {
		super(true);

		InputEntry nameField = makeWorldNameInput("- ", WorldSelectDisplay.getWorldNames(), Localization.getLocalized("New World"));

		SelectEntry nameHelp = new SelectEntry("Trouble with world name?", () -> Game.setDisplay(new BookDisplay(worldNameHelp))) {
			@Override
			public int getColor(boolean isSelected) {
				return Color.get(1, 204);
			}
		};

		nameHelp.setVisible(false);

		HashSet<String> controls = new HashSet<>();
		controls.addAll(Arrays.asList(Game.input.getMapping("cursor-up").split("/")));
		controls.addAll(Arrays.asList(Game.input.getMapping("cursor-down").split("/")));
		for (String key : controls) {
			if (key.matches("^\\w$")) {
				nameHelp.setVisible(true);
				break;
			}
		}

		worldSeed = new InputEntry("World Seed", "[-!\"#%/()=+,a-zA-Z0-9]+", 20, true) {
			@Override
			public boolean isValid() {
				return true;
			}
		};

		menus = new Menu[] {
            new Menu.Builder(false, 10, RelPos.LEFT, nameField, nameHelp,
                    Settings.getEntry("mode"),
                    Settings.getEntry("cheats"),
                    Settings.getEntry("scoretime"),

                    new SelectEntry("Create World", () -> {
                        if (!nameField.isValid()) {
                            return;
                        }
                        WorldSelectDisplay.setWorldName(nameField.getUserInput(), false);
                        Game.setDisplay(new LoadingDisplay());
                    }) {

                @Override
                public void render(Screen screen, int x, int y, boolean isSelected) {
                    Font.draw(toString(), screen, x, y, Color.CYAN);
                }
            },

            Settings.getEntry("size"),
            Settings.getEntry("theme"),
            Settings.getEntry("type"),
            worldSeed)
            .setDisplayLength(8).setScrollPolicies(0.8F, false).setTitle("World Gen Options")
            .createMenu()
        };
	}

	@Override
	public void init(Display parent) {
		super.init(parent instanceof TitleDisplay ? parent : new TitleDisplay());
	}
}
