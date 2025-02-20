package enums;

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
	CHICHI_SALLY(15),
	MICHAEL_DOI(16),
	HUSTLE_KONG(17);

	private int value;

	GameCharacter(int value)
		{ this.value = value; }
	
	public int getValue()
		{ return value; }
	
}
