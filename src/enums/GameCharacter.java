package enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import application.Main;

public enum GameCharacter {

	HEAT(0),
	COMET(1),
	SHORTY(2),
	STRIKE(3),
	TSUTOMO(4),
	CAPOEIRA(5),
	BIO(6),
	HIRO(7),
	KITTY_N(8),
	KELLY(9),
	COLUMBO(10),
	ROBO_Z_GOLD(11),
	PANDER(12),
	SUSHI_BOY(13),
	MCLOAD(14),
	CHICHI_AND_SALLY(15),
	MICHAEL_DOI(16),
	HUSTLE_KONG(17);
	
	private int value;
	private static List<GameCharacter> list;
	
	GameCharacter(int value)
		{ this.value = value; }
	
	public boolean isAtSameRow(GameCharacter character) {
		if (value < 2 || character.value < 2)
			return (value < 2 && character.value < 2);
		return (int)((value - 2) / 4) == (int)((character.value - 2) / 4);
	}
	
	public GameCharacter getPreview(int n) {
		n = value - n;
		while (n < 0)
			n += getListOfAll().size(); 
		return getListOfAll().get(n);
	}
	
	public GameCharacter getPreview()
		{ return getPreview(1); }
	
	public GameCharacter getNext(int n) {
		n = value + n;
		while (n >= getListOfAll().size())
			n -= getListOfAll().size(); 
		return getListOfAll().get(n);
	}
	
	public GameCharacter getNext()
		{ return getNext(1); }

	public GameCharacter getFirst()
		{ return getListOfAll().get(0); }
	
	public GameCharacter getLast()
		{ return getListOfAll().get(getListOfAll().size()); }

	public static GameCharacter getRandom()
		{ return getListOfAll().get(Main.getRandom().nextInt(list.size())); }

	public static List<GameCharacter> getListOfAll() {
		if (list == null)
			list = new ArrayList<>(Arrays.asList(HEAT, COMET, SHORTY, STRIKE, TSUTOMO, CAPOEIRA, BIO, HIRO, KITTY_N, KELLY, COLUMBO, ROBO_Z_GOLD, PANDER, SUSHI_BOY, MCLOAD, CHICHI_AND_SALLY, MICHAEL_DOI, HUSTLE_KONG));
		return Collections.unmodifiableList(list);
	}

	public int getValue()
		{ return value; }
	
}
