
public final class ChessMatch{
	public final GameState gameState;
	public Move lastMove;

	public final Agent agentWhite;
	public final Agent agentBlack;

	public ChessMatch(Agent agentWhite, Agent agentBlack){
		gameState = new ReversibleGameState();
		this.agentWhite = agentWhite;
		this.agentBlack = agentBlack;
		this.lastMove = null;
	}
	public void play(){
		Agent currentPlayer = agentWhite;
		while (true){
			System.out.println(agentWhite.name()+" playing");
			Move move = currentPlayer.findMove(gameState);
			gameState.makeMove(move);
			if (gameState.player == Tile.WHITE){
				currentPlayer = agentWhite;
			} else {
				currentPlayer = agentBlack;
			}
			this.lastMove = move;
		}
	}
}