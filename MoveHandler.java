
import java.util.List;
/**
 * A collection of functions which handle the generation and legalization of moves with an associated {@code GameState}
 */
public class MoveHandler {
	/**
	 * The {@code GameState} all moves will be handled upon
	 */
	public final GameState gameState;

	/**
	 * Generates a {@code MoveHandler} with an associated {@code GameState}
	 * @param gameState
	 */
	public MoveHandler(GameState gameState){
		this.gameState = gameState;
	}

	/**
	 * Adds all the semilegal moves in the gamestate associated with this {@code MoveHandler}
	 * @param moves list the available moves will be added to
	 */
	public void addMoves(List<Move> moves){
		for (int originIndex = 0; originIndex < 64; originIndex++){
			byte piece = gameState.getTile(originIndex);
			if (piece == Tile.BLANK) continue;
			int pieceColor = piece & Tile.COLOR;
			if (pieceColor != gameState.player) continue;
			addMoves(originIndex, moves);
		}
		legalizeMoves(moves);
	}
	/**
	 * Adds all the semilegal moves originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @param moves list the available moves will be added to
	 */
	public void addMoves(int index, List<Move> moves){
		byte piece = gameState.getTile(index);

		int pieceType = piece & Tile.PIECE;
		int pieceColor = piece & Tile.COLOR;

		if (pieceType == Tile.BLANK) return;
		if (pieceColor != gameState.player) return;

		switch (pieceType){
			case Tile.PAWN -> addPawnMoves(index, moves);
			case Tile.KNIGHT -> addKnightMoves(index, moves);
			case Tile.BISHOP -> addBishopMoves(index, moves);
			case Tile.ROOK -> addRookMoves(index, moves);
			case Tile.QUEEN -> addQueenMoves(index, moves);
			case Tile.KING -> addKingMoves(index, moves);
		}
	}
	/**
	 * Removes any moves which put yourself in check from the list
	 * @param moves list the illegal moves will be removed from
	 */
	public void legalizeMoves(List<Move> moves){
		int kingIndex = (gameState.player == Tile.WHITE) ? gameState.whiteKingIndex : gameState.blackKingIndex;
		for (int i = 0; i < moves.size(); i++){
			if (!isLegal(moves.get(i), kingIndex)){
				moves.remove(i);
				i--;
			}
		}
	}
	/**
	 * Tests whether your king is in check after making this move
	 * @param move move to test
	 * @return whether your king isn't/is in check after this move
	 */
	public boolean isLegal(Move move){
		return isLegal(move, (gameState.player == Tile.WHITE) ? gameState.whiteKingIndex : gameState.blackKingIndex);
	}
	/**
	 * Tests whether your king is in check after making this move
	 * @param move move to test
	 * @param kingIndex the index of your king before this move
	 * @return whether your king isn't/is in check after this move
	 */
	public boolean isLegal(Move move, int kingIndex){
		boolean legal = true;
		byte origin = gameState.getTile(move.getOriginIndex());
		byte target = gameState.getTile(move.getTargetIndex());

		gameState.setTile(move.getTargetIndex(), origin);
		if (move.getFlag() == Move.ENPASSANT){
			int d = 8 - 2 * gameState.player; // works because white = 8, black = 0
			gameState.setTile(move.getTargetIndex() + d, Tile.BLANK);
		}
		gameState.setTile(move.getOriginIndex(), Tile.BLANK);

		int tkingIndex = ((origin & Tile.PIECE) == Tile.KING) ? move.getTargetIndex() : kingIndex;
		if (isAttacked(tkingIndex, gameState.player ^ Tile.COLOR)){
			legal = false;
		}

		gameState.setTile(move.getOriginIndex(), origin);
		if (move.getFlag() == Move.ENPASSANT){
			int d = 8 - 2 * gameState.player; // works because white = 8, black = 0
			gameState.setTile(move.getTargetIndex() + d, Tile.PAWN|(gameState.player^Tile.COLOR));
		}
		gameState.setTile(move.getTargetIndex(), target);
		return legal;
	}

	/**
	 * Adds all the semilegal moves a pawn could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @param moves list the avilable moves will be added to
	 */
	public void addPawnMoves(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		if (y == 0 || y == 7) return;
		int dy;
		int dy2;
		boolean doubleMove = false;
		if (color == Tile.WHITE){
			dy = y+1;
			dy2 = y+2;
			if (y == 1) doubleMove = true;
		} else {
			dy = y-1;
			dy2 = y-2;
			if (y == 6) doubleMove = true;
		}
		targetTile = gameState.getTile(x, dy);
		// single move
		if (targetTile == Tile.BLANK){
			if (dy == 0 || dy == 7){
				// promotion
				moves.add(new Move(x, y, x, dy, Move.PROMOTION_KNIGHT));
				moves.add(new Move(x, y, x, dy, Move.PROMOTION_BISHOP));
				moves.add(new Move(x, y, x, dy, Move.PROMOTION_ROOK));
				moves.add(new Move(x, y, x, dy, Move.PROMOTION_QUEEN));
			} else {
				moves.add(new Move(x, y, x, dy));
			}
			// double move
			if (doubleMove){
				targetTile = gameState.getTile(x, dy2);
				if (targetTile== Tile.BLANK){
					moves.add(new Move(x, y, x, dy2, Move.PAWN_DOUBLE));
				}
			}
		}
		// capture moves
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			byte captureTarget = gameState.getTile(dx, dy);
			if (captureTarget != Tile.BLANK && (captureTarget & Tile.COLOR) != color){
				if (dy == 0 || dy == 7){
					// promotion
					moves.add(new Move(x, y, dx, dy, Move.PROMOTION_KNIGHT));
					moves.add(new Move(x, y, dx, dy, Move.PROMOTION_ROOK));
					moves.add(new Move(x, y, dx, dy, Move.PROMOTION_BISHOP));
					moves.add(new Move(x, y, dx, dy, Move.PROMOTION_QUEEN));
				} else {
					moves.add(new Move(x, y, dx, dy));
				}
			} else if (gameState.enpassantIndex == dx+dy*8){
				// en passant capture
				moves.add(new Move(x, y, dx, dy, Move.ENPASSANT));
			}
		}
	}
	/**
	 * Adds all the semilegal moves a knight could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @param moves list the avilable moves will be added to
	 */
	public void addKnightMoves(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		// vertical rectangle
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-2; dy <= y+2; dy += 4){
				if (dy < 0 || dy >= 8) continue;
				targetTile = gameState.getTile(dx, dy);
				if (targetTile == Tile.BLANK || (targetTile & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
		// horizontal rectangle
		for (int dx = x-2; dx <= x+2; dx += 4){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy += 2){
				if (dy < 0 || dy >= 8) continue;
				targetTile = gameState.getTile(dx, dy);
				if (targetTile == Tile.BLANK || (targetTile & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
	}
	/**
	 * Adds all the semilegal moves a bishop could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @param moves list the avilable moves will be added to
	 */
	public void addBishopMoves(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		// northeast
		for (int d = 1; x+d < 8 && y+d < 8; d++){
			targetTile = gameState.getTile(x+d, y+d);
			if (targetTile == Tile.BLANK){
				moves.add(new Move(x, y, x+d, y+d));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x+d, y+d));
			break;
		}
		// northwest
		for (int d = 1; x-d >= 0 && y+d < 8; d++){
			targetTile = gameState.getTile(x-d, y+d);
			if (targetTile == Tile.BLANK){
				moves.add(new Move(x, y, x-d, y+d));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x-d, y+d));
			break;
		}
		// southeast
		for (int d = 1; x+d < 8 && y-d >= 0; d++){
			targetTile = gameState.getTile(x+d, y-d);
			if (targetTile == Tile.BLANK){
				moves.add(new Move(x, y, x+d, y-d));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x+d, y-d));
			break;
		}
		// southwest
		for (int d = 1; x-d >= 0 && y-d >= 0; d++){
			targetTile = gameState.getTile(x-d, y-d);
			if (targetTile == Tile.BLANK){
				moves.add(new Move(x, y, x-d, y-d));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x-d, y-d));
			break;
		}
	}
	/**
	 * Adds all the semilegal moves a rook could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @param moves list the avilable moves will be added to
	 */
	public void addRookMoves(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		// east
		for (int dx = x+1; dx < 8; dx++){
			targetTile = gameState.getTile(dx, y);
			if (targetTile== Tile.BLANK){
				moves.add(new Move(x, y, dx, y));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, dx, y));
			break;
		}
		// west
		for (int dx = x-1; dx >= 0; dx--){
			targetTile = gameState.getTile(dx, y);
			if (targetTile== Tile.BLANK){
				moves.add(new Move(x, y, dx, y));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, dx, y));
			break;
		}
		// north
		for (int dy = y+1; dy < 8; dy++){
			targetTile = gameState.getTile(x, dy);
			if (targetTile== Tile.BLANK){
				moves.add(new Move(x, y, x, dy));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x, dy));
			break;
		}
		// south
		for (int dy = y-1; dy >= 0; dy--){
			targetTile = gameState.getTile(x, dy);
			if (targetTile== Tile.BLANK){
				moves.add(new Move(x, y, x, dy));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x, dy));
			break;
		}
	}
	/**
	 * Adds all the semilegal moves a queen could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @param moves list the avilable moves will be added to
	 */
	public void addQueenMoves(int index, List<Move> moves){
		addRookMoves(index, moves);
		addBishopMoves(index, moves);
	}
	/**
	 * Adds all the semilegal moves a king could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @param moves list the avilable moves will be added to
	 */
	public void addKingMoves(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		for (int dx = x-1; dx <= x+1; dx++){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy++){
				if (dx == x && dy == y) continue;
				if (dy < 0 || dy >= 8) continue;
				targetTile = gameState.getTile(dx, dy);
				if (targetTile == Tile.BLANK || (targetTile & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
		// castling
		if (color == Tile.WHITE){
			if (!isAttacked(4, Tile.BLACK)){
				if ((gameState.castlingRights & 0b0010) > 0 && gameState.getTile(1) == Tile.BLANK && gameState.getTile(2) == Tile.BLANK && gameState.getTile(3) == Tile.BLANK && !isAttacked(3, Tile.BLACK)){
					moves.add(new Move(4, 0, 2, 0, Move.CASTLING));
				}
				if ((gameState.castlingRights & 0b0001) > 0 && gameState.getTile(5) == Tile.BLANK && gameState.getTile(6) == Tile.BLANK && !isAttacked(5, Tile.BLACK)){
					moves.add(new Move(4, 0, 6, 0, Move.CASTLING));
				}
			}
		} else {
			if (!isAttacked(60, Tile.WHITE)){
				if ((gameState.castlingRights & 0b1000) > 0 && gameState.getTile(57) == Tile.BLANK && gameState.getTile(58) == Tile.BLANK && gameState.getTile(59) == Tile.BLANK && !isAttacked(59, Tile.WHITE)){
					moves.add(new Move(4, 7, 2, 7, Move.CASTLING));
				}
				if ((gameState.castlingRights & 0b0100) > 0 && gameState.getTile(61) == Tile.BLANK && gameState.getTile(62) == Tile.BLANK && !isAttacked(61, Tile.WHITE)){
					moves.add(new Move(4, 7, 6, 7, Move.CASTLING));
				}
			}
		}
	}

	/**
	 * Checks to find whether the specified index is attacked by any piece of a specified color. Only guarenteed to work when a king is at the specified index
	 * @param index position whose safety is check
	 * @param byColor the color which is attacking
	 * @return whether or not the piece is attacked
	 */
	public boolean isAttacked(int index, int byColor){
		// TODO make better
		// only intended to check for king, ignore en passant captures
		byte targetTile;
		int x = index & 0b111;
		int y = index >> 3;
		// rook/queen checks
		byte threat1 = (byte)(Tile.ROOK|byColor);
		byte threat2 = (byte)(Tile.QUEEN|byColor);
		for (int dx = x+1; dx < 8; dx++){
			targetTile = gameState.getTile(dx, y);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		for (int dx = x-1; dx >= 0; dx--){
			targetTile = gameState.getTile(dx, y);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		for (int dy = y+1; dy < 8; dy++){
			targetTile = gameState.getTile(x, dy);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		for (int dy = y-1; dy >= 0; dy--){
			targetTile = gameState.getTile(x, dy);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// bishop/queen checks
		threat1 = (byte)(Tile.BISHOP|byColor);
		// northeast
		for (int d = 1; x+d < 8 && y+d < 8; d++){
			targetTile = gameState.getTile(x+d, y+d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// northwest
		for (int d = 1; x-d >= 0 && y+d < 8; d++){
			targetTile = gameState.getTile(x-d, y+d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// southeast
		for (int d = 1; x+d < 8 && y-d >= 0; d++){
			targetTile = gameState.getTile(x+d, y-d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// southwest
		for (int d = 1; x-d >= 0 && y-d >= 0; d++){
			targetTile = gameState.getTile(x-d, y-d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}

		// knight checks
		threat1 = (byte)(Tile.KNIGHT|byColor);
		int dx, dy;
		dx = x-1; dy = y-2;
		if (dy >= 0){
			if (dx >= 0 && gameState.getTile(dx, dy) == threat1) return true;
			dx = x+1;
			if (dx < 8 && gameState.getTile(dx, dy) == threat1) return true;
		} else {
			dx = x+1;
		}
		dy = y+2;
		if (dy < 8){
			if (dx < 8 && gameState.getTile(dx, dy) == threat1) return true;
			dx = x-1;
			if (dx >= 0 && gameState.getTile(dx, dy) == threat1) return true;
		}

		dx = x-2;
		dy = y-1;
		if (dy >= 0){
			if (dx >= 0 && gameState.getTile(dx, dy) == threat1) return true;
			dx = x+2;
			if (dx < 8 && gameState.getTile(dx, dy) == threat1) return true;
		} else {
			dx = x+2;
		}
		dy = y+1;
		if (dy < 8){
			if (dx < 8 && gameState.getTile(dx, dy) == threat1) return true;
			dx = x-2;
			if (dx >= 0 && gameState.getTile(dx, dy) == threat1) return true;
		}




		// king check
		threat1 = (byte)(Tile.KING|byColor);

		dy = y-1;
		if (dy >= 0){
			dx = x-1;
			if (dx >= 0 && gameState.getTile(dx, dy) == threat1) return true;
			dx = x;
			if (gameState.getTile(dx, dy) == threat1) return true;
			dx = x+1;
			if (dx < 8 && gameState.getTile(dx, dy) == threat1) return true;
		} else {
			dx = x+1;
		}

		dy = y;
		// don't check the center square
		if (dx < 8 && gameState.getTile(dx, dy) == threat1) return true;
		dx = x-1;
		if (dx >= 0 && gameState.getTile(dx, dy) == threat1) return true;

		dy = y+1;
		if (dy < 8){
			if (dx >= 0 && dy < 8 && gameState.getTile(dx, dy) == threat1) return true;
			dx = x;
			if (gameState.getTile(dx, dy) == threat1) return true;
			dx = x+1;
			if (dx < 8 && dy < 8 && gameState.getTile(dx, dy) == threat1) return true;
		}



		// pawn check
		dy = 1 + y - byColor/4; // subtract 1 if white, add 1 if black
		if (dy >= 0 && dy < 8){
			threat1 = (byte)(Tile.PAWN|byColor);
			if (x < 7 && gameState.getTile(x+1, dy) == threat1){
				return true;
			}
			if (x > 0 && gameState.getTile(x-1, dy) == threat1){
				return true;
			}
		}
		return false;
	}
	/**
	 * Finds the piece of a certain color of the least value which attacks the index specified
	 * @param index specified index attacked
	 * @param byColor color of attacker
	 * @return a correspondent {@code Tile.PIECE} (or {@code Tile.BLANK} if there are no attackers)
	 */
	public byte cheapestAttacker(int index, int byColor){
		byte targetTile;
		int x = index & 0b111;
		int y = index >> 3;
		// rook/queen checks
		byte threat;
		int dx, dy;



		// pawn check TODO en passant now matters :(
		dy = 1 + y - byColor/4; // subtract 1 if white, add 1 if black
		if (dy >= 0 && dy < 8){
			threat = (byte)(Tile.PAWN|byColor);
			if (x < 7 && gameState.getTile(x+1, dy) == threat){
				return threat;
			}
			if (x > 0 && gameState.getTile(x-1, dy) == threat){
				return threat;
			}
		}

		// knight checks
		threat = (byte)(Tile.KNIGHT|byColor);
		dx = x-1; dy = y-2;
		if (dy >= 0){
			if (dx >= 0 && gameState.getTile(dx, dy) == threat) return threat;
			dx = x+1;
			if (dx < 8 && gameState.getTile(dx, dy) == threat) return threat;
		} else {
			dx = x+1;
		}
		dy = y+2;
		if (dy < 8){
			if (dx < 8 && gameState.getTile(dx, dy) == threat) return threat;
			dx = x-1;
			if (dx >= 0 && gameState.getTile(dx, dy) == threat) return threat;
		}

		dx = x-2;
		dy = y-1;
		if (dy >= 0){
			if (dx >= 0 && gameState.getTile(dx, dy) == threat) return threat;
			dx = x+2;
			if (dx < 8 && gameState.getTile(dx, dy) == threat) return threat;
		} else {
			dx = x+2;
		}
		dy = y+1;
		if (dy < 8){
			if (dx < 8 && gameState.getTile(dx, dy) == threat) return threat;
			dx = x-2;
			if (dx >= 0 && gameState.getTile(dx, dy) == threat) return threat;
		}


		// bishop checks
		threat = (byte)(Tile.BISHOP|byColor);
		// northeast
		for (int d = 1; x+d < 8 && y+d < 8; d++){
			targetTile = gameState.getTile(x+d, y+d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		// northwest
		for (int d = 1; x-d >= 0 && y+d < 8; d++){
			targetTile = gameState.getTile(x-d, y+d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		// southeast
		for (int d = 1; x+d < 8 && y-d >= 0; d++){
			targetTile = gameState.getTile(x+d, y-d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		// southwest
		for (int d = 1; x-d >= 0 && y-d >= 0; d++){
			targetTile = gameState.getTile(x-d, y-d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}

		// rook checks
		threat = (byte)(Tile.ROOK|byColor);
		for (dx = x+1; dx < 8; dx++){
			targetTile = gameState.getTile(dx, y);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		for (dx = x-1; dx >= 0; dx--){
			targetTile = gameState.getTile(dx, y);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		for (dy = y+1; dy < 8; dy++){
			targetTile = gameState.getTile(x, dy);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		for (dy = y-1; dy >= 0; dy--){
			targetTile = gameState.getTile(x, dy);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}

		// queen checks
		for (dx = x+1; dx < 8; dx++){
			targetTile = gameState.getTile(dx, y);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		for (dx = x-1; dx >= 0; dx--){
			targetTile = gameState.getTile(dx, y);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		for (dy = y+1; dy < 8; dy++){
			targetTile = gameState.getTile(x, dy);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		for (dy = y-1; dy >= 0; dy--){
			targetTile = gameState.getTile(x, dy);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		// bishop/queen checks
		int d;
		threat = (byte)(Tile.BISHOP|byColor);
		// northeast
		for (d = 1; x+d < 8 && y+d < 8; d++){
			targetTile = gameState.getTile(x+d, y+d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		// northwest
		for (d = 1; x-d >= 0 && y+d < 8; d++){
			targetTile = gameState.getTile(x-d, y+d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		// southeast
		for (d = 1; x+d < 8 && y-d >= 0; d++){
			targetTile = gameState.getTile(x+d, y-d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}
		// southwest
		for (d = 1; x-d >= 0 && y-d >= 0; d++){
			targetTile = gameState.getTile(x-d, y-d);
			if (targetTile == Tile.BLANK) continue;
			if (targetTile == threat) return threat;
			break;
		}


		return Tile.BLANK;
	}

	/**
	 * Adds all the semilegal captures in the gamestate associated with this {@code MoveHandler}
	 * @param moves list the available captures will be added to
	 */
	public void addCaptures(List<Move> moves){
		for (int originIndex = 0; originIndex < 64; originIndex++){
			byte piece = gameState.getTile(originIndex);
			if (piece == Tile.BLANK) continue;
			int pieceColor = piece & Tile.COLOR;
			if (pieceColor != gameState.player) continue;
			addCaptures(originIndex, moves);
		}
		legalizeMoves(moves);
	}
	/**
	 * Adds all the semilegal captures originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which captures originate from
	 * @param moves list the available captures will be added to
	 */
	public void addCaptures(int index, List<Move> moves){
		byte piece = gameState.getTile(index);

		int pieceType = piece & Tile.PIECE;
		int pieceColor = piece & Tile.COLOR;

		if (pieceType == Tile.BLANK) return;
		if (pieceColor != gameState.player) return;

		switch (pieceType){
			case Tile.PAWN -> addPawnCaptures(index, moves);
			case Tile.KNIGHT -> addKnightCaptures(index, moves);
			case Tile.BISHOP -> addBishopCaptures(index, moves);
			case Tile.ROOK -> addRookCaptures(index, moves);
			case Tile.QUEEN -> addQueenCaptures(index, moves);
			case Tile.KING -> addKingCaptures(index, moves);
		}
	}

	/**
	 * Adds all the semilegal captures a pawn could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which captures originate from
	 * @param moves list the avilable captures will be added to
	 */
	public void addPawnCaptures(int index, List<Move> moves){
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		if (y == 0 || y == 7) return;
		int dy = color == Tile.WHITE ? y + 1 : y - 1;
		// capture moves
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			byte captureTarget = gameState.getTile(dx, dy);
			if (captureTarget != Tile.BLANK && (captureTarget & Tile.COLOR) != color){
				if (dy == 0 || dy == 7){
					// promotion
					moves.add(new Move(x, y, dx, dy, Move.PROMOTION_KNIGHT));
					moves.add(new Move(x, y, dx, dy, Move.PROMOTION_ROOK));
					moves.add(new Move(x, y, dx, dy, Move.PROMOTION_BISHOP));
					moves.add(new Move(x, y, dx, dy, Move.PROMOTION_QUEEN));
				} else {
					moves.add(new Move(x, y, dx, dy));
				}
			} else if (gameState.enpassantIndex == dx+dy*8){
				// en passant capture
				moves.add(new Move(x, y, dx, dy, Move.ENPASSANT));
			}
		}
	}
	/**
	 * Adds all the semilegal captures a knight could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which captures originate from
	 * @param moves list the avilable captures will be added to
	 */
	public void addKnightCaptures(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		// vertical rectangle
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-2; dy <= y+2; dy += 4){
				if (dy < 0 || dy >= 8) continue;
				targetTile = gameState.getTile(dx, dy);
				if (targetTile != Tile.BLANK && (targetTile & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
		// horizontal rectangle
		for (int dx = x-2; dx <= x+2; dx += 4){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy += 2){
				if (dy < 0 || dy >= 8) continue;
				targetTile = gameState.getTile(dx, dy);
				if (targetTile != Tile.BLANK && (targetTile & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
	}
	/**
	 * Adds all the semilegal captures a bishop could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which captures originate from
	 * @param moves list the avilable captures will be added to
	 */
	public void addBishopCaptures(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		// northeast
		for (int d = 1; x+d < 8 && y+d < 8; d++){
			targetTile = gameState.getTile(x+d, y+d);
			if (targetTile == Tile.BLANK) continue;
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x+d, y+d));
			break;
		}
		// northwest
		for (int d = 1; x-d >= 0 && y+d < 8; d++){
			targetTile = gameState.getTile(x-d, y+d);
			if (targetTile == Tile.BLANK) continue;
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x-d, y+d));
			break;
		}
		// southeast
		for (int d = 1; x+d < 8 && y-d >= 0; d++){
			targetTile = gameState.getTile(x+d, y-d);
			if (targetTile == Tile.BLANK) continue;
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x+d, y-d));
			break;
		}
		// southwest
		for (int d = 1; x-d >= 0 && y-d >= 0; d++){
			targetTile = gameState.getTile(x-d, y-d);
			if (targetTile == Tile.BLANK) continue;
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x-d, y-d));
			break;
		}
	}
	/**
	 * Adds all the semilegal captures a rook could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which captures originate from
	 * @param moves list the avilable captures will be added to
	 */
	public void addRookCaptures(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		// east
		for (int dx = x+1; dx < 8; dx++){
			targetTile = gameState.getTile(dx, y);
			if (targetTile== Tile.BLANK) continue;
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, dx, y));
			break;
		}
		// west
		for (int dx = x-1; dx >= 0; dx--){
			targetTile = gameState.getTile(dx, y);
			if (targetTile== Tile.BLANK) continue;
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, dx, y));
			break;
		}
		// north
		for (int dy = y+1; dy < 8; dy++){
			targetTile = gameState.getTile(x, dy);
			if (targetTile== Tile.BLANK) continue;
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x, dy));
			break;
		}
		// south
		for (int dy = y-1; dy >= 0; dy--){
			targetTile = gameState.getTile(x, dy);
			if (targetTile== Tile.BLANK) continue;
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x, dy));
			break;
		}
	}
	/**
	 * Adds all the semilegal captures a queen could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which captures originate from
	 * @param moves list the avilable captures will be added to
	 */
	public void addQueenCaptures(int index, List<Move> moves){
		addBishopCaptures(index, moves);
		addRookCaptures(index, moves);
	}
	/**
	 * Adds all the semilegal captures a king could make originated from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which captures originate from
	 * @param moves list the avilable captures will be added to
	 */
	public void addKingCaptures(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		for (int dx = x-1; dx <= x+1; dx++){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy++){
				if (dx == x && dy == y) continue;
				if (dy < 0 || dy >= 8) continue;
				targetTile = gameState.getTile(dx, dy);
				if (targetTile != Tile.BLANK && (targetTile & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
	}

	/**
	 * Checks if any legal moves exist in the gamestate associated with this {@code MoveHandler}
	 * @return true if a legal move exists, false otherwise
	 */
	public boolean moveExists(){
		for (int originIndex = 0; originIndex < 64; originIndex++){
			byte piece = gameState.getTile(originIndex);
			if (piece == Tile.BLANK) continue;
			if ((piece & Tile.COLOR) != gameState.player) continue;
			if (moveExists(originIndex)) return true;
		}
		return false;
	}
	/**
	 * Checks if any legal moves originating from the designated index exist in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @return true if a legal move exists, false otherwise
	 */
	public boolean moveExists(int index){
		byte piece = gameState.getTile(index);

		int pieceType = piece & Tile.PIECE;
		int pieceColor = piece & Tile.COLOR;

		if (pieceType == Tile.BLANK) return false;
		if (pieceColor != gameState.player) return false;

		return switch (pieceType){
			case Tile.PAWN -> pawnMoveExists(index);
			case Tile.KNIGHT -> knightMoveExists(index);
			case Tile.BISHOP -> bishopMoveExists(index);
			case Tile.ROOK -> rookMoveExists(index);
			case Tile.QUEEN -> queenMoveExists(index);
			case Tile.KING -> kingMoveExists(index);
			default -> false;
		};
	}

	/**
	 * Checks if any legal moves a pawn could make originating from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @return true if a legal move exists, false otherwise
	 */
	public boolean pawnMoveExists(int index){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		byte opColor = (byte) (color ^ Tile.COLOR);
		int x = index & 0b111;
		int y = index >> 3;
		if (y == 0 || y == 7) return false;
		int dy;
		int dy2;
		boolean doubleMove = false;
		if (color == Tile.WHITE){
			dy = y+1;
			dy2 = y+2;
			if (y == 1) doubleMove = true;
		} else {
			dy = y-1;
			dy2 = y-2;
			if (y == 6) doubleMove = true;
		}
		targetTile = gameState.getTile(x, dy);
		// single move
		if (targetTile == Tile.BLANK){
			if (canMoveTo(x, y, x, dy, opColor)) return true;

			// double move
			if (doubleMove && gameState.getTile(x, dy2) == Tile.BLANK && canMoveTo(x, y, x, dy2, opColor)) return true;

		}
		// capture moves
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			if (canMoveTo(x, y, dx, dy, opColor)) return true;
			if (gameState.enpassantIndex == dx+dy*8){
				// en passant capture
				if (isLegal(new Move(x, y, dx, dy, Move.ENPASSANT))) return true;
			}
		}
		return false;
	}
	/**
	 * Checks if any legal moves a knight could make originating from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @return true if a legal move exists, false otherwise
	 */
	public boolean knightMoveExists(int index){
		byte color = (byte) (gameState.getTile(index) & Tile.COLOR);
		byte opColor = (byte) (color ^ Tile.COLOR);
		int x = index & 0b111;
		int y = index >> 3;

		// knight checks
		int dx, dy;
		dx = x-1; dy = y-2;
		if (dy >= 0){
			if (dx >= 0 && canMoveTo(x, y, dx, dy, opColor)) return true;
			dx = x+1;
			if (dx < 8 && canMoveTo(x, y, dx, dy, opColor)) return true;
		} else {
			dx = x+1;
		}
		dy = y+2;
		if (dy < 8){
			if (dx < 8 && canMoveTo(x, y, dx, dy, opColor)) return true;
			dx = x-1;
			if (dx >= 0 && canMoveTo(x, y, dx, dy, opColor)) return true;
		}

		dx = x-2;
		dy = y-1;
		if (dy >= 0){
			if (dx >= 0 && canMoveTo(x, y, dx, dy, opColor)) return true;
			dx = x+2;
			if (dx < 8 && canMoveTo(x, y, dx, dy, opColor)) return true;
		} else {
			dx = x+2;
		}
		dy = y+1;
		if (dy < 8){
			if (dx < 8 && canMoveTo(x, y, dx, dy, opColor)) return true;
			dx = x-2;
			if (dx >= 0 && canMoveTo(x, y, dx, dy, opColor)) return true;
		}
		return false;
	}
	/**
	 * Checks if any legal moves a bishop could make originating from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @return true if a legal move exists, false otherwise
	 */
	public boolean bishopMoveExists(int index){
		int color = gameState.getTile(index) & Tile.COLOR;
		byte opColor = (byte) (color ^ Tile.COLOR);
		int x = index & 0b111;
		int y = index >> 3;
		// northeast
		for (int d = 1; x+d < 8 && y+d < 8; d++){
			if (canMoveTo(x, y, x+d, y+d, opColor)) return true;
			if (gameState.getTile(x+d, y+d) != Tile.BLANK) break;
		}
		// northwest
		for (int d = 1; x-d >= 0 && y+d < 8; d++){
			if (canMoveTo(x, y, x-d, y+d, opColor)) return true;
			if (gameState.getTile(x-d, y+d) != Tile.BLANK) break;
		}
		// southeast
		for (int d = 1; x+d < 8 && y-d >= 0; d++){
			if (canMoveTo(x, y, x+d, y-d, opColor)) return true;
			if (gameState.getTile(x+d, y-d) != Tile.BLANK) break;
		}
		// southwest
		for (int d = 1; x-d >= 0 && y-d >= 0; d++){
			if (canMoveTo(x, y, x-d, y-d, opColor)) return true;
			if (gameState.getTile(x-d, y-d) != Tile.BLANK) break;
		}
		return false;
	}
	/**
	 * Checks if any legal moves a rook could make originating from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @return true if a legal move exists, false otherwise
	 */
	public boolean rookMoveExists(int index){
		int color = gameState.getTile(index) & Tile.COLOR;
		byte opColor = (byte) (color ^ Tile.COLOR);
		int x = index & 0b111;
		int y = index >> 3;
		// east
		for (int dx = x+1; dx < 8; dx++){
			if (canMoveTo(x, y, dx, y, opColor)) return true;
			if (gameState.getTile(dx, y) != Tile.BLANK) break;
		}
		// west
		for (int dx = x-1; dx >= 0; dx--){
			if (canMoveTo(x, y, dx, y, opColor)) return true;
			if (gameState.getTile(dx, y) != Tile.BLANK) break;
		}
		// north
		for (int dy = y+1; dy < 8; dy++){
			if (canMoveTo(x, y, x, dy, opColor)) return true;
			if (gameState.getTile(x, dy) != Tile.BLANK) break;
		}
		// south
		for (int dy = y-1; dy >= 0; dy--){
			if (canMoveTo(x, y, x, dy, opColor)) return true;
			if (gameState.getTile(x, dy) != Tile.BLANK) break;
		}
		return false;
	}
	/**
	 * Checks if any legal moves a queen could make originating from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @return true if a legal move exists, false otherwise
	 */
	public boolean queenMoveExists(int index){
		if (rookMoveExists(index)) return true;
		return bishopMoveExists(index);
	}
	/**
	 * Checks if any legal moves a king could make originating from the designated index in the gamestate associated with this {@code MoveHandler}
	 * @param index position which moves originate from
	 * @return true if a legal move exists, false otherwise
	 */
	public boolean kingMoveExists(int index){
		byte color = (byte) (gameState.getTile(index) & Tile.COLOR);
		byte opColor = (byte) (color ^ Tile.COLOR);
		int x = index & 0b111;
		int y = index >> 3;

		int dx, dy;

		dy = y - 1;
		if (dy >= 0){
			dx = x - 1;
			if (dx >= 0 && canMoveTo(x, y, dx, dy, opColor)) return true;
			dx = x;
			if (canMoveTo(x, y, dx, dy, opColor)) return true;
			dx = x + 1;
			if (dx < 8 && canMoveTo(x, y, dx, dy, opColor)) return true;
		} else {
			dx = x + 1;
		}

		dy = y;
		// don't check the center square
		if (dx < 8 && canMoveTo(x, y, dx, dy, opColor)) return true;
		dx = x - 1;
		if (dx >= 0 && canMoveTo(x, y, dx, dy, opColor)) return true;

		dy = y+1;
		if (dy < 8){
			if (dx >= 0 && dy < 8 && canMoveTo(x, y, dx, dy, opColor)) return true;
			dx = x;
			if (canMoveTo(x, y, dx, dy, opColor)) return true;
			dx = x + 1;
			if (dx < 8 && dy < 8 && canMoveTo(x, y, dx, dy, opColor)) return true;
		}
		// castling
		if (color == Tile.WHITE){
			if (!isAttacked(4, Tile.BLACK)){
				if ((gameState.castlingRights & 0b0010) > 0
					&& gameState.getTile(1) == Tile.BLANK
					&& gameState.getTile(2) == Tile.BLANK
					&& gameState.getTile(3) == Tile.BLANK
					&& !isAttacked(3, Tile.BLACK)
					&& !isAttacked(2, Tile.BLACK)){
					return true;
				}
				if ((gameState.castlingRights & 0b0001) > 0
					&& gameState.getTile(5) == Tile.BLANK
					&& gameState.getTile(6) == Tile.BLANK
					&& !isAttacked(5, Tile.BLACK)
					&& !isAttacked(6, Tile.BLACK)){
					return true;
				}
			}
		} else {
			if (!isAttacked(60, Tile.WHITE)){
				if ((gameState.castlingRights & 0b1000) > 0
					&& gameState.getTile(57) == Tile.BLANK
					&& gameState.getTile(58) == Tile.BLANK
					&& gameState.getTile(59) == Tile.BLANK
					&& !isAttacked(58, Tile.WHITE)
					&& !isAttacked(59, Tile.WHITE)){
					return true;
				}
				if ((gameState.castlingRights & 0b0100) > 0
					&& gameState.getTile(61) == Tile.BLANK
					&& gameState.getTile(62) == Tile.BLANK
					&& !isAttacked(61, Tile.WHITE)
					&& !isAttacked(62, Tile.WHITE)){
					return true;
				}
			}
		}


		return false;
	}

	/**
	 * Checks whether a move can be made from (x1, y1) to (x2, y2) in the gamestate associated with this {@code MoveHandler}. Doesn't support en passant or castling
	 * @param x1 origin x
	 * @param y1 origin y
	 * @param x2 destination x
	 * @param y2 destination y
	 * @param opColor opponents color
	 * @return true if a move can be made from (x1, y1) to (x2, y2), false otherwise
	 */
	private boolean canMoveTo(int x1, int y1, int x2, int y2, byte opColor){
		//CANNOT BE USED FOR CASTLING, EN PASSANT
		byte target = gameState.getTile(x2, y2);
		// semilegal check
		if (target != Tile.BLANK && (target & Tile.COLOR) != opColor) return false;

		byte origin = gameState.getTile(x1, y1);

		gameState.setTile(x2, y2, origin);
		gameState.setTile(x1, y1, Tile.BLANK);

		int tkingIndex = ((origin & Tile.PIECE) == Tile.KING) ? ((y2<<3)|x2) : ((gameState.player == Tile.WHITE) ? gameState.whiteKingIndex : gameState.blackKingIndex);
		boolean legal = !isAttacked(tkingIndex, gameState.player ^ Tile.COLOR);

		gameState.setTile(x1, y1, origin);
		gameState.setTile(x2, y2, target);
		return legal;
	}
}