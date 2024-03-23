package minicraft.screen;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.tinylog.Logger;

import minicraft.core.Game;
import minicraft.core.Renderer;
import minicraft.core.io.FileHandler;
import minicraft.core.io.Localization;
import minicraft.core.io.Sound;
import minicraft.graphic.Color;
import minicraft.graphic.Font;
import minicraft.graphic.Screen;
import minicraft.graphic.SpriteSheet;
import minicraft.screen.entry.ListEntry;
import minicraft.screen.entry.SelectEntry;

public class TexturePackDisplay extends Display {

	private static final String DEFAULT_TEXTURE_PACK = "Default"; // Default texture :)
	private static final String LEGACY_TEXTURE_PACK = "Legacy"; // Legacy texture :)

	private static final String[] ENTRY_NAMES = new String[] {
		"items.png", // Items sheet (0)
		"tiles.png", // Tiles sheet (1)
		"entities.png", // Entities sheet (2)
		"gui.png", // GUI Elements sheet (3)
		"font.png", // Letters and font sheet (4)
		"background.png" // More GUI Elements sheet (5)
	};

	private static boolean shouldUpdate;

	private static final File location = new File(FileHandler.getSystemGameDir() + "/" + FileHandler.getLocalGameDir() + "/texturepacks");

	/* The texture packs are put in a folder generated by the game called "texturepacks".
	 * Many texture packs can be put according to the number of files.
	 */

	public static List<ListEntry> loadTexturePacks() {
		List<ListEntry> textureList = new ArrayList<>();
		textureList.add(new SelectEntry(TexturePackDisplay.DEFAULT_TEXTURE_PACK, TexturePackDisplay::update, false));
		textureList.add(new SelectEntry(TexturePackDisplay.LEGACY_TEXTURE_PACK, TexturePackDisplay::update, false));

		// Generate texture packs folder
		if (Game.debug && location.mkdirs()) {
			Logger.debug("Created {}, as texture packs folder", location);
		}

		// Read and add the .zip file to the texture pack list.
		for (String fileName : Objects.requireNonNull(location.list())) {
			// Only accept files ending with .zip.
			if (fileName.endsWith(".zip")) {
				textureList.add(new SelectEntry(fileName, TexturePackDisplay::update, false));
			}
		}

		return textureList;
	}

	public TexturePackDisplay() {
		super(true, true, new Menu.Builder(false, 2, RelPos.CENTER, loadTexturePacks())
		.setSize(64, 64)
		.createMenu());
	}

	private static void update() {
		shouldUpdate = true;
		Sound.play("Menu_loaded");
	}

	private void updateSheets(Screen screen) throws IOException {
		try {
			SpriteSheet[] sheets = new SpriteSheet[TexturePackDisplay.ENTRY_NAMES.length];

			if (menus[0].getSelection() == 0) {
				// Load default sprite sheet.
				sheets = Renderer.loadDefaultTextures();

			} else if (menus[0].getSelection() == 1) {
				// Load legacy sprite sheet.
				sheets = Renderer.loadLegacyTextures();

			} else {
				try (ZipFile zipFile = new ZipFile(new File(location, Objects.requireNonNull(menus[0].getCurEntry()).toString()))) {
					for (int i = 0; i < TexturePackDisplay.ENTRY_NAMES.length; i++) {
						ZipEntry entry = zipFile.getEntry(TexturePackDisplay.ENTRY_NAMES[i]);

						if (entry != null) {
							try (InputStream inputEntry = zipFile.getInputStream(entry)) {
								sheets[i] = new SpriteSheet(ImageIO.read(inputEntry));

							} catch (IOException exception) {
								exception.printStackTrace();
								Logger.error("Sprites failure, '{}' has unable to be loaded, aborting ...", TexturePackDisplay.ENTRY_NAMES[i]);
								return;
							}

						} else {
							Logger.debug("Couldn't load sheet {}, ignoring.", TexturePackDisplay.ENTRY_NAMES[i]);
						}
					}

				} catch (IllegalStateException | IOException exception) {
					exception.printStackTrace();
					Logger.error("Could not load texture pack with name {} at {}.", Objects.requireNonNull(menus[0].getCurEntry()).toString(), location);
					return;

				} catch (NullPointerException exception) {
					exception.printStackTrace();
					return;
				}
			}

			Renderer.screen.setSheet(sheets[0], sheets[1], sheets[2], sheets[3], sheets[4], sheets[5]);
			Font.updateCharAdvances(sheets[4]);
		} catch(NullPointerException exception) {
			exception.printStackTrace();
			Logger.error("Changing texture pack failed.");
			return;
		}

		Logger.debug("Changed sprites and texture pack to {}.", () -> Objects.requireNonNull(menus[0].getCurEntry()).toString());

	}


	@Override
	public void render(Screen screen) {
		super.render(screen);

		if (shouldUpdate) {
			shouldUpdate = false;

			try {
				updateSheets(screen);
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		}

		// Title
		Font.drawCentered(Localization.getLocalized("Texture Packs"), screen, Screen.h - 280, Color.YELLOW);

		// Movement instructions
		Font.drawCentered("Use " + Game.input.getMapping("MOVE-DOWN") + ", " + Game.input.getMapping("MOVE-UP") + ", " + Game.input.getMapping("SELECT"), screen, Screen.h - 11, Color.GRAY);

		int h = 2;
		int w = 15;
		int xo = (Screen.w - (w << 3)) / 2;
		int yo = 28;

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				screen.render(xo + (x << 3), yo + (y << 3), x + (y << 5), 0, 3); // Texture pack logo
			}
		}

	}
}