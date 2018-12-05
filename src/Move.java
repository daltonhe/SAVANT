import java.util.ArrayList;

/**
 * 
 * @author Dalton He
 * created 10-08-18
 * 
 */
public class Move implements Types, Comparable<Move> {
    public int start;    // starting index of the moving piece
    public int target;   // target index of the moving piece
    public int piece;    // piece type of the moving piece
    public int captured; // piece type of the captured piece (0 if none)
    public int type;     // type of the move (see Definitions interface)
    public int score;    // weight of the move during move ordering
    public int hscore;   // history weight, also for move ordering

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
     * the next 8 bits store the start index. Note: all promotion types on the same square will
     * return the same integer value.
     */
    public int toInteger() {
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
     * Returns the long form algebraic notation of the move (e.g. e2e4, a7a8q).
     */
    public String longNot() {
        String result = Position.indexToAlg(start) + Position.indexToAlg(target);   
        if (type == PROMOTION) result += "nbrq".charAt(Math.abs(piece) - 2);    
        return result;
    }
    
    /**
     * Returns the modifier to the algebraic notation of the given move (e.g. Nbd7, R1a2).
     * Returns an empty string if no modifier is needed.
     */
    public static String algebraicModifier(Move move, ArrayList<Move> moveList) {
        int piece = Math.abs(move.piece);
        if (piece == PAWN || piece == BISHOP || piece == KING) return "";
        for (Move m : moveList) {
            if (   m.piece  == move.piece 
                && m.target == move.target 
                && m.start  != move.start) {
                if (m.start % 16 != move.start % 16)
                    return "" + "abcdefgh".charAt(move.start % 16);
                else
                    return "" + (8 - move.start / 16);
            }
        }
        return "";
    }
}
