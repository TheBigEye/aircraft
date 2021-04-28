package minicraft.core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

import org.jetbrains.annotations.Nullable;

import minicraft.core.io.InputHandler;
import minicraft.core.io.Settings;
import minicraft.core.io.Sound;
import minicraft.entity.mob.Player;
import minicraft.level.Level;
import minicraft.level.tile.Tiles;
import minicraft.network.MinicraftClient;
import minicraft.network.MinicraftProtocol;
import minicraft.network.MinicraftServer;
import minicraft.saveload.Load;
import minicraft.saveload.Version;
import minicraft.screen.Display;
import minicraft.screen.MultiplayerDisplay;
import minicraft.screen.TitleDisplay;

//--------------------------------------------------------------------------------------------------------------------------------------------------

public class Game {
	Game() {} // can't instantiate the Game class.

	private static Random random = new Random();

	public static boolean debug = false;
	public static boolean HAS_GUI = true;

	public static final String NAME = "Aircraft"; // This is the name on the application window
	public static final String BUILD = "0.4"; // Aircraft version
	public static final Version VERSION = new Version("2.0.7-dev2"); // Minicraft mod base version

	// input used in Game, Player, and just about all the *Menu classes.
	public static InputHandler input; 
	public static Player player;

	// The directory in which all the game files are stored
	public static String gameDir; 
	public static List<String> notifications = new ArrayList<>();

	public static int MAX_FPS = (int) Settings.get("fps");
	public static Level level;

	private static String ERROR_TITLE = "";
	
	// Crash window titles
	static String[] array = {
		"Who has put TNT?", "An error has occurred again??",
		"Unexpected error again??", "Oh. That hurts :(",
		"Sorry for the crash :(", "You can play our brother game, Minitale", "F, crash again??" 
	};

//--------------------------------------------------------------------------------------------------------------------------------------------------

	/*****************************************************
	 * This specifies a custom port instead of default to
	 * server-side using --port parameter if something goes
	 * wrong in setting the new port it'll use the default
	 * one {@link MinicraftProtocol#PORT}
	 *****************************************************/

	public static int CUSTOM_PORT = MinicraftProtocol.PORT;

	static Display menu = null, newMenu = null; // the current menu you are on.

	// Sets the current menu.
	public static void setMenu(@Nullable Display display) {
		newMenu = display;
	}

	public static void exitMenu() {

		if (menu == null) {
			if (debug)
				System.out.println("Game.exitMenu(): No menu found, returning!");
			return; // no action required; cannot exit from no menu
		}

		Sound.back.play();
		newMenu = menu.getParent();
	}

	public static Display getMenu() {
		return newMenu;
	}

	public static boolean isMode(String mode) {
		return ((String) Settings.get("mode")).equalsIgnoreCase(mode);
	}

	// MULTIPLAYER
	public static boolean ISONLINE = false;
	public static boolean ISHOST = false;

	public static MinicraftClient client = null;

	public static boolean isValidClient() {
		return ISONLINE && client != null;
	}

	public static boolean isConnectedClient() {
		return isValidClient() && client.isConnected();
	}

	public static MinicraftServer server = null;

	/** Checks if you are a host and the game is a server */
	public static boolean isValidServer() {
		return ISONLINE && ISHOST && server != null;
	}

	public static boolean hasConnectedClients() {
		return isValidServer() && server.hasClients();
	}

	// LEVEL
	public static Level[] levels = new Level[6]; // This array stores the different levels.
	public static int currentLevel = 3; // This is the level the player is on. It defaults to 3, the surface.

	static boolean gameOver = false; // If the player wins this is set to true.

	static boolean running = true;

	// Quit function
	public static void quit() {
		if (isConnectedClient())
			client.endConnection();
		if (isValidServer())
			server.endConnection();
		running = false;
	}

	// Main functions
	public static void main(String[] args) {

// System info in debug ----------------------------------------------------------------------------------------------------------------------------

		if (debug) {
			System.out.println("Current Operating system");
			System.out.println("OS: " + System.getProperty("os.name"));
			System.out.println("OS Version: " + System.getProperty("os.version"));
			System.out.println("OS Arch: " + System.getProperty("os.arch"));
			System.out.println("");
			System.out.println("Java specifications");
			System.out.println("Version: " + System.getProperty("java.version"));
			System.out.println("Vendor: " + System.getProperty("java.vendor"));
			System.out.println("Model: " + System.getProperty("sun.arch.data.model"));
		}

// Crash window log --------------------------------------------------------------------------------------------------------------------------------		

		Game.ERROR_TITLE = array[random.nextInt(6)]; // six titles

		LocalDateTime time = LocalDateTime.now();
		
		// JVM Runtime Memory
		Runtime runtime = Runtime.getRuntime();
		long i = runtime.maxMemory();
		long j = runtime.totalMemory();
		long k = runtime.freeMemory();
		long l = i / 1024L / 1024L;
		long i1 = j / 1024L / 1024L;
		long j1 = k / 1024L / 1024L;

		
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			throwable.printStackTrace();

			StringWriter string = new StringWriter();
			PrintWriter printer = new PrintWriter(string);
			throwable.printStackTrace(printer);

			// Crash log Estructure
			JTextArea errorDisplay = new JTextArea(
			// Nothing
			);

			errorDisplay.setForeground(Color.BLACK);
			errorDisplay.setBackground(Color.WHITE);

			errorDisplay.setText(
				// Crash message
                " An error occurred while trying to Read / Load the game \n" + 
                " This can be due to various things (old / corrupted worlds, some game bug or unexpected Java bug, etc.). \n" + 
                " If the problem persists, send a screenshot to the author.\n" + "\n" + 
                    
                "--- BEGIN ERROR REPORT ---------" + "\n" +
                "Generated: " + time.toLocalDate() + "\n\n" +		                     
					
                "-- System Details --" + "\n" +
                "Details: " + "\n" +
                "        Aircraft Version: " + Game.BUILD + "\n" +
		        "        Operting System: " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") " + System.getProperty("os.version") + "\n" +
		        "        Java Version: " + System.getProperty("java.version") + ", " + System.getProperty("java.vendor")+ "\n" +
		        "        Java VM Version: " + System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor") + "\n" +
			    "        Memory: " + k + " bytes (" + j1 + " MB) / " + j + " bytes (" + i1 + " MB) up to " + i + " bytes (" + l + " MB)" + "\n\n" +
		            
                "~~ERROR~~ " + "\n" +
					
			     string.toString() + "\n" +
			        
                "--- END ERROR REPORT ---------"
			);
			
			errorDisplay.setEditable(false);
			errorDisplay.setFont(new Font("Consolas", Font.PLAIN, 12));
			JScrollPane errorPane = new JScrollPane(errorDisplay);
			errorPane.setSize(600, 400);

			@SuppressWarnings("unused")
			UIManager UI = new UIManager();
			UIManager.put("OptionPane.background", Color.white);
			UIManager.put("Panel.background", Color.white);	 

			Icon Logo = new ImageIcon("src/resources/logo.png");

			JOptionPane.showMessageDialog(null, errorPane, "Aircraft has crashed!", JOptionPane.PLAIN_MESSAGE);
		});

// Start events ------------------------------------------------------------------------------------------------------------------------------------

		Initializer.parseArgs(args);

		// Input declaration
		input = new InputHandler(Renderer.canvas);

		// Load events
		Tiles.initTileList();
		Sound.init();
		Settings.init();

		World.resetGame(); // "half"-starts a new game, to set up initial variables
		player.eid = 0;
		new Load(true); // this loads any saved preferences.

		if (Network.autoclient)
			setMenu(new MultiplayerDisplay("localhost"));
		else if (!HAS_GUI)
			Network.startMultiplayerServer();
		else
			setMenu(new TitleDisplay()); // sets menu to the title screen.

// Window events ----------------------------------------------------------------------------------------------------------------------------------

		// Create a game window
		Initializer.createAndDisplayFrame();

		// Display objets in the screen
		Renderer.initScreen();

		// Start tick() count
		Initializer.run();

// Exit events -------------------------------------------------------------------------------------------------------------------------------------

		if (debug)
			System.out.println("Main game loop ended; Terminating application...");

		System.exit(0);
	}
}