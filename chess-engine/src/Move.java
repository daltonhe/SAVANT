/**
 * 
 * @author Dalton He
 * created 10-08-18
 */
public class Move implements Definitions, Comparable<Move> {
	public int start;
	public int target;
	public int piece;
	public int captured;
	public String prevCastling;
	public int prevEnpassant;
	public int prevFiftyMoves;
	public int type;
	public int priority = 0;
	public int history = 0;
	public String modifier = "";
	
	/**
	 * Creates a move with the given parameters.
	 * @param start - Starting index of the moving piece
	 * @param target - Index to which the piece is moving
	 * @param piece - Type of the moving piece
	 * @param captured - Type of the captured piece (0 if none)
	 * @param prevCastling - Castling rights before this move (needed to unmake move)
	 * @param prevEnpassant - Enpassant square before this move
	 * @param prevFiftyMoves - Fifty move count before this move
	 * @param type - Move type (see Definitions interface)
	 */
	public Move(int start, int target, int piece, int captured, String prevCastling, 
			int prevEnpassant, int prevFiftyMoves, int type) {
		this.start = start;
		this.target = target;
		this.piece = piece;
		this.captured = captured;
		this.prevCastling = prevCastling;
		this.prevEnpassant = prevEnpassant;
		this.prevFiftyMoves = prevFiftyMoves;
		this.type = type;
	}
	
	/**
	 * Returns the algebraic form notation of the move.
	 */
	public String toString() {
		String result = "";
		if (type == CASTLE_SHORT)
			return "0-0";
			
		if (type == CASTLE_LONG)
			return "0-0-0";
		
		if (type == PROMOTION || Math.abs(piece) == PAWN) {
			if (captured != 0)
				result += "abcdefgh".charAt(start % 16);
		}
		else
			result += PIECE_STR.charAt(Math.abs(piece) + 6);
		
		result += modifier;
		
		if (captured != 0)
			result += "x";
		
		result += Board.indexToAlgebraic(target);
		
		if (type == PROMOTION)
			result += "=Q";
		else if (type == ENPASSANT)
			result += " e.p.";

		return result;
	}
	
	/**
	 * 
	 * @return
	 */
	public String longNotation() {
		return Board.indexToAlgebraic(start) + Board.indexToAlgebraic(target);
	}

	public int compareTo(Move other) {
		if (other.priority != this.priority)
			return other.priority - this.priority;

		if (other.history != this.history)
			return other.history - this.history;
		
		return Math.abs(this.piece) - Math.abs(other.piece);
	}
	
	
	public boolean equals(Move other) {
		return (other.start == this.start && other.target == this.target);
	}
}
