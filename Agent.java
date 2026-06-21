public abstract class Agent {
	public ChessDisplay display;
	public abstract Move findMove(GameState gameState, ChessMatch match);

	public void updateDisplay(ChessMatch match){
		display.update(match);
	}
	public abstract String name();
}
