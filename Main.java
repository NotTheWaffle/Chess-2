
import java.util.Arrays;



public class Main{
	public static void main(String[] args){
		int[] arr = {4, 1, 2, 3};
		Arrays.sort(arr);
		System.out.println(Arrays.toString(arr));
		ChessMatch match = new ChessMatch(new HumanAgent(), new AIAgent(5));

		ChessDisplay game = new ChessDisplay(match, false);

		Window window = new Window(game);
		window.resize(288*2, 288*2);
		match.play();
	}
}