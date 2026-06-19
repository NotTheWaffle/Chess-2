
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class GameState{
	public byte[] board;
	public byte player;
	public byte enpassantIndex;	// 0 to 63 (-1 for undefined)
	public byte castlingRights;	// 0b1111 qkQK black queen, black king, white queen, white king
	public byte halfmoves;	//50 move stalemate (all), 0 to 50, byte is big enough
	public short moves;		//useless technically - starts at 1, increments after blacks move
	public List<Long> encounteredPositions = new ArrayList<>();	//repeated position stalemate

	// not neccessary, used for faster lookups
	public transient byte whiteKingIndex;
	public transient byte blackKingIndex;

	private static final long[] ZOBRIST_HASHING_RANDOMS = generateZobristHashingRandoms();
	private static long[] generateZobristHashingRandoms(){
		Random random = new SecureRandom();
		long[] result = new long[14*64];
		for (int i = 0; i < 14*64; i++){
			result[i] = random.nextLong();
		}
		for (int i = 0; i < result.length; i++){
			for (int j = i+1; j < result.length; j++){
				if (result[i] == result[j]){
					System.out.println(i+" equals "+j+" (very bad)");
				}
			}
		}
		return result;
	}

	public long generateZobristHash(){
		//TODO make this encorporate faster hashing by not rehashing the entire board
		long hashCode = 0;
		for (int i = 0; i < 64; i++){
			byte tile = getTile(i);
			if (Tile.piece(tile) != Tile.BLANK){
				int lookupIndex =
					((tile&Tile.PIECE)-1)
					+ 6 * ((tile&Tile.COLOR)>>>3)
					+ i * 12;
				hashCode ^= ZOBRIST_HASHING_RANDOMS[lookupIndex];
			}
		}
		// OK so there is TECHNICALLY a bug here where the en passant index only should be recorded in the hash if its meaningful, well... maybe??
		hashCode ^= ZOBRIST_HASHING_RANDOMS[enpassantIndex+12*64];
		if (player == Tile.WHITE){
			hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64];
		}
		if ((castlingRights & 0b0001) > 0) hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+1];
		if ((castlingRights & 0b0010) > 0) hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+2];
		if ((castlingRights & 0b0100) > 0) hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+3];
		if ((castlingRights & 0b1000) > 0) hashCode ^= ZOBRIST_HASHING_RANDOMS[13*64+4];
		return hashCode;
	}

	public GameState(){
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}
	public GameState(String fenString){
		// this method is allowed be slow (using regex) because its ran once per game
		String[] args = fenString.split(" ");

		// load board
		this.board = new byte[64];
		String[] ranks = args[0].split("/");
		for (int y = 0; y < 8; y++){
			String row = ranks[7-y];
			int x = 0;
			for (char letter : row.toCharArray()){
				switch (letter){
					case 'r' -> setTile(x++, y, Tile.BLACK_ROOK);
					case 'n' -> setTile(x++, y, Tile.BLACK_KNIGHT);
					case 'b' -> setTile(x++, y, Tile.BLACK_BISHOP);
					case 'k' -> setTile(x++, y, Tile.BLACK_KING);
					case 'q' -> setTile(x++, y, Tile.BLACK_QUEEN);
					case 'p' -> setTile(x++, y, Tile.BLACK_PAWN);

					case 'R' -> setTile(x++, y, Tile.WHITE_ROOK);
					case 'N' -> setTile(x++, y, Tile.WHITE_KNIGHT);
					case 'B' -> setTile(x++, y, Tile.WHITE_BISHOP);
					case 'K' -> setTile(x++, y, Tile.WHITE_KING);
					case 'Q' -> setTile(x++, y, Tile.WHITE_QUEEN);
					case 'P' -> setTile(x++, y, Tile.WHITE_PAWN);

					default -> x += (letter-'0');
				}
			}
		}

		// load player to move
		String playerToMove = args[1].toLowerCase(); // should always be lowercase, but secondary ensurance
		switch (playerToMove) {
			case "w" -> this.player = Tile.WHITE;
			case "b" -> this.player = Tile.BLACK;
			default -> throw new IllegalArgumentException("Invalid player in provided FEN String ("+playerToMove+")");
		}

		// load castling rights
		String castling = args[2];
		this.castlingRights = 0b0000;
		// captial = white
		// qkQK
		if (castling.contains("K")) castlingRights |= 0b0001;
		if (castling.contains("Q")) castlingRights |= 0b0010;
		if (castling.contains("k")) castlingRights |= 0b0100;
		if (castling.contains("q")) castlingRights |= 0b1000;

		// load enpassant index
		String enpassant = args[3].toLowerCase();
		if (enpassant.equals("-")){
			enpassantIndex = -1;
		} else {
			// turn algebraic notation into index
			enpassantIndex = (byte) (enpassant.charAt(0)-'a'+8*(enpassant.charAt(1)-'1'));
		}

		// load halfmove clock
		String halfmoves = args[4];
		this.halfmoves = Byte.parseByte(halfmoves);

		// load move clock
		String moves = args[5];
		this.moves = Short.parseShort(moves);

		// find black and white king position for faster lookups
		for (byte i = 0; i < 64; i++){
			if (getTile(i) == Tile.BLACK_KING){
				blackKingIndex = i;
			} else if (getTile(i) == Tile.WHITE_KING){
				whiteKingIndex = i;
			}
		}
	}
	public GameState(GameState gameState){
		this.board = Arrays.copyOf(gameState.board, 64);
		this.player = gameState.player;
		this.enpassantIndex = gameState.enpassantIndex;
		this.castlingRights = gameState.castlingRights;
		this.whiteKingIndex = gameState.whiteKingIndex;
		this.blackKingIndex = gameState.blackKingIndex;
		this.halfmoves = gameState.halfmoves;
		this.moves = gameState.moves;
		this.encounteredPositions = new ArrayList<>(); this.encounteredPositions.addAll(gameState.encounteredPositions);
	}

	public void makeMove(Move move){
		this.halfmoves++;
		// its assumed the move is legal, checking would take too long
		if (move.getOriginIndex() == 0 || move.getTargetIndex() == 0) castlingRights &= 0b1101;		//white queenside rook
		if (move.getOriginIndex() == 4 || move.getTargetIndex() == 4) castlingRights &= 0b1100;		//white king
		if (move.getOriginIndex() == 7 || move.getTargetIndex() == 7) castlingRights &= 0b1110;		//white kingside rook

		if (move.getOriginIndex() == 56 || move.getTargetIndex() == 56) castlingRights &= 0b0111;	//black queenside rook
		if (move.getOriginIndex() == 60 || move.getTargetIndex() == 60) castlingRights &= 0b0011;	//black king
		if (move.getOriginIndex() == 63 || move.getTargetIndex() == 63) castlingRights &= 0b1011;	//black kingside rook

		if ((getTile(move.getTargetIndex()) & Tile.PIECE) != Tile.BLANK) // reset halfmoves on capture, technically this doesn't count en passant, but the next check gets it any way
			this.halfmoves = 0;
		if ((getTile(move.getOriginIndex()) & Tile.PIECE) == Tile.PAWN) // reset halfmoves on pawn moves
			this.halfmoves = 0;
		// update king position
		if (move.getOriginIndex() == whiteKingIndex) whiteKingIndex = (byte) move.getTargetIndex();
		if (move.getOriginIndex() == blackKingIndex) blackKingIndex = (byte) move.getTargetIndex();

		setTile(move.getTargetIndex(), getTile(move.getOriginIndex()));
		enpassantIndex = -1;
		int flag = move.getFlag();
		if (flag != Move.FLAGLESS){
			if (flag == Move.PAWN_DOUBLE){
				int d = 8 - 2 * this.player; // works because white = 8, black = 0
				// white = -8
				// black = 8
				if (getTile(move.getTargetIndex()-1) == ((Tile.PAWN|this.player)^Tile.COLOR)){
					enpassantIndex = (byte) (move.getTargetIndex() + d);
				} else if (getTile(move.getTargetIndex()+1) == ((Tile.PAWN|this.player)^Tile.COLOR)){
					enpassantIndex = (byte) (move.getTargetIndex() + d);
				}
			} else if (flag == Move.ENPASSANT){
				int d = 8 - 2 * this.player; // works because white = 8, black = 0
				// white = -8
				// black = 8
				setTile(move.getTargetIndex() + d, Tile.BLANK);
			} else if (flag == Move.CASTLING){
				if (move.getTargetX() == 6){ // king side
					setTile(move.getTargetIndex() + 1, Tile.BLANK);
					setTile(move.getTargetIndex() - 1, Tile.ROOK|this.player);
				} else { // queen side
					setTile(move.getTargetIndex() - 2, Tile.BLANK);
					setTile(move.getTargetIndex() + 1, Tile.ROOK|this.player);
				}
			} else if ((flag & Move.PROMOTION) != 0){
				setTile(move.getTargetIndex(), Move.getPiece(flag)|this.player);
			}
		}
		setTile(move.getOriginIndex(), Tile.BLANK);
		if (this.player == Tile.BLACK) this.moves++;
		player ^= Tile.COLOR;
		encounteredPositions.add(generateZobristHash());
	}

	protected final void setTile(int x, int y, int tile){
		board[x|(y<<3)] = (byte) tile;
	}
	protected final void setTile(int idx, int tile){
		board[idx] = (byte) tile;
	}
	public final byte getTile(int x, int y){
		return board[x|(y<<3)];
	}
	public final byte getTile(int idx){
		return board[idx];
	}

	public static enum Conclusion{
		WHITE,
		BLACK,
		TIE,
		ONGOING
	}
	public Conclusion findWinner(){
		if (this.halfmoves > 50){
			return Conclusion.TIE;
		}
		long currentPosition = encounteredPositions.getLast();
		int repetition = 0;
		for (Long position : encounteredPositions){
			if (position == currentPosition){
				repetition++;
			}
		}
		if (repetition >= 3){
			return Conclusion.TIE;
		}
		List<Move> moves = new ArrayList<>();
		MoveHandler moveHandler = new MoveHandler((ReversibleGameState) this);
		moveHandler.addLegalMoves(moves);

		if (moves.isEmpty()){
		//	System.out.println("mate of some sort detected");
			if (moveHandler.isAttacked(whiteKingIndex, Tile.BLACK)){
				return Conclusion.BLACK;
			} else if (moveHandler.isAttacked(blackKingIndex, Tile.WHITE)){
				return Conclusion.WHITE;
			} else {
				return Conclusion.TIE;
			}
		}
		return Conclusion.ONGOING;
	}

	@Override
	public int hashCode(){
		return (int) generateZobristHash();
	}
	@Override
	public boolean equals(Object o){
		if (o == this) return true;
		if (o instanceof GameState g){
			return Arrays.equals(g.board, board) && g.castlingRights == castlingRights && g.player == player && g.enpassantIndex == enpassantIndex;
		} else {
			return false;
		}
	}
	public String toAlgebraicMoveNotation(Move move){
		//TODO doesn't include check(mate) or disambiguation
		String suffix = "";
		if (move.getFlag() == Move.CASTLING){
			if (move.getTargetX() == 0){
				return "O-O-O";
			} else {
				return "O-O";
			}
		} else if ((move.getFlag() & Move.PROMOTION) != 0){
			suffix = switch (move.getFlag()){
				case Move.PROMOTION_KNIGHT -> "=N";
				case Move.PROMOTION_BISHOP -> "=B";
				case Move.PROMOTION_ROOK -> "=R";
				case Move.PROMOTION_QUEEN -> "=Q";
				default -> "";
			};
		}
		byte piece = getTile(move.getOriginIndex());
		String moveString = switch (piece & Tile.PIECE){
			case Tile.PAWN -> "";
			case Tile.KNIGHT -> "N";
			case Tile.KING -> "K";
			case Tile.BISHOP -> "B";
			case Tile.ROOK -> "R";
			case Tile.QUEEN -> "Q";
			default -> "";
		};
		String infix = "";
		if (getTile(move.getTargetIndex()) != Tile.BLANK) infix = "x";
		moveString += infix + ("abcedfgh".charAt(move.getTargetX())) + (move.getTargetY() + 1) + suffix;
		return moveString;
	}

	@Override
	public String toString(){
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < 64; i+=8){
			int count = 0;
			for (int j = 0; j < 8; j++){
				if (board[i+j] == Tile.BLANK){
					count++;
				} else {
					if (count > 0){
						result.append(count);
						count = 0;
					}
					result.append(Tile.PIECE_SYMBOLS[board[i+j]]);
				}
			}
			if (count > 0){
				result.append(count);
			}
			if (i != 56) result.append("/");
		}
		result
			.append(' ')
			.append(player == Tile.WHITE ? 'w' : 'b')
			.append(' ');
		if ((castlingRights & 0b1000) != 0) result.append('q');
		if ((castlingRights & 0b0100) != 0) result.append('K');
		if ((castlingRights & 0b0010) != 0) result.append('Q');
		if ((castlingRights & 0b0001) != 0) result.append('K');
		result.append(' ');
		if (enpassantIndex == -1){
			result.append('-');
		} else {
			result.append("abcdefgh".charAt(enpassantIndex%8))
			.append((enpassantIndex/8 + 1));
		}
		result
			.append(' ')
			.append(halfmoves)
			.append(' ')
			.append(moves);
		return result.toString();
	}
}