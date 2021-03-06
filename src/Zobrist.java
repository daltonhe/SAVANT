import java.util.Random;

/**
 * @author Dalton He
 * created 10-30-2018
 */
public class Zobrist implements Types {
    public static long[][] pieces;
    public static long side;
    public static long[] castling;
    public static long[] enpassant;
    // precomputed keys for castling
    public static long w_short_castle;
    public static long w_long_castle;
    public static long b_short_castle;
    public static long b_long_castle;
    public static long[][][] moves;

    /**
     * Initializes the random number tables used for zobrist key generation.
     */
    public static void initialize() {
        Random r  = new Random();
        pieces    = new long[13][120];      // pieces[pieceType][index]
        castling  = new long[16];           // castling[0bKQkq]
        enpassant = new long[8];            // enpassant[file]
        moves     = new long[13][120][120]; // moves[pieceType][startIndex][targetIndex]

        for (int i = 0; i < 13; i++)
            for (int j = 0; j < 120; j++)
                pieces[i][j] = Math.abs(r.nextLong());

        side = Math.abs(r.nextLong());

        for (int i = 0; i < 16; i++)
            castling[i] = Math.abs(r.nextLong());

        for (int i = 0; i < 8; i++)
            enpassant[i] = Math.abs(r.nextLong());
        
        for (int i = 0; i < 13; i++)
            for (int j = 0; j < 120; j++)
                for (int k = 0; k < 120; k++)
                    moves[i][j][k] = pieces[i][j] ^ pieces[i][k];

        w_short_castle = pieces[W_KING + 6][SQ_e1] ^ pieces[W_KING + 6][SQ_g1] ^
                         pieces[W_ROOK + 6][SQ_f1] ^ pieces[W_ROOK + 6][SQ_h1];
        w_long_castle  = pieces[W_KING + 6][SQ_e1] ^ pieces[W_KING + 6][SQ_c1] ^
                         pieces[W_ROOK + 6][SQ_a1] ^ pieces[W_ROOK + 6][SQ_d1];
        b_short_castle = pieces[B_KING + 6][SQ_e8] ^ pieces[B_KING + 6][SQ_g8] ^
                         pieces[B_ROOK + 6][SQ_f8] ^ pieces[B_ROOK + 6][SQ_h8];
        b_long_castle  = pieces[B_KING + 6][SQ_e8] ^ pieces[B_KING + 6][SQ_c8] ^
                         pieces[B_ROOK + 6][SQ_a8] ^ pieces[B_ROOK + 6][SQ_d8];
    }

    /**
     * Returns the zobrist key of the position. This method is, in practice, used only for
     * initialization, since zobrist keys will be incrementally updated in makeMove().
     */
    public static long getKey(Position pos) {
        long key = 0;
        for (int index : pos.pieces)
            key ^= pieces[pos.board[index] + 6][index];

        if (pos.toMove == BLACK) key ^= side;
        key ^= castling[pos.castling];
        if (pos.enpassant != SQ_NONE) key ^= enpassant[pos.enpassant % 16];

        return key;
    }
}
