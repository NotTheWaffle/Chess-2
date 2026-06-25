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
		long start = System.currentTimeMillis();
		ReversibleGameState gameState = new ReversibleGameState(rawGameState);
		MoveHandler moveHandler = new MoveHandler(gameState);
		List<Move> moves = new ArrayList<>();
		moveHandler.addMoves(moves);
		int best = -2_000_000;
		Move bestMove = null;
		int eval = deepEvaluate(moveHandler, gameState, depth);
		String change = (eval > prevEval ? "+" : "") + (eval-prevEval);
		if (match.lastMove != null) System.out.println("They chose "+match.lastMoveString+" : "+eval+" ("+change+")");
		for (Move move : moves){
			gameState.tryMove(move);
			int evaluation = -deepEvaluate(moveHandler, gameState, this.depth);
			if (evaluation > best){
				best = evaluation;
				bestMove = move;
			}
			gameState.untryMove();
		}
		change = (best > prevEval ? "+" : "") + (best-prevEval);
		System.out.println("Choosing "+gameState.toAlgebraicMoveNotation(bestMove)+" : "+best+" ("+change+")");
		prevEval = best;
		System.out.println("Thought for "+(System.currentTimeMillis()-start)/1_000.0+" secs");
		while (System.currentTimeMillis()-start < 250){
			try {Thread.sleep(25);} catch (Exception e){e.printStackTrace();}
		}
		return bestMove;
	}

	public int quiescentEvaluate(MoveHandler moveHandler, ReversibleGameState gameState){
		return quiescentEvaluate(moveHandler, gameState, -10_000_000, 10_000_000);
	}
	public int quiescentEvaluate(MoveHandler moveHandler, ReversibleGameState gameState, int alpha, int beta){
		long hash = moveHandler.gameState.getHash();
		if (transpositionTable.containsKey(hash)){
			Transposition item = transpositionTable.get(hash);
			if (depth == -1) return item.eval;
		}

		List<Move> captures = new ArrayList<>();
		moveHandler.addCaptures(captures);
		if (captures.isEmpty()) return naiveEvaluate(moveHandler);
		int best = naiveEvaluate(moveHandler);
		if (best > alpha) alpha = best;
		if (alpha >= beta) return best;

		// cursed sorting thing again
		int[] indicies = new int[captures.size()];
		for (int i = 0; i < indicies.length; i++){
			gameState.tryMove(captures.get(i));
			// this naiveEval is giving the opps eval, meaning lower = better. this is good because lower = lower idx when sorted
			int eval = naiveEvaluate(moveHandler) + 1_000_000; // shifts all evals up, removing twos complement mess
			gameState.untryMove();
			indicies[i] = (eval << 8) | i ;
		}
		Arrays.sort(indicies);
		for (int i = 0; i < indicies.length; i++){
			indicies[i] = indicies[i] & 0xff;
		}

		for (int i = 0; i < indicies.length; i++){
			Move move = captures.get(indicies[i]);
			gameState.tryMove(move);
			int evaluation = -quiescentEvaluate(moveHandler, gameState, -beta, -alpha);
			gameState.untryMove();

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
		transpositionTable.put(hash, new Transposition(hash, best, -1));
		return best;
	}
	public int deepEvaluate(MoveHandler moveHandler, ReversibleGameState gameState, int depth){
		return deepEvaluate(moveHandler, gameState, depth, -10_000_000, 10_000_000);
	}
	public int deepEvaluate(MoveHandler moveHandler, ReversibleGameState gameState, int depth, int alpha, int beta){
		GameState.Conclusion winner = moveHandler.gameState.findWinner(moveHandler);
		// this stuff isn't included in the hash so it needs to be handled here.
		switch (winner){
			case GameState.Conclusion.WHITE -> {
				if (moveHandler.gameState.player == Tile.WHITE){
					return 1_000_000;
				} else {
					return -1_000_000;
				}
			}
			case GameState.Conclusion.BLACK -> {
				if (moveHandler.gameState.player == Tile.BLACK){
					return 1_000_000;
				} else {
					return -1_000_000;
				}
			}
			case GameState.Conclusion.TIE -> {
				return 0;
			}
			default -> {}
		}

		long hash = moveHandler.gameState.getHash();
		Transposition item = transpositionTable.get(hash);
		if (item != null && item.depth >= depth) return item.eval;

		if (depth == 0) return naiveEvaluate(moveHandler);
		//if (depth == 0) return quiescentEvaluate(moveHandler);
		List<Move> moves = new ArrayList<>();
		moveHandler.addMoves(moves);
		if (moves.isEmpty()) return naiveEvaluate(moveHandler);

		int best = -2_000_000; // best move starts as getting checkmated twice

		// incredibly cursed nonsense generates a list of indicies into moves ordered by the evaluation of them
		int[] indicies = new int[moves.size()];
		for (int i = 0; i < indicies.length; i++){
			gameState.tryMove(moves.get(i));
			// this naiveEval is giving the opps eval, meaning lower = better. this is good because lower = lower idx when sorted
			int eval = naiveEvaluate(moveHandler) + 1_000_000; // shifts and fixes twos complement nonsense
			gameState.untryMove();
			indicies[i] = (eval << 8) | i ;
		}
		Arrays.sort(indicies);
		for (int i = 0; i < indicies.length; i++){
			indicies[i] = indicies[i] & 0xff;
		}


		for (int i = 0; i < indicies.length; i++){
			Move move = moves.get(indicies[i]);
			gameState.tryMove(move);
			int evaluation = -deepEvaluate(moveHandler, gameState, depth-1, -beta, -alpha);
			gameState.untryMove();

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
		// we know that our depth is > than the depth in the table already because if it wasn't we would've skipped out on this method earlier
		transpositionTable.put(hash, new Transposition(hash, best, depth));
		return best;
	}
	public int naiveEvaluate(MoveHandler moveHandler){
		//TODO include 50 move rule and threefold repetition
		GameState.Conclusion winner = moveHandler.gameState.findWinner(moveHandler);
		switch (winner){
			case GameState.Conclusion.WHITE -> {
				if (moveHandler.gameState.player == Tile.WHITE){
					return 1_000_000;
				} else {
					return -1_000_000;
				}
			}
			case GameState.Conclusion.BLACK -> {
				if (moveHandler.gameState.player == Tile.BLACK){
					return 1_000_000;
				} else {
					return -1_000_000;
				}
			}
			case GameState.Conclusion.TIE -> {
				return 0;
			}
			default -> {}
		}

		int evaluation = 0;
		for (int i = 0; i < 64; i++){
			byte contents = moveHandler.gameState.getTile(i);
			if (contents == Tile.BLANK) continue;
			evaluation += MATERIAL_VALUE[contents];
			evaluation += POSITION_VALUE[contents][i];
		}

		if (moveHandler.gameState.player == Tile.WHITE){
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
		@Override
		public int hashCode(){
			return (int) hash;
		}
		@Override
		public boolean equals(Object o){
			if (o == this) return true;
			if (o instanceof AIAgent.Transposition t){
				return t.hash == hash;
			} else {
				return false;
			}
		}
		@Override
		public String toString(){
			return Long.toHexString(hash)+":"+eval+"("+depth+")";
		}
	}
}
