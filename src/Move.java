import java.util.List;

/**
 * 
 * @author Dalton He
 * created 10-08-18
 * 
 */
public class Move implements Types, Comparable<Move> {
    public int start;    // moving piece starting index
    public int target;   // moving piece target index
    public int piece;    // moving piece type
    public int captured; // captured piece type
    public int type;     // move type
    public int score;    // move weight, for move ordering
    public int hscore;   // history weight, for move ordering

    /**
     * Creates a move with the given parameters.
     */
    public Move(int start, int target, int piece, int captured, int type) {
        this.start    = start;
        this.target   = target;
        this.piece    = piece;
        this.captured = captured;
        this.type     = type;
    }
    
    /**
     * Returns {@code true} if the given move is the same as this move.
     */
    public boolean equals(Move other) {
        return (piece == other.piece && start == other.start && target == other.target);
    }
    
    /**
     * Returns a positive integer if the given move has higher sort priority, and a negative
     * integer if it has lower sort priority.
     */
    public int compareTo(Move other) {
        if (score  != other.score)  return other.score  - score;
        if (hscore != other.hscore) return other.hscore - hscore;
        return Math.abs(piece) - Math.abs(other.piece);
    }

    /**
     * Returns the move in a compact integer form. The rightmost 8 bits store the target index,
     * the next 8 bits store the start index. Note: promotions to all piece types on the same 
     * square have the same integer value.
     */
    public int toInt() {
        return ((start << 8) | target);
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
        else result += PIECE_STR.charAt(Math.abs(piece) + 6);

        if (captured != 0) result += "x";
        result += Position.indexToAlg(target);

        if (type == PROMOTION) result += "=" + "NBRQ".charAt(Math.abs(piece) - 2);
        if (type == ENPASSANT) result += " e.p.";

        return result;
    }
    
    /**
     * Returns the short form algebraic notation of the move with modifier added for ambiguous
     * moves (e.g. Nbd7, R1a2). The list of legal moves is needed as input.
     */
    public String shortNot(List<Move> moveList) {
        String modifier = "";
        if (Math.abs(piece) == PAWN || Math.abs(piece) == KING) return toString();
        for (Move move : moveList) {
            if (move.piece == piece && move.start != start && move.target == target) {
                if ((move.start & 7) != (start & 7)) modifier = "" + "abcdefgh".charAt(start & 7);
                else                                 modifier = "" + (8 - (start >> 4));
                break;
            }
        }
        return toString().charAt(0) + modifier + toString().substring(1); 
    }

    /**
     * Returns the long form algebraic notation of the move (e.g. e2e4, a7a8q).
     */
    public String longNot() {
        String result = Position.indexToAlg(start) + Position.indexToAlg(target);   
        if (type == PROMOTION) result += "nbrq".charAt(Math.abs(piece) - 2);    
        return result;
    }
}
