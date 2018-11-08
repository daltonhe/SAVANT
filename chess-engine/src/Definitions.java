/**
 * Definitions of various useful constants.
 * @author Dalton He
 * created 6-10-2018
 */
public interface Definitions {
	public static final int WHITE = 1;
	public static final int BLACK = -1;
	public static final int EMPTY = 0;
	public static final int PAWN = 1;
	public static final int KNIGHT = 2;
	public static final int BISHOP = 3;
	public static final int ROOK = 4;
	public static final int QUEEN = 5;
	public static final int KING = 6;
	public static final int BLACK_PAWN = -1;
	public static final int BLACK_KNIGHT = -2;
	public static final int BLACK_BISHOP = -3;
	public static final int BLACK_ROOK = -4;
	public static final int BLACK_QUEEN = -5;
	public static final int BLACK_KING = -6;
	public static final int[] INDEX_0x88 = {
			0,   1,   2,   3,   4,   5,   6,   7,
		 16,  17,  18,  19,  20,  21,  22,  23,
		 32,  33,  34,  35,  36,  37,  38,  39,
		 48,  49,  50,  51,  52,  53,  54,  55,
		 64,  65,  66,  67,  68,  69,  70,  71,
		 80,  81,  82,  83,  84,  85,  86,  87,
		 96,  97,  98,  99, 100, 101, 102, 103,
		112, 113, 114, 115, 116, 117, 118, 119
	};
	public static final int NONE = -1;
	public static final int A1 = 0;
	public static final int B1 = 1;
	public static final int C1 = 2;
	public static final int D1 = 3;
	public static final int E1 = 4;
	public static final int F1 = 5;
	public static final int G1 = 6;
	public static final int H1 = 7;
	public static final int A2 = 16;
	public static final int B2 = 17;
	public static final int C2 = 18;
	public static final int D2 = 19;
	public static final int E2 = 20;
	public static final int F2 = 21;
	public static final int G2 = 22;
	public static final int H2 = 23;
	public static final int A3 = 32;
	public static final int B3 = 33;
	public static final int C3 = 34;
	public static final int D3 = 35;
	public static final int E3 = 36;
	public static final int F3 = 37;
	public static final int G3 = 38;
	public static final int H3 = 39;
	public static final int A4 = 48;
	public static final int B4 = 49;
	public static final int C4 = 50;
	public static final int D4 = 51;
	public static final int E4 = 52;
	public static final int F4 = 53;
	public static final int G4 = 54;
	public static final int H4 = 55;
	public static final int A5 = 64;
	public static final int B5 = 65;
	public static final int C5 = 66;
	public static final int D5 = 67;
	public static final int E5 = 68;
	public static final int F5 = 69;
	public static final int G5 = 70;
	public static final int H5 = 71;
	public static final int A6 = 80;
	public static final int B6 = 81;
	public static final int C6 = 82;
	public static final int D6 = 83;
	public static final int E6 = 84;
	public static final int F6 = 85;
	public static final int G6 = 86;
	public static final int H6 = 87;
	public static final int A7 = 96;
	public static final int B7 = 97;
	public static final int C7 = 98;
	public static final int D7 = 99;
	public static final int E7 = 100;
	public static final int F7 = 101;
	public static final int G7 = 102;
	public static final int H7 = 103;
	public static final int A8 = 112;
	public static final int B8 = 113;
	public static final int C8 = 114;
	public static final int D8 = 115;
	public static final int E8 = 116;
	public static final int F8 = 117;
	public static final int G8 = 118;
	public static final int H8 = 119;
	public static final int NORMAL_MOVE = 0;
	public static final int PAWN_TWO_SQUARES = 1;
	public static final int ENPASSANT = 2;
	public static final int CASTLE_SHORT = 3;
	public static final int CASTLE_LONG = 4;
	public static final int PROMOTION_QUEEN = 5;
	public static final int PROMOTION_ROOK = 6;
	public static final int PROMOTION_BISHOP = 7;
	public static final int PROMOTION_KNIGHT = 8;
	public static final int[] PAWN_DELTA =
		{15, 17};
	public static final int[] KNIGHT_DELTA =
		{-33, -31, -18, -14, 14, 18, 31, 33};
	public static final int[] BISHOP_DELTA =
		{-17, -15, 15, 17};
	public static final int[] ROOK_DELTA =
		{-16, -1, 1, 16};
  public static final int[] QUEEN_DELTA =
  	{-17, -16, -15, -1, 1, 15, 16, 17};
  public static final int[] MATERIAL_VALUES =
  	{0, -900, -500, -300, -300, -100, 0, 100, 300, 300, 500, 900, 0};
  public static final int INFINITY = 200000;
  public static final int MATE_VALUE = 100000;
  public static final int DRAW_VALUE = 0;
  public static final String PIECES = "kqrbnp.PNBRQK";
	public static final String FILES = "abcdefgh";
	public static final String INITIAL_POSITION_FEN =
			"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

}