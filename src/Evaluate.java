/**
 * 
 * @author Dalton He
 * created 11-05-18
 * 
 */
public class Evaluate implements Types {
    private static int npm_w, npm_b; // non-pawn material
    
    // Score components
    private static int[]  material;  // material
    private static double imbalance; // imbalance
    private static int[]  psqt;      // piece-square tables
    private static int[]  pawns;     // pawns
    private static int[]  pieces;    // pieces
    private static int[]  mobility;  // mobility
    private static int[]  king;      // king safety
    
    // Piece counts
    private static int pieces_w,  pieces_b;
    private static int pawns_w,   pawns_b;
    private static int knights_w, knights_b;
    private static int bishops_w, bishops_b;
    private static int rooks_w,   rooks_b;
    private static int queens_w,  queens_b;
 
    private static int passers_w, passers_b; // number of passed pawns
    
    // number of blocked pawns on the central 4 files (C, D, E, and F)
    private static int blocked_pawns_w, blocked_pawns_b;
    
    // number of pawns in each file
    private static int[] pawn_count_w, pawn_count_b;
    
    // rank of least advanced pawn in each file
    private static int[] pawn_rank_w, pawn_rank_b;
    
    // pawn_color[light squares | dark squares]
    private static int[] pawn_color_w, pawn_color_b;
    
    private static int kp_dist_w, kp_dist_b; // min distance of king to friendly pawn
    
    // Squares excluded from mobility count. A square is excluded if it is:
    //   1) Protected by an enemy pawn
    //   2) Occupied by a friendly pawn on rank 2 or 3
    //   3) Occupied by a blocked friendly pawn
    //   4) Occupied by our king or queen
    private static boolean[] excluded_area_w, excluded_area_b;
    private static int opp_bishops; // used for detecting opposite color bishops
    
    /**
     * Initialize evaluation fields.
     */
    public static void initEval() {
        npm_w = 0; npm_b = 0;
        
        material  = new int[2];
        imbalance = 0;
        psqt      = new int[2];
        pawns     = new int[2];
        pieces    = new int[2];
        mobility  = new int[2];
        king      = new int[2];

        pieces_w  = 0; pieces_b  = 0;
        pawns_w   = 0; pawns_b   = 0;
        knights_w = 0; knights_b = 0;
        bishops_w = 0; bishops_b = 0;
        rooks_w   = 0; rooks_b   = 0;
        queens_w  = 0; queens_b  = 0;
        
        passers_w = 0; passers_b = 0;  
        
        blocked_pawns_w = 0; blocked_pawns_b = 0;
        
        pawn_count_w = new int[8]; pawn_count_b = new int[8];
        
        pawn_rank_w = new int[8]; pawn_rank_b = new int[8];
        for (int f = FILE_A; f <= FILE_H; f++) pawn_rank_b[f] = 7;

        pawn_color_w = new int[2]; pawn_color_b = new int[2];
        
        kp_dist_w = 8; kp_dist_b = 8;
        
        excluded_area_w = new boolean[120]; excluded_area_b = new boolean[120];
        
        opp_bishops = 0; 
    }

    /**
     * Returns the score of the position in centipawns. Scores are from white's perspective.
     */
    public static int staticEval(Position pos) {
        initEval();
        int[] board = pos.board;

        // First pass:
        //   - Piece counts
        //   - Material
        //   - Phase
        //   - Pawn structure
        //   - Mobility area
        for (int index : pos.pieces) {
            int piece = board[index];
            int rank  = (index >> 4);
            int file  = (index & 7);
            
            if (piece > 0) {
                if (piece == W_PAWN) {
                    pawns_w++;
                    pieces_w++;
                    pawn_count_w[file]++;
                    if (rank > pawn_rank_w[file]) pawn_rank_w[file] = rank;
                    pawn_color_w[COLOR_LOOKUP[index]]++;
                    if (file > FILE_B && file < FILE_G && board[index - 16] != 0) blocked_pawns_w++;
                    if (file != FILE_A) excluded_area_b[index - 17] = true;
                    if (file != FILE_H) excluded_area_b[index - 15] = true;
                    if (rank >= RANK_3 || board[index - 16] != 0) excluded_area_w[index] = true;
                    int dist = Position.dist(pos.w_king, index);
                    if (dist < kp_dist_w) kp_dist_w = dist;
                    material[MG] += VALUE_PAWN[MG];
                    material[EG] += VALUE_PAWN[EG];
                    psqt    [MG] += PAWN_PSQT_MG[rank][file];
                    psqt    [EG] += PAWN_PSQT_EG[rank][file];
                }
                else if (piece == W_KNIGHT) {
                    knights_w++;
                    pieces_w++;
                    material[MG] += VALUE_KNIGHT[MG];
                    material[EG] += VALUE_KNIGHT[EG];
                    psqt    [MG] += KNIGHT_PSQT_MG[rank][file];
                    psqt    [EG] += KNIGHT_PSQT_EG[rank][file];
                    npm_w        += VALUE_KNIGHT[MG];
                }
                else if (piece == W_BISHOP) {
                    bishops_w++;
                    pieces_w++;
                    opp_bishops  += COLOR_LOOKUP[index];
                    material[MG] += VALUE_BISHOP[MG];
                    material[EG] += VALUE_BISHOP[EG];
                    psqt    [MG] += BISHOP_PSQT_MG[rank][file];
                    psqt    [EG] += BISHOP_PSQT_EG[rank][file];
                    npm_w        += VALUE_BISHOP[MG];
                }
                else if (piece == W_ROOK) {
                    rooks_w++;
                    pieces_w++;
                    material[MG] += VALUE_ROOK[MG];
                    material[EG] += VALUE_ROOK[EG];
                    psqt    [MG] += ROOK_PSQT_MG[rank][file];
                    psqt    [EG] += ROOK_PSQT_EG[rank][file];
                    npm_w        += VALUE_ROOK[MG];
                }
                else if (piece == W_QUEEN) {
                    queens_w++;
                    pieces_w++;
                    excluded_area_w[index] = true;
                    material[MG] += VALUE_QUEEN[MG];
                    material[EG] += VALUE_QUEEN[EG];
                    psqt    [MG] += QUEEN_PSQT_MG[rank][file];
                    psqt    [EG] += QUEEN_PSQT_EG[rank][file];
                    npm_w        += VALUE_QUEEN[MG];
                }
                else { // piece == W_KING
                    pieces_w++;
                    excluded_area_w[index] = true;
                    psqt[MG] += KING_PSQT_MG[rank][file];
                    psqt[EG] += KING_PSQT_EG[rank][file];
                }
            }
            else { // piece < 0
                if (piece == B_PAWN) {
                    pawns_b++;
                    pieces_b++;
                    pawn_count_b[file]++;
                    if (rank < pawn_rank_b[file]) pawn_rank_b[file] = rank;
                    pawn_color_b[COLOR_LOOKUP[index]]++;
                    if (file > FILE_B && file < FILE_G && board[index + 16] != 0) blocked_pawns_b++;
                    if (file != FILE_A) excluded_area_w[index + 15] = true;
                    if (file != FILE_H) excluded_area_w[index + 17] = true;
                    if (rank <= RANK_6 || board[index + 16] != 0) excluded_area_b[index] = true;
                    int dist = Position.dist(pos.b_king, index);
                    if (dist < kp_dist_b) kp_dist_b = dist;
                    material[MG] -= VALUE_PAWN[MG];
                    material[EG] -= VALUE_PAWN[EG];
                    psqt    [MG] -= PAWN_PSQT_MG[7-rank][file];
                    psqt    [EG] -= PAWN_PSQT_EG[7-rank][file];
                }
                else if (piece == B_KNIGHT) {
                    knights_b++;
                    pieces_b++;
                    material[MG] -= VALUE_KNIGHT[MG];
                    material[EG] -= VALUE_KNIGHT[EG];
                    psqt    [MG] -= KNIGHT_PSQT_MG[7-rank][file];
                    psqt    [EG] -= KNIGHT_PSQT_EG[7-rank][file];
                    npm_b        += VALUE_KNIGHT[MG];
                }
                else if (piece == B_BISHOP) {
                    bishops_b++;
                    pieces_b++;
                    opp_bishops  += COLOR_LOOKUP[index];
                    material[MG] -= VALUE_BISHOP[MG];
                    material[EG] -= VALUE_BISHOP[EG];
                    psqt    [MG] -= BISHOP_PSQT_MG[7-rank][file];
                    psqt    [EG] -= BISHOP_PSQT_EG[7-rank][file];
                    npm_b        += VALUE_BISHOP[MG];
                }
                else if (piece == B_ROOK) {
                    rooks_b++;
                    pieces_b++;
                    material[MG] -= VALUE_ROOK[MG];
                    material[EG] -= VALUE_ROOK[EG];
                    psqt    [MG] -= ROOK_PSQT_MG[7-rank][file];
                    psqt    [EG] -= ROOK_PSQT_EG[7-rank][file];
                    npm_b        += VALUE_ROOK[MG];
                }
                else if (piece == B_QUEEN) {
                    queens_b++;
                    pieces_b++;
                    excluded_area_b[index] = true;
                    material[MG] -= VALUE_QUEEN[MG];
                    material[EG] -= VALUE_QUEEN[EG];
                    psqt    [MG] -= QUEEN_PSQT_MG[7-rank][file];
                    psqt    [EG] -= QUEEN_PSQT_EG[7-rank][file];
                    npm_b        += VALUE_QUEEN[MG];
                }
                else { // piece == B_KING
                    pieces_b++;
                    excluded_area_b[index] = true;
                    psqt[MG] -= KING_PSQT_MG[7-rank][file];
                    psqt[EG] -= KING_PSQT_EG[7-rank][file];
                }
            }
        }

        // KX vs K and KQ vs KR: Bonus for driving the enemy king to the edge of board and
        // for keeping distance between the two kings small.
        if (   (pieces_b == 1 && npm_w >= VALUE_ROOK[MG])
            || (pieces_w == 2 && queens_w == 1 && pieces_b == 2 && rooks_b == 1)) {
            int prox_corner = CORNER_PROXIMITY[pos.b_king >> 4][pos.b_king & 7];
            int prox_king   = KINGS_PROXIMITY[Position.dist(pos.w_king, pos.b_king)];
            return material[MG] + 10 * (prox_corner + prox_king);
        }
        if (   (pieces_w == 1 && npm_b >= VALUE_ROOK[MG])
            || (pieces_b == 2 && queens_b == 1 && pieces_w == 2 && rooks_w == 1)) {
            int prox_corner = CORNER_PROXIMITY[pos.w_king >> 4][pos.w_king & 7];
            int prox_king   = KINGS_PROXIMITY[Position.dist(pos.w_king, pos.b_king)];
            return material[MG] - 10 * (prox_corner + prox_king);
        }
        
        // Imbalance evaluation
        imbalanceEval();
        
        // Pawn shelter
        int shelter_w = shelterScore(pos.w_king, WHITE);
        int shelter_b = shelterScore(pos.b_king, BLACK);
        king[MG] += shelter_w - shelter_b;

        // Second pass: 
        //   - Mobility
        //   - Pieces
        //   - Pawns
        for (int index : pos.pieces) {
            int piece = board[index];
            if (Math.abs(piece) == KING) continue;
            
            int rank  = (index >> 4);
            int file  = (index & 7);
            int squares; // number of attacked squares in the mobility area
            
            if (piece > 0) {
                if (piece == W_PAWN) {
                    boolean opposed, passed, phalanx, doubled, isolated, backward;
                    // First flag the pawn
                    opposed  = (pawn_count_b[file] != 0 && rank > pawn_rank_b[file]);
                    passed   = (!opposed && (file == FILE_A || rank <= pawn_rank_b[file - 1])
                                         && (file == FILE_H || rank <= pawn_rank_b[file + 1]));
                    phalanx  = (board[index - 1] == W_PAWN || board[index + 1] == W_PAWN);
                    doubled  = (board[index + 16] == W_PAWN);
                    isolated = (   (file == FILE_A || pawn_count_w[file - 1] == 0)
                                && (file == FILE_H || pawn_count_w[file + 1] == 0));
                    backward = (!isolated && (file == FILE_A || rank > pawn_rank_w[file - 1])
                                          && (file == FILE_H || rank > pawn_rank_w[file + 1])
                                          && (   (board[index - 16] == B_PAWN)
                                              || (file == FILE_A || board[index - 33] == B_PAWN)
                                              || (file == FILE_H || board[index - 31] == B_PAWN)));
                    int supporters = ((file != FILE_A && board[index + 15] == W_PAWN) ? 1 : 0) +
                                     ((file != FILE_H && board[index + 17] == W_PAWN) ? 1 : 0);
                    
                    // candidate passer
                    if (   !passed && !opposed && supporters > 0
                        && (file == FILE_A || rank <= pawn_rank_b[file - 1] + 1)
                        && (file == FILE_H || rank <= pawn_rank_b[file + 1] + 1))
                        passed = true;

                    // Bonus for passed pawns depending on rank and file, and king proximity.
                    // Any pawn which is unopposed and cannot be contested by an enemy pawn is 
                    // considered passed.
                    if (passed) {
                        passers_w++;
                        int bonus_mg = PASSED_PAWN_MG[rank][file];
                        int bonus_eg = PASSED_PAWN_EG[rank][file];

                        if (rank <= RANK_4) {
                            int w = PASSED_DANGER[rank];

                            // distance from king to block square of pawn, capped at 5
                            int dist_w = Math.min(5, Position.dist(pos.w_king, index - 16));
                            int dist_b = Math.min(5, Position.dist(pos.b_king, index - 16));
                            bonus_eg += dist_b * w * 5;
                            bonus_eg -= dist_w * w * 2;

                            // If the block square is not the queening square, consider a
                            // second push
                            if (rank != RANK_7) {
                                dist_w = Math.min(5, Position.dist(pos.w_king, index - 32));
                                bonus_eg -= dist_w * w;
                            }
                            // If the pawn is free to advance, increase the bonus
                            if (board[index - 16] == 0) {
                                bonus_mg += w * 5;
                                bonus_eg += w * 5;
                            }
                        }
                        // Scale down bonus if there is a friendly pawn in front
                        if (board[index - 16] == W_PAWN) {
                            bonus_mg >>= 1;
                            bonus_eg >>= 1;
                        }
                        pawns[MG] += bonus_mg;
                        pawns[EG] += bonus_eg;
                    }
                    // Penalty for doubled pawns. Any pawn which has a friendly pawn directly
                    // behind it and is not supported diagonally is considered doubled.
                    if (doubled && supporters == 0) {
                        pawns[MG] += DOUBLED_PAWN[MG];
                        pawns[EG] += DOUBLED_PAWN[EG];
                    }
                    // Penalty for isolated pawns. Any pawn which has no friendly pawn on an
                    // adjacent file is considered isolated.
                    if (isolated) {
                        pawns[MG] += ISOLATED_PAWN[MG];
                        pawns[EG] += ISOLATED_PAWN[EG];
                    }
                    // Penalty for backward pawns. Any pawn behind all friendly pawns on adjacent
                    // files and which cannot be safely advanced is considered backward.
                    if (backward) {
                        pawns[MG] += BACKWARD_PAWN[MG];
                        pawns[EG] += BACKWARD_PAWN[EG];
                    }
                    // Penalty for weak, unopposed pawns. The penalty is only applied if the
                    // opponent has a rook or queen.
                    if (!opposed && (isolated || backward) && (rooks_b != 0 || queens_b != 0)) {
                        pawns[MG] += WEAK_PAWN[MG];
                        pawns[EG] += WEAK_PAWN[EG];
                    }
                    // Bonus for connected pawns. Any pawn which is supported diagonally or
                    // adjacent to a friendly pawn (phalanx) is considered connected. Bonus is
                    // adjusted based on rank, whether the pawn is in a phalanx, whether the
                    // pawn is opposed, and the number of supporting pawns. 
                    if (supporters > 0 || phalanx) {
                        int connectedBonus = CONNECTED_PAWN[rank];
                        if (phalanx) connectedBonus += PAWN_PHALANX[rank];
                        if (opposed) connectedBonus >>= 1;
                        connectedBonus += supporters *= SUPPORTED_PAWN;
                        // In the endgame only pawns on the 4th through 7th ranks receive the bonus.
                        pawns[MG] += connectedBonus;
                        if (rank <= RANK_4) pawns[EG] += connectedBonus;
                    }
                }
                else if (piece == W_KNIGHT) {
                    // Penalty if the knight is far from the king.
                    int dist = Position.dist(pos.w_king, index);
                    pieces[MG] -= dist * KING_PROTECTOR;
                    pieces[EG] -= dist * KING_PROTECTOR;

                    // Knight mobility
                    squares = knightMobility(board, excluded_area_w, index);
                    mobility[MG] += KNIGHT_MOB_MG[squares];
                    mobility[EG] += KNIGHT_MOB_EG[squares];
                }
                else if (piece == W_BISHOP) {
                    // Penalty for a trapped bishop. This is to prevent a pawn grab such as Bxa7,
                    // after which b6 traps the bishop.
                    if (index == SQ_a7 && board[SQ_b6] == B_PAWN && board[SQ_c7] == B_PAWN)
                        pieces[MG] += TRAPPED_BISHOP;
                    else if (index == SQ_h7 && board[SQ_g6] == B_PAWN && board[SQ_f7] == B_PAWN)
                        pieces[MG] += TRAPPED_BISHOP;

                    // Penalty if the bishop is far from the king.
                    int dist = Position.dist(pos.w_king, index);
                    pieces[MG] -= dist * KING_PROTECTOR;
                    pieces[EG] -= dist * KING_PROTECTOR;

                    // Penalty for the number of pawns on the same color square
                    // as the bishop. The penalty is increased for each blocked pawn on the
                    // central 4 files (C, D, E, and F).
                    int bishopPawns = pawn_color_w[COLOR_LOOKUP[index]];
                    pieces[MG] += bishopPawns * (blocked_pawns_w + 1) * BAD_BISHOP_PAWN[MG];
                    pieces[EG] += bishopPawns * (blocked_pawns_w + 1) * BAD_BISHOP_PAWN[EG];

                    // Bishop mobility
                    squares = sliderMobility(board, excluded_area_w, index, BISHOP_DELTA, "Qq");
                    mobility[MG] += BISHOP_MOB_MG[squares];
                    mobility[EG] += BISHOP_MOB_EG[squares];
                }
                else if (piece == W_ROOK) {
                    // Bonus for rooks on open and semi-open files. A file without any pawns of 
                    // either color is considered open. A file with an enemy pawn but no friendly
                    // pawn is considered semi-open.
                    if (pawn_count_w[file] == 0) {
                        if (pawn_count_b[file] == 0) {
                            pieces[MG] += ROOK_OPEN_FILE[MG];
                            pieces[EG] += ROOK_OPEN_FILE[EG];
                        }
                        else {
                            pieces[MG] += ROOK_SEMI_FILE[MG];
                            pieces[EG] += ROOK_SEMI_FILE[EG];
                        }
                    }
                    // Bonus for a rook on the 7th rank with the enemy king on the 8th rank.
                    if (rank == RANK_7 && (pos.b_king >> 4) == RANK_8) {
                        pieces[MG] += ROOK_ON_7TH[MG];
                        pieces[EG] += ROOK_ON_7TH[EG];
                    }
                    // Penalty for a rook trapped by its own uncastled king.
                    if (   (index == SQ_a1 || index == SQ_a2 || index == SQ_b1 || index == SQ_b2)
                        && (pos.w_king == SQ_c1 || pos.w_king == SQ_b1))
                        pieces[MG] += TRAPPED_ROOK;
                    else if (   (index == SQ_h1 || index == SQ_h2 || index == SQ_g1 || index == SQ_g2)
                             && (pos.w_king == SQ_g1 || pos.w_king == SQ_f1))
                        pieces[MG] += TRAPPED_ROOK;

                    // Rook mobility
                    squares = sliderMobility(board, excluded_area_w, index, ROOK_DELTA, "QqR");
                    mobility[MG] += ROOK_MOB_MG[squares];
                    mobility[EG] += ROOK_MOB_EG[squares];
                }
                else if (piece == W_QUEEN) {
                    // Bonus for a queen on the 7th rank with the enemy king on the 8th rank.
                    if (rank == RANK_7 && (pos.b_king >> 4) == RANK_8) {
                        pieces[MG] += QUEEN_ON_7TH[MG];
                        pieces[EG] += QUEEN_ON_7TH[EG];
                    }
                    // Queen mobility
                    squares = sliderMobility(board, excluded_area_w, index, QUEEN_DELTA, "");
                    mobility[MG] += QUEEN_MOB_MG[squares];
                    mobility[EG] += QUEEN_MOB_EG[squares];
                }
            }
            else { // piece < 0
                if (piece == B_PAWN) {
                    boolean opposed, passed, phalanx, doubled, isolated, backward;
                    opposed  = (pawn_count_w[file] != 0 && rank < pawn_rank_w[file]);
                    passed   = (!opposed && (file == FILE_A || rank >= pawn_rank_w[file - 1])
                                         && (file == FILE_H || rank >= pawn_rank_w[file + 1]));
                    phalanx  = (board[index - 1] == B_PAWN || board[index + 1] == B_PAWN);
                    doubled  = (board[index - 16] == B_PAWN);
                    isolated = (   (file == FILE_A || pawn_count_b[file - 1] == 0)
                                && (file == FILE_H || pawn_count_b[file + 1] == 0));
                    backward = (!isolated && (file == FILE_A || rank < pawn_rank_b[file - 1])
                                          && (file == FILE_H || rank < pawn_rank_b[file + 1])
                                          && (   (board[index + 16] == W_PAWN)
                                              || (file == FILE_A || board[index + 31] == W_PAWN)
                                              || (file == FILE_H || board[index + 33] == W_PAWN)));
                    int supporters = ((file != FILE_A && board[index - 17] == B_PAWN) ? 1 : 0) +
                                 ((file != FILE_H && board[index - 15] == B_PAWN) ? 1 : 0);
                    
                    if (   !passed && !opposed && supporters > 0
                        && (file == FILE_A || rank >= pawn_rank_w[file - 1] - 1)
                        && (file == FILE_H || rank >= pawn_rank_w[file + 1] - 1))
                        passed = true;

                    if (passed) {
                        passers_b++;
                        int bonus_mg = PASSED_PAWN_MG[7-rank][file];
                        int bonus_eg = PASSED_PAWN_EG[7-rank][file];

                        if (rank >= RANK_5) {
                            int w = PASSED_DANGER[7-rank];

                            int dist_b = Math.min(5, Position.dist(pos.b_king, index + 16));
                            int dist_w = Math.min(5, Position.dist(pos.w_king, index + 16));
                            bonus_eg += dist_w * w * 5;
                            bonus_eg -= dist_b * w * 2;

                            if (rank != RANK_2) {
                                dist_b = Math.min(5, Position.dist(pos.b_king, index + 32));
                                bonus_eg -= dist_b * w;
                            }
                            if (board[index + 16] == 0) {
                                bonus_mg += w * 5;
                                bonus_eg += w * 5;
                            }
                        }
                        if (board[index + 16] == B_PAWN) {
                            bonus_mg >>= 1;
                            bonus_eg >>= 1;
                        }
                        pawns[MG] -= bonus_mg;
                        pawns[EG] -= bonus_eg;
                    }
                    if (doubled && supporters == 0) {
                        pawns[MG] -= DOUBLED_PAWN[MG];
                        pawns[EG] -= DOUBLED_PAWN[EG];
                    }
                    if (isolated) {
                        pawns[MG] -= ISOLATED_PAWN[MG];
                        pawns[EG] -= ISOLATED_PAWN[EG];
                    }
                    if (backward) {
                        pawns[MG] -= BACKWARD_PAWN[MG];
                        pawns[EG] -= BACKWARD_PAWN[EG];
                    }
                    if (!opposed && (isolated || backward) && (rooks_w != 0 || queens_w != 0)) {
                        pawns[MG] -= WEAK_PAWN[MG];
                        pawns[EG] -= WEAK_PAWN[EG];
                    }
                    if (supporters > 0 || phalanx) {
                        int connected_bonus = CONNECTED_PAWN[7-rank];
                        if (phalanx) connected_bonus += PAWN_PHALANX[7-rank];
                        if (opposed) connected_bonus >>= 1;
                        connected_bonus += supporters * SUPPORTED_PAWN;

                        pawns[MG] -= connected_bonus;
                        if (rank >= RANK_5) pawns[EG] -= connected_bonus;
                    }
                }
                else if (piece == B_KNIGHT) {
                    int dist = Position.dist(pos.b_king, index);
                    pieces[MG] += dist * KING_PROTECTOR;
                    pieces[EG] += dist * KING_PROTECTOR;

                    squares = knightMobility(board, excluded_area_b, index);
                    mobility[MG] -= KNIGHT_MOB_MG[squares];
                    mobility[EG] -= KNIGHT_MOB_EG[squares];
                }
                else if (piece == B_BISHOP) {
                    if (index == SQ_a2 && board[SQ_b3] == W_PAWN && board[SQ_c2] == W_PAWN)
                        pieces[MG] -= TRAPPED_BISHOP;
                    else if (index == SQ_h2 && board[SQ_g3] == W_PAWN && board[SQ_f2] == W_PAWN)
                        pieces[MG] -= TRAPPED_BISHOP;

                    int dist = Position.dist(pos.b_king, index);
                    pieces[MG] += dist * KING_PROTECTOR;
                    pieces[EG] += dist * KING_PROTECTOR;

                    int bishopPawns = pawn_color_b[COLOR_LOOKUP[index]];
                    pieces[MG] -= bishopPawns * (blocked_pawns_b + 1) * BAD_BISHOP_PAWN[MG];
                    pieces[EG] -= bishopPawns * (blocked_pawns_b + 1) * BAD_BISHOP_PAWN[EG];

                    squares = sliderMobility(board, excluded_area_b, index, BISHOP_DELTA, "Qq");
                    mobility[MG] -= BISHOP_MOB_MG[squares];
                    mobility[EG] -= BISHOP_MOB_EG[squares];
                }
                else if (piece == B_ROOK) {
                    if (pawn_count_b[file] == 0) {
                        if (pawn_count_w[file] == 0) {
                            pieces[MG] -= ROOK_OPEN_FILE[MG];
                            pieces[EG] -= ROOK_OPEN_FILE[EG];
                        }
                        else {
                            pieces[MG] -= ROOK_SEMI_FILE[MG];
                            pieces[EG] -= ROOK_SEMI_FILE[EG];
                        }
                    }
                    if (rank == RANK_2 && (pos.w_king >> 4) == RANK_1) {
                        pieces[MG] -= ROOK_ON_7TH[MG];
                        pieces[EG] -= ROOK_ON_7TH[EG];
                    }
                    if (   (index == SQ_a8 || index == SQ_a7 || index == SQ_b8 || index == SQ_b7)
                        && (pos.b_king == SQ_c8 || pos.b_king == SQ_b8))
                        pieces[MG] -= TRAPPED_ROOK;
                    else if (   (index == SQ_h8 || index == SQ_h7 || index == SQ_g8 || index == SQ_g7)
                             && (pos.b_king == SQ_g8 || pos.b_king == SQ_f8))
                        pieces[MG] -= TRAPPED_ROOK;

                    squares = sliderMobility(board, excluded_area_b, index, ROOK_DELTA, "Qqr");
                    mobility[MG] -= ROOK_MOB_MG[squares];
                    mobility[EG] -= ROOK_MOB_EG[squares];
                }
                else if (piece == B_QUEEN) {
                    if (rank == RANK_2 && (pos.w_king >> 4) == RANK_1) {
                        pieces[MG] -= QUEEN_ON_7TH[MG];
                        pieces[EG] -= QUEEN_ON_7TH[EG];
                    }
                    squares = sliderMobility(board, excluded_area_b, index, QUEEN_DELTA, "");
                    mobility[MG] -= QUEEN_MOB_MG[squares];
                    mobility[EG] -= QUEEN_MOB_EG[squares];
                }
            }
        }

        // Penalty if the king is far from its pawns.
        if (kp_dist_w != 8) king[EG] += kp_dist_w * KING_PAWN_DIST;
        if (kp_dist_b != 8) king[EG] -= kp_dist_b * KING_PAWN_DIST;

        // Sum the component scores
        double score_mg = material[MG] + psqt[MG] + imbalance + pawns[MG] + pieces[MG] + 
                          mobility[MG] + king[MG];
        double score_eg = material[EG] + psqt[EG] + imbalance + pawns[EG] + pieces[EG] + 
                          mobility[EG] + king[EG];
        
        // Bonus for having the right to move (middlegame only). This helps mitigate the parity
        // problem of scores alternating at even/odd depths.
        score_mg += TEMPO * pos.toMove;

        // Endgame scaling: Scale down scores of likely draws.
        if (npm_w + npm_b < MIDGAME_THRESH && score_eg != VALUE_DRAW) {
            int pawns_s = (score_eg > 0 ? pawns_w : pawns_b); // pawn count of the stronger side
            
            // Winning side has no pawns and insufficient material advantage
            if (pawns_s == 0 && Math.abs(npm_w - npm_b) <= VALUE_BISHOP[MG]) {
                if (score_eg > 0)
                    score_eg = (npm_w < VALUE_ROOK[MG] ? VALUE_DRAW :
                        (npm_b <= VALUE_BISHOP[MG] ? score_eg / 16.0 : score_eg / 5.0));
                else
                    score_eg = (npm_b < VALUE_ROOK[MG] ? VALUE_DRAW : 
                        (npm_w <= VALUE_BISHOP[MG] ? score_eg / 16.0 : score_eg / 5.0));
            }
            // Bishops of opposite colors: Scaling depends on the pawn asymmetry (number of 
            // unopposed pawns + number of passed pawns).
            else if (npm_w == VALUE_BISHOP[MG] && npm_b == VALUE_BISHOP[MG] && opp_bishops == 1) {
                int asymmetry = passers_w + passers_b;
                for (int f = FILE_A; f <= FILE_H; f++)
                    if (   (pawn_count_w[f] == 0 && pawn_count_b[f] != 0)
                        || (pawn_count_b[f] == 0 && pawn_count_w[f] != 0)) asymmetry++;
                score_eg *= ((asymmetry + 2) / 16.0);
            }
            else if ((bishops_w == 1 && bishops_b == 1 && opp_bishops == 1) && pawns_s <= 6)
                score_eg *= ((pawns_s + 20) / 32.0);
            else if (pawns_s <= 2)
                score_eg *= ((pawns_s + 5) / 8.0);
        }

        // Calculate the middlegame and endgame weights (range 0 to 1)
        int npm = Math.max(ENDGAME_THRESH, Math.min(npm_w + npm_b, MIDGAME_THRESH));
        double weight_mg = (npm - ENDGAME_THRESH) / (double) (MIDGAME_THRESH - ENDGAME_THRESH);
        double weight_eg = 1.0 - weight_mg;

        // Calculate the tapered evaluation. This is the interpolated score between separately
        // kept middlegame and endgame scores, weighted by the phase.
        int score_tapered = (int) (score_mg * weight_mg + score_eg * weight_eg);
        
        // debug
//        System.out.println("MATERIAL_MG: " + material[MG]);
//        System.out.println("MATERIAL_EG: " + material[EG]);
//        System.out.println("psqt[MG]:    " + psqt[MG]);
//        System.out.println("psqt[EG]:    " + psqt[EG]);
//        System.out.println("IMBALANCE:   " + imbalance);
//        System.out.println("pawns[MG]:   " + pawns[MG]);
//        System.out.println("pawns[EG]:   " + pawns[EG]);
//        System.out.println("pieces[MG]:  " + pieces[MG]);
//        System.out.println("pieces[EG]:  " + pieces[EG]);
//        System.out.println("MOBILITY_MG: " + mobility[MG]);
//        System.out.println("MOBILITY_EG: " + mobility[EG]);
//        System.out.println("SPACE:       " + space);
//        System.out.println("king[MG]:    " + king[MG]);
//        System.out.println("king[EG]:    " + king[EG]);
        
        return score_tapered;
    }
    
    /**
     * Sets the imbalance score of the position being evaluated.
     */
    private static void imbalanceEval() {        
        if (pawns_w   > 0) imbalance += pawns_w   * (  pawns_w   * P_WITH_P );
        if (knights_w > 0) imbalance += knights_w * (  pawns_w   * N_WITH_P
                                                     + knights_w * N_WITH_N
                                                     + pawns_b   * N_VS_P   );
        if (bishops_w > 0) imbalance += bishops_w * (  pawns_w   * B_WITH_P
                                                     + knights_w * B_WITH_N
                                                     + pawns_b   * B_VS_P
                                                     + knights_b * B_VS_N   );
        if (rooks_w   > 0) imbalance += rooks_w   * (  pawns_w   * R_WITH_P
                                                     + knights_w * R_WITH_N
                                                     + bishops_w * R_WITH_B
                                                     + rooks_w   * R_WITH_R
                                                     + pawns_b   * R_VS_P
                                                     + knights_b * R_VS_N
                                                     + bishops_b * R_VS_B   );
        if (queens_w  > 0) imbalance += queens_w  * (  pawns_w   * Q_WITH_P
                                                     + knights_w * Q_WITH_N
                                                     + bishops_w * Q_WITH_B
                                                     + rooks_w   * Q_WITH_R
                                                     + queens_w  * Q_WITH_Q
                                                     + pawns_b   * Q_VS_P
                                                     + knights_b * Q_VS_N
                                                     + bishops_b * Q_VS_B
                                                     + rooks_b   * Q_VS_R   );
        if (bishops_w > 1) imbalance +=             (  BISHOP_PAIR
                                                     + pawns_w   * P_WITH_BB
                                                     + knights_w * N_WITH_BB
                                                     + rooks_w   * R_WITH_BB
                                                     + queens_w  * Q_WITH_BB
                                                     - pawns_b   * P_VS_BB
                                                     - knights_b * N_VS_BB
                                                     - bishops_b * B_VS_BB
                                                     - rooks_b   * R_VS_BB
                                                     - queens_b  * Q_VS_BB  );
        if (pawns_b   > 0) imbalance -= pawns_b   * (  pawns_b   * P_WITH_P );
        if (knights_b > 0) imbalance -= knights_b * (  pawns_b   * N_WITH_P
                                                     + knights_b * N_WITH_N
                                                     + pawns_w   * N_VS_P   );
        if (bishops_b > 0) imbalance -= bishops_b * (  pawns_b   * B_WITH_P
                                                     + knights_b * B_WITH_N
                                                     + pawns_w   * B_VS_P
                                                     + knights_w * B_VS_N   );
        if (rooks_b   > 0) imbalance -= rooks_b   * (  pawns_b   * R_WITH_P
                                                     + knights_b * R_WITH_N
                                                     + bishops_b * R_WITH_B
                                                     + rooks_b   * R_WITH_R
                                                     + pawns_w   * R_VS_P
                                                     + knights_w * R_VS_N
                                                     + bishops_w * R_VS_B   );
        if (queens_b  > 0) imbalance -= queens_b  * (  pawns_b   * Q_WITH_P
                                                     + knights_b * Q_WITH_N
                                                     + bishops_b * Q_WITH_B
                                                     + rooks_b   * Q_WITH_R
                                                     + queens_b  * Q_WITH_Q
                                                     + pawns_w   * Q_VS_P
                                                     + knights_w * Q_VS_N
                                                     + bishops_w * Q_VS_B
                                                     + rooks_w   * Q_VS_R   );
        if (bishops_b > 1) imbalance -=             (  BISHOP_PAIR
                                                     + pawns_b   * P_WITH_BB
                                                     + knights_b * N_WITH_BB
                                                     + rooks_b   * R_WITH_BB
                                                     + queens_b  * Q_WITH_BB
                                                     - pawns_w   * P_VS_BB
                                                     - knights_w * N_VS_BB
                                                     - bishops_w * B_VS_BB
                                                     - rooks_w   * R_VS_BB
                                                     - queens_w  * Q_VS_BB  );
        imbalance = (int) imbalance;
    }
    
    /**
     * Returns the pawn shelter score for the given index and side.
     */
    public static int shelterScore(int index, int side) {
        int rank = (index >> 4);
        int file = Math.max(1, Math.min(6, index & 7));
        int score = 0;
        for (int f = file - 1; f <= file + 1; f++) {
            if (side == WHITE) {
                if (pawn_count_w[f] > 0 && rank > pawn_rank_w[f])
                    score += PAWN_SHELTER[pawn_rank_w[f]][f];
                else
                    score += PAWN_SHELTER[0][f];
            }
            else {
                if (pawn_count_b[f] > 0 && rank < pawn_rank_b[f])
                    score += PAWN_SHELTER[7 - pawn_rank_b[f]][f];
                else
                    score += PAWN_SHELTER[0][f];
            }
        }
        return score;
    }
    
    /**
     * Returns the number of attacked squares not in the given excluded area for a knight on the
     * given start index.
     */
    private static int knightMobility(int[] board, boolean[] excludedArea, int start) {
        int count = 0;
        for (int d : KNIGHT_DELTA) {
            int target = start + d;
            if (Position.isLegalIndex(target) && !excludedArea[target]) count++;
        }
        return count;
    }

    /**
     * Returns the number of attacked squares not in the given excluded area for a sliding piece
     * on the given start index. Also counts attacks through given 'x-ray' pieces.
     */
    private static int sliderMobility(int[] board, boolean[] excludedArea, int start, int[] delta, String xray) {
        int count = 0;
        for (int d : delta) {
            int target = start + d;
            while (Position.isLegalIndex(target)) {
                int piece = board[target];
                if (!excludedArea[target]) count++;
                if (piece != 0 && xray.indexOf(PIECE_STR.charAt(piece + 6)) == -1) break;
                target += d;
            }
        }
        return count;
    }
}