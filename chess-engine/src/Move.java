/**
 * Represents a chess move.
 * @author Dalton
 * Created 6-11-18
 */
public class Move implements Definitions, Comparable<Move> {

	public int moveType;
	public int movingPiece;
	public int capturedPiece;
	public int startIndex;
	public int endIndex;
	public byte prevCastlingRights;
	public int prevEnpassantSquare;
	public int prevFiftyMoveCount;
	public int sortPriority = 0;

	/**
	 * Creates a move.
	 * @param moveType type of move (e.g. castling, enpassant, promotion)
	 * @param movingPiece moving piece type
	 * @param capturedPiece captured piece (0 if none)
	 * @param startIndex index of starting square
	 * @param endIndex index of ending square
	 */
	public Move(int moveType, int movingPiece, int capturedPiece, int startIndex,
			int endIndex, byte prevCastlingRights, int prevEnpassantSquare,
			int prevFiftyMoveCount) {
		this.moveType = moveType;
		this.movingPiece = movingPiece;
		this.capturedPiece = capturedPiece;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.prevCastlingRights = prevCastlingRights;
		this.prevEnpassantSquare = prevEnpassantSquare;
		this.prevFiftyMoveCount = prevFiftyMoveCount;
		this.sortPriority = Math.abs(capturedPiece) * 10 - Math.abs(movingPiece);
	}

	/**
	 * Returns the short form algebraic notation of this move.
	 */
	@Override
	public String toString() {
		if (moveType == CASTLE_SHORT) {
			return "0-0";
		}
		if (moveType == CASTLE_LONG) {
			return "0-0-0";
		}
		String result = "";
		int piece = Math.abs(movingPiece);
		if (piece != PAWN) {
			result += Position.toLetter(piece);
		}
		if (capturedPiece != EMPTY) {
			if (piece == PAWN) {
				result += Position.getFile(startIndex);
			}
			result += "x";
		}
		result += Position.toSquare(endIndex);
		switch (moveType) {
		case ENPASSANT:
			result += " e.p.";
			break;
		case PROMOTION_QUEEN:
			result += "=Q";
			break;
		case PROMOTION_ROOK:
			result += "=R";
			break;
		case PROMOTION_BISHOP:
			result += "=B";
			break;
		case PROMOTION_KNIGHT:
			result += "=K";
			break;
		}
		return result;
	}

	@Override
	public int compareTo(Move move) {
    return (move.sortPriority - this.sortPriority);
	}

}