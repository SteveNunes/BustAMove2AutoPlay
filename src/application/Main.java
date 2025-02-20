package application;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import entities.CharacterSelector;
import enums.Direction;
import enums.GameAction;
import enums.GameCharacter;
import enums.GameStage;
import enums.GamepadButton;
import enums.ImageScanOrientation;
import enums.PlayerAction;
import globallisteners.GlobalKeyListener;
import gui.util.ImageUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import util.CollectionUtils;
import util.DesktopUtils;
import util.FindFile;
import util.IniFile;
import util.Misc;
import util.Sounds;

public class Main extends Application {
	
	public static boolean close = false;
	public final static IniFile ini = IniFile.getNewIniFileInstance("config.ini");
	public static GameAction gameAction = GameAction.NOTHING;

	/** Parâmetros otimizados para emulador RetroArch em modo fullscren 4:3,
	 *  sem shader. Resolução nativa 1920 x 1080, Escala do windows em 125%
	 *  As imagens dos botões devem ser obtidas através da tecla Z com o programa
	 *  rodando, após se certificar que o campo inteiro dos comandos esteja
	 *  aparecendo corretamente na tela de debug
	 */
	
	/** A ordem das teclas em 'KEY_NAMES' deve ser a mesma de 'KEY_CODES'
	  * As teclas do emulador devem estar configuradas da seguinte forma:
	  * P1:
	  * DIRECIONAIS: ESQUERDA, CIMA, DIREITA, BAIXO      = A, S, D, W
	  * BOTOES (Formato XBOX): A, B, X, Y, RS, RT, START = Numpads 1, 2, 4, 5, 6, 3, ENTER (NÃO é o Numpad ENTER)  
	  * P2:
	  * DIRECIONAIS: ESQUERDA, CIMA, DIREITA, BAIXO      = F, G, H, T
	  * BOTOES (Formato XBOX): A, B, X, Y, RS, RT, START = J, K, U, I, O, L, M  
	  */
	final static String[] KEY_NAMES = {"LEFT", "DOWN", "RIGHT", "UP", "A", "B", "X", "Y", "R1", "R2", "START"};
	final static int[][] KEY_CODES = {{			KeyEvent.VK_A, KeyEvent.VK_S, 	KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, 	KeyEvent.VK_NUMPAD3, 	KeyEvent.VK_NUMPAD8, 	KeyEvent.VK_ENTER},
																				{	KeyEvent.VK_F, KeyEvent.VK_G, 	KeyEvent.VK_H, KeyEvent.VK_T, KeyEvent.VK_J, 				KeyEvent.VK_K, 				KeyEvent.VK_U, 				KeyEvent.VK_I, 				KeyEvent.VK_O, 				KeyEvent.VK_L, 				KeyEvent.VK_M}};
	@SuppressWarnings("serial")
	final static List<BufferedImage> BUTTONS_PNG = new LinkedList<>() {{
		for (int n = 0; n < KEY_NAMES.length; n++)
			add(ImageUtils.loadBufferedImageFromFile(".\\images\\buttons\\" + KEY_NAMES[n].charAt(0) + ".png"));
	}};
	@SuppressWarnings("serial")
	final static List<BufferedImage> BUTTONS2_PNG = new LinkedList<>() {{
		for (int n = 0; n < KEY_NAMES.length; n++)
			add(ImageUtils.loadBufferedImageFromFile(".\\images\\inputs\\" + KEY_NAMES[n].charAt(0) + ".png"));
	}};

	@SuppressWarnings("serial")
	static Map<GameCharacter, Point> picLocations = new LinkedHashMap<>() {{
		for (int y = 0; y < 5; y++)
			for (int x = 0; x < 4; x++)
				put(CharacterSelector.characterGrid[y][x], new Point(557 + x * 106, 166 + y * 101));
	}};
	static Map<String, Image> usersPicture = new LinkedHashMap<>();
	public static File usersPictureDir = new File("images/usersPicture/");
	final static BufferedImage characterSelectScreenPng = ImageUtils.loadBufferedImageFromFile(new File(".\\images\\misc\\character_select_screen.png"));
	final static BufferedImage stageLoadingScreenPng = ImageUtils.loadBufferedImageFromFile(new File(".\\images\\misc\\stage_loading.png"));
	final static BufferedImage readyPng = ImageUtils.loadBufferedImageFromFile(new File(".\\images\\misc\\ready.png"));
	final static Random random = new Random(new SecureRandom().nextInt(Integer.MAX_VALUE));
  final static ExecutorService executor = Executors.newFixedThreadPool(5);

	static int start_X;
	static int start_Y;
	static int width;
	static int height;
	static int spacing;
	static int spacing2;
	static int ready_X;
	static int ready_Y;
	static int ready_X2;
	static int ready_Y2;
	static int special_X1;
	static int special_X2;
	static int special_Xp;
	static int special_Y1;
	static int special_Y2;
	static int special_capture_X;
	static int special_capture_Y;
	static int special_capture_W;
	static int special_capture_H;

	static Task<Void> mainTask;
	static Robot[] robot;
	static Stage stageMain;
	static VBox mainVBox;
	static Canvas canvasMainScreenshot;
	static Canvas canvasButtonsScreenshot;
	static Canvas canvasCatchedInputs;
	static Canvas canvasTikTok;
	static Canvas canvasConsole;
	static GraphicsContext gcMainScreenshot;
	static GraphicsContext gcButtonsScrenshot;
	static GraphicsContext gcCatchedInputs;
	static GraphicsContext gcConsole;
	static GraphicsContext gcTikTok;

	// CONFIGS (Definir os valores através do arquivo ini
	static int debugPlayer; // Qual jogador será debugado
	static int autoPlay; // Ativa o autplay para: 0 (Desligado) 1 (Jogador 1), 2 (Jogador 2) 3 (Ambos)
	static boolean debugCapturedScreenshots; // Janela de debug para visualizar as áreas de capturas dos botões
	static boolean debugCatchedInputs; // Janela de debug que exibe os botões reconhecidos em tempo real (se bater com os comandos atuais do jogo, é porque tudo está configurado corretamente)
	static boolean debugConsole; // Janela de debug que exibe valores em texto
	static boolean fixKeysAreEnabled; // Usar setas para alterar os valores START_X e START_Y, WIDTH e HEIGHT (CTRL pressionado), SPACING e SPACING2 (SHIFT pressionado)
	
	@SuppressWarnings("serial")
	static List<List<GamepadButton>> currentInputs = new LinkedList<>() {{
		add(new LinkedList<>());
		add(new LinkedList<>());
	}};
	@SuppressWarnings("serial")
	static Map<GameStage, int[]> songBeatInfos = new LinkedHashMap<>() {{
				put(GameStage.HEAT, new int[] {2099, 60, 0, 63, 8, 32});
				put(GameStage.COMET, new int[] {1913, 35, 2, 73, 24, 52});
				put(GameStage.SHORTY, new int[] {1994, 80, -4, 73, 12, 32, 48}); 
				put(GameStage.STRIKE, new int[] {2103, 60, -4, 57, 8, 32});
				put(GameStage.CAPOEIRA, new int[] {1946, 40, -4, 61, 8, 24, 48});
				put(GameStage.KITTY_N, new int[] {1813, 45, 0, 84, 16, 39, 52});
				put(GameStage.TSUTOMO, new int[] {1841, 45, 0, 73, 24, 48});
				put(GameStage.HIRO, new int[] {1945, 40, 4, 63, 16, 37});
				put(GameStage.KELLY, new int[] {2393, 100, -3, 57, 8, 24});
				put(GameStage.BIO, new int[] {1840, 80, 3, 65, 16, 32});
				put(GameStage.ROBO_Z_GOLD, new int[] {1709, 40, 3, 77, 32, 64}); 
				put(GameStage.PANDER, new int[] {1495, 50, 2, 97, 16, 48});
	}};
	static Map<String, GameCharacter> lastVotedCharacter = new LinkedHashMap<>();
	static Map<String, GameStage> lastVotedStage = new LinkedHashMap<>();
	static List<GameCharacter> selectedChars = new LinkedList<>();
	static Map<GameCharacter, Integer> votedChars = new LinkedHashMap<>();
	static Map<GameStage, Integer> votedStages = new LinkedHashMap<>();
	static List<GameStage> selectedStages = new LinkedList<>();
	static List<Integer> arrowKeys = new LinkedList<>(Arrays.asList(NativeKeyEvent.VC_LEFT, NativeKeyEvent.VC_UP, NativeKeyEvent.VC_RIGHT, NativeKeyEvent.VC_DOWN));
	static List<String> consoleMsgBuffer = new LinkedList<>();
	static GameStage currentStage = GameStage.HEAT;
	static CharacterSelector characterSelector[];
	static GamepadButton beatButton[] = {null, null};
	static PlayerAction previewPlayerAction[] = {PlayerAction.DANCING, PlayerAction.DANCING};
	static PlayerAction currentPlayerAction[] = {PlayerAction.DANCING, PlayerAction.DANCING};
	static String[] tikTokScrollMessage = {"", ""};
	static List<String> tikTokBottomMessagesBuffer = new LinkedList<>();
	static String[] lastCommand;
	static boolean previewHaveInput[] = {false, false};
	static boolean haveInput[] = {false, false};
	static boolean alternativeOutfit = false;
	static boolean atSolo = false;
	static boolean atPreSolo = false;
	static boolean canStealSolo = false;
	static boolean selectionByVote = false;
	static int songBeatInfo[];
	static int specials[] = {0, 0};
	static int playerDificult[] = {1, 1};
	static int[] tikTokScrollX = {0, 0};
	static int selectedChar = 0;
	static int lastFinishButton = random.nextInt(1);
	static int currentBeat = 0;
	static int nonPreSoloAttackProc;
	static int preSoloAttackProc;
	static int nonPreSoloCounterProc;
	static int preSoloCounterProc;
	static int dodgeUponFailCounterProc;
	static int intentionalMissInputProc;
	static int repeatedCommands[];
	static long nextCTimeToPress = 0;
	static long lastBeatDelay;
	static long selectionByVoteTimeOut;
	static long voteTimeOut = 0;
	static double[] tikTokScrollTextWidth = {0, 0};
	static int fixX = 0;
	static int fixY = 0;
	
	@Override
	public void start(Stage stage) throws Exception {
		if (!usersPictureDir.exists())
			usersPictureDir.mkdir();
		loadConfigs();
		createCanvas();
		setStage(stage);
		initRobot();
		setGlobalKeyListeners();

		ImageUtils.setImageScanOrientation(ImageScanOrientation.VERTICAL);
		ImageUtils.setImageScanIgnoreColor(Color.WHITE);
		if (debugCapturedScreenshots || debugCatchedInputs || debugConsole) {
			refreshDebugWindows(true);
			stageMain.show();
		}
		
		executor.execute(mainTask = new Task<Void> () {
			@Override
			protected Void call() throws Exception {
				mainLoop();
				return null;
			}
		});
		
		if (debugCapturedScreenshots || debugCatchedInputs)
			executor.execute(mainTask = new Task<Void> () {
				@Override
				protected Void call() throws Exception {
					while (!close) {
						for (int pl = 0; pl < 2; pl++)
							getCurrentInputs(pl);
						Misc.sleep(50);
					}
					return null;
				}
			});
		
	}
	
	static void setStage(Stage stage) {
		stageMain = stage;
		stage.setAlwaysOnTop(true);
		stage.initStyle(StageStyle.UNDECORATED);
		stage.setResizable(false);
	}
	
	static void initRobot() {
		try
			{ robot = new Robot[] {new Robot(), new Robot(), new Robot()}; }
		catch (AWTException e)
			{ throw new RuntimeException("Unable to intialize Robots"); }
	}

	static Robot mainRobot()
		{ return robot[2]; }
	
	static void beep(int n)
		{ Sounds.playWav(".\\sounds\\beep" + (n == 1 ? "" : n) + ".wav"); }
	
	static void beep()
		{ beep(1); }

	public static Random getRandom()
		{ return random; }
	
	static int getBeatDelay()
		{ return songBeatInfo[0]; }
	
	static void setBeatDelay(int value)
		{ songBeatInfo[0] = value; }

	static int getBeatStartingDelay()
		{ return songBeatInfo[1]; }
	
	void setBeatStartingDelay(int value)
		{ songBeatInfo[1] = value; }

	static int getBeatFixMsOvertime()
		{ return songBeatInfo[2]; }
	
	static void setBeatFixMsOvertime(int value)
		{ songBeatInfo[2] = value; }

	static int getMusicEndBeat()
		{ return songBeatInfo[3]; }
	
	static void setGlobalKeyListeners() {
		GlobalKeyListener.startListener();
		GlobalKeyListener.setOnKeyPressedEvent(k -> {
			Platform.runLater(() -> {
				if (k.getKeyCode() == NativeKeyEvent.VC_LEFT)
					{ fixX--; System.out.println(fixX + " " + fixY); }
				else if (k.getKeyCode() == NativeKeyEvent.VC_RIGHT)
					{ fixX++; System.out.println(fixX + " " + fixY); }
				else if (k.getKeyCode() == NativeKeyEvent.VC_UP)
					{ fixY--; System.out.println(fixX + " " + fixY); }
				else if (k.getKeyCode() == NativeKeyEvent.VC_DOWN)
					{ fixY++; System.out.println(fixX + " " + fixY); }
				if (k.getKeyCode() == NativeKeyEvent.VC_N)
					voteOnCharacter(CharacterSelector.getRandom(), 2);
				if (k.getKeyCode() == NativeKeyEvent.VC_M)
					voteOnStage(GameStage.getRandom(false), 2);
				if (k.getKeyCode() == NativeKeyEvent.VC_R) // R reseta para o modo 'WAITING_FOR_MUSIC_START'
					gameAction = GameAction.WAITING_FOR_MUSIC_START;
				else if (k.getRawCode() == KeyEvent.VK_1) { // F1 - Altera o player da janela de debug
					debugPlayer = (debugPlayer % 2) + 1;
					sendToConsole("" + debugPlayer);
					refreshDebugWindows();
				}
				else if (k.getRawCode() == KeyEvent.VK_2 && debugCapturedScreenshots) { // Tecla 2 - Oculta/Exibe o debug de captura de screenshots
					debugCatchedInputs = !debugCatchedInputs; 
					refreshDebugWindows();
					if (!debugCapturedScreenshots && !debugCatchedInputs)
						debugCatchedInputs = true;
				}
				else if (k.getRawCode() == KeyEvent.VK_3 && debugCatchedInputs) { // Tecla 3 - Oculta/Exibe o debug de reconhecimento de comandos
					debugCapturedScreenshots = !debugCapturedScreenshots; 
					refreshDebugWindows();
				}
				else if (k.getKeyCode() == NativeKeyEvent.VC_Z) // Z - Tira uma screenshot da sequencia atual de comandos
					takeInputScreenshot();
				else if (k.getKeyCode() == NativeKeyEvent.VC_X) // X - Tira uma screenshot da tela inteira, na resolução escalionada
					takeScreenshot();
				else if (k.getKeyCode() == NativeKeyEvent.VC_SPACE) { // ESPAÇO  - Ativa/desativa o autoPlay
					if (gameAction == GameAction.DANCING) {
						if (++autoPlay == 4)
							autoPlay = 0;
						beep(autoPlay == 0 ? 4 : 2);
						System.out.println("Autoplay está: " + Arrays.asList("Desligado", "Ligado para P1", "Ligado para P2", "Ligado para ambos").get(autoPlay));
					}
				}
				else if (k.getKeyCode() == NativeKeyEvent.VC_Q)
					System.out.println(currentStage + " - Compasso atual: " + currentBeat);
			});
			if (k.getKeyCode() == NativeKeyEvent.VC_ESCAPE) // Aperte ESC para finalizar a macro
				close();
			veryfyArrowKeysPress(k);
		});
		GlobalKeyListener.setOnKeyRepeatedEvent(k -> veryfyArrowKeysPress(k));
	}

	static void veryfyArrowKeysPress(NativeKeyEvent k) {
		if (!fixKeysAreEnabled)
			return;
		if (arrowKeys.contains(k.getKeyCode())) {
			gcButtonsScrenshot.setFill(Color.WHITE);
			gcButtonsScrenshot.fillRect(0, 0, canvasButtonsScreenshot.getWidth(), canvasButtonsScreenshot.getHeight());
			gcCatchedInputs.setFill(Color.BLACK);
			gcCatchedInputs.fillRect(0, 0, canvasCatchedInputs.getWidth(), canvasCatchedInputs.getHeight());
			if (k.getModifiers() == 1) {
				if (k.getKeyCode() == NativeKeyEvent.VC_LEFT)
					spacing--;
				else if (k.getKeyCode() == NativeKeyEvent.VC_RIGHT)
					spacing++;
				else if (k.getKeyCode() == NativeKeyEvent.VC_UP)
					spacing2--;
				else if (k.getKeyCode() == NativeKeyEvent.VC_DOWN)
					spacing2++;
			}
			else if (k.getModifiers() == 2) {
				if (k.getKeyCode() == NativeKeyEvent.VC_LEFT)
					width--;
				else if (k.getKeyCode() == NativeKeyEvent.VC_RIGHT)
					width++;
				else if (k.getKeyCode() == NativeKeyEvent.VC_UP)
					height--;
				else if (k.getKeyCode() == NativeKeyEvent.VC_DOWN)
					height++;
			}
			else {
				if (k.getKeyCode() == NativeKeyEvent.VC_LEFT)
					start_X--;
				else if (k.getKeyCode() == NativeKeyEvent.VC_RIGHT)
					start_X++;
				else if (k.getKeyCode() == NativeKeyEvent.VC_UP)
					start_Y--;
				else if (k.getKeyCode() == NativeKeyEvent.VC_DOWN)
					start_Y++;
			}
			Platform.runLater(() -> refreshDebugWindows(true));
			sendToConsole("START_X: " + start_X + " START_Y: " + start_Y + " WIDTH: " + width + " HEIGHT: " + height + " SPACING: " + spacing + " SPACING2: " + spacing2);
			System.out.println("START_X: " + start_X + " START_Y: " + start_Y + " WIDTH: " + width + " HEIGHT: " + height + " SPACING: " + spacing + " SPACING2: " + spacing2);
		}
	}

	static GamepadButton getAttackButton(int player)
		{ return playerDificult[player] == 0 ? GamepadButton.B : GamepadButton.RS; }

	static GamepadButton getDodgeButton(int player)
		{ return playerDificult[player] == 0 ? GamepadButton.A : GamepadButton.RT; }

	static void takeScreenshot() {
		sendToConsole("TAKING SCREENSHOT");
		Sounds.playMp3(".\\sounds\\screenshot.mp3");
		int i = 0;
		while (new File(".\\images\\screenshots\\screenshot_" + ++i + ".png").exists());
		ImageUtils.saveImageToFile(getScreenShot(2, new Rectangle(0, 0, DesktopUtils.getSystemScreenWidth(), DesktopUtils.getSystemScreenHeight())), new File(".\\images\\screenshots\\screenshot_" + i + ".png"));
	}

	static void takeInputScreenshot() {
		sendToConsole("TAKING INPUT SCREENSHOT");
		Sounds.playMp3(".\\sounds\\screenshot.mp3");
		Rectangle rect = new Rectangle(start_X + debugPlayer * spacing, start_Y, (int)(width * 7.2f), height);
		Rectangle r = new Rectangle(rect);
		BufferedImage sc2 = getScreenShot(2, r);
		int i = 0;
		while (new File(".\\images\\screenshots\\input_screenshot_" + ++i + ".png").exists());
		File	file = new File(".\\images\\screenshots\\input_screenshot_" + i + ".png");
		ImageUtils.saveImageToFile(sc2, file);
	}
	
	static void loadConfigs() {
		nonPreSoloAttackProc = ini.readAsInteger("DIFICULT", "nonPreSoloAttackProc", 5);
		preSoloAttackProc = ini.readAsInteger("DIFICULT", "preSoloAttackProc", 50);
		nonPreSoloCounterProc = ini.readAsInteger("DIFICULT", "nonPreSoloCounterProc", 80);
		preSoloCounterProc = ini.readAsInteger("DIFICULT", "preSoloCounterProc", 80);
		dodgeUponFailCounterProc = ini.readAsInteger("DIFICULT", "dodgeUponFailCounterProc", 100);
		intentionalMissInputProc = ini.readAsInteger("DIFICULT", "intentionalMissInputProc", 0);
		debugPlayer = ini.readAsInteger("CONFIG", "debugPlayer", 0);
		autoPlay = ini.readAsInteger("CONFIG", "autoPlay", 2);
		selectionByVoteTimeOut = ini.readAsLong("CONFIG", "selectionByVoteTimeOut", 30000L);
		debugCapturedScreenshots = ini.readAsBoolean("CONFIG", "debugCapturedScreenshots", false);
		debugCatchedInputs = ini.readAsBoolean("CONFIG", "debugCatchedInputs", false);
		selectionByVote =  ini.readAsBoolean("CONFIG", "selectionByVote", false);
		debugConsole = ini.readAsBoolean("CONFIG", "debugConsole", false);
		fixKeysAreEnabled = ini.readAsBoolean("CONFIG", "fixKeysAreEnabled", false);
		alternativeOutfit = ini.readAsBoolean("CONFIG", "alternativeOutfit", false);
		start_X = ini.readAsInteger("CAPTURE_COORDS", "start_X", 313);
		start_Y = ini.readAsInteger("CAPTURE_COORDS", "start_Y", 413);
		width = ini.readAsInteger("CAPTURE_COORDS", "width", 56);
		height = ini.readAsInteger("CAPTURE_COORDS", "height", 45);
		spacing = ini.readAsInteger("CAPTURE_COORDS", "spacing", 514);
		spacing2 = ini.readAsInteger("CAPTURE_COORDS", "spacing2", 5);
		ready_X = ini.readAsInteger("CAPTURE_COORDS", "ready_X", 600);
		ready_Y = ini.readAsInteger("CAPTURE_COORDS", "ready_Y", 455);
		ready_X2 = ini.readAsInteger("CAPTURE_COORDS", "ready_X2", 740);
		ready_Y2 = ini.readAsInteger("CAPTURE_COORDS", "ready_Y2", 440);
		special_X1 = ini.readAsInteger("CAPTURE_COORDS", "special_X1", 19);
		special_X2 = ini.readAsInteger("CAPTURE_COORDS", "special_X2", 1030);
		special_Xp = ini.readAsInteger("CAPTURE_COORDS", "special_Xp", 19);
		special_Y1 = ini.readAsInteger("CAPTURE_COORDS", "special_Y1", 15);
		special_Y2 = ini.readAsInteger("CAPTURE_COORDS", "special_Y2", 37);
		special_capture_X = ini.readAsInteger("CAPTURE_COORDS", "special_capture_X", 245);
		special_capture_Y = ini.readAsInteger("CAPTURE_COORDS", "special_capture_Y", 740);
		special_capture_W = ini.readAsInteger("CAPTURE_COORDS", "special_capture_W", 1046);
		special_capture_H = ini.readAsInteger("CAPTURE_COORDS", "special_capture_H", 50);
		currentStage = ini.readAsEnum("DEBUG", "currentStage", GameStage.class, GameStage.HEAT);
		gameAction = ini.readAsEnum("DEBUG", "gameAction", GameAction.class, GameAction.NOTHING);
		List<GameCharacter> chars = ini.readAsEnumList("DEBUG", "characters", GameCharacter.class, Arrays.asList(GameCharacter.HEAT, GameCharacter.COMET));
		characterSelector = new CharacterSelector[] {new CharacterSelector((GameCharacter) chars.get(0)), new CharacterSelector((GameCharacter) chars.get(1))};
		if (ini.read("SELECTED", "CHARACTERS") != null)
			for (String item : ini.read("SELECTED", "CHARACTERS").split(" "))
				selectedChars.add(GameCharacter.valueOf(item));
		if (ini.read("SELECTED", "STAGES") != null)
			for (String item : ini.read("SELECTED", "STAGES").split(" "))
				selectedStages.add(GameStage.valueOf(item));
		for (File file : FindFile.findFile(usersPictureDir.getAbsolutePath())) {
			if (!file.getName().equals("circle_image_mask.png") && !file.getName().equals("unknown.png")) {
				String user = file.getName().replace(".png", "");
				Image picture = new Image("file:" + file.getAbsolutePath());
				usersPicture.put(user, picture);
			}
		}
	}

	static void saveConfigs() {
		ini.write("DIFICULT", "nonPreSoloAttackProc", "" + nonPreSoloAttackProc);
		ini.write("DIFICULT", "preSoloAttackProc", "" + preSoloAttackProc);
		ini.write("DIFICULT", "nonPreSoloCounterProc", "" + nonPreSoloCounterProc);
		ini.write("DIFICULT", "preSoloCounterProc", "" + preSoloCounterProc);
		ini.write("DIFICULT", "dodgeUponFailCounterProc", "" + dodgeUponFailCounterProc);
		ini.write("DIFICULT", "intentionalMissInputProc", "" + intentionalMissInputProc);
		ini.write("CONFIG", "debugPlayer", "" + debugPlayer);
		ini.write("CONFIG", "autoPlay", "" + autoPlay);
		ini.write("CONFIG", "debugCapturedScreenshots", "" + debugCapturedScreenshots);
		ini.write("CONFIG", "debugCatchedInputs", "" + debugCatchedInputs);
		ini.write("CONFIG", "debugConsole", "" + debugConsole);
		ini.write("CONFIG", "fixKeysAreEnabled", "" + fixKeysAreEnabled);
		ini.write("CONFIG", "selectionByVote", "" + selectionByVote);
		ini.write("CONFIG", "selectionByVoteTimeOut", "" + selectionByVoteTimeOut);
		ini.write("CONFIG", "alternativeOutfit", "" + alternativeOutfit);
		ini.write("CAPTURE_COORDS", "start_X", "" + start_X);
		ini.write("CAPTURE_COORDS", "start_Y", "" + start_Y);
		ini.write("CAPTURE_COORDS", "width", "" + width);
		ini.write("CAPTURE_COORDS", "height", "" + height);
		ini.write("CAPTURE_COORDS", "spacing", "" + spacing);
		ini.write("CAPTURE_COORDS", "spacing2", "" + spacing2);
		ini.write("CAPTURE_COORDS", "ready_X", "" + ready_X);
		ini.write("CAPTURE_COORDS", "ready_Y", "" + ready_Y);
		ini.write("CAPTURE_COORDS", "ready_X", "" + ready_X2);
		ini.write("CAPTURE_COORDS", "ready_Y", "" + ready_Y2);
		ini.write("CAPTURE_COORDS", "special_X1", "" + special_X1);
		ini.write("CAPTURE_COORDS", "special_X2", "" + special_X2);
		ini.write("CAPTURE_COORDS", "special_Xp", "" + special_Xp);
		ini.write("CAPTURE_COORDS", "special_Y1", "" + special_Y1);
		ini.write("CAPTURE_COORDS", "special_Y2", "" + special_Y2);
		ini.write("CAPTURE_COORDS", "special_capture_X", "" + special_capture_X);
		ini.write("CAPTURE_COORDS", "special_capture_Y", "" + special_capture_Y);
		ini.write("CAPTURE_COORDS", "special_capture_W", "" + special_capture_W);
		ini.write("CAPTURE_COORDS", "special_capture_H", "" + special_capture_H);
		if (!selectedChars.isEmpty())
			ini.write("SELECTED", "CHARACTERS", enumListToString(selectedChars));
		if (!selectedStages.isEmpty())
			ini.write("SELECTED", "STAGES", enumListToString(selectedStages));
		for (String userName : usersPicture.keySet())
			ImageUtils.saveImageToFile(usersPicture.get(userName), usersPictureDir.getAbsolutePath() + "\\" + userName + ".png");
	}

	static <T extends Enum<T>> String enumListToString(List<T> enumList) {
		String s = "";
		for (T e : enumList)
			s += (s.isEmpty() ? "" : " ") + e.name();
		return s;
	}

	public static void close() {
		saveConfigs();
		close = true;
		executor.shutdownNow();
		GlobalKeyListener.stopListener();
		Platform.exit();
		Misc.sleep(1000);
		System.exit(0);
	}

	static void refreshDebugWindows(boolean recreateCanvas) {
		if (!debugCapturedScreenshots && !debugCatchedInputs && !debugConsole)
			return;
		if (recreateCanvas)
			synchronized (canvasMainScreenshot) {
				synchronized (canvasButtonsScreenshot) {
					synchronized (canvasCatchedInputs) {
						synchronized (canvasConsole) {
							synchronized (gcMainScreenshot) {
								synchronized (gcButtonsScrenshot) {
									synchronized (gcCatchedInputs) {
										synchronized (gcConsole) {
											createCanvas();
										}
									}
								}
							}
						}
					}
				}
			}
		mainVBox = new VBox();
		if (debugCatchedInputs)
			mainVBox.getChildren().add(canvasMainScreenshot);
		if (debugCapturedScreenshots)
			mainVBox.getChildren().addAll(canvasButtonsScreenshot, canvasCatchedInputs);
		if (debugConsole)
			mainVBox.getChildren().add(canvasConsole);
		stageMain.setScene(new Scene(mainVBox));
		int y = 0;
		for (int n = 0; n < mainVBox.getChildren().size(); n++)
			y += ((Canvas)mainVBox.getChildren().get(n)).getHeight();
		stageMain.setX(debugPlayer == 1 ? 0 : DesktopUtils.getSystemScreenWidth() - stageMain.getWidth());
		stageMain.setY(start_Y - y - 10);
	}
	
	static void createCanvas() {
		canvasMainScreenshot = new Canvas(width * 7f, height + 20);
		canvasButtonsScreenshot = new Canvas(width * 7f, height);
		canvasCatchedInputs = new Canvas(width * 7f, height);
		canvasConsole = new Canvas(width * 7f, 230);
		gcMainScreenshot = canvasMainScreenshot.getGraphicsContext2D();
		gcButtonsScrenshot = canvasButtonsScreenshot.getGraphicsContext2D();
		gcCatchedInputs = canvasCatchedInputs.getGraphicsContext2D();
		gcConsole = canvasConsole.getGraphicsContext2D();
	}

	static void setGameAction(GameAction gameAction) {
		Main.gameAction = gameAction;
		drawConsole();		
	}

	static void refreshDebugWindows()
		{ refreshDebugWindows(false); }
	
	static void verifyWhichScreenIsRightNow() {
		BufferedImage sc = getScreenShot();
		if (ImageUtils.isImageContained(characterSelectScreenPng, sc) != null)
			runCharacterScreen();
		else if (gameAction == GameAction.WAITING_FOR_MUSIC_START)
			waitForSongStartAndCatchSongBpm();
		drawConsole();
	}
	
	static void mainLoop() {
		verifyWhichScreenIsRightNow();
		while (!close) {
			if (gameAction == GameAction.DANCING) {
				if (System.currentTimeMillis() >= nextCTimeToPress) {
					lastBeatDelay = System.currentTimeMillis() - nextCTimeToPress;
					int fixOverTime = (getBeatFixMsOvertime() != 0 && currentBeat % Math.abs(getBeatFixMsOvertime()) == 0 ? (getBeatFixMsOvertime() < 0 ? -1 : 1) : 0);
					long waitForNewInputs = (long)(nextCTimeToPress + getBeatDelay() / 2.75);
					nextCTimeToPress += getBeatDelay() + fixOverTime;
					pressFourthBeatButtons();
					CollectionUtils.copyArray(currentPlayerAction, previewPlayerAction);
					CollectionUtils.copyArray(haveInput, previewHaveInput);
					if (System.currentTimeMillis() < waitForNewInputs)
						Misc.sleep(waitForNewInputs - System.currentTimeMillis());
					if (!close && gameAction == GameAction.DANCING) {
						currentBeat++;
						updateStatus();
						if (getMusicEndBeat() == currentBeat) {
							setGameAction(GameAction.AT_WIN_SCREEN);
							Misc.sleep(12000);
							setGameAction(GameAction.AT_FERVER_TIME);
						}
						else if (getMusicEndBeat() == currentBeat + 1)
							atPreSolo = true;
					}
					drawConsole();
				}
				Misc.sleep(1);
			}
			else if (gameAction == GameAction.WAITING_FOR_MUSIC_START)
				waitForSongStartAndCatchSongBpm();
			else {
				verifyWhichScreenIsRightNow();
				drawConsole();
				Misc.sleep(100);
			}
		}
	}
	
	public static void voteOnCharacter(GameCharacter character, int total) {
		if (gameAction != GameAction.AT_CHAR_SELECT_SCREEN)
			return;
		if (!votedChars.containsKey(character))
			votedChars.put(character, 0);
		votedChars.put(character, votedChars.get(character) + total);
	}
	
	public static void voteOnCharacter(GameCharacter character)
		{ voteOnCharacter(character, 2); }

	public static void voteOnStage(GameStage stage, int total) {
		if (gameAction != GameAction.AT_STAGE_SELECT_SCREEN)
			return;
		if (!votedStages.containsKey(stage))
			votedStages.put(stage, 0);
		votedStages.put(stage, votedStages.get(stage) + total);
	}
	
	public static void voteOnStage(GameStage stage)
		{ voteOnStage(stage, 2); }

	static void clickOnCenterScreen() {
		mainRobot().mouseMove(DesktopUtils.getSystemScreenWidth() / 2, DesktopUtils.getSystemScreenHeight() / 2);
		mainRobot().delay(30);
		mainRobot().mousePress(MouseEvent.BUTTON1_DOWN_MASK);
		mainRobot().delay(30);
		mainRobot().mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);
	}
	
	static void runCharacterScreen() {
		sendToConsole("ENTERED AT THE SELECT CHARACTER SCREEN");
		setGameAction(GameAction.AT_CHAR_SELECT_SCREEN);
		clickOnCenterScreen();
		currentBeat = 0;
		voteTimeOut = System.currentTimeMillis() + selectionByVoteTimeOut;
		selectedChar = 0;
		CharacterSelector[] choice = {new CharacterSelector(CharacterSelector.getRandom()), new CharacterSelector(CharacterSelector.getRandom())};
		if (selectedChars.size() >= CharacterSelector.getCharactersList().size()) { // Se a lista de personagens já escolhidos chegar ao limite, zerar a lista e alterar o outfit para a próxima remessa de escolhas
			selectedChars.clear();
			alternativeOutfit = !alternativeOutfit;
		}
		if (selectedStages.size() >= GameStage.getListOfAll().size())
			selectedStages.clear();
		for (int pl = 0; !close && pl < 2; pl++) {
			playerDificult[pl] = 1;
			while (!close && (selectedChars.contains(choice[pl].getCharacter()) || choice[pl].getCharacter().equals(choice[pl == 0 ? 1 : 0].getCharacter()))) // Enquanto o char escolhido aleatoriamente já tiver sido escolhido previamente, ou for o mesmo personagem do outro jogador, escolher o proximo
				choice[pl].getNext();
			robot[pl].delay(random.nextInt(1000) + 1000);
			votedChars.put(choice[pl].getCharacter(), 1);
		}
		for (int pll = 0; !close && pll < 2; pll++) {
			final int pl = pll;
			new Thread(() -> { // Selecionando personagem
				while (!close && currentVotedChars() != null && (!selectionByVote || System.currentTimeMillis() < voteTimeOut)) {
					moveCursorTo(pl, new CharacterSelector(currentVotedChars().get(pl)));
					Misc.sleep(100);
					if (!selectionByVote)
						break;
				}
				if (!selectedChars.contains(characterSelector[pl].getCharacter()))
					selectedChars.add(characterSelector[pl].getCharacter());
				if (!close) {
					robot[pl].delay(random.nextInt(100) + 100);
					pressButton(pl, alternativeOutfit ? GamepadButton.Y : GamepadButton.B);
					robot[pl].delay(random.nextInt(100) + 100);
					for (int n = 0, r = random.nextInt(6); !close && n <= r; n++) { // Seleciona a dificuldade aleatoriamente
						robot[pl].delay(random.nextInt(150) + 150);
						if (n < r && ++playerDificult[pl] == 3)
							playerDificult[pl] = 0;
						pressButton(pl, n < r ? GamepadButton.DOWN : GamepadButton.B);
					}
					selectedChar++;
				}
				votedChars.remove(choice[pl].getCharacter());
			}).start();
		}
		while (!close && selectedChar < 2)
			Misc.sleep(500);
		voteTimeOut = System.currentTimeMillis() + selectionByVoteTimeOut;
		gameAction = GameAction.AT_STAGE_SELECT_SCREEN;
		boolean someoneSelectedRoboZ = characterSelector[0].getCharacter().equals(GameCharacter.ROBO_Z_GOLD) || characterSelector[1].getCharacter().equals(GameCharacter.ROBO_Z_GOLD);
		// Seleciona o stage aleatoriamente, tomando cuidado para não tentar escolher stage já escolhido previamente, ou escolher o stage do Robo Z sem que ninguém tenha escolhido ele, pois o stage dele só aparece se alguém tiver escolhido ele
		if (!close)
			robot[0].delay(random.nextInt(1000) + 1000);
		GameStage stage = GameStage.getRandom(someoneSelectedRoboZ);
		clickOnCenterScreen();
		while (!close && selectedStages.contains(stage))
			stage = stage.getNext(someoneSelectedRoboZ);
		votedStages.put(stage, 1);
		while (!close && currentVotedStages() != null && (!selectionByVote || System.currentTimeMillis() < voteTimeOut)) {
			stage = currentVotedStages().get(0);
			while (!close && !currentStage.equals(stage)) {
				pressButton(0, currentStage.getValue() < stage.getValue() ? GamepadButton.RIGHT : GamepadButton.LEFT);
				currentStage = currentStage.getValue() < stage.getValue() ? currentStage.getNext(someoneSelectedRoboZ) : currentStage.getPreview(someoneSelectedRoboZ);
				robot[0].delay(random.nextInt(150) + 150);
			}
			Misc.sleep(100);
			if (!selectionByVote)
				break;
		}
		for (int pl = 0; pl < 2; pl++)
			if (votedChars.containsKey(characterSelector[pl].getCharacter()))
				votedChars.remove(characterSelector[pl].getCharacter());
		if (votedStages.containsKey(currentStage))
				votedStages.remove(currentStage);
		if (!close) {
			selectedStages.add(stage);
			pressButton(0, GamepadButton.B);
			Platform.runLater(() -> {
				String[] dif = {"EASY", "NORMAL", "MIX"};
				sendToConsole("P1: " + characterSelector[0].getCharacter() + " (" + dif[playerDificult[0]] + ")");
				sendToConsole("P2: " + characterSelector[1].getCharacter() + " (" + dif[playerDificult[1]] + ")");
				sendToConsole("STAGE: " + currentStage);
			});
			while (!close && ImageUtils.isImageContained(stageLoadingScreenPng, getScreenShot()) == null);
			while (!close && ImageUtils.isImageContained(stageLoadingScreenPng, getScreenShot()) != null);
			waitForSongStartAndCatchSongBpm();
		}
	}
	
	static LinkedList<GameCharacter> currentVotedChars() {
		if (votedChars == null)
			return null;
		synchronized (votedChars) {
			LinkedList<GameCharacter> top = new LinkedList<>(votedChars.keySet());
			top.sort((o1, o2) -> votedChars.get(o1) < votedChars.get(o2) ? 1 :
				votedChars.get(o1) > votedChars.get(o2) ? -1 : 0);
			return top;
		}
	}

	static LinkedList<GameStage> currentVotedStages() {
		if (votedStages == null)
			return null;
		synchronized (votedStages) {
			LinkedList<GameStage> top = new LinkedList<>(votedStages.keySet());
			top.sort((o1, o2) -> votedStages.get(o1) < votedStages.get(o2) ? 1 :
				votedStages.get(o1) > votedStages.get(o2) ? -1 : 0);
			return top;
		}
	}

	static void moveCursorTo(int player, CharacterSelector selector) {
		while (!close && !characterSelector[player].isAtSameRow(selector)) { // Movendo o cursor verticalmente até chegar na linha do personagem escolhido
			if (characterSelector[player].getCursorY() > selector.getCursorY() && characterSelector[player].getCursorY() == 1 &&
					(characterSelector[player].getCursorX() == 0 || characterSelector[player].getCursorX() == 3)) {
						robot[player].delay(random.nextInt(100) + 200);
						pressButton(player, characterSelector[player].getCursorX() == 0 ? GamepadButton.RIGHT : GamepadButton.LEFT);
						characterSelector[player].moveCursorPosition(characterSelector[player].getCursorX() == 0 ? Direction.RIGHT : Direction.LEFT);
			}
			robot[player].delay(random.nextInt(100) + 200);
			pressButton(player, characterSelector[player].getCursorY() < selector.getCursorY() ? GamepadButton.DOWN : GamepadButton.UP);
			characterSelector[player].moveCursorPosition(characterSelector[player].getCursorY() < selector.getCursorY() ? Direction.DOWN : Direction.UP);
		}
		while (!close && !characterSelector[player].isAtSameColumn(selector)) { // Movendo o cursor horizontalmente até chegar na coluna do personagem escolhido
			robot[player].delay(random.nextInt(100) + 200);
			pressButton(player, characterSelector[player].getCursorX() < selector.getCursorX() ? GamepadButton.RIGHT : GamepadButton.LEFT);
			characterSelector[player].moveCursorPosition(characterSelector[player].getCursorX() < selector.getCursorX() ? Direction.RIGHT : Direction.LEFT);
		}
	}

	static void updateSpecialCount() {
		BufferedImage bi = ImageUtils.getScreenShot(new Rectangle(special_capture_X, special_capture_Y, special_capture_W, special_capture_H));
		int[] previewSpecials = {specials[0], specials[1]};
		for (int rgb, pl = 0; pl < 2; pl++) {
			if (currentPlayerAction[pl] != PlayerAction.STUNNED) {
				rgb = 0;
				specials[pl] = 0;
				for (int z = 0; z < 2; z++) {
					int x = z == 0 ? (pl == 0 ? special_X1 : special_X2) : (pl == 0 ? special_X1 + special_Xp : special_X2 - special_Xp);
					int y = z == 0 ? special_Y1 : special_Y2;
					rgb = ImageUtils.getRgbaArray(bi.getRGB(x, y))[0] +
								ImageUtils.getRgbaArray(bi.getRGB(x, y))[1] +
								ImageUtils.getRgbaArray(bi.getRGB(x, y))[2];
					specials[pl] += rgb > 200 ? 1 : 0;
				}
				if (previewSpecials[pl] != specials[pl]) // Se o contador de special mudou, marca o player como 'atacando'
					currentPlayerAction[pl] = PlayerAction.ATTACKING;
			}
		}
	}

	static void updateStatus() {
		if (close || gameAction != GameAction.DANCING)
			return;
		updateCurrentInputs();
		runPlayerInputs();
		if (atSolo && haveInput[0] && haveInput[1])
			atSolo = false;
		canStealSolo = false;
		for (int n = 4; n < songBeatInfo.length; n++)
			for (int n2 = 1; n2 <= 3; n2++)
				if (songBeatInfo[n] == currentBeat + n2)
					canStealSolo = true;
		for (int n = 4; n < songBeatInfo.length; n++) {
			if (songBeatInfo[n] == currentBeat - 1) {
				atSolo = true;
				atPreSolo = false;
			}
			if (songBeatInfo[n] == currentBeat)
				atPreSolo = true;
		}
		for (int pl = 0; pl < 2; pl++) {
			if (!haveInput[pl]) // Se não há comandos na tela do jogador, marca-lo como 'STUNNED'
				currentPlayerAction[pl] = PlayerAction.STUNNED;
			else if (!previewHaveInput[pl]) // Se não tinha comandos no último compasso, e agora tem, marcar jogador como 'DANCING'
				currentPlayerAction[pl] = PlayerAction.DANCING;
			else if (currentPlayerAction[pl] == PlayerAction.COUNTERING) // Se o jogador estava marcado como 'COUNTERING', marca-lo como 'ATTAKCING'
				currentPlayerAction[pl] = PlayerAction.ATTACKING;
		}
		updateSpecialCount();
		updateFourthBeatButtons();
	}

	static void waitForSongStartAndCatchSongBpm() {
		setGameAction(GameAction.WAITING_FOR_MUSIC_START);
		Rectangle rect1 = new Rectangle(ready_X, ready_Y, 3, 3), rect2 = new Rectangle(ready_X2, ready_Y2, 3, 3);
		while (!close && gameAction == GameAction.WAITING_FOR_MUSIC_START && ImageUtils.isImageContained(readyPng, getScreenShot(2, rect1)) == null);
		while (!close && gameAction == GameAction.WAITING_FOR_MUSIC_START && ImageUtils.isImageContained(readyPng, getScreenShot(2, rect2)) == null);
		while (!close && gameAction == GameAction.WAITING_FOR_MUSIC_START && ImageUtils.isImageContained(readyPng, getScreenShot(2, rect2)) != null);
		if (close || gameAction != GameAction.WAITING_FOR_MUSIC_START)
			return;
		currentBeat = 1;
		songBeatInfo = songBeatInfos.get(currentStage);
		for (int pl = 0; pl < 2; pl++) {
			specials[pl] = 2;
			currentPlayerAction[pl] = PlayerAction.DANCING;
		}
		atSolo = false;
		atPreSolo = false;
		repeatedCommands = new int[] {0, 0};
		lastCommand = new String[] {"", ""};
		int bpm = getBeatDelay();
		nextCTimeToPress = (long)(System.currentTimeMillis() + (bpm - bpm / 4 - bpm / getBeatStartingDelay()));
		setGameAction(GameAction.DANCING);
		for (int pl = 0; pl < 2; pl++) {
			specials[pl] = 2;
			currentPlayerAction[pl] = PlayerAction.DANCING;
		}
		updateStatus();
	}
	
	static void updateCurrentInputs() {
		for (int pl = 0; pl < 2; pl++) {
			currentInputs.set(pl, getCurrentInputs(pl));
			if (pl + 1 == debugPlayer && debugCatchedInputs)
				drawCommands(currentInputs.get(pl));
			haveInput[pl] = !currentInputs.get(pl).isEmpty();			
			beatButton[pl] = !haveInput[pl] ? null : currentInputs.get(pl).get(currentInputs.get(pl).size() - 1);
		}
	}
	
	static void runPlayerInputs() {
		for (int pl = 0; pl < 2; pl++) {
			final int player = pl;
			if (gameAction == GameAction.DANCING) {
				final String inputsStr = inputToString(currentInputs.get(player));
				if (!inputsStr.isEmpty() && lastCommand[player].equals(inputsStr)) {
					if (++repeatedCommands[player] >= 3) {
						sendToConsole("3+ MISS FOUND. DOING RETRY");
						setGameAction(GameAction.WAITING_FOR_MUSIC_START);
						Misc.sleep(2000);
						pressButton(player, GamepadButton.START);
						Misc.sleep(2000);
						pressButton(player, GamepadButton.B);
					}
				}
				lastCommand[player] = inputsStr;
			}
			if (currentInputs.get(player).size() > 1) {
				executor.execute(new Task<Void> () {
					@Override
					protected Void call() throws Exception {
						List<GamepadButton> inputs = new LinkedList<>(currentInputs.get(player).subList(0, currentInputs.get(player).size() - 1));
						int totalInputs = inputs.size();
						if (random.nextInt(10000) + 1 <= intentionalMissInputProc) {
							sendToConsole("P" + (player + 1) + " ERROU O COMANDO PROPOSITALMENTE");
							for (int n = 0; n < 4; n++)
								inputs.add(getAttackButton(player));
						}
						if (((player + 1) & autoPlay) != 0 && totalInputs > 0) {
							int delay = (int)(nextCTimeToPress - System.currentTimeMillis()) / (totalInputs + 1); 
							pressButtons(player, inputs, delay);
						}
						return null;
					}
				});
			}
		}
	}

	static String inputToString(List<GamepadButton> inputs) {
		String s = "";
		if (!inputs.isEmpty()) {
			if (inputs.size() > 1)
				for (GamepadButton b : inputs.subList(0, inputs.size() - 1))
					s += b.name().charAt(0) + "  ";
			s += "[" + inputs.get(inputs.size() - 1).name().charAt(0) + "]";
		}
		return s;
	}

	static List<GamepadButton> getCurrentInputs(int player) {
		boolean debug = player + 1 == debugPlayer;
		List<GamepadButton> inputs = new LinkedList<>();
		Rectangle rect = new Rectangle(start_X + player * spacing, start_Y, (int)(width * 7.2f), height);
		BufferedImage sc = getScreenShot(player, rect);
		if (debug && debugCapturedScreenshots)
			gcCatchedInputs.drawImage(SwingFXUtils.toFXImage(sc, null), 0, 0);
		for (int w = 0; w < 7; w++) {
			int x = (int)(w * width + (w < 6 ? 0 : spacing2));
			Rectangle rect2 = new Rectangle(x + 10, 0, width - 20, height);
			if (debug && debugCapturedScreenshots)
				gcButtonsScrenshot.drawImage(SwingFXUtils.toFXImage(ImageUtils.copyAreaFromBufferedImage(sc, rect2), null), x + (w < 6 ? 1 * w : 1) - 19 * w, 0);
			for (int n = 0; n < 8; n++) {
				Point p = ImageUtils.isImageContained(BUTTONS_PNG.get(n), sc, rect2);
				if (p != null)
					inputs.add(GamepadButton.getButton(n));
			}
		}
		return inputs;
	}

	static void updateFourthBeatButtons() {
		for (int p1 = 0, p2 = 1; p1 < 2; p1++, p2--) {
			if (!atSolo && currentPlayerAction[p1] != PlayerAction.STUNNED) { // Se não está no SOLO, e o jogador não estiver marcado como 'STUNNED'
				if (currentPlayerAction[p2] == PlayerAction.ATTACKING) { // Se não estiver no pre-solo, e o oponente estiver marcado como 'ATTACKING'
					if (!atPreSolo && random.nextInt(10000) + 1 <= (canStealSolo ? preSoloCounterProc : nonPreSoloCounterProc)) { // Chance de refletir o special do oponente
						beatButton[p1] = getAttackButton(p1);
						currentPlayerAction[p1] = PlayerAction.COUNTERING;
					}
					else if (random.nextInt(10000) + 1 <= dodgeUponFailCounterProc) // Chance de esquivar o ataque, caso falhe a condição anterior
						beatButton[p1] = getDodgeButton(p1);
					else // Se falhar as 2 condições, simplesmente recebe o ataque
						beatButton[p1] = null;
					sendToConsole("P" + (p1 + 1) + (beatButton[p1] == null ? " FALHOU AO TENTAR REFLETIR SPECIAL" : beatButton[p1] == getDodgeButton(p1) ? " DECIDIU ESQUIVAR AO INVÉS DE REFLETIR" : " REFLETIU SPECIAL"));
				}
				else if (specials[p1] > 0 && !atPreSolo && currentPlayerAction[p2] != PlayerAction.STUNNED &&
								 previewPlayerAction[p1] != PlayerAction.ATTACKING &&
								 currentPlayerAction[p1] != PlayerAction.ATTACKING &&
								 random.nextInt(10000) <= (canStealSolo ? preSoloAttackProc : nonPreSoloAttackProc)) {
										beatButton[p1] = getAttackButton(p1);
										sendToConsole("P" + (p1 + 1) + " USOU SPECIAL");
				}
			}
		}
	}

	static void pressFourthBeatButtons() {
		lastFinishButton = (lastFinishButton + 1) % 2;
		int p1 = lastFinishButton, p2 = (p1 + 1) % 2;
		if (((p1 + 1) & autoPlay) != 0 && beatButton[p1] != null && currentPlayerAction[p1] != PlayerAction.STUNNED)
			robot[p1].keyPress(KEY_CODES[p1][beatButton[p1].getValue()]);
		if (((p2 + 1) & autoPlay) != 0 && beatButton[p2] != null && currentPlayerAction[p2] != PlayerAction.STUNNED)
			robot[p2].keyPress(KEY_CODES[p2][beatButton[p2].getValue()]);
		for (int pl = 0; pl < 2; pl++) {
			if (beatButton[pl] != null) {
				robot[pl].delay(40);
				robot[pl].keyRelease(KEY_CODES[pl][beatButton[pl].getValue()]);
			}
		}
		for (int pl = 0; pl < 2; pl++) {
			if (beatButton[pl] == getDodgeButton(pl)) // Se o botão pressionado atualmente for o botão de esquiva, marcar o jogador como 'DODGEING'
				currentPlayerAction[pl] = PlayerAction.DODGEING;
			else if (beatButton[pl] != getAttackButton(pl)) // Se o botão pressionado atualmente não for nem esquiva e nem ataque, marcar jogador como 'DANCING'
				currentPlayerAction[pl] = PlayerAction.DANCING;
			if (beatButton[pl] == getDodgeButton(pl) || beatButton[pl] == getAttackButton(pl)) {
				repeatedCommands[pl] = 0;
				lastCommand[pl] = "";
			}

		}
	}

	static void drawCommands(List<GamepadButton> inputs) {
		Platform.runLater(() -> {
			gcMainScreenshot.setFill(Color.BLACK);
			gcMainScreenshot.fillRect(0, 0, canvasMainScreenshot.getWidth(), canvasMainScreenshot.getHeight());
			int w = (int)(canvasCatchedInputs.getWidth());
			for (int i = inputs.size() - 1; i >= 0; i--) {
				GamepadButton b = inputs.get(i);
				gcMainScreenshot.drawImage(SwingFXUtils.toFXImage(BUTTONS2_PNG.get(b.getValue()), null), w -= width, 12);
			}
		});
	}
	
	public static void pressButtons(int player, List<GamepadButton> inputs, int holdDelay) {
		new Thread(() -> {
			for (GamepadButton b : inputs)
				if (b != null) {
					robot[player].keyPress(KEY_CODES[player == 2 ? 0 : player][b.getValue()]);
					robot[player].delay(holdDelay / 2);
					robot[player].keyRelease(KEY_CODES[player == 2 ? 0 : player][b.getValue()]);  
					robot[player].delay(holdDelay / 2);
				}
		}).start();
	}

	public static void pressButton(int player, GamepadButton input, int holdDelay) //
		{ pressButtons(player, Arrays.asList(input), holdDelay); }

	public static void pressButton(int player, GamepadButton input) //
		{ pressButton(player, input, 50); }
	
	public static void pressButton(GamepadButton input) //
		{ pressButton(2, input, 50); }

	static BufferedImage getScreenShot(int player, Rectangle captureArea) {
		MultiResolutionImage multiResolutionImage = robot[player].createMultiResolutionScreenCapture(captureArea);
		return (BufferedImage) multiResolutionImage.getResolutionVariant(captureArea.width, captureArea.height);
	}
	
	static BufferedImage getScreenShot()
		{ return getScreenShot(2, new Rectangle(0, 0, DesktopUtils.getSystemScreenWidth(), DesktopUtils.getSystemScreenHeight())); }

	static void drawConsole() {
		Platform.runLater(() -> {
			clearConsole();
			gcConsole.setFill(Color.YELLOW);

			int w = (int)canvasConsole.getWidth();
			int h = (int)canvasConsole.getHeight();
			int line = 20, lineSpacing = 15;
			int halfWidth = w / 2 + 10;
			
			gcConsole.fillText(String.format("Game action: %s", gameAction.name()), 10, line);
			gcConsole.fillText(String.format("Last beat delay: %d", lastBeatDelay), 10, line += lineSpacing);
			gcConsole.fillText(String.format("Beat: %d", currentBeat), 10, line += lineSpacing);
			gcConsole.fillText(String.format("Can steal solo: %s", canStealSolo ? "TRUE" : "FALSE"), halfWidth, line);
			gcConsole.fillText(String.format("Pre-Solo: %s", atPreSolo ? "TRUE" : "FALSE"), 10, line += lineSpacing);
			gcConsole.fillText(String.format("Solo: %s", atSolo ? "TRUE" : "FALSE"), halfWidth, line);

			for (int pl = 0, l = line += lineSpacing, col; pl < 2; pl++) {
				line = l;
				col = pl == 0 ? 10 : halfWidth;
				gcConsole.fillText(String.format("[Player %d]", pl + 1), col, line += lineSpacing);
				gcConsole.fillText(String.format("Current Action: %s", currentPlayerAction[pl].name()), col, line += lineSpacing);
				gcConsole.fillText(String.format("Beat button: %s", beatButton[pl] == null ? "NONE" : beatButton[pl].name()), col, line += lineSpacing);
				gcConsole.fillText(String.format("Specials: %d", specials[pl]), col, line += lineSpacing);
				gcConsole.fillText(String.format("Inputs: %s", currentInputs.get(pl) == null ? "NULL" : inputToString(currentInputs.get(pl))), col, line += lineSpacing);
			}
			if (debugConsole && !consoleMsgBuffer.isEmpty()) {
				line = h - lineSpacing * 4 - 8;
				for (String s : consoleMsgBuffer)
					gcConsole.fillText(s, 10, line += lineSpacing);
			}
		});
	}
	
	static void clearConsole() {
		if (debugConsole) {
			gcConsole.setFill(Color.BLACK);
			gcConsole.fillRect(0, 0, canvasConsole.getWidth(), canvasConsole.getHeight());
			drawConsole();
		}
	}
	
	static void sendToConsole(String text) {
		if (debugConsole) {
			consoleMsgBuffer.add("[Beat " + currentBeat + "]: " + text);
			if (consoleMsgBuffer.size() > 4)
				consoleMsgBuffer.remove(0);
		}
	}
	
	static <T extends Number & Comparable<? super T>> void sendToConsole(T number)
		{ sendToConsole("" + number); }
	
	public static void main(String[] args)
		{ launch(args); }
	
}