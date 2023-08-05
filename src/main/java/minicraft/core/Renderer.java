package minicraft.core;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.tinylog.Logger;

import minicraft.core.io.Settings;
import minicraft.entity.Entity;
import minicraft.entity.furniture.Bed;
import minicraft.entity.mob.AirWizard;
import minicraft.entity.mob.EyeQueen;
import minicraft.entity.mob.Player;
import minicraft.graphic.Color;
import minicraft.graphic.Ellipsis;
import minicraft.graphic.Ellipsis.DotUpdater.TickUpdater;
import minicraft.graphic.Ellipsis.SmoothEllipsis;
import minicraft.graphic.Font;
import minicraft.graphic.FontStyle;
import minicraft.graphic.Rectangle;
import minicraft.graphic.Screen;
import minicraft.graphic.Sprite;
import minicraft.graphic.SpriteSheet;
import minicraft.item.Items;
import minicraft.item.PotionType;
import minicraft.item.ToolItem;
import minicraft.item.ToolType;
import minicraft.level.Level;
import minicraft.screen.InfoDisplay;
import minicraft.screen.LoadingDisplay;
import minicraft.screen.RelPos;
import minicraft.util.TimeData;
import minicraft.util.Utils;

/*
* Make the game display logic
*/
public class Renderer extends Game {
	private Renderer() {}

	public static final int HEIGHT = 288; // This is the height of the game * scale
	public static final int WIDTH = 432; // This is the width of the game * scale

	protected static float SCALE = 2; // Scales the window

	private static String levelName; // Used to store the names of the levels in the debug GUI
	public static Screen screen; // Creates the main screen

	protected static final Canvas canvas = new Canvas();

	private static BufferedImage image; // Creates an image to be displayed on the screen.
	public static Screen lightScreen; // Creates a front screen to render the darkness in caves (Fog of war).
	public static boolean readyToRenderGameplay = false;
	public static boolean showDebugInfo = false;
	public static boolean renderRain = false;

	private static final Ellipsis ellipsis = (Ellipsis) new SmoothEllipsis(new TickUpdater());

	public static SpriteSheet[] loadDefaultTextures() {
	    String[] sheetsPaths = {
    		"/resources/textures/items.png",
    		"/resources/textures/tiles.png",
    		"/resources/textures/entities.png",
    		"/resources/textures/gui.png",
    		"/resources/textures/font.png",
    		"/resources/textures/background.png"
	    };
	    
	    SpriteSheet[] sheets = new SpriteSheet[sheetsPaths.length];

	    try {
	        for (int i = 0; i < sheetsPaths.length; i++) {
	        	if (debug) Logger.debug("Loading sprite '{}', for default textures ...", sheetsPaths[i]);
	        	sheets[i] = new SpriteSheet(ImageIO.read(Objects.requireNonNull(Game.class.getResourceAsStream(sheetsPaths[i]))));
	        }
	    }  catch (NullPointerException | IllegalArgumentException | IOException exception) {
	    	Logger.error("Sprites failure, default textures are unable to load an sprite sheet!");
	        exception.printStackTrace();
	        System.exit(-1);
	        return null;
	    }
			
	    return sheets;
	}
		
	public static SpriteSheet[] loadLegacyTextures() {
	    String[] sheetsPaths = {
    		"/resources/textures/legacy/items.png",
    		"/resources/textures/legacy/tiles.png",
    		"/resources/textures/legacy/entities.png",
    		"/resources/textures/legacy/gui.png",
    		"/resources/textures/legacy/font.png",
    		"/resources/textures/legacy/background.png"
	    };

	    SpriteSheet[] sheets = new SpriteSheet[sheetsPaths.length];

	    try {
	        for (int i = 0; i < sheetsPaths.length; i++) {
	        	if (debug) Logger.debug("Loading sprite '{}', for legacy textures ...", sheetsPaths[i]);
	        	sheets[i] = new SpriteSheet(ImageIO.read(Objects.requireNonNull(Game.class.getResourceAsStream(sheetsPaths[i]))));
	        }
	    }  catch (NullPointerException | IllegalArgumentException | IOException exception) {
	    	Logger.error("Sprites failure, legacy textures are unable to load an sprite sheet!");
	        exception.printStackTrace();
	        System.exit(-1);
	        return null;
	    }

	    return sheets;
	}

	static void initScreen() {
		Logger.debug("Initializing game display ...");

		SpriteSheet[] sheets = loadDefaultTextures();
		
		screen = new Screen(sheets[0], sheets[1], sheets[2], sheets[3], sheets[4], sheets[5]);
		lightScreen = new Screen(sheets[0], sheets[1], sheets[2], sheets[3], sheets[4], sheets[5]);
		
		Font.updateCharAdvances(sheets[4]);
		
		image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		screen.pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
		
		Initializer.startCanvasRendering();
		canvas.createBufferStrategy(3);
		canvas.requestFocus();
	}

	/** Renders the current screen. Called in game loop, a bit after tick(). */
	public static void render() {
		if (screen == null) {
			return; // No point in this if there's no gui... :P
		}

		if (readyToRenderGameplay) {
			renderLevel();
			renderGui();
		}

		if (display != null) { // Renders menu, if present.
			display.render(screen);
		}

		if (!canvas.hasFocus()) {
			renderFocusNagger(); // Calls the renderFocusNagger() method, which creates the "Click to Focus" message.
		}

		BufferStrategy bufferStrategy = canvas.getBufferStrategy(); // Creates a buffer strategy to determine how the graphics should be buffered.
		Graphics graphics = bufferStrategy.getDrawGraphics(); // Gets the graphics in which java draws the picture
		graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight()); // draws a rect to fill the whole window (to cover last?)

		// Scale the pixels.
		int windowWidth = getWindowSize().width;
		int windowHeight = getWindowSize().height;

		// Gets the image offset.
		int xo = (canvas.getWidth() - windowWidth) / 2 + canvas.getParent().getInsets().left;
		int yo = (canvas.getHeight() - windowHeight) / 2 + canvas.getParent().getInsets().top;
		graphics.drawImage(image, xo, yo, windowWidth, windowHeight, null); // Draws the image on the window
		graphics.dispose(); // Releases any system items that are using this method. (so we don't have crappy framerates)

		bufferStrategy.show(); // Makes the picture visible. (probably)
	}

	private static void renderLevel() {
		Level level = levels[currentLevel];

		if (level == null) {
			return;
		}

		int xScroll = player.x - (Screen.w / 2); // Scrolls the screen in the x axis.
		int yScroll = player.y - (Screen.h - 8) / 2; // Scrolls the screen in the y axis.

		// Stop scrolling if the screen is at the ...
		if (xScroll < 0) xScroll = 0; // ...Left border.
		if (yScroll < 0) yScroll = 0; // ...Top border.
		if (xScroll > ((level.w << 4) - Screen.w)) xScroll = ((level.w << 4) - Screen.w); // ...right border.
		if (yScroll > ((level.h << 4) - Screen.h)) yScroll = ((level.h << 4) - Screen.h); // ...bottom border.

		if (currentLevel > 3) { // If the current level is higher than 3 (which only the sky level (and dungeon) is)
			for (int y = 0; y < 56; y++) {
				for (int x = 0; x < 96; x++) {
					// Creates the background for the sky (and dungeon) level:
					screen.render(x * 8 - ((xScroll >> 2) & 7), y * 8 - ((yScroll >> 2) & 7), 3 + 23 * 32, 0, 1); // for rain
				}
			}
		}
		
		// for earthquakes
		level.renderBackground(screen, xScroll, yScroll); // Renders current level background
		level.renderSprites(screen, xScroll, yScroll); // Renders level sprites on screen

		// this creates the darkness in the caves
		if (currentLevel != 3 || Updater.tickCount < Updater.dayLength / 4 || Updater.tickCount > Updater.dayLength / 2) {

			// This doesn't mean that the pixel will be black; it means that the pixel will
			// be DARK, by default; lightScreen is about light vs. dark, not necessarily a
			// color. The light level it has is compared with the minimum light values in
			// dither to decide whether to leave the cell alone, or mark it as "dark", which
			// will do different things depending on the game level and time of day.
			lightScreen.clear(0); 

			// Brightens all
			int brightnessMultiplier = player.potionEffects.containsKey(PotionType.Light) ? 12 : 8; 

			// Light sources by a factor of 1.5 when the player has the Light potion effect. (8 above is normal)
			level.renderLight(lightScreen, xScroll, yScroll, brightnessMultiplier); // Finds (and renders) all the light from objects (like the player, lanterns, and lava).
			screen.overlay(lightScreen, currentLevel, xScroll, yScroll); // Overlays the light screen over the main screen.
		}
		
		if (player != null) {
			if (!player.isNiceNight && (currentLevel == 3 || currentLevel == 4)) {
				lightScreen.clear(0); 
	
				// Brightens all
				int brightnessMultiplier = player.potionEffects.containsKey(PotionType.Light) ? 12 : 8; 
				level.renderLight(lightScreen, xScroll, yScroll, brightnessMultiplier); // Finds (and renders) all the light from objects (like the player, lanterns, and lava).
				screen.darkness(lightScreen, currentLevel, xScroll, yScroll);
			}
		}
		
	}

	/**
	 * Renders the main game GUI (hearts, Stamina bolts, name of the current item,
	 * etc.)
	 */
	@SuppressWarnings("unchecked")
	private static void renderGui() {
		
		if (player.activeItem instanceof ToolItem) {
			ToolItem tool = (ToolItem) player.activeItem;
			
			// ARROWS COUNT STATUS
			if (((ToolItem) player.activeItem).type == ToolType.Bow) {
				int arrowsCount = player.getInventory().count(Items.arrowItem);
				
				// Renders the box
				Font.drawBox(screen, (Screen.w) / 2 - 32 - tool.arrowOffset, (Screen.h - 8) - 13, 3, 1);

				if (isMode("Creative") || arrowsCount >= 10000) {
					Font.draw(" x" + "∞", screen, 184 - tool.arrowOffset, Screen.h - 24);
				} else {
					Font.draw(" x" + arrowsCount, screen, 184 - tool.arrowOffset, Screen.h - 24);
				}

				// Displays the arrow icon
				screen.render(20 * 8 + 20 - player.activeItem.arrowOffset, Screen.h - 24, 5 + 3 * 32, 0, 3);
			}
			
			// TOOL DURABILITY STATUS
			int durability = (tool.durability * 100) / (tool.type.durability * (tool.level + 1));
			int color = (int)(durability * 2.55f);

			// Renders the box and the durability text
			Font.drawBox(screen, (Screen.w / 2) + 8 + tool.durabilityOffset, (Screen.h - 8) - 13, 3, 1);
			Font.draw(durability + "%", screen, 221 + tool.durabilityOffset, Screen.h - 24, Color.get(1, 255 - color, color, 0));
		}

		// Shows active item sprite and name in bottom toolbar.
		if (player.activeItem != null) {
			player.activeItem.renderHUD(screen, (Screen.w / 2) - (Font.textWidth(player.activeItem.getDisplayName()) / 2) , Screen.h - 9, Color.GRAY);
		}

		ArrayList<String> permStatus = new ArrayList<>();

		if (Updater.saving) {
			permStatus.add("Saving... " + Math.round(LoadingDisplay.getPercentage()) + "%");
		}

		if (Bed.sleeping()) {
			permStatus.add("Sleeping" + ellipsis.updateAndGet());
		}

		if (!permStatus.isEmpty()) {
			FontStyle style = new FontStyle(Color.WHITE).setYPos(Screen.h / 2 - 25).setRelTextPos(RelPos.TOP).setShadowType(Color.DARK_GRAY, false);
			Font.drawParagraph(permStatus, screen, style, 1);
		}

		/// NOTIFICATIONS
		if (permStatus.isEmpty() && !notifications.isEmpty()) {
			Updater.notetick++;
			if (notifications.size() > 3) { // Only show 3 notifs max at one time; erase old notifs.
				notifications = notifications.subList(notifications.size() - 3, notifications.size());
			}

			if (Updater.notetick > 100) { // Display time per notification.
				notifications.remove(0);
				Updater.notetick = 0;
			}

			// draw each current notification, with shadow text effect.
			FontStyle style = new FontStyle(Color.WHITE).setShadowType(Color.DARK_GRAY, false).setYPos(Screen.h * 2 / 5).setRelTextPos(RelPos.TOP, false);
			Font.drawParagraph(notifications, screen, style, 0);
		}

		// SCORE MODE ONLY:
		if (isMode("score")) {
			String scoreString = "Current score: " + player.getScore();
			
			int seconds = (int) Math.ceil(Updater.scoreTime / (double) Updater.normalSpeed);
			int minutes = seconds / 60;
			int hours = minutes / 60;
			minutes %= 60;
			seconds %= 60;

			int timeTextColor;
			if (Updater.scoreTime >= 18000) {
				timeTextColor = Color.get(0, 555);
			} else if (Updater.scoreTime >= 3600) {
				timeTextColor = Color.get(330, 555);
			} else {
				timeTextColor = Color.get(400, 555);
			}

			Font.draw("Time left " + (hours > 0 ? hours + "h " : "") + minutes + "m " + seconds + "s", screen, Screen.w / 2 - 9 * 8, 2, timeTextColor);
			Font.draw(scoreString, screen, Screen.w - Font.textWidth(scoreString) - 2, 3 + 8, Color.WHITE);

			if (player.getMultiplier() > 1) {
				int multColor = player.getMultiplier() < Player.MAX_MULTIPLIER ? Color.get(-1, 540) : Color.RED;
				String mult = "X" + player.getMultiplier();

				Font.draw(mult, screen, Screen.w - Font.textWidth(mult) - 2, 4 + 2 * 8, multColor);
			}
		}

		/// This renders the potions overlay
		if (player.showPotionEffects && !player.potionEffects.isEmpty()) {
			Map.Entry <PotionType, Integer> [] effects = player.potionEffects.entrySet().toArray(new Map.Entry[0]);
				
			String title = "(" + input.getMapping("potionEffects") + " to hide)";
			
			PotionType potionType;
			Sprite potionIcon = new Sprite(0, 7, 0);
			
			for (int i = 0; i < effects.length; i++) {
				int x = (Screen.w - 118) / 2 + (Screen.w - 8) / 3 - 8; // the width of the box
				int y = (HEIGHT - 8) / 2 + (Screen.h - 8) / 36 - 130; // the height of the box
				potionType = effects[i].getKey();
				
				// Renders the GUI box
				Font.drawBox(screen, x, y, 16, effects.length);
			}

			for (int effectIndex = 0; effectIndex < effects.length; effectIndex++) {
				potionType = effects[effectIndex].getKey();
				int potionTime = effects[effectIndex].getValue() / Updater.normalSpeed;
				
				potionIcon.color = potionType.displayColor;
                
				int minutes = potionTime / 60;
				int seconds = potionTime % 60;
				
				// Title background
				for (int j = 0; j < title.length(); j++) {
					screen.render(311 + (j * 8) - 1, 9, 3 + 21 * 32, 0, 3);
				}
				Font.draw(title, screen, 310, 9, Color.YELLOW);
				
				Font.draw(potionType + " ", screen, 300 , 17 + effectIndex * Font.textHeight(), potionType.displayColor);
				Font.draw("(" + minutes + ":" + (seconds < 10? "0" + seconds:seconds) + ")", screen, 373 , 17 + effectIndex * Font.textHeight(), potionType.displayColor);
				potionIcon.render(screen, 290, 17 + effectIndex * Font.textHeight());
			}
		}

		// This is the status icons, like health hearts, stamina bolts, hunger "burger", and armor points.
		if (!isMode("Creative")) {
			
			Font.drawBox(screen, Screen.w - 80, Screen.h - 17, 10, 2); // the hunger and armor background
			Font.drawBox(screen, 0, Screen.h - 17, 10, 2); // the health and stamina background
			
			for (int i = 0; i < Player.maxStat; i++) {
				
				/// Renders your current red hearts, hardcore hearts, or black hearts for damaged health.

				if (!isMode("Hardcore")) { // Survival hearts
					if (i < player.health) {
						
						// wobbling hearts if the player health is less than 5
						if (player.health < 5) {
							int blinking = (random.nextInt(2) - random.nextInt(2));
							screen.render(2 + i * 8, Screen.h - (18 + blinking), 0 + 2 * 32, 0, 3);
						} else { // normal hearts if more than 4
							screen.render(2 + i * 8, Screen.h - 18, 0 + 2 * 32, 0, 3);
						}
					} else { // hearts cotainer
						if (player.health < 5) {
							int blinking = (random.nextInt(2) - random.nextInt(2));
							screen.render(2 + i * 8, Screen.h - (18 + blinking), 0 + 3 * 32, 0, 3); // wobbling hearts cotainer
						} else {
							screen.render(2 + i * 8, Screen.h - 18 , 0 + 3 * 32, 0, 3); // nomral hearts container
						}
					}
				
				} else { // Hardcore hearts
					
					// the same that the survival hearts, but with hardcore spirtes
					if (i < player.health) {
						if (player.health < 6) {
							int blinking = (random.nextInt(2) - random.nextInt(2));
							screen.render(2 + i * 8, Screen.h - (18 + blinking), 7 + 2 * 32, 0, 3);
						} else {
							screen.render(2 + i * 8, Screen.h - 18, 7 + 2 * 32, 0, 3);
						}
					
				} else { // hearts cotainer
						if (player.health < 5) {
							int blinking = (random.nextInt(2) - random.nextInt(2));
							screen.render(2 + i * 8, Screen.h - (18 + blinking), 7 + 3 * 32, 0, 3); // wobbling hearts cotainer
						} else {
							screen.render(2 + i * 8, Screen.h - 18 , 7 + 3 * 32, 7, 3); // nomral hearts container
						}
					}
				}

				if (player.staminaRechargeDelay > 0) {
					// Creates the white/gray blinking effect when you run out of stamina.
					if (player.staminaRechargeDelay / 4 % 2 == 0) {
						screen.render(2 + i * 8, Screen.h - 9, 1 + 4 * 32, 0, 3);
					} else {
						screen.render(2 + i * 8, Screen.h - 9, 1 + 3 * 32, 0, 3);
					}
				} else {
					// Renders your current stamina, and uncharged gray stamina.
					if (i < player.stamina) {
						screen.render(2 + i * 8, Screen.h - 9, 1 + 2 * 32, 0, 3);
					} else {
						screen.render(2 + i * 8, Screen.h - 9, 1 + 3 * 32, 0, 3);
					}
				}
							
				// Renders hunger icons
				if (i < player.hunger) {
					screen.render(i * 8 + (Screen.w - 82), Screen.h - 18, 2 + 2 * 32, 0, 3);
				} else {
					screen.render(i * 8 + (Screen.w - 82), Screen.h - 18, 2 + 3 * 32, 0, 3);
				}
				
				// Renders armor icons
				int armor = player.armor * Player.maxStat / Player.maxArmor;
				if (i <= armor && player.currentArmor != null) {
					screen.render(i * 8 + (Screen.w - 82), Screen.h - 9, (player.currentArmor.level - 1) + 9 * 32, 0, 0);
				} else {
					screen.render(i * 8 + (Screen.w - 82), Screen.h - 9, 8 + 2 * 32, 0, 3);
				}
				
			}
		}
		
        // Render the bosses health bar on screen
		if (!player.isRemoved() && Settings.get("bossbar").equals("On screen")) {
			// Check if are a boss close to the player in the current level
			List<Entity> entitiesInRange = Game.levels[Game.currentLevel].getEntitiesInRect(new Rectangle(player.x, player.y, 360 << 1, 360 << 1, Rectangle.CENTER_DIMS));
			
			if (AirWizard.active) {
            	AirWizard boss = AirWizard.entity;
            	for (Entity entity: entitiesInRange) { 
            		if (entity instanceof AirWizard) {
            			if (boss.secondform) {
            				renderBossbar((int) ((Float.valueOf(boss.health) / boss.maxHealth) * 100), "Air Wizard II");
            			} else {
            				renderBossbar((int) ((Float.valueOf(boss.health) / boss.maxHealth) * 100), "Air Wizard");
            			}
            		}
            	}    
            } 
			
			if (EyeQueen.active) {
            	EyeQueen boss = EyeQueen.entity;
            	for (Entity entity: entitiesInRange) {
            		if (entity instanceof EyeQueen) {
            			renderBossbar((int) ((Float.valueOf(boss.health) / boss.maxHealth) * 100), "Eye Queen");
            		}
            	}           	
            }
		}
		
		renderDebugInfo();
	}

	public static void renderBossbar(int barLength, String title) {
		if (!showDebugInfo) {
			int x = (Screen.w / 4) + 4;
			int y = (Screen.h / 8) - 27;
	
			int maxBarLength = 100;
	
			if (barLength > 1) {
				screen.render(x + (maxBarLength * 2) , y , 0 + 24 * 32, 1, 3); // left corner
		
				// The middle
				for (int bx = 0; bx < maxBarLength; bx++) {
						screen.render(x + (bx * 2), y, 1 + 24 * 32, 0, 3);
				}  
		
				screen.render(x - 5 , y , 0 + 25 * 32, 0, 3); // right corner
		
				for (int bx = 0; bx < barLength; bx++) {
					screen.render(x + (bx * 2), y, 1 + 25 * 32, 0, 3);
				}
				
				FontStyle style = new FontStyle(Color.WHITE).setShadowType(Color.BLACK, true).setYPos(2);
		
				Font.drawParagraph(title, screen, style, 2);
			}
		}
	}

	// Renders show debug info on the screen.
	private static void renderDebugInfo() {
		if (showDebugInfo) {
			ArrayList <String> info = new ArrayList <> ();
			ArrayList <String> subinfo = new ArrayList <> ();

			info.add("Version: " + Game.BUILD + " (" + Game.VERSION + ")");                 subinfo.add("Played: " + InfoDisplay.getTimeString());
			info.add("Base: " + "Minicraft Plus Legacy");                                   subinfo.add("Java: " + Utils.JAVA_VERSION + " x" + Utils.JAVA_ARCH);
			info.add("" + TimeData.date());                                                 subinfo.add(Utils.getGeneralMemoryUsage());
			info.add(Initializer.fra + " fps" );                                            subinfo.add(Utils.getMemoryAllocation());
			info.add("Day tiks: " + Updater.tickCount + " (" + Updater.getTime() + ")");
			info.add((Updater.normalSpeed * Updater.gameSpeed) + " tps");
             
			// player info
			info.add("Walk spd: " + player.moveSpeed);
			info.add("X: " + (player.x >> 4) + "." + (player.x % 16));
			info.add("Y: " + (player.y >> 4) + "." + (player.y % 16));
			info.add("");

			info.add("Tile: " + levels[currentLevel].getTile(player.x >> 4, player.y >> 4).name);
			info.add("Id: " + levels[currentLevel].getTile(player.x >> 4, player.y >> 4).id);
			info.add("Data: " + levels[currentLevel].getData(player.x >> 4, player.y >> 4));
			info.add("Depth: " + levels[currentLevel].depth + " (" + levels[currentLevel].w +"x" + levels[currentLevel].h +")");
			info.add("");

			if (isMode("score")) {
				info.add("Score " + player.getScore());
			}

			if (levels[currentLevel] != null) {
				switch (levels[currentLevel].depth) {
					case 1: levelName = "Heaven"; break;
					case 0: levelName = "Surface"; break;
					case -1: levelName = "Caves"; break;
					case -2: levelName = "Caverns"; break;
					case -3: levelName = "Hell"; break;
					case -4: levelName = "Dungeon"; break;
					case -5: levelName = "Void"; break;
					default: levelName = "Unknown"; break;
				}
				
				info.add("Current level: " + levelName);
				info.add("Mobs Count: " + levels[currentLevel].mobCount + "/" + levels[currentLevel].maxMobCount);
				
				/// Displays number of chests left, if on dungeon level.
				if (currentLevel == 6) {
					if (levels[6].chestCount > 0) {
						info.add("Chests: " + levels[6].chestCount);
					} else {
						info.add("Chests: Complete!");
					}
				}
				
				info.add("Hunger stam: " + player.getDebugHunger());
				if (player.armor > 0) {
					info.add("Armor: " + player.armor);
					info.add("Dam buffer: " + player.armorDamageBuffer);
				}

				info.add("");
				info.add("Level seed: " + levels[currentLevel].getSeed());
				info.add("Moon phase: " + player.isNiceNight + " -> " + player.nightCount + "/4");
			}

			FontStyle style = new FontStyle(Color.WHITE).setShadowType(Color.BLACK, true).setXPos(1).setYPos(1);
			FontStyle substyle = new FontStyle(Color.WHITE).setShadowType(Color.BLACK, true).setXPos(Screen.w - 121).setYPos(1);

			Font.drawParagraph(info, screen, style, 2);
			Font.drawParagraph(subinfo, screen, substyle, 2);
		}
	}

	/** Renders the "Click to focus" box when you click off the screen. */
	private static void renderFocusNagger() {
		String msg = "Click to focus!"; // the message when you click off the screen.
		Updater.paused = true; // perhaps paused is only used for this.

		int x = (Screen.w - Font.textWidth(msg)) / 2;
		int y = (HEIGHT - 8) / 2;

		Font.drawBox(screen, x, y, Font.textWidth(msg) / 8, 1);

		// Renders the focus nagger text with a flash effect...
		if ((Updater.tickCount / 20) % 2 == 0) { // ...medium yellow color
			Font.draw(msg, screen, x, y, Color.get(1, 153));
		} else { // ...bright yellow color
			Font.draw(msg, screen, x, y, Color.get(5, 255));
		}
	}

	public static java.awt.Dimension getWindowSize() {
		return new java.awt.Dimension((int) (WIDTH * SCALE), (int) (HEIGHT * SCALE));
	}
}