import java.util.ArrayList;
import java.util.List;

public class AIAgent extends Agent {
	public final int depth;
	public AIAgent(int depth){
		this.depth = depth;
	}
	@Override
	public Move findMove(GameState rawGameState){
		ReversibleGameState gameState = (ReversibleGameState) rawGameState;
		MoveHandler moveHandler = new MoveHandler(gameState);
		List<Move> moves = new ArrayList<>();
		moveHandler.addLegalMoves(moves);
		int best = Integer.MIN_VALUE;
		Move bestMove = null;
		for (Move move : moves){
			gameState.tryMove(move);
			int evaluation = -deepEvaluate(moveHandler, this.depth);
			if (evaluation > best){
				best = evaluation;
				bestMove = move;
			}
			gameState.untryMove();
			System.out.println(gameState.toAlgebraicMoveNotation(move)+"gives a "+evaluation);
		}
		System.out.println("Choosing "+gameState.toAlgebraicMoveNotation(bestMove)+" ("+best+")");
		return bestMove;
	}
	public int deepEvaluate(MoveHandler moveHandler, int depth){
		if (depth == 0) return naiveEvaluate(moveHandler.gameState);
		List<Move> moves = new ArrayList<>();
		moveHandler.addLegalMoves(moves);
		if (moves.isEmpty()){
			return naiveEvaluate(moveHandler.gameState);
		}
		int best = Integer.MIN_VALUE;
		for (Move move : moves){
			moveHandler.gameState.tryMove(move);
			int evaluation = -deepEvaluate(moveHandler, depth-1);
			if (evaluation > best){
				best = evaluation;
			}
			moveHandler.gameState.untryMove();
		}
		return best;
	}
	public int naiveEvaluate(GameState gameState){
		int[] values = {
			 0,  100,  500,  320,  330,  900,  0,  0,
			-0, -100, -500, -320, -330, -900, -0, -0
		};
		GameState.Conclusion winner = gameState.findWinner();
		if (winner == GameState.Conclusion.WHITE){
			if (gameState.player == Tile.WHITE){
//				System.out.println("White is about to mate black (im white)");
				return 1_000_000;
			} else {
//				System.out.println("White is about to mate black (im black)");
				return -1_000_000;
			}
		} else if (winner == GameState.Conclusion.BLACK){
			if (gameState.player == Tile.BLACK){
//				System.out.println("Black is about to mate white (im black)");
				return 1_000_000;
			} else {
//				System.out.println("Black is about to mate white (im white)");
				return -1_000_000;
			}
		} else if (winner == GameState.Conclusion.TIE){
//			System.out.println("Stalemate");
			return 0;
		}
		int evaluation = 0;
		for (int i = 0; i < 64; i++){
			evaluation += values[gameState.getTile(i)];
		}
		if (gameState.player == Tile.WHITE){
			evaluation = -evaluation;
		}
		return evaluation;
	}
	@Override
	public String name(){
		return "AI Agent";
	}
}
