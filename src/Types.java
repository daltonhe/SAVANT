/**
 * 
 * @author Dalton He
 * created 10-07-18
 * 
 */
public interface Types {
    /* POSITION */
    
    public static final String INITIAL_FEN = 
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public static final String PIECE_STR = "kqrbnp.PNBRQK";
    
    // Board coordinates
    public static final int FILE_A = 0;
    public static final int FILE_B = 1;
    public static final int FILE_C = 2;
    public static final int FILE_D = 3;
    public static final int FILE_E = 4;
    public static final int FILE_F = 5;
    public static final int FILE_G = 6;
    public static final int FILE_H = 7;
    
    public static final int RANK_1 = 7;
    public static final int RANK_2 = 6;
    public static final int RANK_3 = 5;
    public static final int RANK_4 = 4;
    public static final int RANK_5 = 3;
    public static final int RANK_6 = 2;
    public static final int RANK_7 = 1;
    public static final int RANK_8 = 0;
    
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

    // Color
    public static final int WHITE =  1;
    public static final int BLACK = -1;

    // Pieces (generic)
    public static final int PAWN   = 1;
    public static final int KNIGHT = 2;
    public static final int BISHOP = 3;
    public static final int ROOK   = 4;
    public static final int QUEEN  = 5;
    public static final int KING   = 6;

    // Pieces (White)
    public static final int W_PAWN   = 1;
    public static final int W_KNIGHT = 2;
    public static final int W_BISHOP = 3;
    public static final int W_ROOK   = 4;
    public static final int W_QUEEN  = 5;
    public static final int W_KING   = 6;

    // Pieces (Black)
    public static final int B_PAWN   = -1;
    public static final int B_KNIGHT = -2;
    public static final int B_BISHOP = -3;
    public static final int B_ROOK   = -4;
    public static final int B_QUEEN  = -5;
    public static final int B_KING   = -6;

    // Castling rights (0bKQkq)
    public static final int W_ALL_CASTLING = 0b1100;
    public static final int B_ALL_CASTLING = 0b0011;
    public static final int W_SHORT_CASTLE = 0b1000;
    public static final int W_LONG_CASTLE  = 0b0100;
    public static final int B_SHORT_CASTLE = 0b0010;
    public static final int B_LONG_CASTLE  = 0b0001;
    
    /* MOVE GENERATION */

    // Move type
    public static final int NORMAL       = 0;
    public static final int CASTLE_SHORT = 1;
    public static final int CASTLE_LONG  = 2;
    public static final int PAWN_TWO     = 3;
    public static final int ENPASSANT    = 4;
    public static final int PROMOTION    = 5;
    
    // Generation stage
    public static final int GEN_ALL     = 0;
    public static final int GEN_SEARCH  = 1;
    public static final int GEN_QSEARCH = 2;

    // Piece deltas
    public static final int[] PAWN_DELTA    = {  16,  15,  17                      };
    public static final int[] KNIGHT_DELTA  = { -33, -31, -18, -14, 14, 18, 31, 33 };
    public static final int[] BISHOP_DELTA  = { -17, -15,  15,  17                 };
    public static final int[] ROOK_DELTA    = { -16,  -1,   1,  16                 };
    public static final int[] QUEEN_DELTA   = { -17, -16, -15,  -1,  1, 15, 16, 17 };
    public static final int[] KING_DELTA    = { -17, -16, -15,  -1,  1, 15, 16, 17 };
    public static final int[][] PIECE_DELTA = {
        {}, PAWN_DELTA, KNIGHT_DELTA, BISHOP_DELTA, ROOK_DELTA, QUEEN_DELTA, KING_DELTA
    };

    /* SEARCH */
    
    // Bounds
    public static final int VALUE_INF        = 10001;
    public static final int VALUE_MATE       = 10000;
    public static final int VALUE_MATETHRESH = 9900;
    public static final int VALUE_DRAW       = 0;
    
    // Depth
    public static final int DEPTH_MAX = 99;
    public static final int DEPTH_QS  = -1;

    // Node type
    public static final int NODE_PV  =  0;
    public static final int NODE_CUT =  1;
    public static final int NODE_ALL = -1;

    // Pruning
    public static final int INITIAL_WINDOW      = 10;
    public static final int DELTA_MARGIN        = 100;
    public static final int FUTILITY_MARGIN     = 200;
    public static final int EXT_FUTILITY_MARGIN = 450;
    public static final int RAZOR_MARGIN        = 800;
    public static final int HISTORY_MAX         = 50000;

    // Move ordering
    public static final int PRIORITY_HASH_MOVE = 1000;
    public static final int PRIORITY_PROMOTION = 100;
    public static final int PRIORITY_CAPTURE   = 50;
    public static final int PRIORITY_KILLER    = 40;

    // Transposition table
    public static final int HASH_SIZE_TT = 65536;
    public static final int HASH_SIZE_PV = 16384;
    public static final int BOUND_EXACT  = 0;
    public static final int BOUND_LOWER  = 1;
    public static final int BOUND_UPPER  = 2;

    /* EVALUATION */
    
    // Game stage
    public static final int MG = 0;
    public static final int EG = 1;
    
    // Phase threshold
    public static final int MIDGAME_THRESH = 7336;
    public static final int ENDGAME_THRESH = 1882;
    
    // Piece value [MG | EG]
    public static final int[] VALUE_PAWN   = {   65,  100 };
    public static final int[] VALUE_KNIGHT = {  376,  416 };
    public static final int[] VALUE_BISHOP = {  399,  441 };
    public static final int[] VALUE_ROOK   = {  620,  663 };
    public static final int[] VALUE_QUEEN  = { 1216, 1292 };
    public static final int[] VALUE_PIECE  = { 0, 100, 416, 441, 663, 1292 };
    public static final int   VALUE_PROMOTION = 1192; // VALUE_QUEEN - VALUE_PAWN
    

    // Bonuses/penalties [MG | EG]
    public static final int   TEMPO          =  13;
    public static final int   TRAPPED_BISHOP = -150;
    public static final int   TRAPPED_ROOK   = -46;
    public static final int[] ROOK_OPEN_FILE = { 21,  10 };
    public static final int[] ROOK_SEMI_FILE = {  9,   4 }; 
    public static final int[] DOUBLED_PAWN   = { -5, -27 };
    public static final int[] ISOLATED_PAWN  = { -2,  -7 };
    public static final int[] BACKWARD_PAWN  = { -4, -12 };
    public static final int[] WEAK_PAWN      = { -6, -11 };
    public static final int[] BISHOP_PAWN    = { -1,  -3 };
    public static final int[] KING_PROTECTOR = {  3,   4 };
    public static final int[] CONNECTED_PAWN = { 0, 84, 48, 31,  9, 12, 6, 0 };
    public static final int[] PAWN_PHALANX   = { 0, 37, 18,  8, 11, -1, 3, 0 };
    public static final int   SUPPORTED_PAWN =  8;
    public static final int   KING_PAWN_DIST = -8;
    
    // Imbalance
    public static final double P_WITH_P =  1.1;
    public static final double N_WITH_P =  7.7;
    public static final double N_WITH_N = -1.9;
    public static final double B_WITH_P =  3.1;
    public static final double B_WITH_N =  0.1;
    public static final double R_WITH_P = -0.1;
    public static final double R_WITH_N =  1.4;
    public static final double R_WITH_B =  3.2;
    public static final double R_WITH_R = -6.3;
    public static final double Q_WITH_P =  0.7;
    public static final double Q_WITH_N =  3.5;
    public static final double Q_WITH_B =  4.0;
    public static final double Q_WITH_R = -4.0;
    public static final double Q_WITH_Q = -0.2; 
    
    public static final double N_VS_P =  1.9;
    public static final double B_VS_P =  2.0;
    public static final double B_VS_N =  1.3;
    public static final double R_VS_P =  1.2;
    public static final double R_VS_N =  0.7;
    public static final double R_VS_B = -0.7;
    public static final double Q_VS_P =  3.0;
    public static final double Q_VS_N = -1.3;
    public static final double Q_VS_B =  4.1;
    public static final double Q_VS_R =  8.1;
    
    public static final double BISHOP_PAIR = 43.2;
    public static final double P_WITH_BB =  1.2;
    public static final double N_WITH_BB =  1.0;
    public static final double R_WITH_BB = -0.8;
    public static final double Q_WITH_BB = -5.7;
    public static final double P_VS_BB   =  1.1;
    public static final double N_VS_BB   =  0.3;
    public static final double B_VS_BB   =  1.8;
    public static final double R_VS_BB   =  1.4;
    public static final double Q_VS_BB   =  2.9;

    // Piece-square tables
    public static final int[][] PAWN_PSQT_MG = {
            {                                        },
            {  -1,  10,  -5,  -1,  -1,  -5,  10,  -1 },
            {  -5,  -6,  -1,   2,   2,  -1,  -6,  -5 },
            {  -2,  -1,   0,   6,   6,   0,  -1,  -2 },
            {  -7,  -3,  10,  12,  12,  10,  -3,  -7 },     
            {  -8,  -1,  11,  11,  11,  11,  -1,  -8 }, 
            {  -5,   3,   3,   8,   8,   3,   3,  -5 },
            {                                        }
    };
    public static final int[][] PAWN_PSQT_EG = {
            {                                        },
            {   0,  -6,   3,  12,  12,   3,  -6,   0 },
            {   8,   3,   0,   8,   8,   0,   3,   8 },
            {   6,   5,   0,  -4,  -4,   0,   5,   6 },
            {   3,  -2,  -4,   1,   1,  -4,  -2,   3 },
            {  -1,   1,   3,   0,   0,   3,   1,  -1 },
            {  -1,   0,   3,   1,   1,   3,   0,  -1 },
            {                                        }
    };
    public static final int[][] KNIGHT_PSQT_MG = {
            { -96, -38, -25, -15, -15, -25, -38, -96 },
            { -32, -10,   3,  18,  18,   3, -10, -32 },
            {  -5,  14,  30,  26,  26,  30,  14,  -5 },
            { -14,   6,  20,  25,  25,  20,   6, -14 },
            { -14,   2,  20,  23,  23,  20,   2, -14 },
            { -31, -10,   2,   9,   9,   2, -10, -31 },
            { -38, -19, -12,  -4,  -4, -12, -19, -38 },
            { -81, -46, -38, -38, -38, -38, -46, -81 }
    };
    public static final int[][] KNIGHT_PSQT_EG = {
            { -47, -43, -25,  -8,  -8, -25, -43, -47 },
            { -31, -22, -18,   8,   8, -18, -22, -31 },
            { -25, -18,  -8,   9,   9,  -8, -18, -25 },
            { -20, -10,   2,  17,  17,   2, -10, -20 },
            { -17,   0,   6,  16,  16,   6,   0, -17 },
            { -18, -16,  -2,  13,  13,  -2, -16, -18 },
            { -34, -27,  -7,   3,   3,  -7, -27, -34 },
            { -50, -36, -22,  -9,  -9, -22, -36, -50 }
    };
    public static final int[][] BISHOP_PSQT_MG = {
            { -23,  -1,  -6, -12, -12,  -6,  -1, -23 },
            { -10,  -9,   5,  -3,  -3,   5,  -9, -10 },
            {  -8,   2,   0,   4,   4,   0,   2,  -8 },
            {  -3,  14,  11,  13,  13,  11,  14,  -3 },
            {   0,   4,  13,  18,  18,  13,   4,   0 },
            {  -4,  12,  -1,   7,   7,  -1,  12,  -4 },
            {  -9,   3,   7,   1,   1,   7,   3,  -9 },
            { -21,  -2,  -5, -13, -13,  -5,  -2, -21 }  
    };
    public static final int[][] BISHOP_PSQT_EG = {
            { -25, -19, -19, -10, -10, -19, -19, -25 },
            { -16,  -9,  -3,   4,   4,  -3,  -9, -16 },
            { -13,   1,   0,   8,   8,   0,   1, -13 },
            { -12,  -3,  -5,   8,   8,  -5,  -3, -12 },
            { -13,  -1,   0,   8,   8,   0,  -1, -13 },
            {  -9,   0,  -3,   6,   6,  -3,   0,  -9 },
            { -18,  -6,  -7,   0,   0,  -7,  -6, -18 },
            { -30, -14, -17,  -4,  -4, -17, -14, -30 }  
    };
    public static final int[][] ROOK_PSQT_MG = {
            { -11, -12,  -3,   2,   2,  -3, -12, -11 },
            {  -4,   3,   5,   6,   6,   5,   3,  -4 },
            { -12,  -2,   2,   5,   5,   2,  -2, -12 },
            { -12,  -6,   0,   3,   3,   0,  -6, -12 },
            {  -6,  -2,  -2,  -3,  -3,  -2,  -2,  -6 },
            { -10,  -3,   1,   0,   0,   1,  -3, -10 },
            {  -9,  -5,  -2,   4,   4,  -2,  -5,  -9 },
            { -12,  -6,  -3,   1,   1,  -3,  -6, -12 }  
    };
    public static final int[][] ROOK_PSQT_EG = {
            {   6,  -3,   6,   3,   3,   6,  -3,   6 },
            {   0,   1,   8,  -4,  -4,   8,   1,   0 },
            {   1,  -1,  -5,   3,   3,  -5,  -1,   1 },
            {  -4,   2,   2,  -4,  -4,   2,   2,  -4 },
            {  -2,   1,  -4,   4,   4,  -4,   1,  -2 },
            {   5,  -2,   1,  -1,  -1,   1,  -2,   5 },
            {  -5,  -3,   0,   0,   0,   0,  -3,  -5 },
            {  -1,  -3,  -1,  -1,  -1,  -1,  -3,  -1 }
    };

    public static final int[][] QUEEN_PSQT_MG = {
            {  -1,  -1,   0,  -1,  -1,   0,  -1,  -1 },
            {  -2,   3,   5,   4,   4,   5,   3,  -2 },
            {  -2,   5,   3,   4,   4,   3,   5,  -2 },
            {   0,   7,   3,   4,   4,   3,   7,   0 },
            {   2,   2,   4,   4,   4,   4,   2,   2 },
            {  -1,   3,   6,   3,   3,   6,   3,  -1 },
            {  -1,   2,   4,   6,   6,   4,   2,  -1 },
            {   1,  -2,  -2,   2,   2,  -2,  -2,   1 }
    };
    public static final int[][] QUEEN_PSQT_EG = {
            { -33, -27, -23, -13, -13, -23, -27, -33 },
            { -24, -13, -12,  -4,  -4, -12, -13, -24 },
            { -18,  -9,  -6,   0,   0,  -6,  -9, -18 },
            { -14,  -3,   4,  10,  10,   4,  -3, -14 },
            { -11,  -1,   6,  12,  12,   6,  -1, -11 },
            { -19,  -9,  -4,   1,   1,  -4,  -9, -19 },
            { -26, -15, -11,  -2,  -2, -11, -15, -26 },
            { -36, -25, -21, -17, -17, -21, -25, -36 }
    };
    public static final int[][] KING_PSQT_MG = {
            {  31,  42,  24,   0,   0,  24,  42,  31 },
            {  42,  58,  31,  12,  12,  31,  58,  42 },
            {  59,  76,  41,  17,  17,  41,  76,  59 },
            {  70,  85,  54,  33,  33,  54,  85,  70 },
            {  81,  92,  65,  52,  52,  65,  92,  81 },
            {  95, 122,  81,  58,  58,  81, 122,  95 },
            { 133, 147, 116,  88,  88, 116, 147, 133 },
            { 131, 156, 131,  91,  91, 131, 156, 131 }
    };
    public static final int[][] KING_PSQT_EG = {
            {   2,  29,  36,  36,  36,  36,  29,   2 },
            {  19,  48,  62,  68,  68,  62,  48,  19 },
            {  42,  79,  84,  91,  91,  84,  79,  42 },
            {  47,  80,  95,  93,  93,  95,  80,  47 },
            {  50,  73,  81,  81,  81,  81,  73,  50 },
            {  41,  66,  79,  83,  83,  79,  66,  41 },
            {  27,  47,  66,  63,  63,  66,  47,  27 },
            {   0,  20,  38,  45,  45,  38,  20,   0 }	
    };

    // Mobility
    public static final int[] KNIGHT_MOB_MG = 
        {-30, -25, -6, -2, 1, 6, 11, 13, 16};
    public static final int[] KNIGHT_MOB_EG = 
        {-39, -27,-14, -7, 4, 7, 11, 13, 16};
    public static final int[] BISHOP_MOB_MG = 
        {-23, -10, 8, 13, 18, 25, 26, 30, 30, 33, 39, 39, 44, 47};
    public static final int[] BISHOP_MOB_EG = 
        {-28, -11, -1,  6, 12, 20, 26, 27, 31, 35, 38, 41, 42, 47};
    public static final int[] ROOK_MOB_MG   = 
        {-28, -13, -7, -5, -2, -1, 4, 8, 14, 14, 15, 18, 22, 23, 28};
    public static final int[] ROOK_MOB_EG   = 
        {-37, -9, 13, 26, 33, 39, 54, 57, 63, 68, 75, 79, 80, 81, 82};
    public static final int[] QUEEN_MOB_MG  = 
        {-19, -10, 1, 1, 7, 11, 13, 20, 21, 23, 27, 29, 29, 32, 
          32, 34, 34, 35, 38, 42, 42, 47, 49, 49, 51, 52, 54, 56};
    public static final int[] QUEEN_MOB_EG  = 
        {-17, -7, 4, 9, 16, 26, 29, 35, 38, 44, 45, 50, 54, 58,
          59, 61, 64, 65, 67, 69, 71, 80, 82, 84, 88, 92, 99, 102};

    // Passed pawns
    public static final int[] PASSED_DANGER  = { 0, 10, 5, 3, 1, 0, 0, 0 };
    public static final int PASSED_PAWN_MG[][] = {
            {                                        },
            { 130, 131, 126, 116, 116, 126, 131, 130 },
            {  78,  78,  74,  64,  64,  74,  78,  78 },
            {  27,  27,  23,  13,  13,  23,  27,  27 },
            {   4,   5,   0,   0,   0,   0,   5,   4 },
            {   5,   6,   0,   0,   0,   0,   6,   5 },
            {   2,   2,   0,   0,   0,   0,   2,   2 },
            {                                        }
    };
    public static final int PASSED_PAWN_EG[][] = {
            {                                        },
            { 124, 125, 116, 113, 113, 116, 125, 124 },
            {  84,  85,  76,  74,  74,  76,  85,  84 },
            {  33,  34,  26,  23,  23,  26,  34,  33 },
            {  18,  19,  11,   8,   8,  11,  19,  18 },
            {  14,  15,   7,   4,   4,   7,  15,  14 },
            {  12,  13,   5,   2,   2,   5,  13,  12 },
            {                                        }
    };
    
    // King safety
    public static final int PAWN_SHELTER[][] = {
            {  -3, -21,  -5, -19, -19,  -5, -21,  -3 },
            {  12, -30, -22, -80, -80, -22, -30,  12 },
            {   9,  -5,   1, -32, -32,   1,  -5,   9 },
            {  19, -14,  15, -23, -23,  15, -14,  19 },
            {  28, -24,  -1, -25, -25,  -1, -24,  29 },
            {  45,  17,  11, -14, -14,  11,  17,  45 },
            {  39,  29,  36,  -6,  -6,  36,  29,  39 },  
            {                                        }
    };

    // Endgame
    public static final int[][] CORNER_PROXIMITY = {
            { 50, 45, 40, 35, 35, 40, 45, 50 },
            { 45, 35, 30, 25, 25, 30, 35, 45 },
            { 40, 30, 20, 15, 15, 20, 30, 40 },
            { 35, 25, 15, 10, 10, 15, 25, 35 },
            { 35, 25, 15, 10, 10, 15, 25, 35 },
            { 40, 30, 20, 15, 15, 20, 30, 40 },
            { 45, 35, 30, 25, 25, 30, 35, 45 },
            { 50, 45, 40, 35, 35, 40, 45, 50 }
    };
    public static final int[] KINGS_PROXIMITY = {0, 0, 50, 40, 30, 20, 10, 5};
    
    // Chebyshev distance lookup: Use DIST_LOOKUP[index1 - index2 + 0x77]
    public static final int[] DIST_LOOKUP = {
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 0,
            7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 0,
            7, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 7, 0,
            7, 6, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 6, 7, 0,
            7, 6, 5, 4, 3, 3, 3, 3, 3, 3, 3, 4, 5, 6, 7, 0,
            7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 0,
            7, 6, 5, 4, 3, 2, 1, 1, 1, 2, 3, 4, 5, 6, 7, 0,
            7, 6, 5, 4, 3, 2, 1, 0, 1, 2, 3, 4, 5, 6, 7, 0,
            7, 6, 5, 4, 3, 2, 1, 1, 1, 2, 3, 4, 5, 6, 7, 0,
            7, 6, 5, 4, 3, 2, 2, 2, 2, 2, 3, 4, 5, 6, 7, 0,
            7, 6, 5, 4, 3, 3, 3, 3, 3, 3, 3, 4, 5, 6, 7, 0,
            7, 6, 5, 4, 4, 4, 4, 4, 4, 4, 4, 4, 5, 6, 7, 0,
            7, 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 6, 7, 0,
            7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 7, 0,
            7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 0
    };
    
    // Square color lookup (0 = light square, 1 = dark square): Use COLOR_LOOKUP[index]
    public static final int[] COLOR_LOOKUP = {
            0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 1, 0, 1, 0, 1, 0
    };
}
