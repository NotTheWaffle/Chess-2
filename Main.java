

public class Main{
	public static void main(String[] args){
		ChessMatch match = new ChessMatch(new HumanAgent(), new AIAgent(4));

		ChessDisplay game = new ChessDisplay(match);

		Window window = new Window(game);
		window.resize(288*2, 288*2);
		match.play();
	}
}