import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIAgent extends Agent {
	public final int depth;
	private int prevEval = 0;
	private final Map<Long, Transposition> transpositionTable;

	public AIAgent(int depth){
		this.depth = depth;
		this.transpositionTable = new HashMap<>();
	}
	@Override
	public Move findMove(GameState rawGameState, ChessMatch match){
		ReversibleGameState gameState = (ReversibleGameState) rawGameState;
		MoveHandler moveHandler = new MoveHandler(gameState);
		List<Move> moves = new ArrayList<>();
		moveHandler.addMoves(moves);
		int best = -2_000_000;
		Move bestMove = null;
		int eval = deepEvaluate(moveHandler, depth);
		String change = (eval > prevEval ? "+" : "") + (eval-prevEval);
		if (match.lastMove != null) System.out.println("They chose "+match.lastMoveString+" : "+eval+" ("+change+")");
		for (Move move : moves){
			gameState.tryMove(move);
			int evaluation = -deepEvaluate(moveHandler, this.depth);
			if (evaluation > best){
				best = evaluation;
				bestMove = move;
			}
			gameState.untryMove();
		}
		change = (best > prevEval ? "+" : "") + (best-prevEval);
		System.out.println("Choosing "+gameState.toAlgebraicMoveNotation(bestMove)+" : "+best+" ("+change+")");
		prevEval = best;
		System.out.println(transpositionTable.size());
		return bestMove;
	}

	public int quiescentEvaluate(MoveHandler moveHandler){
		return quiescentEvaluate(moveHandler, -10_000_000, 10_000_000);
	}
	public int quiescentEvaluate(MoveHandler moveHandler, int alpha, int beta){
		List<Move> captures = new ArrayList<>();
		moveHandler.addCaptures(captures);
		if (captures.isEmpty()) return naiveEvaluate(moveHandler.gameState);
		int best = naiveEvaluate(moveHandler.gameState);
		if (best > alpha) alpha = best;
		if (alpha >= beta) return best;


		//TODO sort these
		for (Move move : captures){
			moveHandler.gameState.tryMove(move);
			int evaluation = -quiescentEvaluate(moveHandler, -beta, -alpha);
			moveHandler.gameState.untryMove();

			if (evaluation > best) best = evaluation;
			if (best > alpha) alpha = best;
			if (alpha >= beta) break;
		}
		// this move was a mate
		if (best > 100_000){
			best -= 1; // make earlier winning mates better
		}
		if (best < -100_000){
			best += 1; // make later losing mates better
		}

		return best;
	}
	public int deepEvaluate(MoveHandler moveHandler, int depth){
		return deepEvaluate(moveHandler, depth, -10_000_000, 10_000_000);
	}
	public int deepEvaluate(final MoveHandler moveHandler, int depth, int alpha, int beta){
		long hash = moveHandler.gameState.getHash();
		if (transpositionTable.containsKey(hash)){
			Transposition item = transpositionTable.get(hash);
			if (item.depth >= depth) return item.eval;
		}
		if (depth == 0) return naiveEvaluate(moveHandler.gameState);
		//if (depth == 0) return quiescentEvaluate(moveHandler);
		List<Move> moves = new ArrayList<>();
		moveHandler.addMoves(moves);
		if (moves.isEmpty()) return naiveEvaluate(moveHandler.gameState);

		int best = -2_000_000; // best move starts as getting checkmated twice

		// incredibly cursed nonsense generates a list of indicies into moves ordered by the evaluation of them
		int[] indicies = new int[moves.size()];
		for (int i = 0; i < indicies.length; i++){
			moveHandler.gameState.tryMove(moves.get(i));
			// this naiveEval is giving the opps eval, meaning lower = better. this is good because lower = lower idx when sorted
			int eval = naiveEvaluate(moveHandler.gameState) + 1_000_000; // makes me happy
			moveHandler.gameState.untryMove();
			indicies[i] = (eval << 8) | i ;
		}
		Arrays.sort(indicies);
		for (int i = 0; i < indicies.length; i++){
			indicies[i] = indicies[i] & 0xff;
		}


		for (int i = 0; i < indicies.length; i++){
			Move move = moves.get(indicies[i]);
			moveHandler.gameState.tryMove(move);
			int evaluation = -deepEvaluate(moveHandler, depth-1, -beta, -alpha);
			moveHandler.gameState.untryMove();

			if (evaluation > best) best = evaluation;
			if (evaluation > alpha) alpha = evaluation;
			if (alpha >= beta) break;
		}
		// this move was a mate
		if (best > 100_000){
			best -= 1; // make earlier winning mates better
		}
		if (best < -100_000){
			best += 1; // make later losing mates better
		}
		// we know that our depth is > than the depth in the table already because if it wasn't we would've skipped out on this method really early on
		transpositionTable.put(hash, new Transposition(hash, best, depth));
		return best;
	}
	public int naiveEvaluate(GameState gameState){

		GameState.Conclusion winner = gameState.findWinner();
		if (winner == GameState.Conclusion.WHITE){
			if (gameState.player == Tile.WHITE){
				return 1_000_000;
			} else {
				return -1_000_000;
			}
		} else if (winner == GameState.Conclusion.BLACK){
			if (gameState.player == Tile.BLACK){
				return 1_000_000;
			} else {
				return -1_000_000;
			}
		} else if (winner == GameState.Conclusion.TIE){
			return 0;
		}
		int evaluation = 0;
		for (int i = 0; i < 64; i++){
			byte contents = gameState.getTile(i);
			if (contents == Tile.BLANK) continue;
			evaluation += MATERIAL_VALUE[contents];
			evaluation += POSITION_VALUE[contents][i];
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

	private static final int[] BLACK_PAWN = {0, 0, 0, 0, 0, 0, 0, 0, 50, 50, 50, 50, 50, 50, 50, 50, 10, 10, 20, 30, 30, 20, 10, 10, 5, 5, 10, 25, 25, 10, 5, 5, 0, 0, 0, 20, 20, 0, 0, 0, 5, -5, -10, 0, 0, -10, -5, 5, 5, 10, 10, -20, -20, 10, 10, 5, 0, 0, 0, 0, 0, 0, 0, 0};
	private static final int[] BLACK_ROOK = {0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, 10, 10, 10, 10, 5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, -5, 0, 0, 0, 0, 0, 0, -5, 0, 0, 0, 5, 5, 0, 0, 0};
	private static final int[] BLACK_KNIGHT = {-50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 0, 0, 0, -20, -40, -30, 0, 10, 15, 15, 10, 0, -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 10, 15, 15, 10, 5, -30, -40, -20, 0, 5, 5, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50};
	private static final int[] BLACK_BISHOP = {-20, -10, -10, -10, -10, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 10, 10, 5, 0, -10, -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 10, 10, 10, 10, 0, -10, -10, 10, 10, 10, 10, 10, 10, -10, -10, 5, 0, 0, 0, 0, 5, -10, -20, -10, -10, -10, -10, -10, -10, -20};
	private static final int[] BLACK_QUEEN = {-20, -10, -10, -5, -5, -10, -10, -20, -10, 0, 0, 0, 0, 0, 0, -10, -10, 0, 5, 5, 5, 5, 0, -10, -5, 0, 5, 5, 5, 5, 0, -5, 0, 0, 5, 5, 5, 5, 0, -5, -10, 5, 5, 5, 5, 5, 0, -10, -10, 0, 5, 0, 0, 0, 0, -10, -20, -10, -10, -5, -5, -10, -10, -20};
	private static final int[] BLACK_KING = {-30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30, -20, -30, -30, -40, -40, -30, -30, -20, -10, -20, -20, -20, -20, -20, -20, -10, 20, 20, 0, 0, 0, 0, 20, 20, 20, 30, 10, 0, 0, 10, 30, 20};
	private static final int[] BLACK_KING_LATE = {-50, -40, -30, -20, -20, -30, -40, -50, -30, -20, -10, 0, 0, -10, -20, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 30, 40, 40, 30, -10, -30, -30, -10, 20, 30, 30, 20, -10, -30, -30, -30, 0, 0, 0, 0, -30, -30, -50, -30, -30, -30, -30, -30, -30, -50};

	private static final int[] WHITE_PAWN = {0, 0, 0, 0, 0, 0, 0, 0, -5, -10, -10, 20, 20, -10, -10, -5, -5, 5, 10, 0, 0, 10, 5, -5, 0, 0, 0, -20, -20, 0, 0, 0, -5, -5, -10, -25, -25, -10, -5, -5, -10, -10, -20, -30, -30, -20, -10, -10, -50, -50, -50, -50, -50, -50, -50, -50, 0, 0, 0, 0, 0, 0, 0, 0};
	private static final int[] WHITE_ROOK = {0, 0, 0, -5, -5, 0, 0, 0, 5, 0, 0, 0, 0, 0, 0, 5, 5, 0, 0, 0, 0, 0, 0, 5, 5, 0, 0, 0, 0, 0, 0, 5, 5, 0, 0, 0, 0, 0, 0, 5, 5, 0, 0, 0, 0, 0, 0, 5, -5, -10, -10, -10, -10, -10, -10, -5, 0, 0, 0, 0, 0, 0, 0, 0};
	private static final int[] WHITE_KNIGHT = {50, 40, 30, 30, 30, 30, 40, 50, 40, 20, 0, -5, -5, 0, 20, 40, 30, -5, -10, -15, -15, -10, -5, 30, 30, 0, -15, -20, -20, -15, 0, 30, 30, -5, -15, -20, -20, -15, -5, 30, 30, 0, -10, -15, -15, -10, 0, 30, 40, 20, 0, 0, 0, 0, 20, 40, 50, 40, 30, 30, 30, 30, 40, 50};
	private static final int[] WHITE_BISHOP = {20, 10, 10, 10, 10, 10, 10, 20, 10, -5, 0, 0, 0, 0, -5, 10, 10, -10, -10, -10, -10, -10, -10, 10, 10, 0, -10, -10, -10, -10, 0, 10, 10, -5, -5, -10, -10, -5, -5, 10, 10, 0, -5, -10, -10, -5, 0, 10, 10, 0, 0, 0, 0, 0, 0, 10, 20, 10, 10, 10, 10, 10, 10, 20};
	private static final int[] WHITE_QUEEN = {20, 10, 10, 5, 5, 10, 10, 20, 10, 0, -5, 0, 0, 0, 0, 10, 10, -5, -5, -5, -5, -5, 0, 10, 0, 0, -5, -5, -5, -5, 0, 5, 5, 0, -5, -5, -5, -5, 0, 5, 10, 0, -5, -5, -5, -5, 0, 10, 10, 0, 0, 0, 0, 0, 0, 10, 20, 10, 10, 5, 5, 10, 10, 20};
	private static final int[] WHITE_KING_EARLY = {-20, -30, -10, 0, 0, -10, -30, -20, -20, -20, 0, 0, 0, 0, -20, -20, 10, 20, 20, 20, 20, 20, 20, 10, 20, 30, 30, 40, 40, 30, 30, 20, 30, 40, 40, 50, 50, 40, 40, 30, 30, 40, 40, 50, 50, 40, 40, 30, 30, 40, 40, 50, 50, 40, 40, 30, 30, 40, 40, 50, 50, 40, 40, 30};
	private static final int[] WHITE_KING_LATE = {50, 30, 30, 30, 30, 30, 30, 50, 30, 30, 0, 0, 0, 0, 30, 30, 30, 10, -20, -30, -30, -20, 10, 30, 30, 10, -30, -40, -40, -30, 10, 30, 30, 10, -30, -40, -40, -30, 10, 30, 30, 10, -20, -30, -30, -20, 10, 30, 30, 20, 10, 0, 0, 10, 20, 30, 50, 40, 30, 20, 20, 30, 40, 50};

	private static final int[][] POSITION_VALUE = {
		new int[64], BLACK_PAWN, BLACK_ROOK, BLACK_KNIGHT, BLACK_BISHOP, BLACK_QUEEN, BLACK_KING, BLACK_KING_LATE,
		new int[64], WHITE_PAWN, WHITE_ROOK, WHITE_KNIGHT, WHITE_BISHOP, WHITE_QUEEN, WHITE_KING_EARLY, WHITE_KING_LATE
	};
	private static final int[] MATERIAL_VALUE = {
		0,  100,  500,  320,  330,  900,  0,  0,
		-0, -100, -500, -320, -330, -900, -0, -0
	};
	private static class Transposition{
		public final int depth;
		public final long hash;
		public int eval;
		public Transposition(long hash, int eval, int depth){
			this.hash = hash;
			this.eval = eval;
			this.depth = depth;
		}
		public Transposition(GameState gameState, int eval, int depth){
			this(gameState.regenerateZobristHash(), eval, depth);
		}
		@Override
		public int hashCode(){
			return (int) hash;
		}
		@Override
		public boolean equals(Object o){
			if (o == this) return true;
			if (o instanceof AIAgent.Transposition t){
				return t.hash == hash;
			}
			return false;
		}
		@Override
		public String toString(){
			return Long.toHexString(hash)+":"+eval+"("+depth+")";
		}
	}
}
