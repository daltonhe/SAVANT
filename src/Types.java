/**
 * 
 * @author Dalton He
 * created 10-07-18
 * 
 */
public interface Types {
	public static final String INITIAL_FEN = 
			"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	
	// Sides
	public static final int WHITE =  1;
	public static final int BLACK = -1;
	
	// Pieces (generic)
	public static final int PIECE_NONE = 0;
	public static final int PAWN       = 1;
	public static final int KNIGHT     = 2;
	public static final int BISHOP     = 3;
	public static final int ROOK       = 4;
	public static final int QUEEN      = 5;
	public static final int KING       = 6;
	public static final String PIECE_STR = "kqrbnp.PNBRQK";
	
	// White pieces
	public static final int W_PAWN   = 1;
	public static final int W_KNIGHT = 2;
	public static final int W_BISHOP = 3;
	public static final int W_ROOK   = 4;
	public static final int W_QUEEN  = 5;
	public static final int W_KING   = 6;
	
	// Black pieces
	public static final int B_PAWN   = -1;
	public static final int B_KNIGHT = -2;
	public static final int B_BISHOP = -3;
	public static final int B_ROOK   = -4;
	public static final int B_QUEEN  = -5;
	public static final int B_KING   = -6;
	
	// Castling rights (KQkq)
	public static final int W_ALL_CASTLING = 0b1100;
	public static final int B_ALL_CASTLING = 0b0011;
	public static final int W_SHORT_CASTLE = 0b1000;
	public static final int W_LONG_CASTLE  = 0b0100;
	public static final int B_SHORT_CASTLE = 0b0010;
	public static final int B_LONG_CASTLE  = 0b0001;
	
	// Move types
	public static final int NORMAL       = 0;
	public static final int CASTLE_SHORT = 1;
	public static final int CASTLE_LONG  = 2;
	public static final int PAWN_TWO     = 3;
	public static final int ENPASSANT    = 4;
	public static final int PROMOTION    = 5;
	
	// Move generation deltas
	public static final int[][] MOVE_DELTA = {
		{},
		{ -17, -16, -15                      }, // Pawn
		{ -33, -31, -18, -14, 14, 18, 31, 33 }, // Knight
		{ -17, -15,  15,  17                 }, // Bishop
		{ -16,  -1,   1,  16                 }, // Rook
		{ -17, -16, -15,  -1,  1, 15, 16, 17 }, // Queen
		{ -17, -16, -15,  -1,  1, 15, 16, 17 }, // King
	};
	
	// Piece values
	public static final int PAWN_MG   = 65,   PAWN_EG   = 100;
	public static final int KNIGHT_MG = 376,  KNIGHT_EG = 416;
	public static final int BISHOP_MG = 399,  BISHOP_EG = 441;
	public static final int ROOK_MG   = 620,  ROOK_EG   = 663;
	public static final int QUEEN_MG  = 1216, QUEEN_EG  = 1292;
	
	public static final int PIECE_VALUE_MG[]   = {0, PAWN_MG, KNIGHT_MG, BISHOP_MG, ROOK_MG, QUEEN_MG, 0};
	public static final int PIECE_VALUE_EG[]   = {0, PAWN_EG, KNIGHT_EG, BISHOP_EG, ROOK_EG, QUEEN_EG, 0};
	public static final int PIECE_UNIT_VALUE[] = {0, 0, 3, 3, 5, 9, 0};
	
	// Search
	public static final int VALUE_INF            = 10001;
	public static final int VALUE_MATE           = 10000;
	public static final int VALUE_MATE_THRESHOLD = 9900;
	public static final int VALUE_KNOWN_WIN      = 1000;
	public static final int VALUE_DRAW           = 0;
	public static final int VALUE_PATH_DRAW      = 1;
		// This is a special value to distinguish path-dependent draws (i.e. draws by three-fold
		// repetition or by the 50-move rule). These draws are linked to move order rather than
	    // being solely position-dependent (i.e. draw by stalemate or insufficient material).
		// We want to be able to distinguish path-dependent draw evaluations, specifically to
	    // exclude them from being used as TT cutoffs.
	public static final int VALUE_CONTEMPT       = 20;
	
	public static final int NODE_PV  =  0;
	public static final int NODE_CUT =  1;
	public static final int NODE_ALL = -1;

	public static final int INITIAL_WINDOW_SIZE = 10;
	public static final int DELTA_MARGIN        = PAWN_EG * 2;
	public static final int FUTILITY_MARGIN     = KNIGHT_EG;
	public static final int FUTILITY_EXT_MARGIN = ROOK_EG;
	public static final int RAZOR_MARGIN        = QUEEN_EG;
	
	public static final int HISTORY_MAX = 50000;
	
	// Move ordering
	public static final int PRIORITY_HASH_MOVE   = 121;
	public static final int PRIORITY_PROMOTION_Q = 120;
	public static final int PRIORITY_PROMOTION_U = 4;
	public static final int PRIORITY_CASTLING    = 3;
	
	public static final int TIME_INF = 9999000;
	
	// Transposition table
	public static final int HASH_SIZE_TT  = 524309;
	public static final int HASH_SIZE_REP = 16411;
	public static final int HASH_SIZE_PV  = 131101;
	
	public static final int HASH_MAX_AGE = 8;
	
	public static final int BOUND_EXACT = 0;
	public static final int BOUND_LOWER = 1;
	public static final int BOUND_UPPER = 2;
	
	// Evaluation
	public static final int PHASE_WEIGHT[] = {0, 0, KNIGHT_MG, BISHOP_MG, ROOK_MG, QUEEN_MG, 0};
	public static final int PHASE_MAX      = 8012;
	public static final int PHASE_MAX_MG   = 7337;
	public static final int PHASE_MAX_EG   = 1883;
	
	public static final int LAZY_THRESHOLD = 720;
	
	// Assorted bonuses/penalties {middlegame | endgame}
	public static final int MG = 0;
	public static final int EG = 1;
	
	public static final int TEMPO                =  10;
	public static final int ROOK_PAWN            = -15;
	public static final int BISHOP_PAIR          =  43;
	public static final int REDUNDANT_KNIGHT     = -4;
	public static final int REDUNDANT_ROOK       = -12;
	public static final int REDUNDANT_QUEEN      = -4;
	public static final int KNIGHT_PAWN_SYNERGY	 =  7;
	public static final int[] DOUBLED_PAWN       = {-5, -28};
	public static final int[] ISOLATED_PAWN      = {-3,  -8};
	public static final int[] BACKWARD_PAWN      = {-5, -12};
	public static final int[] CONNECTED_PAWN     = {0, 84, 48, 31,  9, 12, 6, 0};
	public static final int[] PAWN_PHALANX       = {0, 37, 18,  8, 11, -1, 3, 0};
	public static final int SUPPORTED_PAWN       =  8;
	public static final int[] BAD_BISHOP_PAWN    = {-2, -5};
	public static final int TRAPPED_BISHOP       = -100;
	public static final int TRAPPED_ROOK         = -47;
	public static final int[] ROOK_OPEN_FILE     = {22, 10};
	public static final int[] ROOK_SEMIOPEN_FILE = { 9,  4};
	public static final int[] ROOK_ON_7TH        = {20, 40};
	public static final int[] QUEEN_ON_7TH       = {10, 20};
	
	// Piece-square tables
	public static final int PSQT_MG[][][] = {
	    {{}},
	    { // Pawn
	    	{  0,   0,   0,   0,   0,   0,   0,   0 },
	    	{ -1,  10,  -5,  -1,  -1,  -5,  10,  -1 },
	    	{ -5,  -6,  -1,   2,   2,  -1,  -6,  -5 },
	    	{ -2,  -1,   0,   6,   6,   0,  -1,  -2 },
	    	{ -7,  -3,  10,  12,  12,  10,  -3,  -7 },    	
	    	{ -8,  -1,  11,  11,  11,  11,  -1,  -8 },	
	    	{ -5,   3,   3,   8,   8,   3,   3,  -5 },
	    	{  0,   0,   0,   0,   0,   0,   0,   0 }},
	    { // Knight
	    	{-96, -38, -25, -15, -15, -25, -38, -96 },
	    	{-32, -10,   3,  18,  18,   3, -10, -32 },
	    	{ -5,  14,  30,  26,  26,  30,  14,  -5 },
	    	{-14,   6,  20,  25,  25,  20,   6, -14 },
	    	{-14,   2,  20,  23,  23,  20,   2, -14 },
	    	{-31, -10,   2,   9,   9,   2, -10, -31 },
	    	{-38, -19, -12,  -4,  -4, -12, -19, -38 },
	    	{-81, -46, -38, -38, -38, -38, -46, -81 }},
	    { // Bishop
	    	{-23,  -3,  -8, -14, -14,  -8,  -3, -23 },
	    	{ -9,  -6,   3,  -5,  -5,   3,  -6,  -9 },
	    	{ -8,   7,  -3,   3,   3,  -3,   7,  -8 },
	    	{ -4,  13,   6,  14,  14,   6,  13,  -4 },
	    	{  2,   4,   9,  19,  19,   9,   4,   2 },
	    	{ -4,  11,  -1,   6,   6,  -1,  11,  -4 },
	    	{-12,   4,   7,   0,   0,   7,   4, -12 },
	    	{-24,  -3,  -5, -16,- 16,  -5,  -3, -24 }},
	    { // Rook
	    	{-12,  -9,  -5,   1,   1,  -5,  -9, -12 },
	    	{ -5,   4,   4,   6,   6,   4,   4,  -5 },
	    	{-11,  -5,   0,   3,   3,   0,  -5, -11 },
	    	{-10,  -6,   0,   2,   2,   0,  -6, -10 },
	    	{-10,  -3,  -2,  -2,  -2,  -2,  -3, -10 },
	    	{ -9,  -5,   0,   0,   0,   0,  -5,  -9 },
	    	{ -9,  -2,   0,   0,   0,   0,  -2,  -9 },
	    	{-12,  -7,  -4,   0,   0,  -4,  -7, -12 }},
	    { // Queen
	    	{ -1,  -1,   0,  -1,  -1,   0,  -1,  -1 },
	    	{ -2,   3,   5,   4,   4,   5,   3,  -2 },
	    	{ -2,   5,   3,   4,   4,   3,   5,  -2 },
	    	{  0,   7,   3,   4,   4,   3,   7,   0 },
	    	{  2,   2,   4,   4,   4,   4,   2,   2 },
	    	{ -1,   3,   6,   3,   3,   6,   3,  -1 },
	    	{ -1,   2,   4,   6,   6,   4,   2,  -1 },
	    	{  1,  -2,  -2,   2,   2,  -2,  -2,   1 }},
	    { // King
	    	{ 31,  42,  24,   0,   0,  24,  42,  31 },
	    	{ 42,  58,  31,  12,  12,  31,  58,  42 },
	    	{ 59,  76,  41,  17,  17,  41,  76,  59 },
	    	{ 70,  85,  54,  33,  33,  55,  85,  70 },
	    	{ 81,  92,  65,  52,  52,  65,  92,  81 },
	    	{ 95, 122,  81,  58,  58,  81, 122,  95 },
	    	{133, 147, 116,  88,  88, 116, 147, 133 },
	    	{131, 156, 131,  91,  91, 131, 156, 131 }}
	};
	public static final int PSQT_EG[][][] = {
		{{}},
	    { // Pawn
			{  0,   0,   0,   0,   0,   0,   0,   0 },
	    	{  0,  -6,   3,  12,  12,   3,   6,   0 },
	    	{  8,   3,   0,   8,   8,   0,   3,   8 },
	    	{  6,   5,   0,  -4,  -4,   0,   5,   6 },
	    	{  3,  -2,  -4,   1,   1,  -4,  -2,   3 },
	    	{ -1,   1,   3,   0,   0,   3,   1,  -1 },
	    	{ -1,   0,   3,   1,   1,   3,   0,  -1 },
	    	{  0,   0,   0,   0,   0,   0,   0,   0 }},
	    { // Knight
	    	{-47, -43, -25,  -8,  -8, -25, -43, -47 },
	    	{-31, -22, -18,   8,   8, -18, -22, -31 },
	    	{-25, -18,  -8,   9,   9,  -8, -18, -25 },
	    	{-20, -10,   2,  17,  17,   2, -10, -20 },
	    	{-17,   0,   6,  16,  16,   6,   0, -17 },
	    	{-18, -16,  -2,  13,  13,  -2, -16, -18 },
	    	{-34, -27,  -7,   3,   3,  -7, -27, -33 },
	    	{-50, -36, -22,  -9,  -9, -22, -36, -50 }},
	    { // Bishop
	    	{-26, -15, -17,  -8,  -8, -17, -15, -26 },
	    	{-16,  -5,  -6,   3,   3,  -6,  -5, -16 },
	    	{-12,  -1,   0,   6,   6,   0,  -1, -12 },
	    	{-13,  -2,  -3,   7,   7,  -3,  -2, -13 },
	    	{-13,  -1,  -2,   8,   8,  -2,  -1, -13 },
	    	{-11,   0,  -1,   8,   8,  -1,   0, -11 },
	    	{-16,  -4,  -7,   2,   2,  -7,  -4, -16 },
	    	{-28, -15, -18,  -9,  -9, -18, -15, -28 }},
	    { // Rook
	    	{  3,   2,   3,   1,   1,   3,   2,   3 },
	    	{  0,   3,   5,   0,   0,   5,   3,   0 },
	    	{  1,   1,   0,   1,   1,   0,   1,   1 },
	    	{ -3,   2,  -2,  -3,  -3,  -2,   2,  -3 },
	    	{  0,   2,  -1,   0,   0,  -1,   2,   0 },
	    	{  3,  -3,   1,   1,   1,   1,  -3,   3 },
	    	{ -3,  -2,  -2,   0,   0,  -2,  -2,  -3 },
	    	{  0,   1,   0,   1,   1,   0,   1,   0 }},
	    { // Queen
	    	{-33, -27, -23, -13, -13, -23, -27, -33 },
	    	{-24, -13, -12,  -4,  -4, -12, -13, -24 },
	    	{-18,  -9,  -6,   0,   0,  -6,  -9, -18 },
	    	{-14,  -3,   4,  10,  10,   4,  -3, -14 },
	    	{-11,  -1,   6,  12,  12,   6,  -1, -11 },
	    	{-19,  -9,  -4,   1,   1,  -4,  -9, -19 },
	    	{-26, -15, -11,  -2,  -2, -11, -15, -26 },
	    	{-36, -25, -21, -17, -17, -21, -25, -36 }
	    },
	    { // King
	    	{  2,  29,  36,  36,  36,  36,  29,   2 },
	    	{ 19,  48,  62,  68,  68,  62,  48,  19 },
	    	{ 42,  79,  84,  91,  91,  84,  79,  42 },
	    	{ 47,  80,  95,  93,  93,  95,  80,  47 },
	    	{ 50,  73,  81,  81,  81,  81,  73,  50 },
	    	{ 41,  66,  79,  83,  83,  79,  66,  41 },
	    	{ 27,  47,  66,  63,  63,  66,  47,  27 },
	    	{  0,  20,  38,  45,  45,  38,  20,   0 }}
	};
	
	// Mobility
	public static final int MOB_MG[][] = {
		{},
		{},
		{-30, -25, -6, -2,  1,  6, 11, 13, 16                          }, // Knight
		{-23, -10,  8, 13, 18, 25, 26, 30, 30, 33, 39, 39, 44, 47      }, // Bishop
		{-28, -13, -7, -5, -2, -1,  4,  8, 14, 14, 15, 18, 22, 23, 28  }, // Rook
		{-19, -10,  1,  1,  7, 11, 13, 20, 21, 23, 27, 29, 29, 32, 32,    // Queen
		  34,  34, 35, 38, 42, 42, 47, 49, 49, 51, 52, 54, 56          },
		{}
	};
	public static final int MOB_EG[][] = {
		{},
		{},
		{-39, -27,-14, -7,  4,  7, 11, 13, 16                         }, // Knight
		{-28, -11, -1,  6, 12, 20, 26, 27, 31, 35, 38, 41, 42, 47     }, // Bishop
		{-37, -9,  13, 26, 33, 39, 54, 57, 63, 68, 75, 79, 80, 81, 82 }, // Rook
		{-17, -7,   4,  9, 16, 26, 29, 35, 38, 44, 45, 50, 54, 58, 59,   // Queen
		  61,  64, 65, 67, 69, 71, 80, 82, 84, 88, 92, 99,102         },
		{}
	};
	
	// Passed pawns
	public static final int PASSED_PAWN_MG[][] = {
		{   0,   0,   0,   0,   0,   0,   0,   0 },
		{ 130, 131, 126, 116, 116, 126, 131, 130 },
		{  78,  78,  74,  64,  64,  74,  78,  78 },
		{  27,  27,  23,  13,  13,  23,  27,  27 },
		{   4,   5,   0, -10, -10,   0,   5,   4 },
		{   5,   6,   1, -11, -11,   1,   6,   5 },
		{   2,   2,  -2, -12, -12,  -2,   2,   2 },
		{   0,   0,   0,   0,   0,   0,   0,   0 }
	};
	public static final int PASSED_PAWN_EG[][] = {
		{   0,   0,   0,   0,   0,   0,   0,   0 },
		{ 257, 259, 242, 236, 236, 242, 259, 257 },
		{ 174, 176, 159, 153, 153, 159, 176, 174 },
		{  69,  71,  54,  48,  48,  62,  54,  69 },
		{  38,  40,  23,  17,  17,  23,  40,  38 },
		{  30,  32,  15,   9,   9,  15,  32,  30 },
		{  25,  27,  10,   4,   4,  10,  27,  25 },
		{   0,   0,   0,   0,   0,   0,   0,   0 }
	};
	public static final int PASSED_DANGER[] = {0, 10, 6, 4, 2, 0, 0, 0};
	
	// Endgame
	public static final int EDGE_PROXIMITY[][] = {
		{ 50, 45, 40, 35, 35, 40, 45, 50 },
		{ 45, 35, 30, 25, 25, 30, 35, 45 },
		{ 40, 30, 20, 15, 15, 20, 30, 40 },
		{ 35, 25, 15, 10, 10, 15, 25, 35 },
		{ 35, 25, 15, 10, 10, 15, 25, 35 },
		{ 40, 30, 20, 15, 15, 20, 30, 40 },
		{ 45, 35, 30, 25, 25, 30, 35, 45 },
		{ 50, 45, 40, 35, 35, 40, 45, 50 }
	};
	public static final int KINGS_PROXIMITY[] = {0, 0, 50, 40, 30, 20, 10, 5};
	
	// Board coordinates
	public static final int SQ_NONE = -2;
	public static final int SQ_a8 = 0;
	public static final int SQ_b8 = 1;
	public static final int SQ_c8 = 2;
	public static final int SQ_d8 = 3;
	public static final int SQ_e8 = 4;
	public static final int SQ_f8 = 5;
	public static final int SQ_g8 = 6;
	public static final int SQ_h8 = 7;
	public static final int SQ_a7 = 16;
	public static final int SQ_b7 = 17;
	public static final int SQ_c7 = 18;
	public static final int SQ_d7 = 19;
	public static final int SQ_e7 = 20;
	public static final int SQ_f7 = 21;
	public static final int SQ_g7 = 22;
	public static final int SQ_h7 = 23;
	public static final int SQ_a6 = 32;
	public static final int SQ_b6 = 33;
	public static final int SQ_c6 = 34;
	public static final int SQ_d6 = 35;
	public static final int SQ_e6 = 36;
	public static final int SQ_f6 = 37;
	public static final int SQ_g6 = 38;
	public static final int SQ_h6 = 39;
	public static final int SQ_a5 = 48;
	public static final int SQ_b5 = 49;
	public static final int SQ_c5 = 50;
	public static final int SQ_d5 = 51;
	public static final int SQ_e5 = 52;
	public static final int SQ_f5 = 53;
	public static final int SQ_g5 = 54;
	public static final int SQ_h5 = 55;
	public static final int SQ_a4 = 64;
	public static final int SQ_b4 = 65;
	public static final int SQ_c4 = 66;
	public static final int SQ_d4 = 67;
	public static final int SQ_e4 = 68;
	public static final int SQ_f4 = 69;
	public static final int SQ_g4 = 70;
	public static final int SQ_h4 = 71;
	public static final int SQ_a3 = 80;
	public static final int SQ_b3 = 81;
	public static final int SQ_c3 = 82;
	public static final int SQ_d3 = 83;
	public static final int SQ_e3 = 84;
	public static final int SQ_f3 = 85;
	public static final int SQ_g3 = 86;
	public static final int SQ_h3 = 87;
	public static final int SQ_a2 = 96;
	public static final int SQ_b2 = 97;
	public static final int SQ_c2 = 98;
	public static final int SQ_d2 = 99;
	public static final int SQ_e2 = 100;
	public static final int SQ_f2 = 101;
	public static final int SQ_g2 = 102;
	public static final int SQ_h2 = 103;
	public static final int SQ_a1 = 112;
	public static final int SQ_b1 = 113;
	public static final int SQ_c1 = 114;
	public static final int SQ_d1 = 115;
	public static final int SQ_e1 = 116;
	public static final int SQ_f1 = 117;
	public static final int SQ_g1 = 118;
	public static final int SQ_h1 = 119;
}
