/**
 * This class provides expectations for a Agent to play chess
 */
public abstract class Agent {
	public ChessDisplay display;

	public abstract Move findMove(GameState gameState, ChessMatch match);
	public abstract String name();

	public final void updateDisplay(ChessMatch match){display.update(match);}
}
