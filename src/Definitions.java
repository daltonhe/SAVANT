/**
 * 
 * @author Dalton He
 * created 10-07-18
 */
public interface Definitions {
	public static final String INITIAL_FEN = 
			"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	
	// Sides
	public static final int WHITE =  1;
	public static final int BLACK = -1;
	
	// Pieces (generic)
	public static final int PAWN   = 1;
	public static final int KNIGHT = 2;
	public static final int BISHOP = 3;
	public static final int ROOK   = 4;
	public static final int QUEEN  = 5;
	public static final int KING   = 6;
	
	public static final String PIECE_STR = "kqrbnp.PNBRQK";
	
	// White pieces
	public static final int W_PAWN   =  1;
	public static final int W_KNIGHT =  2;
	public static final int W_BISHOP =  3;
	public static final int W_ROOK   =  4;
	public static final int W_QUEEN  =  5;
	public static final int W_KING   =  6;
	
	// Black pieces
	public static final int B_PAWN   = -1;
	public static final int B_KNIGHT = -2;
	public static final int B_BISHOP = -3;
	public static final int B_ROOK   = -4;
	public static final int B_QUEEN  = -5;
	public static final int B_KING   = -6;
	
	// Move types
	public static final int NORMAL       = 0;
	public static final int PAWN_TWO     = 1;
	public static final int CASTLE_SHORT = 2;
	public static final int CASTLE_LONG  = 3;
	public static final int ENPASSANT    = 4;
	public static final int PROMOTION    = 5;
	
	// Move generation deltas
	public static final int[] DELTA_PAWN   = {-17, -16, -15};
	public static final int[] DELTA_W_PAWN = { 15,  17};
	public static final int[] DELTA_B_PAWN = {-17, -15};
	public static final int[] DELTA_KNIGHT = {-33, -31, -18, -14, 14, 18, 31, 33};
	public static final int[] DELTA_BISHOP = {-17, -15,  15,  17};
	public static final int[] DELTA_ROOK   = {-16,  -1,   1,  16};
	public static final int[] DELTA_QUEEN  = {-17, -16, -15,  -1,  1, 15, 16, 17};
	
	public static final String ATTACK_LOOKUP_W[] = {"P", "N", "BQ", "RQ", "K"};
	public static final String ATTACK_LOOKUP_B[] = {"p", "n", "bq", "rq", "k"};
	
	// Search
	public static final int VALUE_INF            = 10001;
	public static final int VALUE_MATE           = 10000;
	public static final int VALUE_MATE_THRESHOLD = 9900;
	public static final int VALUE_DRAW           = 0;

	public static final int DELTA_MARGIN        = 200;
	public static final int INITIAL_WINDOW_SIZE = 10;
	
	// Transposition table
	public static final int HASH_SIZE_TT  = 1048576;
	public static final int HASH_SIZE_REP = 32768;
	public static final int HASH_SIZE_PV  = 32768;
	
	public static final int BOUND_EXACT = 0;
	public static final int BOUND_LOWER = 1;
	public static final int BOUND_UPPER = 2;
	
	// Evaluation
	public static final int PHASE_WEIGHT[] = {0, 0, 1, 1, 2, 4, 0};
	public static final int MIDGAME_PHASE_LIMIT  = 22;
	public static final int ENDGAME_PHASE_LIMIT  = 5;
	
	public static final int PAWN_MG   = 100,  PAWN_EG  = 100;
	public static final int KNIGHT_MG = 350,  KNIGHT_EG = 350;
	public static final int BISHOP_MG = 350,  BISHOP_EG = 350;
	public static final int ROOK_MG   = 525,  ROOK_EG   = 525;
	public static final int QUEEN_MG  = 1000, QUEEN_EG  = 1000;
	
	public static final int PIECE_VALUE_MG[] = 
		{0, PAWN_MG, KNIGHT_MG, BISHOP_MG, ROOK_MG, QUEEN_MG, 0};
	public static final int PIECE_VALUE_EG[] =
		{0, PAWN_EG, KNIGHT_EG, BISHOP_EG, ROOK_EG, QUEEN_EG, 0};
	
	public static final int PSQT_MG[][][] = {
		    {{}},
		    { // Pawn
		    	{ 0,  0,  0,  0,  0,  0,  0,  0},
		    	{-1, 10, -5, -1, -1, -5, 10, -1},
		    	{-5, -6, -1,  2,  2, -1, -6, -5},
		    	{-2, -1,  0,  6,  6,  0, -1, -2},
		    	{-7, -3, 10, 12, 12, 10, -3, -7},    	
		    	{-8, -1, 11, 11, 11, 11, -1, -8},	
		    	{-5,  3,  3,  8,  8,  3,  3, -5},
		    	{ 0,  0,  0,  0,  0,  0,  0,  0}
		    },
		    { // Knight
		    	{-100, -40, -26, -16, -16, -26, -40,-100},
		    	{ -33, -10,   3,  18,  18,   3, -10, -33},
		    	{  -5,  14,  31,  27,  27,  31,  14,  -5},
		    	{ -14,   6,  21,  26,  26,  21,   6, -14},
		    	{ -14,   2,  20,  23,  23,  20,   2, -14},
		    	{ -32, -10,   2,   9,   9,   2, -10, -32},
		    	{ -39, -19, -12,  -4,  -4, -12, -19, -39},
		    	{ -84, -48, -40, -39, -39, -40, -48, -84}
		    },
		    
		    { // Bishop
		    	{-23, -3, -8, -14, -14, -8, -3, -23},
		    	{ -9, -6,  3,  -5,  -5,  3, -6,  -9},
		    	{ -8,  7, -3,   3,   3, -3,  7,  -8},
		    	{ -4, 13,  6,  15,  15,  6, 13,  -4},
		    	{  2,  4,  9,  20,  20,  9,  4,   2},
		    	{ -4, 11, -1,   6,   6, -1, 11,  -4},
		    	{-12,  4,  7,   0,   0,  7,  4, -12},
		    	{-24, -3, -5, -17,- 17, -5, -3, -24}
		    },
		    { // Rook
		    	{-12, -7, -5, -2, -2, -5, -7, -12},
		    	{ -6,  2,  4,  6,  6,  4,  2,  -6},
		    	{-10, -3,  0,  1,  1,  0, -3, -10},
		    	{-11, -3,  0,  0,  0,  0, -3, -11},
		    	{-11, -3,  0,  1,  1,  0, -3, -11},
		    	{-10, -4, -2,  1,  1, -2, -4, -10},
		    	{-10, -4, -1,  0,  0, -1, -4, -10},
		    	{-12, -8, -5,  3,  3, -5, -8, -12}
		    },
		    { // Queen
		    	{ 0, -2,  0,  0,  0,  0, -2,  0},
		    	{-1,  3,  3,  3,  3,  3,  3, -1},
		    	{-1,  3,  4,  5,  5,  4,  3, -1},
		    	{-1,  4,  4,  3,  3,  4,  4, -1},
		    	{ 0,  4,  5,  3,  3,  5,  4,  0},
		    	{-1,  3,  4,  4,  4,  4,  3, -1},
		    	{-2,  3,  4,  4,  4,  4,  3, -2},
		    	{ 0, -2, -1,  0,  0, -1, -2,  0}
		    },
		    { // King
		    	{ 32,  43,  24,  0,  0,  24,  43,  32},
		    	{ 43,  60,  32, 12, 12,  32,  60,  43},
		    	{ 61,  79,  42, 18, 18,  42,  79,  61},
		    	{ 72,  88,  56, 34, 34,  56,  88,  72},
		    	{ 84,  95,  68, 54, 54,  68,  95,  84},
		    	{ 99, 126,  84, 60, 60,  84, 126,  99},
		    	{138, 152, 120, 91, 91, 120, 152, 138},
		    	{136, 162, 136, 95, 95, 136, 162, 136}
		    }
		};
		public static final int PSQT_EG[][][] = {
			{{}},
		    { // Pawn
				{ 0,  0,  0,  0,  0,  0,  0,  0},
		    	{ 0, -6,  3, 12, 12,  3,  6,  0},
		    	{ 8,  3,  0,  8,  8,  0,  3,  8},
		    	{ 6,  5,  0, -4, -4,  0,  5,  6},
		    	{ 3, -2, -4,  1,  1, -4, -2,  3},
		    	{-1,  1,  3,  0,  0,  3,  1, -1},
		    	{-1,  0,  3,  1,  1,  3,  0, -1},
		    	{ 0,  0,  0,  0,  0,  0,  0,  0}
		    },
		    { // Knight
		    	{-49, -44, -26, -8, -8, -26, -44, -49},
		    	{-32, -22, -18,  8,  8, -18, -22, -32},
		    	{-25, -19,  -8,  9,  9,  -8, -19, -25},
		    	{-20, -10,   2, 17, 17,   2, -10, -20},
		    	{-18,   0,   6, 17, 17,   6,   0, -18},
		    	{-19, -16,  -2, 13, 13,  -2, -16, -19},
		    	{-35, -28,  -7,  3,  3, -30,  -7, -35},
		    	{-52, -37, -23, -9, -9, -23, -37, -52}
		    },
		    { // Bishop
		    	{-27, -16, -18, -8, -8, -18, -16, -27},
		    	{-17,  -5,  -6,  3,  3,  -6,  -5, -17},
		    	{-12,  -1,   0,  6,  6,   0,  -1, -12},
		    	{-13,  -2,  -3,  7,  7,  -3,  -2, -13},
		    	{-13,  -1,  -2,  8,  8,  -2,  -1, -13},
		    	{-11,   0,  -1,  8,  8,  -1,   0, -11},
		    	{-17,  -4,  -7,  2,  2,  -7,  -4, -17},
		    	{-29, -15, -18, -9, -9, -18, -15, -29}
		    },
		    { // Rook
		    	{0, 0, 0, 0, 0, 0, 0, 0},
		    	{0, 0, 0, 0, 0, 0, 0, 0},
		    	{0, 0, 0, 0, 0, 0, 0, 0},
		    	{0, 0, 0, 0, 0, 0, 0, 0},
		    	{0, 0, 0, 0, 0, 0, 0, 0},
		    	{0, 0, 0, 0, 0, 0, 0, 0},
		    	{0, 0, 0, 0, 0, 0, 0, 0},
		    	{0, 0, 2, 2, 2, 2, 0, 0}
		    },
		    { // Queen
		    	{-37, -27, -21, -15, -15, -21,  27, -37},
		    	{-27, -15, -10,  -3,  -3, -10, -15, -27},
		    	{-20,  -8,  -5,   1,   1,  -5,  -8, -20},
		    	{-13,  -2,   5,  10,  10,   5,  -2, -13},
		    	{-14,  -2,   4,   9,   9,   4,  -2, -14},
		    	{-19,  -8,  -4,   2,   2,  -4,  -8, -19},
		    	{-28, -15, -10,  -2,  -2, -10, -15, -28},
		    	{-35, -28, -21, -14, -14, -21, -28, -35}
		    },
		    { // King
		    	{ 2, 30, 37, 37, 37, 37, 30,  2},
		    	{20, 49, 64, 70, 70, 64, 49, 20},
		    	{43, 82, 87, 94, 94, 87, 82, 43},
		    	{49, 83, 98, 97, 97, 98, 83, 49},
		    	{51, 76, 84, 84, 84, 84, 76, 51},
		    	{43, 69, 82, 86, 86, 82, 69, 43},
		    	{28, 49, 69, 65, 65, 69, 49, 28},
		    	{ 0, 20, 40, 46, 46, 40, 20,  0}
		    }
		};
	
	public static final int TEMPO                    =  10;
	public static final int MINOR_WITH_NO_PAWNS      = -150;
	public static final int BISHOP_PAIR              =  50;
	public static final int REDUNDANT_ROOK           = -6;
	public static final int REDUNDANT_QUEEN          = -4;
	public static final int KNIGHT_PAWN_SYNERGY	   =  6;
	public static final int ROOK_PAWN_SYNERGY        = -12;
	public static final int DOUBLED_PAWN_MG          = -5;
	public static final int DOUBLED_PAWN_EG          = -28;
	public static final int ISOLATED_PAWN_MG         = -2;
	public static final int ISOLATED_PAWN_EG         = -7;
	public static final int SUPPORTED_PAWN           =  8;
	public static final int[] CONNECTED_PAWN         = {0, 87, 50, 32, 15, 12, 6, 0};
	public static final int PAWN_ON_BISHOP_COLOR_MG  = -3;
	public static final int PAWN_ON_BISHOP_COLOR_EG  = -7;
	public static final int ROOK_OPEN_FILE_MG        =  22;
	public static final int ROOK_OPEN_FILE_EG        =  10;
	public static final int ROOK_SEMIOPEN_FILE_MG    =  9;
	public static final int ROOK_SEMIOPEN_FILE_EG    =  3;
	public static final int TRAPPED_BISHOP		   = -100;
	public static final int TRAPPED_ROOK             = -50;
	public static final int ROOK_ON_7TH_MG           =  20;
	public static final int ROOK_ON_7TH_EG           =  40;
	public static final int QUEEN_ON_7TH_MG          =  10;
	public static final int QUEEN_ON_7TH_EG          =  20;
	
	public static final int PASSED_PAWN_MG[][] = {
		{  0,   0,   0,   0,   0,   0,   0,   0},
		{130, 130, 126, 115, 115, 126, 130, 130},
		{ 81,  81,  77,  66,  66,  77,  81,  81},
		{ 28,  28,  24,  13,  13,  24,  28,  28},
		{  5,   5,   1,   0,   0,   1,   5,   5},
		{  6,   6,   2,   0,   0,   2,   6,   6},
		{  2,   2,  -2,   0,   0,  -2,   2,   2},
		{  0,   0,   0,   0,   0,   0,   0,   0}
	};
	public static final int PASSED_PAWN_EG[][] = {
		{  0,   0,   0,   0,   0,   0,   0,   0},
		{128, 129, 121, 118, 118, 121, 129, 128},
		{ 86,  87,  79,  76,  76,  79,  87,  86},
		{ 34,  35,  27,  24,  24,  27,  35,  34},
		{ 18,  19,  11,   8,   8,  11,  19,  18},
		{ 14,  14,   7,   4,   4,   7,  14,  14},
		{ 12,  13,   5,   2,   2,   5,  13,  12},
		{  0,   0,   0,   0,   0,   0,   0,   0}
	};
	
	public static final int MOBILITY_MG[][] = {
		{},
		{},
		{-31, -26, -6, -2,  1,  6, 11, 14, 16}, // Knight
		{-24, -10,  8, 13, 19, 25, 27, 31, 31, 34, 40, 40, 45, 49}, // Bishop
		{-29, -13, -7, -5, -2, -1,  4,  8, 15, 14, 16, 19, 23, 24, 29}, // Rook
		{-19, -10,  1,  1,  7, 11, 14, 20, 21, 24, 28, 30, 30, 33, 33,
		  35,  35, 36, 39, 44, 44, 49, 51, 51, 53, 54, 56, 58}, // Queen
		{}
	};
	public static final int MOBILITY_EG[][] = {
		{},
		{},
		{-40,-28,-15, -7,  4,  7, 11, 13, 16}, // Knight
		{-29,-11, -1,  6, 12, 21, 27, 28, 32, 36, 39, 43,  44, 48}, // Bishop
		{-38, -9, 14, 27, 34, 41, 56, 59, 66, 71, 77, 82,  83, 84, 85}, // Rook
		{-18, -7,  4,  9, 17, 27, 30, 36, 39, 46, 47, 52,  56, 60, 61,
		  63, 66, 68, 70, 71, 74, 83, 85, 87, 92, 95, 103, 106}, // Queen
		{}
	};
	
	// Board coordinates
	public static final int a8 = 0;
	public static final int b8 = 1;
	public static final int c8 = 2;
	public static final int d8 = 3;
	public static final int e8 = 4;
	public static final int f8 = 5;
	public static final int g8 = 6;
	public static final int h8 = 7;
	public static final int a7 = 16;
	public static final int b7 = 17;
	public static final int c7 = 18;
	public static final int d7 = 19;
	public static final int e7 = 20;
	public static final int f7 = 21;
	public static final int g7 = 22;
	public static final int h7 = 23;
	public static final int a6 = 32;
	public static final int b6 = 33;
	public static final int c6 = 34;
	public static final int d6 = 35;
	public static final int e6 = 36;
	public static final int f6 = 37;
	public static final int g6 = 38;
	public static final int h6 = 39;
	public static final int a5 = 48;
	public static final int b5 = 49;
	public static final int c5 = 50;
	public static final int d5 = 51;
	public static final int e5 = 52;
	public static final int f5 = 53;
	public static final int g5 = 54;
	public static final int h5 = 55;
	public static final int a4 = 64;
	public static final int b4 = 65;
	public static final int c4 = 66;
	public static final int d4 = 67;
	public static final int e4 = 68;
	public static final int f4 = 69;
	public static final int g4 = 70;
	public static final int h4 = 71;
	public static final int a3 = 80;
	public static final int b3 = 81;
	public static final int c3 = 82;
	public static final int d3 = 83;
	public static final int e3 = 84;
	public static final int f3 = 85;
	public static final int g3 = 86;
	public static final int h3 = 87;
	public static final int a2 = 96;
	public static final int b2 = 97;
	public static final int c2 = 98;
	public static final int d2 = 99;
	public static final int e2 = 100;
	public static final int f2 = 101;
	public static final int g2 = 102;
	public static final int h2 = 103;
	public static final int a1 = 112;
	public static final int b1 = 113;
	public static final int c1 = 114;
	public static final int d1 = 115;
	public static final int e1 = 116;
	public static final int f1 = 117;
	public static final int g1 = 118;
	public static final int h1 = 119;
}
