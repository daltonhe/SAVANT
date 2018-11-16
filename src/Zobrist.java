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
	
	/**
	 * Initializes the random number tables used for zobrist key generation.
	 */
	public static void initialize() {
		Random r  = new Random();
		pieces    = new long[13][120]; // pieces[pieceType][index]
		castling  = new long[16];      // castling[0bKQkq]
		enpassant = new long[8];       // enpassant[file]
		
		for (int i = 0; i < 13; i++)
			for (int j = 0; j < 120; j++)
				pieces[i][j] = Math.abs(r.nextLong());

		side = Math.abs(r.nextLong());
		
		for (int i = 0; i < 16; i++)
			castling[i] = Math.abs(r.nextLong());
		
		for (int i = 0; i < 8; i++)
			enpassant[i] = Math.abs(r.nextLong());
	}
	
	/**
	 * Returns the zobrist key of the position.
	 */
	public static long getKey(Position pos) {
		long key = 0;
		for(int index = SQ_a8; index <= SQ_h1; index++) {
			if ((index & 0x88) != 0) continue;
			if (pos.board[index] != 0)
				key ^= pieces[pos.board[index] + 6][index];
		}
		if (pos.sideToMove == BLACK) key ^= side;
		key ^= castling[pos.castling];
		if (pos.enpassant != SQ_NONE) key ^= enpassant[pos.enpassant % 16];

		return key;
	}
}
