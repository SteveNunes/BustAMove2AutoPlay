package enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum GamepadButton {

	LEFT(0),
	DOWN(1),
	RIGHT(2),
	UP(3),
	A(4),
	B(5),
	X(6),
	Y(7),
	RS(8),
	RT(9),
	START(10);

	private int value;
	private static List<GamepadButton> list = new ArrayList<>(Arrays.asList(LEFT, DOWN, RIGHT, UP, A, B, X, Y, RS, RT, START));
	
	GamepadButton(int value)
		{ this.value = value; }
	
	public int getValue()
		{ return value; }
	
	public static GamepadButton getButton(int index)
		{ return index < 0 || index >= list.size() ? null : list.get(index); }
	
}
