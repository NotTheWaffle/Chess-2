import java.util.ArrayList;
import java.util.List;

public class ReversibleGameState extends GameState{
	protected final List<ReversibleMove> moveHistory;
	protected ReversibleMove liteMoveHistory;
	public ReversibleGameState(){
		this.moveHistory = new ArrayList<>();
		this.liteMoveHistory = null;
		super();
	}
	public void syncTree(){
		moveHistory.clear();
	}
	@Override
	public void makeMove(Move move){
		super.makeMove(move);
		syncTree();
	}


	public void tryMove(Move move){
		moveHistory.add(new ReversibleMove(move, this));
		super.makeMove(move);
	}
	public void untryMove(){
		ReversibleMove move = moveHistory.removeLast();
		this.player ^= Tile.COLOR;
		if (this.player == Tile.BLACK){
			this.moves--;
		}
		setTile(move.getOriginIndex(), getTile(move.getTargetIndex()));
		int flag = move.getFlag();
		if (flag != Move.FLAGLESS){
			if (flag == Move.ENPASSANT){
				int d = 8 - 2 * this.player; // works because white = 8, black = 0
				setTile(move.getTargetIndex() + d, Tile.PAWN|(player^Tile.COLOR));
			} else if (flag == Move.CASTLING){
				if (move.getTargetX() == 6){ // king side
					setTile(move.getTargetIndex() + 1, Tile.ROOK|player);
					setTile(move.getTargetIndex() - 1, Tile.BLANK);
				} else { // queen side
					setTile(move.getTargetIndex() - 2, Tile.ROOK|player);
					setTile(move.getTargetIndex() + 1, Tile.BLANK);
				}
			} else if ((flag & Move.PROMOTION) != 0){
				setTile(move.getOriginIndex(), Tile.PAWN|player);
			} // double moves work by default
		}
		enpassantIndex = (byte) move.getEnpassantIndex();
		setTile(move.getTargetIndex(), move.getTargetPiece());

		if (move.getTargetIndex() == whiteKingIndex) whiteKingIndex = (byte) move.getOriginIndex();
		if (move.getTargetIndex() == blackKingIndex) blackKingIndex = (byte) move.getOriginIndex();

		halfmoves = (byte) move.getHalfmoves();
		castlingRights = (byte) move.getCastlingRights();
	}
}
