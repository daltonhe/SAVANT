import java.util.Random;

/**
 * 
 * @author Dalton He
 * created 10-30-2018
 */
public class Zobrist implements Definitions {
	public static long[][] PIECES = new long[13][120];
	public static long SIDE;
	public static long W_SHORT_CASTLE;
	public static long W_LONG_CASTLE;
	public static long B_SHORT_CASTLE;
	public static long B_LONG_CASTLE;
	public static long[] ENPASSANT = new long[8];
	
	/**
	 * 
	 */
	public static void initialize() {
		Random r = new Random();
		for (int i = 0; i < 13; i++)
			for (int j = 0; j < 120; j++)
				PIECES[i][j] = Math.abs(r.nextLong());

		SIDE = Math.abs(r.nextLong());
		W_SHORT_CASTLE = Math.abs(r.nextLong());
		W_LONG_CASTLE = Math.abs(r.nextLong());
		B_SHORT_CASTLE = Math.abs(r.nextLong());
		B_LONG_CASTLE = Math.abs(r.nextLong());
		
		for (int i = 0; i < 8; i++)
			ENPASSANT[i] = Math.abs(r.nextLong());

	}
	
	/**
	 * 
	 * @param board
	 * @return
	 */
	public static long getKey(Board board) {
		long key = 0;
		for(int index = a8; index <= h1; index++) {
			if ((index & 0x88) != 0)
				continue;

			if (board.board[index] != 0)
				key ^= PIECES[board.board[index] + 6][index];

		}
		if (board.sideToMove == BLACK)
			key ^= SIDE;

		if (board.castling.contains("K"))
			key ^= W_SHORT_CASTLE;

		if (board.castling.contains("k"))
			key ^= B_SHORT_CASTLE;

		if (board.castling.contains("Q"))
			key ^= W_LONG_CASTLE;

		if (board.castling.contains("q"))
			key ^= B_LONG_CASTLE;

		if (board.enpassant != -2)
			key ^= ENPASSANT[board.enpassant % 16];

		return key;
	}
}
