
import java.util.List;

public class MoveHandler {
	public final ReversibleGameState gameState;
	public MoveHandler(ReversibleGameState gameState){
		this.gameState = gameState;
	}
	public void addLegalMoves(List<Move> moves){
		for (int originIndex = 0; originIndex < 64; originIndex++){
			byte piece = gameState.getTile(originIndex);
			if (piece == Tile.BLANK) continue;
			int pieceColor = piece & Tile.COLOR;
			if (pieceColor != gameState.player) continue;
			addLegalMoves(originIndex, moves);
		}
		legalizeMoves(moves);
	}
	public void addLegalMoves(int originIndex, List<Move> moves){
		byte piece = gameState.getTile(originIndex);

		int pieceType = piece & Tile.PIECE;
		int pieceColor = piece & Tile.COLOR;

		if (pieceType == Tile.BLANK) return;
		if (pieceColor != gameState.player) return;

		switch (pieceType){
			case Tile.PAWN -> addLegalPawnMoves(originIndex, moves);
			case Tile.KNIGHT -> addLegalKnightMoves(originIndex, moves);
			case Tile.BISHOP -> addLegalBishopMoves(originIndex, moves);
			case Tile.ROOK -> addLegalRookMoves(originIndex, moves);
			case Tile.QUEEN -> addLegalQueenMoves(originIndex, moves);
			case Tile.KING -> addLegalKingMoves(originIndex, moves);
		}

	}
	public void legalizeMoves(List<Move> moves){
		int kingIndex = (gameState.player == Tile.WHITE) ? gameState.whiteKingIndex : gameState.blackKingIndex;

		for (int i = 0; i < moves.size(); i++){
			ReversibleMove move = new ReversibleMove(moves.get(i), gameState);

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
				moves.remove(i);
				i--;
			}

			gameState.setTile(move.getOriginIndex(), origin);
			if (move.getFlag() == Move.ENPASSANT){
				int d = 8 - 2 * gameState.player; // works because white = 8, black = 0
				gameState.setTile(move.getTargetIndex() + d, Tile.PAWN|(gameState.player^Tile.COLOR));
			}
			gameState.setTile(move.getTargetIndex(), target);
		}
	}

	private void addLegalPawnMoves(int index, List<Move> moves){
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
		if ((targetTile & Tile.PIECE) == Tile.BLANK){
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
				if ((targetTile & Tile.PIECE) == Tile.BLANK){
					moves.add(new Move(x, y, x, dy2, Move.PAWN_DOUBLE));
				}
			}
		}
		// capture moves
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			byte captureTarget = gameState.getTile(dx, dy);
			if ((captureTarget & Tile.PIECE) != Tile.BLANK && (captureTarget & Tile.COLOR) != color){
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
	private void addLegalKnightMoves(int index, List<Move> moves){
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
				if ((targetTile & Tile.PIECE) == Tile.BLANK || (targetTile & Tile.COLOR) != color){
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
				if ((targetTile & Tile.PIECE) == Tile.BLANK || (targetTile & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
	}
	private void addLegalBishopMoves(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		// northeast
		for (int d = 1; x+d < 8 && y+d < 8; d++){
			targetTile = gameState.getTile(x+d, y+d);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, x+d, y+d));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x+d, y+d));
			break;
		}
		// northwest
		for (int d = 1; x-d >= 0 && y+d < 8; d++){
			targetTile = gameState.getTile(x-d, y+d);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, x-d, y+d));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x-d, y+d));
			break;
		}
		// southeast
		for (int d = 1; x+d < 8 && y-d >= 0; d++){
			targetTile = gameState.getTile(x+d, y-d);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, x+d, y-d));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x+d, y-d));
			break;
		}
		// southwest
		for (int d = 1; x-d >= 0 && y-d >= 0; d++){
			targetTile = gameState.getTile(x-d, y-d);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, x-d, y-d));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x-d, y-d));
			break;
		}
	}
	private void addLegalRookMoves(int index, List<Move> moves){
		byte targetTile;
		int color = gameState.getTile(index) & Tile.COLOR;
		int x = index & 0b111;
		int y = index >> 3;
		// east
		for (int dx = x+1; dx < 8; dx++){
			targetTile = gameState.getTile(dx, y);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, dx, y));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, dx, y));
			break;
		}
		// west
		for (int dx = x-1; dx >= 0; dx--){
			targetTile = gameState.getTile(dx, y);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, dx, y));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, dx, y));
			break;
		}
		// north
		for (int dy = y+1; dy < 8; dy++){
			targetTile = gameState.getTile(x, dy);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, x, dy));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x, dy));
			break;
		}
		// south
		for (int dy = y-1; dy >= 0; dy--){
			targetTile = gameState.getTile(x, dy);
			if ((targetTile & Tile.PIECE) == Tile.BLANK){
				moves.add(new Move(x, y, x, dy));
				continue;
			}
			if ((targetTile & Tile.COLOR) != color) moves.add(new Move(x, y, x, dy));
			break;
		}

	}
	private void addLegalQueenMoves(int index, List<Move> moves){
		addLegalRookMoves(index, moves);
		addLegalBishopMoves(index, moves);
	}
	private void addLegalKingMoves(int index, List<Move> moves){
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
				if ((targetTile & Tile.PIECE) == Tile.BLANK || (targetTile & Tile.COLOR) != color){
					moves.add(new Move(x, y, dx, dy));
				}
			}
		}
		// castling
		if (color == Tile.WHITE){
			if (!isAttacked(4, Tile.BLACK)){
				if ((gameState.castlingRights & 0b0010) > 0 && Tile.piece(gameState.getTile(1)) == Tile.BLANK && Tile.piece(gameState.getTile(2)) == Tile.BLANK && Tile.piece(gameState.getTile(3)) == Tile.BLANK && !isAttacked(3, Tile.BLACK)){
					moves.add(new Move(4, 0, 2, 0, Move.CASTLING));
				}
				if ((gameState.castlingRights & 0b0001) > 0 && Tile.piece(gameState.getTile(5)) == Tile.BLANK && Tile.piece(gameState.getTile(6)) == Tile.BLANK && !isAttacked(5, Tile.BLACK)){
					moves.add(new Move(4, 0, 6, 0, Move.CASTLING));
				}
			}
		} else {
			if (!isAttacked(60, Tile.WHITE)){
				if ((gameState.castlingRights & 0b1000) > 0 && Tile.piece(gameState.getTile(57)) == Tile.BLANK && Tile.piece(gameState.getTile(58)) == Tile.BLANK && Tile.piece(gameState.getTile(59)) == Tile.BLANK && !isAttacked(59, Tile.WHITE)){
					moves.add(new Move(4, 7, 2, 7, Move.CASTLING));
				}
				if ((gameState.castlingRights & 0b0100) > 0 && Tile.piece(gameState.getTile(61)) == Tile.BLANK && Tile.piece(gameState.getTile(62)) == Tile.BLANK && !isAttacked(61, Tile.WHITE)){
					moves.add(new Move(4, 7, 6, 7, Move.CASTLING));
				}
			}
		}
	}
	public boolean isAttacked(int index, int byColor){
		// only intended to check for king, ignore en passant captures
		byte targetTile;
		int x = index & 0b111;
		int y = index >> 3;
		// rook/queen checks
		byte threat1 = (byte)(Tile.ROOK|byColor);
		byte threat2 = (byte)(Tile.QUEEN|byColor);
		for (int dx = x+1; dx < 8; dx++){
			targetTile = gameState.getTile(dx, y);
			if ((targetTile & Tile.PIECE) == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		for (int dx = x-1; dx >= 0; dx--){
			targetTile = gameState.getTile(dx, y);
			if ((targetTile & Tile.PIECE) == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		for (int dy = y+1; dy < 8; dy++){
			targetTile = gameState.getTile(x, dy);
			if ((targetTile & Tile.PIECE) == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		for (int dy = y-1; dy >= 0; dy--){
			targetTile = gameState.getTile(x, dy);
			if ((targetTile & Tile.PIECE) == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// bishop/queen checks
		threat1 = (byte)(Tile.BISHOP|byColor);
		// northeast
		for (int d = 1; x+d < 8 && y+d < 8; d++){
			targetTile = gameState.getTile(x+d, y+d);
			if ((targetTile & Tile.PIECE) == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// northwest
		for (int d = 1; x-d >= 0 && y+d < 8; d++){
			targetTile = gameState.getTile(x-d, y+d);
			if ((targetTile & Tile.PIECE) == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// southeast
		for (int d = 1; x+d < 8 && y-d >= 0; d++){
			targetTile = gameState.getTile(x+d, y-d);
			if ((targetTile & Tile.PIECE) == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// southwest
		for (int d = 1; x-d >= 0 && y-d >= 0; d++){
			targetTile = gameState.getTile(x-d, y-d);
			if ((targetTile & Tile.PIECE) == Tile.BLANK) continue;
			if (targetTile == threat1 || targetTile == threat2) return true;
			break;
		}
		// knight checks TODO replace with unrolled loops
		threat1 = (byte)(Tile.KNIGHT|byColor);
		for (int dx = x-1; dx <= x+1; dx += 2){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-2; dy <= y+2; dy += 4){
				if (dy < 0 || dy >= 8) continue;
				if (gameState.getTile(dx, dy) == threat1) return true;
			}
		}
		for (int dx = x-2; dx <= x+2; dx += 4){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy += 2){
				if (dy < 0 || dy >= 8) continue;
				if (gameState.getTile(dx, dy) == threat1) return true;
			}
		}
		// king check TODO replace with unrolled loops
		threat1 = (byte)(Tile.KING|byColor);
		for (int dx = x-1; dx <= x+1; dx++){
			if (dx < 0 || dx >= 8) continue;
			for (int dy = y-1; dy <= y+1; dy++){
				if (dx == x && dy == y) continue;
				if (dy < 0 || dy >= 8) continue;
				targetTile = gameState.getTile(dx, dy);
				if (targetTile == threat1) return true;
			}
		}
		// pawn check
		int dy = 1 + y - byColor/4; // subtract 1 if white, add 1 if black
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
}
