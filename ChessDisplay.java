import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class ChessDisplay extends Game{
	private final SpriteSet sprites;
	// game stuff
	private final ChessMatch match;
	private final ReversibleGameState gameState;
	private final MoveHandler moveHandler;
	private final Agent playerWhite;
	private final Agent playerBlack;

	private boolean cursor = false;
	private boolean grabbing = false;

	// display stuff
	private byte[] board;
	private final List<Move> renderedMoves;
	private int selectedIndex;
	private Move lastMove;
	private Move chosenMove;

	// promotion stuff
	private boolean promoting;
	private byte promotingColor;
	private List<Move> promotions;

	public ChessDisplay(ChessMatch match){
		super(288, 288);
		sprites = new SpriteSet("Gambit", "Gambit");
		this.match = match;
		this.moveHandler = match.moveHandler;
		gameState = new ReversibleGameState(match.gameState);
		this.selectedIndex = -1;

		renderedMoves = new ArrayList<>();
		promoting = false;
		promotingColor = 0;
		promotions = new ArrayList<>();
		lastMove = null;

		playerWhite = match.agentWhite;
		playerBlack = match.agentBlack;
		match.agentWhite.display = this;
		match.agentBlack.display = this;
	}
	@Override
	public void tick(){}
	@Override
	public void onMouseDown(GamePanel panel){
		if (input.mouseDown == Input.MOUSE_RIGHT){
			return;
		}
		if (input.mouseDown != Input.MOUSE_LEFT) return;
		if (chosenMove != null) return;
		// de-select a piece when you mousedown on a different index OR when you click on it again and let go in its position
		int tileX = inverseTransformX(input.mouseX);
		int tileY = inverseTransformY(input.mouseY);

		if (tileX < 0) return;
		if (tileX > 7) return;
		if (tileY < 0) return;
		if (tileY > 7) return;
		int mouseIndex = tileX + tileY*8;
		if (gameState.getTile(mouseIndex) != Tile.BLANK){
			selectedIndex = mouseIndex;
			this.grabbing = true;
		} else {
			selectedIndex = -1;
		}


		if (promoting){
			int index;
			if (tileX != promotions.get(0).getTargetX()){
				return;
			}
			if (promotingColor == Tile.WHITE){
				if (tileY < 4){
					return;
				}
				index = 7-tileY;
			} else {
				if (tileY > 3){
					return;
				}
				index = tileY;
			}
			lastMove = promotions.get(index);
			chosenMove = lastMove;

			renderedMoves.clear();
			promoting = false;
			promotions.clear();
			promotingColor = -1;
			return;
		}


		List<Move> foundMoves = new ArrayList<>();
		chosenMove = null;
		promoting = false;
		for (Move move : renderedMoves){
			if (move.getTargetIndex() == mouseIndex){
				foundMoves.add(move);
			}
		}
		if (foundMoves.size() == 1){
			chosenMove = foundMoves.get(0);
			selectedIndex = -1;
		} else if (foundMoves.size() == 4){
			promoting = true;
			promotions = foundMoves;
			promotingColor = gameState.player;
			selectedIndex = -1;
		}

		renderedMoves.clear();
		moveHandler.addMoves(mouseIndex, renderedMoves);
		moveHandler.legalizeMoves(renderedMoves);
		render();
	}
	@Override
	public void onMouseUp(GamePanel panel){
		if (chosenMove != null) return;
		this.grabbing = false;

		int tileX = inverseTransformX(input.mouseX);
		int tileY = inverseTransformY(input.mouseY);

		if (tileX < 0) return;
		if (tileX > 7) return;
		if (tileY < 0) return;
		if (tileY > 7) return;
		int mouseIndex = tileX + tileY*8;

		if (promoting) return;

		List<Move> foundMoves = new ArrayList<>();
		chosenMove = null;
		promoting = false;
		for (Move move : renderedMoves){
			if (move.getTargetIndex() == mouseIndex){
				foundMoves.add(move);
			}
		}
		if (foundMoves.size() == 1){
			chosenMove = foundMoves.get(0);
			renderedMoves.clear();
		} else if (foundMoves.size() == 4){
			promoting = true;
			promotions = foundMoves;
			promotingColor = gameState.player;
			renderedMoves.clear();
		}

		render();
	}
	@Override
	public void onMouseMotion(GamePanel panel){
		int tileX = inverseTransformX(input.mouseX);
		int tileY = inverseTransformY(input.mouseY);

		if (tileX < 0) return;
		if (tileX > 7) return;
		if (tileY < 0) return;
		if (tileY > 7) return;
		int mouseIndex = tileX + tileY*8;
		boolean grabbing = (gameState.getTile(mouseIndex) != Tile.BLANK) || this.grabbing;
		if (cursor ^ grabbing){
			cursor = !cursor;
			if (cursor){
				panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			} else {
				panel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
		if (cursor) render();
	}
	public Move findMove(){
		lastMove = match.lastMove;

		while (chosenMove == null){
			// fast and slow enough to not cause performance issues but also not cause input lag
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				System.out.println("Human response interrupted");
				e.printStackTrace();
			}
		}

		// preempt the move so that the display can show it
		lastMove = chosenMove;
		chosenMove = null;
		return lastMove;
	}
	public void update(ChessMatch match){
		gameState.makeMove(match.lastMove);
		render();
	}
	public void render(){
		window.render();
		// calls updateframe somewhere down the line
	}
	public int transformX(int tileX){
		return tileX*36+2;
	}
	public int transformY(int tileY){
		return (7-tileY)*36+2;
	}
	public int inverseTransformX(int x){
		return x/36;
	}
	public int inverseTransformY(int y){
		return 7-y/36;
	}

	@Override
	public void updateFrame(Graphics2D g2d){
		g2d.drawImage(sprites.BOARD_WHITE, 0, 0, null);

		if (lastMove != null){
			BufferedImage image;
			int ox = lastMove.getOriginX();
			int oy = lastMove.getOriginY();
			if ((ox + oy) % 2 == 0){
				image = sprites.BLACK_HIGHLIGHT;
			} else {
				image = sprites.WHITE_HIGHLIGHT;
			}
			g2d.drawImage(image, transformX(ox)-2, transformY(oy)-2, null);

			int dx = lastMove.getTargetX();
			int dy = lastMove.getTargetY();
			if ((dx + dy) % 2 == 0){
				image = sprites.BLACK_HIGHLIGHT;
			} else {
				image = sprites.WHITE_HIGHLIGHT;
			}
			g2d.drawImage(image, transformX(dx)-2, transformY(dy)-2, null);
		}

		if (selectedIndex != -1){
			int x = selectedIndex % 8;
			int y = selectedIndex / 8;
			BufferedImage image;
			if ((x + y) % 2 == 0){
				image = sprites.BLACK_HIGHLIGHT;
			} else {
				image = sprites.WHITE_HIGHLIGHT;
			}
			g2d.drawImage(image, transformX(x)-2, transformY(y)-2, null);
		}

		g2d.drawImage(sprites.BOARD_WHITE_TEXT, 0, 0, null);



		for (int x = 0; x < 8; x++){
			for (int y = 0; y < 8; y++){
				byte piece = gameState.getTile(x, y);
				if (piece == Tile.BLANK) continue;
				if ((x + y * 8) == selectedIndex && input.mouseDown == Input.MOUSE_LEFT) continue;
				g2d.drawImage(sprites.getImage(piece), transformX(x), transformY(y), null);
			}
		}

		// show possible moves
		for (Move move : renderedMoves){
			int dx = move.getTargetX();
			int dy = move.getTargetY();
			int idx = move.getTargetIndex();
			g2d.setColor(new Color(0, 0, 0, 96));
			if (gameState.getTile(idx) != Tile.BLANK){
				int x = transformX(dx);
				int y = transformY(dy);

				Area donut = new Area(new Ellipse2D.Double(x, y, 32, 32));
				donut.subtract(new Area(new Ellipse2D.Double(x+4, y+4, 24, 24)));

				g2d.fill(donut);
			} else {
				int x = transformX(dx);
				int y = transformY(dy);
				g2d.fillOval(x+10, y+10, 12, 12);
			}
		}


		// promotion dialogue
		if (promoting) {
			int dx = promotions.get(0).getTargetX();
			int dy = promotions.get(0).getTargetY();
			g2d.setColor(Color.GRAY);
			int m;
			if (promotingColor == Tile.WHITE){
				g2d.fillRect(transformX(dx)-2, transformY(dy)-2, 36, 144);
				m = 1;
			} else {
				g2d.fillRect(transformX(dx)-2, transformY(dy)-2-108, 36, 144);
				m = -1;
			}
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(0).getFlag()))), transformX(dx), transformY(dy), null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(1).getFlag()))), transformX(dx), transformY(dy)+36*m, null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(2).getFlag()))), transformX(dx), transformY(dy)+72*m, null);
			g2d.drawImage(sprites.getImage((byte) (promotingColor|Move.getPiece(promotions.get(3).getFlag()))), transformX(dx), transformY(dy)+108*m, null);
			return;
		}


		if (selectedIndex != -1 && input.mouseDown == Input.MOUSE_LEFT) {
			int x = selectedIndex % 8;
			int y = selectedIndex / 8;
			g2d.drawImage(sprites.getImage(gameState.getTile(x, y)), input.mouseX-16, input.mouseY-16, null);
		}
	}
	@Override
	public String name(){
		return "Chess Game 2";
	}
}
