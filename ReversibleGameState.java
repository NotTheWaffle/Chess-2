import java.util.ArrayList;
import java.util.List;

public class ReversibleGameState extends GameState{
	protected final List<ReversibleMove> moveHistory;
	public ReversibleGameState(){
		this.moveHistory = new ArrayList<>();
		super();
	}
	public ReversibleGameState(GameState gameState){
		super(gameState);
		this.moveHistory = new ArrayList<>();
	}
	public ReversibleGameState(ReversibleGameState gameState){
		super(gameState);
		this.moveHistory = new ArrayList<>();
		this.moveHistory.addAll(gameState.moveHistory);
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
		ReversibleMove reversibleMove;
		if (move instanceof ReversibleMove reversibleMove1){
			reversibleMove = reversibleMove1;
		} else {
			reversibleMove = new ReversibleMove(move, this);
		}
		moveHistory.add(reversibleMove);
		super.makeMove(reversibleMove);
	}
	public void untryMove(){
		super.hash = -1;
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
		int enpassant = move.getEnpassantIndex();
		if (enpassant >= 8){
			enpassantIndex = -1;
		} else {
			enpassantIndex = (byte) (enpassant + 40 - 3 * (this.player ^ Tile.COLOR));
		}
		if (move.getTargetPiece() != Tile.BLANK)
			setTile(move.getTargetIndex(), move.getTargetPiece() | (this.player ^ Tile.COLOR));
		else
			setTile(move.getTargetIndex(), Tile.BLANK);

		if (move.getTargetIndex() == whiteKingIndex) whiteKingIndex = (byte) move.getOriginIndex();
		if (move.getTargetIndex() == blackKingIndex) blackKingIndex = (byte) move.getOriginIndex();

		halfmoves = (byte) move.getHalfmoves();
		castlingRights = (byte) move.getCastlingRights();
		encounteredPositionCount--;
	}
}
