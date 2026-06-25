



public class Main{
	public static void main(String[] args){
		ChessMatch match = new ChessMatch(new AIAgent(5, false), new HumanAgent());

		ChessDisplay game = new ChessDisplay(match, false);

		Window window = new Window(game);
		window.resize(288*3, 288*3);
		match.play();
	}
}