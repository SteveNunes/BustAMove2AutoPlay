package enums;

public enum WhoCanUseTTS {
	
	NOBODY("Ninguém"),
	EVERYONE("Todos"),
	FOLLOWERS("Seguidores"),
	SUBSCRIBEDS("Inscritos"),
	GIFT_GIVERS("Quem presenteia");

	private String value;
	
	WhoCanUseTTS(String value)
		{ this.value = value; }
	
	public String getValue()
		{ return value; }

}
