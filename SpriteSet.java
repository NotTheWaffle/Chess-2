
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SpriteSet{
	public final BufferedImage PAWN_WHITE;
	public final BufferedImage ROOK_WHITE;
	public final BufferedImage KNIGHT_WHITE;
	public final BufferedImage BISHOP_WHITE;
	public final BufferedImage KING_WHITE;
	public final BufferedImage QUEEN_WHITE;

	public final BufferedImage PAWN_BLACK;
	public final BufferedImage ROOK_BLACK;
	public final BufferedImage KNIGHT_BLACK;
	public final BufferedImage BISHOP_BLACK;
	public final BufferedImage KING_BLACK;
	public final BufferedImage QUEEN_BLACK;


	public final BufferedImage BOARD_WHITE;
	public final BufferedImage BOARD_WHITE_TEXT;
	public final BufferedImage BOARD_BLACK;
	public final BufferedImage BOARD_BLACK_TEXT;

	public final BufferedImage BLACK_SELECTED;
	public final BufferedImage WHITE_SELECTED;
	public final BufferedImage BLACK_HIGHLIGHT;
	public final BufferedImage WHITE_HIGHLIGHT;
	public final BufferedImage BLACK_SELECTED_CAPTURE;
	public final BufferedImage WHITE_SELECTED_CAPTURE;

	public SpriteSet(String boardStyle, String pieceStyle){
		this(boardStyle, pieceStyle, pieceStyle);
	}
	public SpriteSet(String boardStyle, String whiteStyle, String blackStyle){
		PAWN_BLACK   = loadImage("Pieces/"+blackStyle+"/pawnBlack.png");
		ROOK_BLACK   = loadImage("Pieces/"+blackStyle+"/rookBlack.png");
		KNIGHT_BLACK = loadImage("Pieces/"+blackStyle+"/knightBlack.png");
		BISHOP_BLACK = loadImage("Pieces/"+blackStyle+"/bishopBlack.png");
		KING_BLACK   = loadImage("Pieces/"+blackStyle+"/kingBlack.png");
		QUEEN_BLACK  = loadImage("Pieces/"+blackStyle+"/queenBlack.png");

		PAWN_WHITE   = loadImage("Pieces/"+whiteStyle+"/pawnWhite.png");
		ROOK_WHITE   = loadImage("Pieces/"+whiteStyle+"/rookWhite.png");
		KNIGHT_WHITE = loadImage("Pieces/"+whiteStyle+"/knightWhite.png");
		BISHOP_WHITE = loadImage("Pieces/"+whiteStyle+"/bishopWhite.png");
		KING_WHITE   = loadImage("Pieces/"+whiteStyle+"/kingWhite.png");
		QUEEN_WHITE  = loadImage("Pieces/"+whiteStyle+"/queenWhite.png");

		BOARD_WHITE  = loadImage("Boards/"+boardStyle+"/boardWhite.png");
		BOARD_WHITE_TEXT  = loadImage("Boards/"+boardStyle+"/boardWhiteText.png");
		BOARD_BLACK  = loadImage("Boards/"+boardStyle+"/boardBlack.png");
		BOARD_BLACK_TEXT  = loadImage("Boards/"+boardStyle+"/boardBlackText.png");

		BLACK_SELECTED = loadImage("Boards/"+boardStyle+"/blackSelected.png");
		WHITE_SELECTED = loadImage("Boards/"+boardStyle+"/whiteSelected.png");
		BLACK_HIGHLIGHT = loadImage("Boards/"+boardStyle+"/blackHighlight.png");
		WHITE_HIGHLIGHT = loadImage("Boards/"+boardStyle+"/whiteHighlight.png");
		BLACK_SELECTED_CAPTURE = loadImage("Boards/"+boardStyle+"/blackSelectedCapture.png");
		WHITE_SELECTED_CAPTURE = loadImage("Boards/"+boardStyle+"/whiteSelectedCapture.png");

		PIECES = new BufferedImage[] {
			null, PAWN_BLACK, ROOK_BLACK, KNIGHT_BLACK, BISHOP_BLACK, QUEEN_BLACK, KING_BLACK, null,
			null, PAWN_WHITE, ROOK_WHITE, KNIGHT_WHITE, BISHOP_WHITE, QUEEN_WHITE, KING_WHITE, null,
		};
	}
	private final BufferedImage[] PIECES;

	public BufferedImage getImage(byte b){
		if (b == Tile.BLANK) return null;
		return PIECES[b];
	}
	public static BufferedImage loadImage(String filePath){
		BufferedImage image = null;
		try {image = ImageIO.read(new File("Assets/"+filePath));} catch (IOException e) {System.out.println("Failed to load "+filePath);}
		return image;
	}
}
