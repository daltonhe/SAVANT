/**
 * 
 * @author Dalton He
 * created 11-05-18
 * 
 */
public class Evaluate implements Types {
    private static int phase;        // game phase weight
    private static int npm_w, npm_b; // non-pawn material
    
    // Score components
    private static int[] material;  // material
    private static int   imbalance; // imbalance
    private static int[] psqt;      // piece-square tables
    private static int[] pawns;     // pawns
    private static int[] pieces;    // pieces
    private static int[] mobility;  // mobility
    private static int   space;     // space advantage (opening only)
    private static int[] king;      // king safety
    
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
    
    // pawn_file[file A..H][number of pawns on file | rank of least advanced pawn]
    private static int[][] pawn_file_w, pawn_file_b;
    
    // pawn_color[pawns on light squares | dark squares]
    private static int[] pawn_color_w, pawn_color_b;
    
    private static int kp_dist_w, kp_dist_b; // minimal distance of king to friendly pawns
    
    // Area excluded from mobility. A square is excluded if it is:
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
        phase = 0;
        npm_w = 0; npm_b = 0;
        
        material  = new int[2];
        imbalance = 0;
        psqt      = new int[2];
        pawns     = new int[2];
        pieces    = new int[2];
        mobility  = new int[2];
        space     = 0;
        king      = new int[2];

        pieces_w  = 0; pieces_b  = 0;
        pawns_w   = 0; pawns_b   = 0;
        knights_w = 0; knights_b = 0;
        bishops_w = 0; bishops_b = 0;
        rooks_w   = 0; rooks_b   = 0;
        queens_w  = 0; queens_b  = 0;
        
        passers_w = 0; passers_b = 0;  
        
        blocked_pawns_w = 0; blocked_pawns_b = 0;
        
        pawn_file_w = new int[8][2]; pawn_file_b = new int[8][2];
        for (int i = 0; i < 8; i++) pawn_file_b[i][1] = 7;

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
        for (int index : pos.pieceList) {
            int piece = board[index];
            int rank  = (index >> 4);
            int file  = (index & 7);

            switch (piece) {
            case W_PAWN:
                pawns_w++;
                pawn_file_w[file][0]++;
                if (rank > pawn_file_w[file][1]) pawn_file_w[file][1] = rank;
                pawn_color_w[COLOR_LOOKUP[index]]++;
                if (file > 1 && file < 6 && board[index - 16] != PIECE_NONE)
                    blocked_pawns_w++;
                if (file != 0) excluded_area_b[index - 17] = true;
                if (file != 7) excluded_area_b[index - 15] = true;
                if (rank == 6 || rank == 5 || board[index - 16] != PIECE_NONE)
                    excluded_area_w[index] = true;
                int dist = Position.dist(pos.king_pos_w, index);
                if (dist < kp_dist_w) kp_dist_w = dist;
                material[MG] += VALUE_PAWN[MG];
                material[EG] += VALUE_PAWN[EG];
                psqt    [MG] += PAWN_PSQT_MG[rank][file];
                psqt    [EG] += PAWN_PSQT_EG[rank][file];
                break;

            case B_PAWN:
                pawns_b++;
                pawn_file_b[file][0]++;
                if (rank < pawn_file_b[file][1]) pawn_file_b[file][1] = rank;
                pawn_color_b[COLOR_LOOKUP[index]]++;
                if (file > 1 && file < 6 && board[index + 16] != PIECE_NONE)
                    blocked_pawns_b++;
                if (file != 0) excluded_area_w[index + 15] = true;
                if (file != 7) excluded_area_w[index + 17] = true;
                if (rank == 1 || rank == 2 || board[index + 16] != PIECE_NONE)
                    excluded_area_b[index] = true;
                dist = Position.dist(pos.king_pos_b, index);
                if (dist < kp_dist_b) kp_dist_b = dist;
                material[MG] -= VALUE_PAWN[MG];
                material[EG] -= VALUE_PAWN[EG];
                psqt    [MG] -= PAWN_PSQT_MG[7-rank][file];
                psqt    [EG] -= PAWN_PSQT_EG[7-rank][file];
                break;

            case W_KNIGHT:
                knights_w++;
                material[MG] += VALUE_KNIGHT[MG];
                material[EG] += VALUE_KNIGHT[EG];
                psqt    [MG] += KNIGHT_PSQT_MG[rank][file];
                psqt    [EG] += KNIGHT_PSQT_EG[rank][file];
                npm_w        += VALUE_KNIGHT[MG];
                phase        += PHASE_WT_MINOR;
                break;

            case B_KNIGHT:
                knights_b++;
                material[MG] -= VALUE_KNIGHT[MG];
                material[EG] -= VALUE_KNIGHT[EG];
                psqt    [MG] -= KNIGHT_PSQT_MG[7-rank][file];
                psqt    [EG] -= KNIGHT_PSQT_EG[7-rank][file];
                npm_b        += VALUE_KNIGHT[MG];
                phase        += PHASE_WT_MINOR;
                break;

            case W_BISHOP:
                bishops_w++;
                opp_bishops  += COLOR_LOOKUP[index];
                material[MG] += VALUE_BISHOP[MG];
                material[EG] += VALUE_BISHOP[EG];
                psqt    [MG] += BISHOP_PSQT_MG[rank][file];
                psqt    [EG] += BISHOP_PSQT_EG[rank][file];
                npm_w        += VALUE_BISHOP[MG];
                phase        += PHASE_WT_MINOR;
                break;

            case B_BISHOP:
                bishops_b++;
                opp_bishops  += COLOR_LOOKUP[index];
                material[MG] -= VALUE_BISHOP[MG];
                material[EG] -= VALUE_BISHOP[EG];
                psqt    [MG] -= BISHOP_PSQT_MG[7-rank][file];
                psqt    [EG] -= BISHOP_PSQT_EG[7-rank][file];
                npm_b        += VALUE_BISHOP[MG];
                phase        += PHASE_WT_MINOR;
                break;

            case W_ROOK:
                rooks_w++;
                material[MG] += VALUE_ROOK[MG];
                material[EG] += VALUE_ROOK[EG];
                psqt    [MG] += ROOK_PSQT_MG[rank][file];
                psqt    [EG] += ROOK_PSQT_EG[rank][file];
                npm_w        += VALUE_ROOK[MG];
                phase        += PHASE_WT_ROOK;
                break;

            case B_ROOK:
                rooks_b++;
                material[MG] -= VALUE_ROOK[MG];
                material[EG] -= VALUE_ROOK[EG];
                psqt    [MG] -= ROOK_PSQT_MG[7-rank][file];
                psqt    [EG] -= ROOK_PSQT_EG[7-rank][file];
                npm_b        += VALUE_ROOK[MG];
                phase        += PHASE_WT_ROOK;
                break;

            case W_QUEEN:
                queens_w++;
                excluded_area_w[index] = true;
                material[MG] += VALUE_QUEEN[MG];
                material[EG] += VALUE_QUEEN[EG];
                psqt    [MG] += QUEEN_PSQT_MG[rank][file];
                psqt    [EG] += QUEEN_PSQT_EG[rank][file];
                npm_w        += VALUE_QUEEN[MG];
                phase        += PHASE_WT_QUEEN;
                break;

            case B_QUEEN:
                queens_b++;
                excluded_area_b[index] = true;
                material[MG] -= VALUE_QUEEN[MG];
                material[EG] -= VALUE_QUEEN[EG];
                psqt    [MG] -= QUEEN_PSQT_MG[7-rank][file];
                psqt    [EG] -= QUEEN_PSQT_EG[7-rank][file];
                npm_b        += VALUE_QUEEN[MG];
                phase        += PHASE_WT_QUEEN;
                break;

            case W_KING:
                excluded_area_w[index] = true;
                psqt[MG] += KING_PSQT_MG[rank][file];
                psqt[EG] += KING_PSQT_EG[rank][file];
                break;

            case B_KING:
                excluded_area_b[index] = true;
                psqt[MG] -= KING_PSQT_MG[7-rank][file];
                psqt[EG] -= KING_PSQT_EG[7-rank][file];
                break;
            }
        }

        // Sum piece counts
        pieces_w = pawns_w + knights_w + bishops_w + rooks_w + queens_w + 1;
        pieces_b = pawns_b + knights_b + bishops_b + rooks_b + queens_b + 1;

        // Mate with KX vs K: Bonus for driving the enemy king to the edge of board and
        // for keeping distance between the two kings small.
        if (pieces_b == 1 && pawns_w == 0) {
            int cornerProximity = CORNER_PROXIMITY[pos.king_pos_b >> 4][pos.king_pos_b & 7];
            int kingProximity   = KINGS_PROXIMITY[Position.dist(pos.king_pos_w, pos.king_pos_b)];
            return material[MG] + (cornerProximity + kingProximity) * 10;
        }
        if (pieces_w == 1 && pawns_b == 0) {
            int cornerProximity = CORNER_PROXIMITY[pos.king_pos_w >> 4][pos.king_pos_w & 7];
            int kingProximity   = KINGS_PROXIMITY[Position.dist(pos.king_pos_w, pos.king_pos_b)];
            return material[MG] - (cornerProximity + kingProximity) * 10;
        }
        
        // Imbalance evaluation
        imbalanceEval();

        // Space evaluation
        spaceEval(pos);
        
        // Pawn shelter
        int king_rank_w = (pos.king_pos_w >> 4);
        int king_file_w = Math.max(1, Math.min(6, pos.king_pos_w & 7));
        int shelter_w = 0;
        for (int f = king_file_w - 1; f <= king_file_w + 1; f++) {
            if (pawn_file_w[f][0] > 0 && king_rank_w > pawn_file_w[f][1])
                shelter_w += PAWN_SHELTER[pawn_file_w[f][1]][f];
            else
                shelter_w += PAWN_SHELTER[0][f];
        }
        king[MG] += shelter_w;
        
        int king_rank_b = (pos.king_pos_b >> 4);
        int king_file_b = Math.max(1, Math.min(6, pos.king_pos_b & 7));
        int shelter_b = 0;
        for (int f = king_file_b - 1; f <= king_file_b + 1; f++) {
            if (pawn_file_b[f][0] > 0 && king_rank_b < pawn_file_b[f][1])
                shelter_b += PAWN_SHELTER[7-pawn_file_b[f][1]][f];
            else
                shelter_b += PAWN_SHELTER[0][f];
        }
        king[MG] -= shelter_b;

        // Second pass: 
        //   - Mobility
        //   - Pieces
        //   - Pawns
        for (int index : pos.pieceList) {
            int piece = board[index];
            int rank  = (index >> 4);
            int file  = (index & 7);
            int squares = 0; // number of attacked squares in the mobility area

            switch (piece) {
            case W_PAWN:
                boolean opposed, passed, phalanx, doubled, isolated, backward;
                int supporters;

                // First flag the pawn
                opposed    = pawn_file_b[file][0] != 0 && rank > pawn_file_b[file][1];
                passed     = !opposed && (file == 0 || rank <= pawn_file_b[file - 1][1])
                                      && (file == 7 || rank <= pawn_file_b[file + 1][1]);
                phalanx    = board[index - 1] == W_PAWN || board[index + 1] == W_PAWN;
                doubled    = pawn_file_w[file][0] > 1;
                isolated   =    (file == 0 || pawn_file_w[file - 1][0] == 0)
                             && (file == 7 || pawn_file_w[file + 1][0] == 0);
                backward   = !isolated && (file == 0 || rank > pawn_file_w[file - 1][1])
                                       && (file == 7 || rank > pawn_file_w[file + 1][1])
                                       && (   (board[index - 16] == B_PAWN)
                                           || (file == 0 || board[index - 33] == B_PAWN)
                                           || (file == 7 || board[index - 31] == B_PAWN));
                supporters = ((file != 0 && board[index + 15] == W_PAWN) ? 1 : 0) +
                             ((file != 7 && board[index + 17] == W_PAWN) ? 1 : 0);

                // Bonus for passed pawns depending on rank and file, and king proximity.
                // Any pawn which is unopposed and cannot be contested by an enemy pawn is 
                // considered passed.
                if (passed) {
                    passers_w++;
                    int passed_bonus_mg = PASSED_PAWN_MG[rank][file];
                    int passed_bonus_eg = PASSED_PAWN_EG[rank][file];

                    if (rank <= 4) {
                        int rankBonus = PASSED_DANGER[rank];

                        // distance from both kings to block square of pawn, capped at 5
                        int kingDist_w = Math.min(5, Position.dist(pos.king_pos_w, index - 16));
                        int kingDist_b = Math.min(5, Position.dist(pos.king_pos_b, index - 16));
                        passed_bonus_eg += kingDist_b * rankBonus * 5;
                        passed_bonus_eg -= kingDist_w * rankBonus * 2;

                        // If the block square is not the queening square, consider a second push
                        if (rank != 1) {
                            kingDist_w = Math.min(5, Position.dist(pos.king_pos_w, index - 32));
                            passed_bonus_eg -= kingDist_w * rankBonus;
                        }
                        // If the pawn is free to advance, increase the bonus
                        if (board[index - 16] == PIECE_NONE) {
                            passed_bonus_mg += rankBonus * 5;
                            passed_bonus_eg += rankBonus * 5;
                        }
                    }
                    // Scale down bonus if there is a friendly pawn in front
                    if (board[index - 16] == W_PAWN) {
                        passed_bonus_mg /= 2;
                        passed_bonus_eg /= 2;
                    }
                    pawns[MG] += passed_bonus_mg;
                    pawns[EG] += passed_bonus_eg;
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
                // Penalty for backward pawns. Any pawn which is behind all pawns of the same
                // color on adjacent files and cannot be safely advanced is considered backward.
                if (backward) {
                    pawns[MG] += BACKWARD_PAWN[MG];
                    pawns[EG] += BACKWARD_PAWN[EG];
                }
                // Penalty for weak and unopposed pawns. The penalty is only applied if the
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
                    if (opposed) connectedBonus /= 2;
                    connectedBonus += supporters *= SUPPORTED_PAWN;
                    // In the endgame only pawns on the 4th through 7th ranks receive the bonus.
                    pawns[MG] += connectedBonus;
                    if (rank <= 4) pawns[EG] += connectedBonus;
                }
                break;

            case B_PAWN:
                opposed    = pawn_file_w[file][0] != 0 && rank < pawn_file_w[file][1];
                passed     = !opposed && (file == 0 || rank >= pawn_file_w[file - 1][1])
                                      && (file == 7 || rank >= pawn_file_w[file + 1][1]);
                phalanx    = board[index - 1] == B_PAWN || board[index + 1] == B_PAWN;
                doubled    = pawn_file_b[file][0] > 1;
                isolated   =    (file == 0 || pawn_file_b[file - 1][0] == 0)
                             && (file == 7 || pawn_file_b[file + 1][0] == 0);
                backward   = !isolated && (file == 0 || rank < pawn_file_b[file - 1][1])
                                       && (file == 7 || rank < pawn_file_b[file + 1][1])
                                       && (   (board[index + 16] == W_PAWN)
                                           || (file == 0 || board[index + 31] == W_PAWN)
                                           || (file == 7 || board[index + 33] == W_PAWN));
                supporters = ((file != 0 && board[index - 17] == B_PAWN) ? 1 : 0) +
                             ((file != 7 && board[index - 15] == B_PAWN) ? 1 : 0);

                if (passed) {
                    passers_b++;
                    int passed_bonus_mg = PASSED_PAWN_MG[7-rank][file];
                    int passed_bonus_eg = PASSED_PAWN_EG[7-rank][file];

                    if (rank >= 3) {
                        int rankBonus = PASSED_DANGER[7-rank];

                        int kingDist_b = Math.min(5, Position.dist(pos.king_pos_b, index + 16));
                        int kingDist_w = Math.min(5, Position.dist(pos.king_pos_w, index + 16));
                        passed_bonus_eg += kingDist_w * rankBonus * 5;
                        passed_bonus_eg -= kingDist_b * rankBonus * 2;

                        if (rank != 6) {
                            kingDist_b = Math.min(5, Position.dist(pos.king_pos_b, index + 32));
                            passed_bonus_eg -= kingDist_b * rankBonus;
                        }

                        if (board[index + 16] == PIECE_NONE) {
                            passed_bonus_mg += rankBonus * 5;
                            passed_bonus_eg += rankBonus * 5;
                        }
                    }
                    if (board[index + 16] == B_PAWN) {
                        passed_bonus_mg /= 2;
                        passed_bonus_eg /= 2;
                    }
                    pawns[MG] -= passed_bonus_mg;
                    pawns[EG] -= passed_bonus_eg;
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
                    if (opposed) connected_bonus /= 2;
                    connected_bonus += supporters * SUPPORTED_PAWN;

                    pawns[MG] -= connected_bonus;
                    if (rank >= 3) pawns[EG] -= connected_bonus;
                }
                break;

            case W_KNIGHT:
                // Penalty if the knight is far from the king
                int kingDist = Position.dist(pos.king_pos_w, index);
                pieces[MG] -= kingDist * KING_PROTECTOR;
                pieces[EG] -= kingDist * KING_PROTECTOR;

                // Knight mobility
                squares = mobilityAttack(board, excluded_area_w, index, KNIGHT_DELTA, false, "");
                mobility[MG] += KNIGHT_MOB_MG[squares];
                mobility[EG] += KNIGHT_MOB_EG[squares];
                break;

            case B_KNIGHT:
                kingDist = Position.dist(pos.king_pos_b, index);
                pieces[MG] += kingDist * KING_PROTECTOR;
                pieces[EG] += kingDist * KING_PROTECTOR;

                squares = mobilityAttack(board, excluded_area_b, index, KNIGHT_DELTA, false, "");
                mobility[MG] -= KNIGHT_MOB_MG[squares];
                mobility[EG] -= KNIGHT_MOB_EG[squares];
                break;

            case W_BISHOP:
                // Penalty for a trapped bishop. This is to prevent the pawn grab Bxa7, after
                // which b6 traps the bishop.
                if (index == SQ_a7 && board[SQ_b6] == B_PAWN && board[SQ_c7] == B_PAWN)
                    pieces[MG] += TRAPPED_BISHOP;
                else if (index == SQ_h7 && board[SQ_g6] == B_PAWN && board[SQ_f7] == B_PAWN)
                    pieces[MG] += TRAPPED_BISHOP;

                // Penalty if the bishop is far from the king
                kingDist = Position.dist(pos.king_pos_w, index);
                pieces[MG] -= kingDist * 3;
                pieces[EG] -= kingDist * 3;

                // Penalty for the number of pawns on the same color square
                // as the bishop. The penalty is increased for each blocked pawn on the
                // central 4 files (C, D, E, and F).
                int bishopPawns = pawn_color_w[COLOR_LOOKUP[index]];
                pieces[MG] += bishopPawns * (blocked_pawns_w + 1) * BAD_BISHOP_PAWN[MG];
                pieces[EG] += bishopPawns * (blocked_pawns_w + 1) * BAD_BISHOP_PAWN[EG];

                // Bishop mobility
                squares = mobilityAttack(board, excluded_area_w, index, BISHOP_DELTA, true, "Qq");
                mobility[MG] += BISHOP_MOB_MG[squares];
                mobility[EG] += BISHOP_MOB_EG[squares];
                break;

            case B_BISHOP:
                if (index == SQ_a2 && board[SQ_b3] == W_PAWN && board[SQ_c2] == W_PAWN)
                    pieces[MG] -= TRAPPED_BISHOP;
                else if (index == SQ_h2 && board[SQ_g3] == W_PAWN && board[SQ_f2] == W_PAWN)
                    pieces[MG] -= TRAPPED_BISHOP;

                kingDist = Position.dist(pos.king_pos_b, index);
                pieces[MG] += kingDist * 3;
                pieces[EG] += kingDist * 3;

                bishopPawns = pawn_color_b[COLOR_LOOKUP[index]];
                pieces[MG] -= bishopPawns * (blocked_pawns_b + 1) * BAD_BISHOP_PAWN[MG];
                pieces[EG] -= bishopPawns * (blocked_pawns_b + 1) * BAD_BISHOP_PAWN[EG];

                squares = mobilityAttack(board, excluded_area_b, index, BISHOP_DELTA, true, "Qq");
                mobility[MG] -= BISHOP_MOB_MG[squares];
                mobility[EG] -= BISHOP_MOB_EG[squares];
                break;

            case W_ROOK:
                // Bonus for rooks on open and semi-open files. A file without
                // any pawns of either  color is considered open. A file with an enemy
                // pawn but no friendly pawn is considered semi-open.
                if (pawn_file_w[file][0] == 0) {
                    if (pawn_file_b[file][0] == 0) {
                        pieces[MG] += ROOK_OPEN_FILE[MG];
                        pieces[EG] += ROOK_OPEN_FILE[EG];
                    }
                    else {
                        pieces[MG] += ROOK_SEMI_FILE[MG];
                        pieces[EG] += ROOK_SEMI_FILE[EG];
                    }
                }

                // Bonus for a rook on the 7th rank with the enemy king on the 8th rank
                if (rank == 1 && (pos.king_pos_b >> 4) == 0) {
                    pieces[MG] += ROOK_ON_7TH[MG];
                    pieces[EG] += ROOK_ON_7TH[EG];
                }

                // Penalty for a rook trapped by its own uncastled king
                if (   (index == SQ_a1 || index == SQ_a2 || index == SQ_b1)
                    && (pos.king_pos_w == SQ_c1 || pos.king_pos_w == SQ_b1))
                    pieces[MG] += TRAPPED_ROOK;
                else if (   (index == SQ_h1 || index == SQ_h2 || index == SQ_g1)
                         && (pos.king_pos_w == SQ_g1 || pos.king_pos_w == SQ_f1))
                    pieces[MG] += TRAPPED_ROOK;

                // Rook mobility
                squares = mobilityAttack(board, excluded_area_w, index, ROOK_DELTA, true, "QqR");
                mobility[MG] += ROOK_MOB_MG[squares];
                mobility[EG] += ROOK_MOB_EG[squares];
                break;

            case B_ROOK:
                if (pawn_file_b[file][0] == 0) {
                    if (pawn_file_w[file][0] == 0) {
                        pieces[MG] -= ROOK_OPEN_FILE[MG];
                        pieces[EG] -= ROOK_OPEN_FILE[EG];
                    }
                    else {
                        pieces[MG] -= ROOK_SEMI_FILE[MG];
                        pieces[EG] -= ROOK_SEMI_FILE[EG];
                    }
                }

                if (rank == 6 && (pos.king_pos_w >> 4) == 7) {
                    pieces[MG] -= ROOK_ON_7TH[MG];
                    pieces[EG] -= ROOK_ON_7TH[EG];
                }

                if (   (index == SQ_a8 || index == SQ_a7 || index == SQ_b8)
                    && (pos.king_pos_b == SQ_c8 || pos.king_pos_b == SQ_b8))
                    pieces[MG] -= TRAPPED_ROOK;
                else if (   (index == SQ_h8 || index == SQ_h7 || index == SQ_g8)
                         && (pos.king_pos_b == SQ_g8 || pos.king_pos_b == SQ_f8))
                    pieces[MG] -= TRAPPED_ROOK;

                squares = mobilityAttack(board, excluded_area_b, index, ROOK_DELTA, true, "Qqr");
                mobility[MG] -= ROOK_MOB_MG[squares];
                mobility[EG] -= ROOK_MOB_EG[squares];
                break;

            case W_QUEEN:
                // Bonus for a queen on the 7th rank with the enemy king on the 8th rank
                if (rank == 1 && (pos.king_pos_b >> 4) == 0) {
                    pieces[MG] += QUEEN_ON_7TH[MG];
                    pieces[EG] += QUEEN_ON_7TH[EG];
                }
                // Queen mobility
                squares = mobilityAttack(board, excluded_area_w, index, QUEEN_DELTA, true, "");
                mobility[MG] += QUEEN_MOB_MG[squares];
                mobility[EG] += QUEEN_MOB_EG[squares];
                break;

            case B_QUEEN:
                if (rank == 6 && (pos.king_pos_w >> 4) == 7) {
                    pieces[MG] -= QUEEN_ON_7TH[MG];
                    pieces[EG] -= QUEEN_ON_7TH[EG];
                }
                
                squares = mobilityAttack(board, excluded_area_b, index, QUEEN_DELTA, true, "");
                mobility[MG] -= QUEEN_MOB_MG[squares];
                mobility[EG] -= QUEEN_MOB_EG[squares];
                break;
            }
        }

        // Penalty if the king is far from its pawns
        if (kp_dist_w != 8) king[EG] += kp_dist_w * KING_PAWN_DIST;
        if (kp_dist_b != 8) king[EG] -= kp_dist_b * KING_PAWN_DIST;

        // Sum the component scores
        double score_mg = material[MG] + 
                          psqt    [MG] + 
                          imbalance    + 
                          pawns   [MG] + 
                          pieces  [MG] + 
                          mobility[MG] + 
                          space        + 
                          king    [MG];
        double score_eg = material[EG] + 
                          psqt    [EG] + 
                          imbalance    + 
                          pawns   [EG] + 
                          pieces  [EG] + 
                          mobility[EG] + 
                          king    [EG];
        
        // Give a small bonus for having the right to move (middlegame only)
        score_mg += TEMPO * pos.sideToMove;

        // Endgame scaling: Scale down scores of likely draws. Definite draws will be caught by a
        // check during search and don't need to be handled here.
        if (score_eg != VALUE_DRAW) {
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
                for (int i = 0; i < 8; i++)
                    if (   (pawn_file_w[i][0] == 0 && pawn_file_b[i][0] != 0)
                        || (pawn_file_b[i][0] == 0 && pawn_file_w[i][0] != 0))
                        asymmetry++;
                score_eg *= ((asymmetry + 2) / 16.0);
            }
            else if ((bishops_w == 1 && bishops_b == 1 && opp_bishops == 1) && pawns_s <= 6)
                score_eg *= ((pawns_s + 20) / 32.0);
            else if (pawns_s <= 2)
                score_eg *= ((pawns_s + 5) / 8.0);
        }

        // Calculate the middlegame and endgame weights (range 0 to 1)
        phase = Math.max(PHASE_EG, Math.min(phase, PHASE_MG));
        double weight_mg = (phase - PHASE_EG) / (double) (PHASE_MG - PHASE_EG);
        double weight_eg = 1.0 - weight_mg;

        // Calculate the tapered evaluation. This is the interpolated score between separately
        // kept middlegame and endgame scores, weighted by the phase.
        int score_tapered = (int) (score_mg * weight_mg + score_eg * weight_eg);
        
        return score_tapered;
        
        // debug
        //System.out.println("MATERIAL_MG: " + material[MG]);
        //System.out.println("MATERIAL_EG: " + material[EG]);
        //System.out.println("psqt[MG]:    " + psqt[MG]);
        //System.out.println("psqt[EG]:    " + psqt[EG]);
        //System.out.println("IMBALANCE:   " + imbalance);
        //System.out.println("pawns[MG]:   " + pawns[MG]);
        //System.out.println("pawns[EG]:   " + pawns[EG]);
        //System.out.println("pieces[MG]:  " + pieces[MG]);
        //System.out.println("pieces[EG]:  " + pieces[EG]);
        //System.out.println("MOBILITY_MG: " + mobility[MG]);
        //System.out.println("MOBILITY_EG: " + mobility[EG]);
        //System.out.println("SPACE:       " + space);
        //System.out.println("king[MG]:    " + king[MG]);
        //System.out.println("king[EG]:    " + king[EG]);
        //System.out.println("TEMPO:       " + tempo);
    }
    
    /**
     * Sets the imbalance score of the position being evaluated.
     */
    private static void imbalanceEval() {
        // Bonus for having the bishop pair.
        if (bishops_w >= 2) {
            imbalance += BISHOP_PAIR;
            // The bishop pair is worth less with the queen on board
            if (queens_w >= 1) imbalance -= 6;
        }
        if (bishops_b >= 2) {
            imbalance -= BISHOP_PAIR;
            if (queens_b >= 1) imbalance += 6;
        }

        // Give a small penalty for having the knight pair
        if (knights_w >= 2) imbalance += REDUNDANT_KNIGHT;
        if (knights_b >= 2) imbalance -= REDUNDANT_KNIGHT;

        // Give penalties for redundant major pieces
        if (rooks_w >= 1) {
            imbalance += (rooks_w - 1) * REDUNDANT_ROOK;
            imbalance += queens_w * rooks_w * REDUNDANT_QUEEN;
            // Bonus to rook value for each enemy pawn
            imbalance += pawns_b;
        }
        if (rooks_b >= 1) {
            imbalance -= (rooks_b - 1) * REDUNDANT_ROOK;
            imbalance -= queens_b * rooks_b * REDUNDANT_QUEEN;
            imbalance -= pawns_w;
        }

        // Bonus to knights for having more pawns
        imbalance += knights_w * (5 - pawns_w) * KNIGHT_PAWN_SYNERGY;
        imbalance -= knights_b * (5 - pawns_b) * KNIGHT_PAWN_SYNERGY;

        // Bonus to queen value for each enemy pawn
        if (queens_w >= 1) imbalance += pawns_b * 3;
        if (queens_b >= 1) imbalance -= pawns_w * 3;
    }
    
    /**
     * Sets the space score of the given position.
     */
    private static void spaceEval(Position pos) {
        if (npm_w + npm_b < SPACE_THRESHOLD) return;
        
        int[] board = pos.board;
        // Count number of open files
        int openFiles = 0;
        for (int file = 0; file < 8; file++)
            if (pawn_file_w[file][0] == 0 && pawn_file_b[file][0] == 0)
                openFiles++;

        // Space area: Number of safe squares available for minor pieces on the central four
        // files on ranks 2 to 4. Safe squares one, two, or three squares behind a friendly
        // pawn are counted twice.
        int space_area_w = 0, space_area_b = 0;
        for (int file = 2; file <= 5; file++) {
            for (int rank = 1; rank <= 3; rank++) {
                int index = rank * 16 + file;
                if (   board[index] != B_PAWN 
                    && board[index + 15] != W_PAWN 
                    && board[index + 17] != W_PAWN) {
                    space_area_b++;
                    if (   board[index + 16] == B_PAWN
                        || board[index + 32] == B_PAWN
                        || board[index + 48] == B_PAWN)
                        space_area_b++;
                }
            }
            for (int rank = 4; rank <= 6; rank++) {
                int index = rank * 16 + file;
                if (   board[index] != W_PAWN 
                    && board[index - 15] != B_PAWN 
                    && board[index - 17] != B_PAWN) {
                    space_area_w++;
                    if (   board[index - 16] == W_PAWN
                        || board[index - 32] == W_PAWN
                        || board[index - 48] == W_PAWN)
                        space_area_w++;
                }
            }
        }
        
        // Weight by number of pieces - 2 * number of open files
        int weight_w = pieces_w - 2 * openFiles;
        int weight_b = pieces_b - 2 * openFiles;
        space += space_area_w * weight_w * weight_w / 33;
        space -= space_area_b * weight_b * weight_b / 33;
    }

    /**
     * Returns the number of attacked squares not in the excludedArea. Allow attacks through 
     * given 'x-ray' pieces.
     */
    private static int mobilityAttack(int[] board, boolean[] excludedArea, int start, int[] delta,
            boolean slider, String xray) {
        int count = 0;
        for (int i = 0; i < delta.length; i++) {
            int target = start + delta[i];
            while (Position.isLegalIndex(target)) {
                int piece = board[target];
                if (!excludedArea[target]) count++;
                if (!slider) break;
                if (   piece != PIECE_NONE 
                    && xray.indexOf(PIECE_STR.charAt(piece + 6)) == -1) break;
                target += delta[i];
            }
        }
        return count;
    }
}