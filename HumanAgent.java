public class HumanAgent implements Agent{
	public ChessDisplay display;
	@Override
	public Move findMove(GameState gameState){
		return display.findMove();
	}
	@Override
	public String name(){
		return "Human Agent";
	}
}