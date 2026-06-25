import java.util.ArrayList;
import java.util.List;

public final class ChessMatch{
	private final MoveHandler moveHandler;
	public final GameState gameState;
	public Move lastMove;
	public String lastMoveString;

	public final Agent agentWhite;
	public final Agent agentBlack;
	public byte winner;

	public ChessMatch(Agent agentWhite, Agent agentBlack){
		gameState = new GameState();
		moveHandler = new MoveHandler(gameState);
		this.agentWhite = agentWhite;
		this.agentBlack = agentBlack;
		this.lastMove = null;
	}
	public void play(){
		Agent currentPlayer = agentWhite;
		while (true){
			System.out.println(currentPlayer.name() + " playing");

			Move move = currentPlayer.findMove(gameState, this);
			this.lastMove = move;
			this.lastMoveString = gameState.toAlgebraicMoveNotation(lastMove);

			gameState.makeMove(move);

			currentPlayer.updateDisplay(this);

			if (gameState.halfmoves > 50){
				winner = 4;
				break;
			}
			long currentPosition = gameState.getHash();
			int repetition = 0;
			for (Long position : gameState.encounteredPositions){
				if (position == currentPosition){
					repetition++;
				}
			}
			if (repetition >= 3){
				winner = 4;
				break;
			}
			List<Move> moves = new ArrayList<>(40);
			moveHandler.addMoves(moves);
			if (moves.isEmpty()){
				if (moveHandler.isAttacked(gameState.whiteKingIndex, Tile.BLACK)){
					winner = Tile.BLACK;
					break;
				} else if (moveHandler.isAttacked(gameState.blackKingIndex, Tile.WHITE)){
					winner = Tile.WHITE;
					break;
				} else {
					winner = 4;
					break;
				}
			}
			if (gameState.player == Tile.WHITE){
				currentPlayer = agentWhite;
			} else {
				currentPlayer = agentBlack;
			}
		}

		if (winner == Tile.BLACK){
			System.out.println("Game over: White was Checkmated");
		} else if (winner == Tile.WHITE){
			System.out.println("Game over: Black was Checkmated");
		} else {
			System.out.println("Game over: Stalemate");
		}
	}
}