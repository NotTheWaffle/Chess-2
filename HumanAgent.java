public class HumanAgent extends Agent{
	@Override
	public Move findMove(GameState gameState, ChessMatch match){
		return display.findMove();
	}
	@Override
	public String name(){
		return "Human Agent";
	}
}