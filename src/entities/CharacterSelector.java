package entities;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import application.Main;
import enums.Direction;
import enums.GameCharacter;

public class CharacterSelector {
	
	private static List<GameCharacter> list = new LinkedList<>(Arrays.asList(
			GameCharacter.HEAT, GameCharacter.COMET, GameCharacter.SHORTY,
			GameCharacter.STRIKE, GameCharacter.TSUTOMO, GameCharacter.CAPOEIRA,
			GameCharacter.BIO, GameCharacter.HIRO, GameCharacter.KITTY_N,
			GameCharacter.KELLY, GameCharacter.COLUMBO, GameCharacter.ROBO_Z_GOLD,
			GameCharacter.PANDER, GameCharacter.SUSHI_BOY, GameCharacter.MCLOAD,
			GameCharacter.CHICHI_SALLY, GameCharacter.MICHAEL_DOI, GameCharacter.HUSTLE_KONG));
	
	public static GameCharacter[][] characterGrid = new GameCharacter[][] {
		{null, GameCharacter.HEAT, GameCharacter.COMET, null},
		{GameCharacter.SHORTY, GameCharacter.STRIKE, GameCharacter.TSUTOMO, GameCharacter.CAPOEIRA},
		{GameCharacter.BIO, GameCharacter.HIRO, GameCharacter.KITTY_N, GameCharacter.KELLY},
		{GameCharacter.COLUMBO, GameCharacter.ROBO_Z_GOLD, GameCharacter.PANDER, GameCharacter.SUSHI_BOY},
		{GameCharacter.MCLOAD, GameCharacter.CHICHI_SALLY, GameCharacter.MICHAEL_DOI, GameCharacter.HUSTLE_KONG},
	};
	
	private GameCharacter character;
	private int cursorX, cursorY;

	public CharacterSelector(GameCharacter initialCharacter) {
		character = initialCharacter;
		cursorX = 1;
		cursorY = 0;
		while (character != characterGrid[cursorY][cursorX])
			if (++cursorX == (cursorY == 0 ? 3 : 4)) {
				cursorX = 0; cursorY++;
			}
	}
	
	public static GameCharacter getCharacterAtPosition(int x, int y)
		{ return characterGrid[y][x]; }
	
	public GameCharacter getCharacter()
		{ return character; }

	public GameCharacter getPreview(int n) {
		for (int i = 0; i < n; i++)
			getPreview();
		return character;
	}
	
	public GameCharacter getPreview()
		{ return moveCursorPosition(Direction.LEFT, true); }
	
	public GameCharacter getNext(int n) {
		for (int i = 0; i < n; i++)
			getNext();
		return character;
	}
	
	public GameCharacter getNext()
		{ return moveCursorPosition(Direction.RIGHT, true); }
	
	public GameCharacter moveCursorPosition(Direction direction, boolean circular) {
		if (direction == Direction.LEFT || direction == Direction.RIGHT)
			cursorX += direction == Direction.LEFT ? -1 : 1;
		else
			cursorY += direction == Direction.UP ? -1 : 1;
		
		if (cursorX > (cursorY == 0 ? 2 : 3)) {
			cursorX = (!circular && cursorY == 0 || circular && cursorY == 4) ? 1 : 0;
			if (circular)
				cursorY++;
		}
		else if (cursorX < (cursorY == 0 ? 1 : 0)) {
			cursorX = (!circular && cursorY == 0 || circular && cursorY == 1) ? 2 : 3;
			if (circular)
				cursorY--;
		}
		if (cursorY > 4)
			cursorY = cursorX == 0 || cursorX == 3 ? 1 : 0;
		else if (cursorY < (cursorX == 0 ? 1 : 0))
			cursorY = 4;
		character = getCharacterAtPosition(cursorX, cursorY);
		return character;
	}
	
	public GameCharacter moveCursorPosition(Direction direction)
		{ return moveCursorPosition(direction, false);	}
	
	public boolean isAtSameRow(CharacterSelector character)
		{ return getCursorY() == character.getCursorY(); }

	public boolean isAtSameColumn(CharacterSelector character)
		{ return getCursorX() == character.getCursorX(); }
	
	public int getCursorX()
		{ return cursorX; }

	public int getCursorY()
		{ return cursorY; }

	public static GameCharacter getRandom()
		{ return list.get(Main.getRandom().nextInt(list.size())); }

	public static List<GameCharacter> getCharactersList() { return Collections.unmodifiableList(list);	}

}
