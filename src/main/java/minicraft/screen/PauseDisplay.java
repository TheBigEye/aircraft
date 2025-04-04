package minicraft.screen;

import minicraft.core.Game;
import minicraft.core.io.InputHandler;
import minicraft.core.io.Localization;
import minicraft.graphic.Color;
import minicraft.saveload.Save;
import minicraft.screen.entry.BlankEntry;
import minicraft.screen.entry.ListEntry;
import minicraft.screen.entry.SelectEntry;
import minicraft.screen.entry.StringEntry;

import java.util.ArrayList;
import java.util.Arrays;

public class PauseDisplay extends Display {

	public PauseDisplay() {
		String upString = Game.input.getMapping("cursor-up") + Localization.getLocalized(" and ") + Game.input.getMapping("cursor-down") + Localization.getLocalized(" to Scroll");
		String selectString = Game.input.getMapping("select") + Localization.getLocalized(": Choose");

		ArrayList<ListEntry> entries = new ArrayList<>(Arrays.asList(
			new BlankEntry(),
			new SelectEntry("Return to Game", () -> Game.setDisplay(null)),
			new SelectEntry("World info", () -> Game.setDisplay(new WorldInfoDisplay())),
			new SelectEntry("Options", () -> Game.setDisplay(new OptionsDisplay())),
			new SelectEntry("Achievements", () -> Game.setDisplay(new AchievementsDisplay()))
		));

		entries.add(new SelectEntry("Save Game", () -> {
			Game.setDisplay(null);
			new Save(WorldSelectDisplay.getWorldName());
		}));


		entries.addAll(Arrays.asList(
			new SelectEntry("Main Menu", () -> {
				ArrayList<ListEntry> items = new ArrayList<>(Arrays.asList(StringEntry.useLines(Localization.getLocalized("Are you sure you want to exit the game?"))));

				items.add(new BlankEntry());
				items.addAll(Arrays.asList(StringEntry.useLines(Color.RED, Localization.getLocalized("All unsaved progress will be lost"))));
				items.add(new BlankEntry());
				items.add(new BlankEntry());
				items.add(new SelectEntry("Cancel", Game::exitDisplay));

				items.add(new SelectEntry(Localization.getLocalized("Quit without saving"), () -> Game.setDisplay(new TitleDisplay())));

				Game.setDisplay(new Display(false, true, new Menu.Builder(true, 8, RelPos.CENTER, items).createMenu()));
			}),

			new BlankEntry(),

			new StringEntry(upString, Color.GRAY),
			new StringEntry(selectString, Color.GRAY)
		));

		menus = new Menu[] {
			new Menu.Builder(true, 4, RelPos.CENTER, entries)
			.setTitle("Paused", Color.YELLOW)
			.createMenu()
		};
	}

	@Override
	public void init(Display parent) {
		super.init(null); // ignore; pause menus always lead back to the game
	}

	@Override
	public void tick(InputHandler input) {
		super.tick(input);
		if (input.getKey("pause").clicked) {
			Game.exitDisplay();
		}
	}
}
