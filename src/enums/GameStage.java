package enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import application.Main;

public enum GameStage {

	HEAT(0),
	COMET(1),
	SHORTY(2),
	STRIKE(3),
	CAPOEIRA(4),
	KITTY_N(5),
	TSUTOMO(6),
	HIRO(7),
	KELLY(8),
	BIO(9),
	ROBO_Z_GOLD(10),
	PANDER(11);
	
	private int value;
	private static List<GameStage> list;
	
	GameStage(int value)
		{ this.value = value; }
	
	public GameStage getPreview(int n, boolean someoneSelectedRoboZ) {
		n = value - n;
		while (n < 0)
			n += getListOfAll().size(); 
		if (n == ROBO_Z_GOLD.value && !someoneSelectedRoboZ)
			n--;
		return getListOfAll().get(n);
	}
	
	public GameStage getPreview(boolean someoneSelectedRoboZ)
		{ return getPreview(1, someoneSelectedRoboZ); }

	public GameStage getNext(int n, boolean someoneSelectedRoboZ) {
		n = value + n;
		while (n >= getListOfAll().size())
			n -= getListOfAll().size();
		if (n == ROBO_Z_GOLD.value && !someoneSelectedRoboZ)
			n++;
		return getListOfAll().get(n);
	}
	
	public GameStage getNext(boolean someoneSelectedRoboZ)
		{ return getNext(1, someoneSelectedRoboZ); }
	
	public GameStage getFirst()
		{ return getListOfAll().get(0); }
	
	public GameStage getLast()
		{ return getListOfAll().get(getListOfAll().size()); }

	public static GameStage getRandom(boolean someoneSelectedRoboZ) {
		GameStage stage = getListOfAll().get(Main.getRandom().nextInt(list.size()));
		if (!someoneSelectedRoboZ && stage.equals(ROBO_Z_GOLD))
			stage = stage.getNext(someoneSelectedRoboZ);
		return stage;
	}

	public static List<GameStage> getListOfAll() {
		if (list == null)
			list = new ArrayList<>(Arrays.asList(HEAT, COMET, SHORTY, STRIKE, CAPOEIRA, KITTY_N, TSUTOMO, HIRO, KELLY, BIO, ROBO_Z_GOLD, PANDER));
		return Collections.unmodifiableList(list);
	}

	public int getValue()
		{ return value; }

}
