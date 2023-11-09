package application;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

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
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import util.DesktopUtils;
import util.IniFile;
import util.Misc;
import util.Sounds;

public class Main extends Application {
	
		/** Parâmetros otimizados para emulador RetroArch em modo janela maximizada,
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
	final static String[] KEY_NAMES = {"LEFT", "DOWN", "RIGHT", "UP", "A", "B", "X", "Y"};
	final static int[][] KEY_CODES = {{KeyEvent.VK_A, KeyEvent.VK_S, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_NUMPAD1, KeyEvent.VK_NUMPAD2, KeyEvent.VK_NUMPAD4, KeyEvent.VK_NUMPAD5, KeyEvent.VK_NUMPAD6, KeyEvent.VK_NUMPAD8, KeyEvent.VK_ENTER},
																				{KeyEvent.VK_F, KeyEvent.VK_G, KeyEvent.VK_H, KeyEvent.VK_T, KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_U, KeyEvent.VK_I, KeyEvent.VK_O, KeyEvent.VK_L, KeyEvent.VK_M}};
	@SuppressWarnings("serial")
	final static List<BufferedImage> BUTTONS_PNG = new ArrayList<>() {{
		for (int n = 0; n < KEY_NAMES.length; n++)
			add(ImageUtils.loadBufferedImageFromFile(".\\images\\buttons\\" + KEY_NAMES[n].charAt(0) + ".png"));
	}};
	@SuppressWarnings("serial")
	final static List<BufferedImage> BUTTONS2_PNG = new ArrayList<>() {{
		for (int n = 0; n < KEY_NAMES.length; n++)
			add(ImageUtils.loadBufferedImageFromFile(".\\images\\inputs\\" + KEY_NAMES[n].charAt(0) + ".png"));
	}};

	final static BufferedImage characterSelectScreenPng = ImageUtils.loadBufferedImageFromFile(new File(".\\images\\misc\\character_select_screen.png"));
	final static BufferedImage stageLoadingScreenPng = ImageUtils.loadBufferedImageFromFile(new File(".\\images\\misc\\stage_loading.png"));
	final static BufferedImage readyPng = ImageUtils.loadBufferedImageFromFile(new File(".\\images\\misc\\ready.png"));
	final static BufferedImage missPng = ImageUtils.loadBufferedImageFromFile(new File(".\\images\\misc\\miss.png"));
	final static BufferedImage continuePng = ImageUtils.loadBufferedImageFromFile(new File(".\\images\\misc\\continue.png"));
	final static Random random = new Random(new SecureRandom().nextInt(Integer.MAX_VALUE));
	final static IniFile ini = IniFile.getNewIniFileInstance("config.ini");
  final static ExecutorService executor = Executors.newFixedThreadPool(3);

	static int start_X;
	static int start_Y;
	static int width;
	static int height;
	static int spacing;
	static int spacing2;

	static Task<Void> mainTask;
	static Robot[] robot;
	static Stage mainStage;
	static VBox mainVBox;
	static Canvas canvasMainScreenshot;
	static Canvas canvasButtonsScreenshot;
	static Canvas canvasCatchedInputs;
	static Canvas canvasConsole;
	static GraphicsContext gcMainScreenshot;
	static GraphicsContext gcButtonsScrenshot;
	static GraphicsContext gcCatchedInputs;
	static GraphicsContext gcConsole;

	// CONFIGS (Definir os valores através do arquivo ini
	static int debugPlayer; // Qual jogador será debugado
	static int autoPlay; // Ativa o autplay para: 0 (Desligado) 1 (Jogador 1), 2 (Jogador 2) 3 (Ambos)
	static boolean debugCapturedScreenshots; // Janela de debug para visualizar as áreas de capturas dos botões
	static boolean debugCatchedInputs; // Janela de debug que exibe os botões reconhecidos em tempo real (se bater com os comandos atuais do jogo, é porque tudo está configurado corretamente)
	static boolean debugConsole; // Janela de debug que exibe valores em texto
	static boolean fixKeysAreEnabled; // Usar setas para alterar os valores START_X e START_Y, WIDTH e HEIGHT (CTRL pressionado), SPACING e SPACING2 (SHIFT pressionado)
	
	@SuppressWarnings("serial")
	static List<List<GamepadButton>> currentInputs = new ArrayList<>() {{
		add(new ArrayList<>());
		add(new ArrayList<>());
	}};
	@SuppressWarnings("serial")
	static Map<GameStage, int[]> songBeatInfos = new HashMap<>() {{
				put(GameStage.HEAT, new int[] {2099, 50, 4, 62, 8, 31});
				put(GameStage.COMET, new int[] {1913, 25, 2, 72, 24, 52});
				put(GameStage.SHORTY, new int[] {1994, 0, -1, 0, 0, 0}); 
				put(GameStage.STRIKE, new int[] {2099, 0, 0, 0, 0, 0});
				put(GameStage.CAPOEIRA, new int[] {2099, 0, 0, 0, 0, 0});
				put(GameStage.KITTY_N, new int[] {2099, 0, 0, 0, 0, 0});
				put(GameStage.TSUTOMO, new int[] {2099, 0, 0, 0, 0, 0});
				put(GameStage.HIRO, new int[] {2099, 0, 0, 0, 0, 0});
				put(GameStage.KELLY, new int[] {2099, 0, 0, 0, 0, 0});
				put(GameStage.BIO, new int[] {2099, 0, 0, 0, 0, 0});
				put(GameStage.ROBO_Z_GOLD, new int[] {2099, 0, 0, 0, 0, 0}); 
				put(GameStage.PANDER, new int[] {1495, 40, 2, 96, 16, 48});
	}};
	static boolean testing = false;
	static int testFixStartDelay = 80;
	static List<GameCharacter> selectedChars = new ArrayList<>();
	static List<GameStage> selectedStages = new ArrayList<>();
	static List<Integer> arrowKeys = new ArrayList<>(Arrays.asList(NativeKeyEvent.VC_LEFT, NativeKeyEvent.VC_UP, NativeKeyEvent.VC_RIGHT, NativeKeyEvent.VC_DOWN));
	static List<String> consoleMsgBuffer = new ArrayList<>();
	static GameStage currentStage = GameStage.HEAT;
	static GameAction gameAction = GameAction.NOTHING;
	static GameCharacter character[] = {GameCharacter.HEAT, GameCharacter.COMET};
	static PlayerAction previewPlayerAction[] = {PlayerAction.DANCING, PlayerAction.DANCING};
	static PlayerAction currentPlayerAction[] = {PlayerAction.DANCING, PlayerAction.DANCING};
	static GamepadButton beatButton[] = {null, null};
	static int songBeatInfo[];
	static boolean close = false;
	static boolean alternativeOutfit = false;
	static boolean atSolo = false;
	static boolean atPreSolo = false;
	static boolean canStealSolo = false;
	static boolean previewHaveInput[] = {false, false};
	static boolean haveInput[] = {false, false};
	static int selectedChar = 0;
	static long nextCTimeToPress = 0;
	static int lastFinishButton = random.nextInt(1);
	static int currentBeat = 0;
	static int specials[] = {0, 0};
	static int playerDificult[] = {1, 1};
	static long lastBeatDelay;
	static int nonPreSoloAttackProc;
	static int preSoloAttackProc;
	static int nonPreSoloCounterProc;
	static int preSoloCounterProc;
	static int dodgeUponFailCounterProc;
	static int intentionalMissInputProc;
	static List<Integer> beatsTestDelay = new ArrayList<>();
	static long lastBeatTestDelay = 0;
	
	
	@Override
	public void start(Stage stage) throws Exception {
		loadConfigs();
		createCanvas();
		setStage(stage);
		initRobot();
		setGlobalKeyListeners();

		ImageUtils.setImageScanOrientation(ImageScanOrientation.VERTICAL);
		ImageUtils.setImageScanIgnoreColor(Color.WHITE);
		if (debugCapturedScreenshots || debugCatchedInputs || debugConsole) {
			refreshDebugWindows(true);
			mainStage.show();
		}
		
		executor.execute(mainTask = new Task<Void> () {
			@Override
			protected Void call() throws Exception {
				mainLoop();
				return null;
			}
		});
	}
	
	static void setStage(Stage stage) {
		mainStage = stage;
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
		{ return songBeatInfo[1] + testFixStartDelay; }
	
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
				if (k.getKeyCode() == NativeKeyEvent.VC_R) // R reseta para o modo 'WAITING_FOR_MUSIC_START'
					gameAction = GameAction.WAITING_FOR_MUSIC_START;
				else if (k.getKeyCode() == NativeKeyEvent.VC_UP) {
					testFixStartDelay += 5;
					Platform.runLater(() -> sendToConsole("testFixStartDelay: " + testFixStartDelay));
				}
				else if (k.getKeyCode() == NativeKeyEvent.VC_DOWN) {
					testFixStartDelay -= 5;
					Platform.runLater(() -> sendToConsole("testFixStartDelay: " + testFixStartDelay));
				}
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
			if (testing && lastBeatTestDelay > 0 && (k.getKeyCode() == NativeKeyEvent.VC_1 || k.getKeyCode() == NativeKeyEvent.VC_2)) {
				int delay = (int)(System.currentTimeMillis() - lastBeatTestDelay), media = 0;
				beatsTestDelay.add(delay);
				for (int i : beatsTestDelay)
					media += i;
				media /= beatsTestDelay.size();
				System.out.println(delay + "ms (Media: " + media + "ms, x4 = " + (media * 4) + "ms)");
			}
			lastBeatTestDelay = System.currentTimeMillis();
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
		debugCapturedScreenshots = ini.readAsBoolean("CONFIG", "debugCapturedScreenshots", false);
		debugCatchedInputs = ini.readAsBoolean("CONFIG", "debugCatchedInputs", false);
		debugConsole = ini.readAsBoolean("CONFIG", "debugConsole", false);
		fixKeysAreEnabled = ini.readAsBoolean("CONFIG", "fixKeysAreEnabled", false);
		start_X = ini.readAsInteger("CAPTURE_COORDS", "start_X", 313);
		start_Y = ini.readAsInteger("CAPTURE_COORDS", "start_Y", 413);
		width = ini.readAsInteger("CAPTURE_COORDS", "width", 56);
		height = ini.readAsInteger("CAPTURE_COORDS", "height", 45);
		spacing = ini.readAsInteger("CAPTURE_COORDS", "spacing", 514);
		spacing2 = ini.readAsInteger("CAPTURE_COORDS", "spacing2", 5);
		currentStage = ini.readAsEnum("DEBUG", "currentStage", GameStage.class, GameStage.HEAT);
		gameAction = ini.readAsEnum("DEBUG", "gameAction", GameAction.class, GameAction.NOTHING);
		List<GameCharacter> chars = ini.readAsEnumList("DEBUG", "characters", GameCharacter.class, Arrays.asList(GameCharacter.HEAT, GameCharacter.COMET));
		for (int n = 0; n < 2; n++)
			character[n] = (GameCharacter) chars.get(n);
		if (ini.read("SELECTED", "CHARACTERS") != null)
			for (String item : ini.read("SELECTED", "CHARACTERS").split(" "))
				selectedChars.add(GameCharacter.valueOf(item));
		if (ini.read("SELECTED", "STAGES") != null)
			for (String item : ini.read("SELECTED", "STAGES").split(" "))
				selectedStages.add(GameStage.valueOf(item));
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
		ini.write("CAPTURE_COORDS", "start_X", "" + start_X);
		ini.write("CAPTURE_COORDS", "start_Y", "" + start_Y);
		ini.write("CAPTURE_COORDS", "width", "" + width);
		ini.write("CAPTURE_COORDS", "height", "" + height);
		ini.write("CAPTURE_COORDS", "spacing", "" + spacing);
		ini.write("CAPTURE_COORDS", "spacing2", "" + spacing2);
		if (!selectedChars.isEmpty())
			ini.write("SELECTED", "CHARACTERS", enumListToString(selectedChars));
		if (!selectedStages.isEmpty())
			ini.write("SELECTED", "STAGES", enumListToString(selectedStages));
	}

	private static <T extends Enum<T>> String enumListToString(List<T> enumList) {
		String s = "";
		for (T e : enumList)
			s += (s.isEmpty() ? "" : " ") + e.name();
		return s;
	}

	static void close() {
		saveConfigs();
		close = true;
		executor.shutdown();
		GlobalKeyListener.stopListener();
		Platform.exit();
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
		mainStage.setScene(new Scene(mainVBox));
		int y = 0;
		for (int n = 0; n < mainVBox.getChildren().size(); n++)
			y += ((Canvas)mainVBox.getChildren().get(n)).getHeight();
		mainStage.setX(debugPlayer == 1 ? 0 : DesktopUtils.getSystemScreenWidth() - mainStage.getWidth());
		mainStage.setY(start_Y - y - 10);
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
		else if (ImageUtils.isImageContained(continuePng, sc) != null) {
			sendToConsole("PRESSED START FOR CONTINUE");
			setGameAction(GameAction.WAITING_FOR_MUSIC_START);
			pressButton(GamepadButton.START);
		}
		else if (gameAction == GameAction.WAITING_FOR_MUSIC_START)
			waitForSongStartAndCatchSongBpm();
		drawConsole();
	}
	
	static void mainLoop() {
		boolean pair = false;
		verifyWhichScreenIsRightNow();
		while (!close) {
			if (gameAction == GameAction.DANCING) {
				if (System.currentTimeMillis() >= nextCTimeToPress) {
					lastBeatDelay = System.currentTimeMillis() - nextCTimeToPress;
					int fixOverTime = (getBeatFixMsOvertime() != 0 && currentBeat % Math.abs(getBeatFixMsOvertime()) == 0 ? (getBeatFixMsOvertime() < 0 ? -1 : 1) : 0);
					long waitForNewInputs = (long)(nextCTimeToPress + getBeatDelay() / 2.75);
					nextCTimeToPress += getBeatDelay() + fixOverTime;
					pair = !pair;
					pressFourthBeatButtons();
					if (!testing)
						beep();
					mainRobot().mouseMove(start_X + width * 7, start_Y + (pair ? height : 0));
					Misc.copyArray(currentPlayerAction, previewPlayerAction);
					Misc.copyArray(haveInput, previewHaveInput);
					for (int pl = 0; pl < 2; pl++)
						checkForMiss(pl);
					if (System.currentTimeMillis() < waitForNewInputs)
						Misc.sleep(waitForNewInputs - System.currentTimeMillis());
					if (!close && gameAction == GameAction.DANCING) {
						currentBeat++;
						updateStatus();
						if (getMusicEndBeat() == currentBeat) {
							setGameAction(GameAction.AT_WIN_SCREEN);
							Misc.sleep(random.nextLong(1500) + 12000);
							pressButton(0, GamepadButton.START);
							pressButton(1, GamepadButton.START);
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
	
	static void runCharacterScreen() {
		sendToConsole("ENTERED AT THE SELECT CHARACTER SCREEN");
		setGameAction(GameAction.AT_CHAR_SELECT_SCREEN);
		selectedChar = 0;
		GameCharacter[] choice = {GameCharacter.getRandom(), GameCharacter.getRandom()};
		for (int pll = 0; !close && pll < 2; pll++) {
			final int pl = pll;
			new Thread(() -> { // Selecionando personagem
				playerDificult[pl] = 1;
				while (!close && (selectedChars.contains(choice[pl]) || choice[pl].equals(choice[pl == 0 ? 1 : 0]))) // Enquanto o char escolhido aleatoriamente já tiver sido escolhido previamente, ou for o mesmo personagem do outro jogador, escolher o proximo
					choice[pl] = choice[pl].getNext();
				selectedChars.add(choice[pl]);
				robot[pl].delay(random.nextInt(1000) + 1000);
				if (selectedChars.size() == GameCharacter.getListOfAll().size()) { // Se a lista de personagens já escolhidos chegar ao limite, zerar a lista e alterar o outfit para a próxima remessa de escolhas
					selectedChars.clear();
					alternativeOutfit = !alternativeOutfit;
				}
				while (!close && !character[pl].isAtSameRow(choice[pl])) { // Movendo o cursor para baixo até chegar na linha do personagem escolhido
					robot[pl].delay(random.nextInt(100) + 400);
					pressButton(pl, GamepadButton.DOWN);
					character[pl] = character[pl].getNext(character[pl].getValue() < 2 ? 3 : 4);
				}
				while (!close && !character[pl].equals(choice[pl])) { // Movendo o cursor horizontalmente até chegar na linha do personagem escolhido
					robot[pl].delay(random.nextInt(100) + 400);
					pressButton(pl, character[pl].getValue() < choice[pl].getValue() ? GamepadButton.RIGHT : GamepadButton.LEFT);
					character[pl] = character[pl].getValue() < choice[pl].getValue() ? character[pl].getNext() : character[pl].getPreview();
				}
				robot[pl].delay(random.nextInt(100) + 500);
				pressButton(pl, alternativeOutfit ? GamepadButton.Y : GamepadButton.B);
				robot[pl].delay(random.nextInt(500) + 500);
				for (int n = 0, r = random.nextInt(6); !close && n <= r; n++) { // Seleciona a dificuldade aleatoriamente
					robot[pl].delay(random.nextInt(100) + 400);
					if (n < r && ++playerDificult[pl] == 3)
						playerDificult[pl] = 0;
					pressButton(pl, n < r ? GamepadButton.DOWN : GamepadButton.B);
				}
				selectedChar++;
			}).start();
		}
		while (!close && selectedChar < 2)
			Misc.sleep(100);
		boolean someoneSelectedRoboZ = character[0].equals(GameCharacter.ROBO_Z_GOLD) || character[1].equals(GameCharacter.ROBO_Z_GOLD);
		// Seleciona o stage aleatoriamente, tomando cuidado para não tentar escolher stage já escolhido previamente, ou escolher o stage do Robo Z sem que ninguém tenha escolhido ele, pois o stage dele só aparece se alguém tiver escolhido ele
		robot[0].delay(random.nextInt(1000) + 1000);
		GameStage stage = GameStage.getRandom(someoneSelectedRoboZ);
		while (!close && selectedStages.contains(stage))
			stage = stage.getNext(someoneSelectedRoboZ);
		selectedStages.add(stage);
		if (selectedStages.size() == GameStage.getListOfAll().size())
			selectedStages.clear();
		while (!close && !currentStage.equals(stage)) {
			pressButton(0, currentStage.getValue() < stage.getValue() ? GamepadButton.RIGHT : GamepadButton.LEFT);
			currentStage = currentStage.getValue() < stage.getValue() ? currentStage.getNext(someoneSelectedRoboZ) : currentStage.getPreview(someoneSelectedRoboZ);
			robot[0].delay(random.nextInt(150) + 200);
		}
		pressButton(0, GamepadButton.B);
		Platform.runLater(() -> {
			String[] dif = {"EASY", "NORMAL", "MIX"};
			sendToConsole("P1: " + character[0] + " (" + dif[playerDificult[0]] + ")");
			sendToConsole("P2: " + character[1] + " (" + dif[playerDificult[1]] + ")");
			sendToConsole("STAGE: " + currentStage);
		});
		waitForSongStartAndCatchSongBpm();
	}
	
	static void updateSpecialCount() {
		BufferedImage bi = ImageUtils.getScreenShot(new Rectangle(290, 700, DesktopUtils.getSystemScreenWidth() - 580, 50));
		int[] previewSpecials = {specials[0], specials[1]};
		for (int rgb, pl = 0; pl < 2; pl++) {
			if (currentPlayerAction[pl] != PlayerAction.STUNNED) {
				rgb = 0;
				specials[pl] = 0;
				for (int z = 0; z < 2; z++) {
					int x = z == 0 ? (pl == 0 ? 25 : 930) : (pl == 0 ? 45 : 915);
					int y = z == 0 ? 15 : 37;
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
		Rectangle rect1 = new Rectangle(600, 455, 3, 3), rect2 = new Rectangle(740, 440, 3, 3);
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
		if (testing)
			return;
		for (int pl = 0; pl < 2; pl++) {
			final int player = pl;
			if (currentInputs.get(player).size() > 1) {
				executor.execute(new Task<Void> () {
					@Override
					protected Void call() throws Exception {
						List<GamepadButton> inputs = new ArrayList<>(currentInputs.get(player).subList(0, currentInputs.get(player).size() - 1)); 
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

	private static List<GamepadButton> getCurrentInputs(int player) {
		boolean debug = player + 1 == debugPlayer;
		List<GamepadButton> inputs = new ArrayList<>();
		Rectangle rect = new Rectangle(start_X + player * spacing, start_Y, (int)(width * 7.2f), height);
		BufferedImage sc = getScreenShot(player, rect);
		if (debug && debugCapturedScreenshots)
			gcCatchedInputs.drawImage(SwingFXUtils.toFXImage(sc, null), 0, 0);
		for (int w = 0; w < 7; w++) {
			int x = (int)(w * width + (w < 6 ? 0 : spacing2));
			Rectangle rect2 = new Rectangle(x + 10, 0, width - 20, height);
			if (debug && debugCapturedScreenshots)
				gcButtonsScrenshot.drawImage(SwingFXUtils.toFXImage(ImageUtils.copyFrom(sc, rect2), null), x + (w < 6 ? 1 * w : 1) - 19 * w, 0);
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
				if (!atPreSolo && currentPlayerAction[p2] == PlayerAction.ATTACKING) { // Se não estiver no pre-solo, e o oponente estiver marcado como 'ATTACKING'
					if (random.nextInt(10000) + 1 <= (canStealSolo ? preSoloCounterProc : nonPreSoloCounterProc)) { // Chance de refletir o special do oponente
						beatButton[p1] = getAttackButton(p1);
						currentPlayerAction[p1] = PlayerAction.COUNTERING;
					}
					else if (atPreSolo || random.nextInt(10000) + 1 <= dodgeUponFailCounterProc) // Chance de esquivar o ataque, caso falhe a condição anterior ou seja o pre-solo
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
		if (testing)
			return;
		System.out.println("1");
		lastFinishButton = (lastFinishButton + 1) % 2;
		System.out.println("2");
		if (testFixStartDelay > -1)
			lastFinishButton = 0;
		System.out.println("3");
		int p1 = lastFinishButton, p2 = (p1 + 1) % 2;
		System.out.println("4");
		if (((p1 + 1) & autoPlay) != 0 && beatButton[p1] != null && currentPlayerAction[p1] != PlayerAction.STUNNED)
			robot[p1].keyPress(KEY_CODES[p1][beatButton[p1].getValue()]);
		System.out.println("5");
		if (((p2 + 1) & autoPlay) != 0 && beatButton[p2] != null && currentPlayerAction[p2] != PlayerAction.STUNNED)
			robot[p2].keyPress(KEY_CODES[p2][beatButton[p2].getValue()]);
		System.out.println("6");
		for (int pl = 0; pl < 2; pl++) {
			robot[pl].delay(40);
			System.out.println("7");
			robot[pl].keyRelease(KEY_CODES[pl][beatButton[pl].getValue()]);
			System.out.println("8");
		}
		System.out.println("9");
		for (int pl = 0; pl < 2; pl++) {
			if (beatButton[pl] == getDodgeButton(pl)) // Se o botão pressionado atualmente for o botão de esquiva, marcar o jogador como 'DODGEING'
				currentPlayerAction[pl] = PlayerAction.DODGEING;
			else if (beatButton[pl] != getAttackButton(pl)) // Se o botão pressionado atualmente não for nem esquiva e nem ataque, marcar jogador como 'DANCING'
				currentPlayerAction[pl] = PlayerAction.DANCING;
		}
		System.out.println("8");
	}

	static void checkForMiss(int player) { // ARRUMAR COORDENADA DO MISS TIRANDO SCREENSHOT PELO PROGRAMA USANDO X
		if (gameAction == GameAction.DANCING &&
				ImageUtils.isImageContained(missPng, getScreenShot(player, new Rectangle(453 + player * 550, 623, 80, 30))) != null) {
					sendToConsole("MISS FOUND. DOING RETRY");
					setGameAction(GameAction.WAITING_FOR_MUSIC_START);
					Misc.sleep(2000);
					pressButton(player, GamepadButton.START);
					Misc.sleep(2000);
					pressButton(player, GamepadButton.B);
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
	
	static void pressButtons(int player, List<GamepadButton> inputs, int holdDelay) {
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

	static void pressButton(int player, GamepadButton input, int holdDelay) //
		{ pressButtons(player, Arrays.asList(input), holdDelay); }

	static void pressButton(int player, GamepadButton input) //
		{ pressButton(player, input, 50); }
	
	static void pressButton(GamepadButton input) //
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