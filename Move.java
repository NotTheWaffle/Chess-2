public class Move {
	// xxxxxxxx_xxxxxxxx_xffftttt_ttoooooo
	// f = flag, t = target idx, o = origin index
	public final int data;	// 3 bit flag, 6 bit targ, 6 bit orig


	public final static int FLAGLESS    = 0b000;
	public final static int ENPASSANT   = 0b001;
	public final static int PAWN_DOUBLE = 0b010;
	public final static int CASTLING    = 0b011;


	public final static int PROMOTION   = 0b100;

	public final static int PROMOTION_KNIGHT = 0b100;
	public final static int PROMOTION_BISHOP = 0b101;
	public final static int PROMOTION_ROOK   = 0b110;
	public final static int PROMOTION_QUEEN  = 0b111;

	/*
		000 normal
		001 enpassant capture
		010 pawn double move
		011 castling
		100 knight promotion
		101 bishop promotion
		110 rook promotion
		111 queen promotion
	*/


	public static int getPiece(int flag){
		return switch (flag){
			case PROMOTION_KNIGHT -> Tile.KNIGHT;
			case PROMOTION_BISHOP -> Tile.BISHOP;
			case PROMOTION_ROOK -> Tile.ROOK;
			case PROMOTION_QUEEN -> Tile.QUEEN;
			default -> Tile.BLANK;
		};
	}
	public static String flagToString(int flag){
		return switch (flag){
			case FLAGLESS -> "";
			case ENPASSANT -> "via en passant";
			case PAWN_DOUBLE -> "via double pawn";
			case PROMOTION_KNIGHT -> "promoting to a Knight";
			case PROMOTION_BISHOP -> "promoting to a Bishop";
			case PROMOTION_ROOK -> "promoting to a Rook";
			case PROMOTION_QUEEN -> "promoting to a Queen";
			case CASTLING -> "via castling";
			default -> "";
		};
	}

	protected Move(int data){
		this.data = data;
	}
	public Move(int originIndex, int targetIndex){
		this(originIndex, targetIndex, FLAGLESS);
	}
	public Move(int originIndex, int targetIndex, GameState gameState){
		byte flag = Move.FLAGLESS;
		if ((gameState.getTile(originIndex) & Tile.PIECE) == Tile.PAWN && Math.abs(originIndex-targetIndex) == 16){
			flag = Move.PAWN_DOUBLE;
		} else if ((gameState.getTile(originIndex) & Tile.PIECE) == Tile.KING && ((originIndex == 4 && targetIndex == 6) || (originIndex == 4 && targetIndex == 2) || (originIndex == 60 && targetIndex == 62) || (originIndex == 60 && targetIndex == 58))){
			flag = Move.CASTLING;
		} else if (gameState.getTile(originIndex) == (Tile.PAWN|gameState.player) && targetIndex == gameState.enpassantIndex){
			flag = Move.ENPASSANT;
		} else if (gameState.getTile(originIndex) == (Tile.PAWN|gameState.player) && (targetIndex < 8 || targetIndex >= 56)){
			throw new IllegalArgumentException("Promotion moves must be created with the 4 argument constructor");
		}
		this(originIndex, targetIndex, flag);
	}
	public Move(int originIndex, int targetIndex, int flag){
		this.data = flag << 12 | targetIndex << 6 | originIndex;
	}
	public Move(int originX, int originY, int targetX, int targetY){
		this(originX, originY, targetX, targetY, FLAGLESS);
	}
	public Move(int originX, int originY, int targetX, int targetY, int flag){
		this(originY << 3 | originX, targetY << 3 | targetX, flag);
	}
	public int getOriginIndex(){
		return data & 0b111111;
	}
	public int getOriginX(){
		return data & 0b111;
	}
	public int getOriginY(){
		return (data >>> 3) & 0b111;
	}
	public int getTargetIndex(){
		return (data >>> 6) & 0b111111;
	}
	public int getTargetX(){
		return (data >>> 6) & 0b111;
	}
	public int getTargetY(){
		return (data >>> 9) & 0b111;
	}
	public int getFlag(){
		return (data >>> 12) & 0b111;
	}

	@Override
	public boolean equals(Object o){
		if (o == this) return true;
		if (o instanceof Move m){
			return m.data == data;
		} else {
			return false;
		}
	}
	@Override
	public int hashCode(){
		return data;
	}
	@Override
	public String toString(){
		return getOriginIndex()+" to "+getTargetIndex()+"("+flagToString(getFlag())+")";
	}
	public String toAlgebraicMoveString(GameState state){
		int rank = getTargetY();
		int file = getTargetX();
		int piece = state.getTile(getOriginIndex()) & Tile.PIECE;
		String pieceC = switch (piece){
			case Tile.PAWN -> "";
			case Tile.ROOK -> "R";
			case Tile.KNIGHT -> "N";
			case Tile.BISHOP -> "B";
			case Tile.QUEEN -> "Q";
			case Tile.KING -> "K";
			default -> "";
		};
		return pieceC+"abcdefgh".charAt(file)+(rank+1);
	}
}
