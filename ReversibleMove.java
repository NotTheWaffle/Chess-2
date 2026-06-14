public class ReversibleMove extends Move{
	// cccceeee_hhhhhhdd_dffftttt_ttoooooo
	// f = flag, t = target idx, o = origin index
	// d = piece at target before move (we know what color is)
	// e = en passant rank
	// h = halfmove count before move
	// c = castling rights

	public ReversibleMove(int originIndex, int targetIndex, GameState gameState){
		this(new Move(originIndex, targetIndex, gameState), gameState);
	}
	public ReversibleMove(int originIndex, int targetIndex, byte flag, GameState gameState){
		this(new Move(originIndex, targetIndex, flag), gameState);
	}
	public ReversibleMove(Move move, GameState gameState){
		this(move, gameState.getTile(move.getTargetIndex()), gameState.halfmoves, gameState.enpassantIndex, gameState.castlingRights);
	}
	public ReversibleMove(Move move, byte tileContents, byte halfmoves, byte enpassantIndex, byte castlingRights){
		super(move.data | tileContents << 16 | enpassantIndex << 20 | halfmoves << 26 | castlingRights << 28);
	}

	public int getTargetPiece(){
		return (data >>> 15) & (0b111);
	}
	public int getHalfmoves(){
		return (data >>> 18) & (0b111111);
	}
	public int getEnpassantIndex(){
		return (data >>> 24) & (0b1111);
	}
	public int getCastlingRights(){
		return (data >>> 28) & (0b1111);
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
		return super.toString()+"(piece at "+getTargetIndex()+" originally: "+getTargetPiece();
	}
}
