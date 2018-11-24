/**
 * 
 * @author Dalton He
 * created 10-08-18
 * 
 */
public class Move implements Types, Comparable<Move> {
	public int start;           // starting index of the moving piece
	public int target;          // target index of the moving piece
	public int piece;           // piece type of the moving piece
	public int captured;        // piece type of the captured piece (0 if none)
	public int prevCastling;    // castling rights prior to the move being made
	public int prevEnpassant;   // enpassant square index prior to the move being made
	public int prevFiftyMoves;  // fifty moves count prior to the move being made
	public int type;            // type of the move (see Definitions interface)
	public int priority;        // priority of the move, used for move ordering
	public int historyScore;    // history heuristic weight, used for move ordering
	public String modifier;     // algebraic notation modifier

	/**
	 * Creates a move with the given parameters.
	 */
	public Move(int start, int target, int piece, int captured, int prevCastling,
				int prevEnpassant, int prevFiftyMoves, int type) {
		this.start          = start;
		this.target         = target;
		this.piece          = piece;
		this.captured       = captured;
		this.prevCastling   = prevCastling;
		this.prevEnpassant  = prevEnpassant;
		this.prevFiftyMoves = prevFiftyMoves;
		this.type           = type;
		this.priority       = 0;
		this.historyScore   = 0;
		this.modifier       = "";
	}
	
	/**
	 * Returns the short form algebraic notation of the move (e.g. e4, 0-0, Bxf7).
	 */
	public String toString() {
		if (type == CASTLE_SHORT) return "0-0";
		if (type == CASTLE_LONG)  return "0-0-0";
		
		String result = "";
		
		if (type == PROMOTION || Math.abs(piece) == PAWN) {
			if (captured != 0) result += "abcdefgh".charAt(start % 16);
		}
		else
			result += PIECE_STR.charAt(Math.abs(piece) + 6);
		
		result += modifier;
		if (captured != 0) result += "x";
		result += Position.indexToAlg(target);
		
		if (type == PROMOTION) result += "=" + "NBRQ".charAt(Math.abs(piece) - 2);
		if (type == ENPASSANT) result += " e.p.";

		return result;
	}
	
	/**
	 * Returns the long form algebraic notation of the move (e.g. e2e4, a7a8q).
	 */
	public String longNotation() {
		String result = Position.indexToAlg(start) + Position.indexToAlg(target);	
		if (type == PROMOTION) result += "nbrq".charAt(Math.abs(piece) - 2);	
		return result;
	}
	
	/**
	 * Returns a positive integer if the given move has higher sort priority, and a negative
	 * integer if it has lower sort priority.
	 */
	public int compareTo(Move other) {
		if (other.priority != this.priority)
			return other.priority - this.priority;

		if (other.historyScore != this.historyScore)
			return other.historyScore - this.historyScore;
		
		return Math.abs(this.piece) - Math.abs(other.piece);
	}

	/**
	 * Returns true if the given move is the same as this move.
	 */
	public boolean equals(Move other) {
		return (other.start == this.start && other.target == this.target);
	}
}
