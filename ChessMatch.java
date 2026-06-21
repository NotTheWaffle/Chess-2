import java.util.ArrayList;
import java.util.List;

public final class ChessMatch{
	public final MoveHandler moveHandler;
	public final ReversibleGameState gameState;
	public Move lastMove;
	public String lastMoveString;

	public final Agent agentWhite;
	public final Agent agentBlack;
	public byte winner;

	public ChessMatch(Agent agentWhite, Agent agentBlack){
		gameState = new ReversibleGameState();
		moveHandler = new MoveHandler(gameState);
		this.agentWhite = agentWhite;
		this.agentBlack = agentBlack;
		this.lastMove = null;
	}
	public void play(){
		Agent currentPlayer = agentWhite;
		while (true){
			System.out.println(currentPlayer.name()+" playing");

			Move move = currentPlayer.findMove(gameState, this);
			this.lastMoveString = gameState.toAlgebraicMoveNotation(move);
			gameState.tryMove(move);
			List<Move> captures = new ArrayList<>();
			moveHandler.addCaptures(captures);
			System.out.println(captures);
			this.lastMove = move;
			currentPlayer.updateDisplay(this);

			if (gameState.halfmoves > 50){
				winner = 4;
				break;
			}
			long currentPosition = gameState.encounteredPositions.getLast();
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
			List<Move> moves = new ArrayList<>();
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
		System.out.println("game over: "+winner);
	}
}