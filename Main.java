

public class Main{
	public static void main(String[] args){
		ChessMatch match = new ChessMatch(new HumanAgent(), new AIAgent(3));

		ChessDisplay game = new ChessDisplay(match);

		Window window = new Window(game);
		window.resize(288*3, 288*3);
		match.play();
	}
}