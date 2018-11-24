/**
 * 
 * @author Dalton He
 * created 11-05-18
 * 
 */
public class Evaluate implements Types {
	
	/**
	 * Returns the static position evaluation in centipawns. Positive scores are good for White;
	 * negative scores are good for black.
	 * @param board - The board state to be evaluated
	 * @return        Score of the position
	 */
	public static int staticEval(Position pos) {
		int[] board = pos.board;
		
		int phase = 0;
		int npm_w = 0, npm_b = 0; // non-pawn material
		
		// Initialize score components
		int mat_mg    = 0, mat_eg    = 0; // material
		int imbal     = 0;				  // imbalance
		int psqt_mg   = 0, psqt_eg   = 0; // piece-square tables
		int pawns_mg  = 0, pawns_eg  = 0;
		int pieces_mg = 0, pieces_eg = 0;
		int mob_mg    = 0, mob_eg    = 0; // mobility
		int tempo     = TEMPO * pos.sideToMove;
		
		// Initialize piece counts
		int pawns_w   = 0, pawns_b   = 0;
		int knights_w = 0, knights_b = 0;
		int bishops_w = 0, bishops_b = 0;
		int rooks_w   = 0, rooks_b   = 0;
		int queens_w  = 0, queens_b  = 0;
		
		int bishopParity = 0; // used for determining opposite color bishops
		
		// pawn_file[file A..H][count | rank of least advanced pawn]
		int[][] pawn_file_w = new int[8][2]; 
		int[][] pawn_file_b = new int[8][2];
		for (int i = 0; i < 8; i++) pawn_file_b[i][1] = 7;
		
		// pawn_color[pawns on light squares | dark squares]
		int[] pawn_color_w = new int[2];
		int[] pawn_color_b = new int[2];
		
		// Area excluded from mobility. A square is excluded if it is:
		//   1) protected by an enemy pawn
		//   2) occupied by a friendly pawn on rank 2 or 3
		//   3) occupied by a blocked friendly pawn
		//   4) occupied by our king or queen
		boolean[] excluded_area_w = new boolean[120];
		boolean[] excluded_area_b = new boolean[120];
		
		// number of blocked pawns on the 4 center files (C, D, E, and F)
		int blocked_pawns_w = 0, blocked_pawns_b = 0;
		
		// First pass: piece count, material, phase, mobility area, pawn info
		for (int index : pos.pieceList) {
			
			int piece = board[index];
			int rank = index / 16;
			int file = index % 16;
			
			switch (piece) {
			case W_PAWN:
				pawns_w++;
				pawn_file_w[file][0]++;
				if (rank > pawn_file_w[file][1]) pawn_file_w[file][1] = rank;
				pawn_color_w[(file + rank) % 2]++;
				if (file > 1 && file < 6 && board[index - 16] != PIECE_NONE)
					blocked_pawns_w++;
				if (file != 0) excluded_area_b[index - 17] = true;
				if (file != 7) excluded_area_b[index - 15] = true;
				if (rank == 6 || rank == 5 || board[index - 16] != PIECE_NONE)
					excluded_area_w[index] = true;
				mat_mg  += PAWN_MG;
				mat_eg  += PAWN_EG;
				psqt_mg += PSQT_MG[PAWN][rank][file];
				psqt_eg += PSQT_EG[PAWN][rank][file];
				break;
				
			case B_PAWN:
				pawns_b++;
				pawn_file_b[file][0]++;
				if (rank < pawn_file_b[file][1]) pawn_file_b[file][1] = rank;
				pawn_color_b[(file + rank) % 2]++;
				if (file > 1 && file < 6 && board[index + 16] != PIECE_NONE)
					blocked_pawns_b++;
				if (file != 0) excluded_area_w[index + 15] = true;
				if (file != 7) excluded_area_w[index + 17] = true;
				if (rank == 1 || rank == 2 || board[index + 16] != PIECE_NONE)
					excluded_area_b[index] = true;
				mat_mg  -= PAWN_MG;
				mat_eg  -= PAWN_EG;
				psqt_mg -= PSQT_MG[PAWN][7 - rank][file];
				psqt_eg -= PSQT_EG[PAWN][7 - rank][file];
				break;
				
			case W_KNIGHT:
				knights_w++;
				mat_mg  += KNIGHT_MG;
				mat_eg  += KNIGHT_EG;
				npm_w   += KNIGHT_MG;
				phase   += PHASE_WT_MINOR;
				psqt_mg += PSQT_MG[KNIGHT][rank][file];
				psqt_eg += PSQT_EG[KNIGHT][rank][file];
				break;
				
			case B_KNIGHT:
				knights_b++;
				mat_mg  -= KNIGHT_MG;
				mat_eg  -= KNIGHT_EG;
				npm_b   += KNIGHT_MG;
				phase   += PHASE_WT_MINOR;
				psqt_mg -= PSQT_MG[KNIGHT][7 - rank][file];
				psqt_eg -= PSQT_EG[KNIGHT][7 - rank][file];
				break;
				
			case W_BISHOP:
				bishops_w++;
				bishopParity += (file + rank) % 2;
				mat_mg  += BISHOP_MG;
				mat_eg  += BISHOP_EG;
				npm_w   += BISHOP_MG;
				phase   += PHASE_WT_MINOR;
				psqt_mg += PSQT_MG[BISHOP][rank][file];
				psqt_eg += PSQT_EG[BISHOP][rank][file];
				break;
				
			case B_BISHOP:
				bishops_b++;
				bishopParity += (file + rank) % 2;
				mat_mg  -= BISHOP_MG;
				mat_eg  -= BISHOP_EG;
				npm_b   += BISHOP_MG;
				phase   += PHASE_WT_MINOR;
				psqt_mg -= PSQT_MG[BISHOP][7 - rank][file];
				psqt_eg -= PSQT_EG[BISHOP][7 - rank][file];
				break;
				
			case W_ROOK:
				rooks_w++;
				mat_mg  += ROOK_MG;
				mat_eg  += ROOK_EG;
				npm_w   += ROOK_MG;
				phase   += PHASE_WT_ROOK;
				psqt_mg += PSQT_MG[ROOK][rank][file];
				psqt_eg += PSQT_EG[ROOK][rank][file];
				break;
				
			case B_ROOK:
				rooks_b++;
				mat_mg  -= ROOK_MG;
				mat_eg  -= ROOK_EG;
				npm_b   += ROOK_MG;
				phase   += PHASE_WT_ROOK;
				psqt_mg -= PSQT_MG[ROOK][7 - rank][file];
				psqt_eg -= PSQT_EG[ROOK][7 - rank][file];
				break;
				
			case W_QUEEN:
				queens_w++;
				excluded_area_w[index] = true;
				mat_mg  += QUEEN_MG;
				mat_eg  += QUEEN_EG;
				npm_w   += QUEEN_MG;
				phase   += PHASE_WT_QUEEN;
				psqt_mg += PSQT_MG[QUEEN][rank][file];
				psqt_eg += PSQT_EG[QUEEN][rank][file];
				break;
				
			case B_QUEEN:
				queens_b++;
				excluded_area_b[index] = true;
				mat_mg  -= QUEEN_MG;
				mat_eg  -= QUEEN_EG;
				npm_b   += QUEEN_MG;
				phase   += PHASE_WT_QUEEN;
				psqt_mg -= PSQT_MG[QUEEN][7 - rank][file];
				psqt_eg -= PSQT_EG[QUEEN][7 - rank][file];
				break;
			
			case W_KING:
				excluded_area_w[index] = true;
				psqt_mg += PSQT_MG[KING][rank][file];
				psqt_eg += PSQT_EG[KING][rank][file];
				break;
			
			case B_KING:
				excluded_area_b[index] = true;
				psqt_mg -= PSQT_MG[KING][7 - rank][file];
				psqt_eg -= PSQT_EG[KING][7 - rank][file];
				break;
			}
		}
		
		// Mate with KX vs K. Give a bonus for driving the enemy king to the edge of board and
		// for keeping distance between the two kings small.
		if (pawns_b == 0 && knights_b == 0 && bishops_b == 0 && rooks_b == 0 && queens_b == 0) {
			int cornerProximity = EDGE_PROXIMITY[pos.king_pos_b / 16][pos.king_pos_b % 16];
			int kingDist = Position.dist(pos.king_pos_w, pos.king_pos_b);
			int kingProximity = KINGS_PROXIMITY[kingDist];
			return mat_mg + (cornerProximity + kingProximity) * 10;
		}
		if (pawns_w == 0 && knights_w == 0 && bishops_w == 0 && rooks_w == 0 && queens_w == 0) {
			int cornerProximity = EDGE_PROXIMITY[pos.king_pos_w / 16][pos.king_pos_w % 16];
			int kingDist = Position.dist(pos.king_pos_w, pos.king_pos_b);
			int kingProximity = KINGS_PROXIMITY[kingDist];
			return mat_mg - (cornerProximity + kingProximity) * 10;
		}
		
		// Return a lazy eval if the score is high
		int score_lazy = (mat_mg + psqt_mg + mat_eg + psqt_eg) / 2;
		if (Math.abs(score_lazy) > LAZY_THRESHOLD) return score_lazy;
		
		// Second pass: calculate mobility, and evaluate pieces and pawns.
		for (int index : pos.pieceList) {
			
			int piece = board[index];
			int rank = index / 16;
			int file = index % 16;
			int squares = 0; // number of attacked squares in the mobility area
			
			switch (piece) {
			case W_PAWN:
				boolean opposed, passed, phalanx, doubled, isolated, backward;
				int supporters;

				// First flag the pawn
				opposed    = pawn_file_b[file][0] != 0 && rank > pawn_file_b[file][1];
				passed     =    !opposed
							 && (file == 0 || rank <= pawn_file_b[file - 1][1])
							 && (file == 7 || rank <= pawn_file_b[file + 1][1]);
				phalanx    = board[index - 1] == W_PAWN || board[index + 1] == W_PAWN;
				doubled    = pawn_file_w[file][0] > 1;
				isolated   =    (file == 0 || pawn_file_w[file - 1][0] == 0)
						     && (file == 7 || pawn_file_w[file + 1][0] == 0);
				backward   =    !isolated
							 && (file == 0 || rank > pawn_file_w[file - 1][1])
						     && (file == 7 || rank > pawn_file_w[file + 1][1])
							 && (  (board[index - 16] == B_PAWN)
								|| (file == 0 || board[index - 33] == B_PAWN)
								|| (file == 7 || board[index - 31] == B_PAWN));
				supporters = ((file != 0 && board[index + 15] == W_PAWN) ? 1 : 0) +
							 ((file != 7 && board[index + 17] == W_PAWN) ? 1 : 0);
						
				// Bonus for passed pawns depending on rank and file, and king proximity.
				// Any pawn which is unopposed and cannot be contested by an enemy pawn is 
				// considered passed.
				if (passed) {
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
					
					pawns_mg += passed_bonus_mg;
					pawns_eg += passed_bonus_eg;
				}
				
				// Penalty for doubled pawns. Any pawn which has a friendly pawn directly
				// behind it and is not supported diagonally is considered doubled.
				if (doubled && supporters == 0) {
					pawns_mg += DOUBLED_PAWN[MG];
					pawns_eg += DOUBLED_PAWN[EG];
				}
				
				// Penalty for isolated pawns. Any pawn which has no friendly pawn on an
				// adjacent file is considered isolated.
				if (isolated) {
					pawns_mg += ISOLATED_PAWN[MG];
					pawns_eg += ISOLATED_PAWN[EG];
				}
				
				// Penalty for backward pawns. Any pawn which is behind all pawns of the same
				// color on adjacent files and cannot be safely advanced is considered backward.
				if (backward) {
					pawns_mg += BACKWARD_PAWN[MG];
					pawns_eg += BACKWARD_PAWN[EG];
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
					pawns_mg += connectedBonus;
					if (rank <= 4) pawns_eg += connectedBonus;
				}
				break;
				
			case B_PAWN:
				opposed    = pawn_file_w[file][0] != 0 && rank < pawn_file_w[file][1];
				passed     =    !opposed
							 && (file == 0 || rank >= pawn_file_w[file - 1][1])
				        	 && (file == 7 || rank >= pawn_file_w[file + 1][1]);
				phalanx    = board[index - 1] == B_PAWN || board[index + 1] == B_PAWN;
				doubled    = pawn_file_b[file][0] > 1;
				isolated   =    (file == 0 || pawn_file_b[file - 1][0] == 0)
					    	 && (file == 7 || pawn_file_b[file + 1][0] == 0);
				backward   =    !isolated
							 && (file == 0 || rank < pawn_file_b[file - 1][1])
							 && (file == 7 || rank < pawn_file_b[file + 1][1])
							 && (   (board[index + 16] == W_PAWN)
							     || (file == 0 || board[index + 31] == W_PAWN)
							     || (file == 7 || board[index + 33] == W_PAWN));
				supporters = ((file != 0 && board[index - 17] == B_PAWN) ? 1 : 0) +
					     	 ((file != 7 && board[index - 15] == B_PAWN) ? 1 : 0);
				
				if (passed) {
					int passed_bonus_mg = PASSED_PAWN_MG[7 - rank][file];
					int passed_bonus_eg = PASSED_PAWN_EG[7 - rank][file];
					
					if (rank >= 3) {
						int rankBonus = PASSED_DANGER[7 - rank];
						
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
					
					pawns_mg -= passed_bonus_mg;
					pawns_eg -= passed_bonus_eg;
				}

				if (doubled && supporters == 0) {
					pawns_mg -= DOUBLED_PAWN[MG];
					pawns_eg -= DOUBLED_PAWN[EG];
				}
				
				if (isolated) {
					pawns_mg -= ISOLATED_PAWN[MG];
					pawns_eg -= ISOLATED_PAWN[EG];
				}
				
				if (backward) {
					pawns_mg -= BACKWARD_PAWN[MG];
					pawns_eg -= BACKWARD_PAWN[EG];
				}
				
				if (supporters > 0 || phalanx) {
					int connected_bonus = CONNECTED_PAWN[7 - rank];
					if (phalanx) connected_bonus += PAWN_PHALANX[7 - rank];
					if (opposed) connected_bonus /= 2;
					connected_bonus += supporters * SUPPORTED_PAWN;
					
					pawns_mg -= connected_bonus;
					if (rank >= 3) pawns_eg -= connected_bonus;
				}
				break;
				
			case W_KNIGHT:
				// Knight mobility
				squares = mobScan(board, excluded_area_w, index, MOVE_DELTA[KNIGHT], false, "");
				mob_mg += MOB_MG[KNIGHT][squares];
				mob_eg += MOB_EG[KNIGHT][squares];
				break;
				
			case B_KNIGHT:
				squares = mobScan(board, excluded_area_b, index, MOVE_DELTA[KNIGHT], false, "");
				mob_mg -= MOB_MG[KNIGHT][squares];
				mob_eg -= MOB_EG[KNIGHT][squares];
				break;
				
			case W_BISHOP:
				// Give a penalty for a trapped bishop. This is to prevent a pawn grab Bxa7, where
				// b6 would immediately trap the bishop.
				if (index == SQ_a7 && board[SQ_b6] == B_PAWN && board[SQ_c7] == B_PAWN)
					pieces_mg += TRAPPED_BISHOP;
				else if (index == SQ_h7 && board[SQ_g6] == B_PAWN && board[SQ_f7] == B_PAWN)
					pieces_mg += TRAPPED_BISHOP;
				
				// Give a penalty for the number of pawns on the same color square
				// as the bishop. The penalty is increased for each blocked pawn on the
				// 4 center files (C, D, E, and F).
				int bishopPawns = pawn_color_w[(rank + file) % 2];
				pieces_mg += bishopPawns * (blocked_pawns_w + 1) * BAD_BISHOP_PAWN[MG];
				pieces_eg += bishopPawns * (blocked_pawns_w + 1) * BAD_BISHOP_PAWN[EG];
				
				// Bishop mobility
				squares = mobScan(board, excluded_area_w, index, MOVE_DELTA[BISHOP], true, "Qq");
				mob_mg += MOB_MG[BISHOP][squares];
				mob_eg += MOB_EG[BISHOP][squares];
				break;
				
			case B_BISHOP:
				if (index == SQ_a2 && board[SQ_b3] == W_PAWN && board[SQ_c2] == W_PAWN)
					pieces_mg -= TRAPPED_BISHOP;
				else if (index == SQ_h2 && board[SQ_g3] == W_PAWN && board[SQ_f2] == W_PAWN)
					pieces_mg -= TRAPPED_BISHOP;
				
				bishopPawns = pawn_color_b[(rank + file) % 2];
				pieces_mg -= bishopPawns * (blocked_pawns_b + 1) * BAD_BISHOP_PAWN[MG];
				pieces_eg -= bishopPawns * (blocked_pawns_b + 1) * BAD_BISHOP_PAWN[EG];
				
				squares = mobScan(board, excluded_area_b, index, MOVE_DELTA[BISHOP], true, "Qq");
				mob_mg -= MOB_MG[BISHOP][squares];
				mob_eg -= MOB_EG[BISHOP][squares];
				break;
				
			case W_ROOK:
				// Give a bonus for rooks on open and semi-open files. A file without
				// any pawns of either  color is considered open. A file with an enemy
				// pawn but no friendly pawn is considered semi-open.
				if (pawn_file_w[file][0] == 0) {
					if (pawn_file_b[file][0] == 0) {
						pieces_mg += ROOK_OPEN_FILE[MG];
						pieces_eg += ROOK_OPEN_FILE[EG];
					}
					else {
						pieces_mg += ROOK_SEMI_FILE[MG];
						pieces_eg += ROOK_SEMI_FILE[EG];
					}
				}
				
				// Give a bonus for a rook on the 7th rank with the enemy king on the 8th rank
				if (rank == 1 && pos.king_pos_b / 16 == 0) {
					pieces_mg += ROOK_ON_7TH[MG];
					pieces_eg += ROOK_ON_7TH[EG];
				}
				
				// Give a penalty for a rook trapped by its own uncastled king
				if (   (index == SQ_a1 || index == SQ_a2 || index == SQ_b1)
					&& (pos.king_pos_w == SQ_c1 || pos.king_pos_w == SQ_b1))
						pieces_mg += TRAPPED_ROOK;
				else if (   (index == SQ_h1 || index == SQ_h2 || index == SQ_g1)
					&& (pos.king_pos_w == SQ_g1 || pos.king_pos_w == SQ_f1))
						pieces_mg += TRAPPED_ROOK;
				
				// Rook mobility
				squares = mobScan(board, excluded_area_w, index, MOVE_DELTA[ROOK], true, "QqR");
				mob_mg += MOB_MG[ROOK][squares];
				mob_eg += MOB_EG[ROOK][squares];
				break;
				
			case B_ROOK:
				if (pawn_file_b[file][0] == 0) {
					if (pawn_file_w[file][0] == 0) {
						pieces_mg -= ROOK_OPEN_FILE[MG];
						pieces_eg -= ROOK_OPEN_FILE[EG];
					}
					else {
						pieces_mg -= ROOK_SEMI_FILE[MG];
						pieces_eg -= ROOK_SEMI_FILE[EG];
					}
				}
				
				if (rank == 6 && pos.king_pos_w / 16 == 7) {
					pieces_mg -= ROOK_ON_7TH[MG];
					pieces_eg -= ROOK_ON_7TH[EG];
				}
				
				if (   (index == SQ_a8 || index == SQ_a7 || index == SQ_b8)
					&& (pos.king_pos_b == SQ_c8 || pos.king_pos_b == SQ_b8))
						pieces_mg -= TRAPPED_ROOK;
				else if (   (index == SQ_h8 || index == SQ_h7 || index == SQ_g8)
					&& (pos.king_pos_b == SQ_g8 || pos.king_pos_b == SQ_f8))
						pieces_mg -= TRAPPED_ROOK;
				
				squares = mobScan(board, excluded_area_b, index, MOVE_DELTA[ROOK], true, "Qqr");
				mob_mg -= MOB_MG[ROOK][squares];
				mob_eg -= MOB_EG[ROOK][squares];
				
				break;
				
			case W_QUEEN:
				// Give a bonus for a queen on the 7th rank with the enemy king on the 8th rank
				if (rank == 1 && pos.king_pos_b / 16 == 0) {
					pieces_mg += QUEEN_ON_7TH[MG];
					pieces_eg += QUEEN_ON_7TH[EG];
				}
				
				// Queen mobility
				squares = mobScan(board, excluded_area_w, index, MOVE_DELTA[QUEEN], true, "");
				mob_mg += MOB_MG[QUEEN][squares];
				mob_eg += MOB_EG[QUEEN][squares];
				break;
				
			case B_QUEEN:
				if (rank == 6 && pos.king_pos_w / 16 == 7) {
					pieces_mg -= QUEEN_ON_7TH[MG];
					pieces_eg -= QUEEN_ON_7TH[EG];
				}
				
				squares = mobScan(board, excluded_area_b, index, MOVE_DELTA[QUEEN], true, "");
				mob_mg -= MOB_MG[QUEEN][squares];
				mob_eg -= MOB_EG[QUEEN][squares];
				break;
			}
		}
		
		// Give a bonus for having the bishop pair.
		if (bishops_w >= 2) {
			imbal += BISHOP_PAIR;
			// The bishop pair is worth less with the queen on board
			if (queens_w >= 1) imbal -= 6;
		}
		if (bishops_b >= 2) {
			imbal -= BISHOP_PAIR;
			if (queens_b >= 1) imbal += 6;
		}
		
		// Give a small penalty for having the knight pair
		if (knights_w >= 2) imbal += REDUNDANT_KNIGHT;
		if (knights_b >= 2) imbal -= REDUNDANT_KNIGHT;
		
		// Give penalties for redundant major pieces
		if (rooks_w >= 1) {
			imbal += (rooks_w - 1) * REDUNDANT_ROOK;
			imbal += queens_w * rooks_w * REDUNDANT_QUEEN;
			// Increase rook value for each enemy pawn
			imbal += pawns_b;
		}
		if (rooks_b >= 1) {
			imbal -= (rooks_b - 1) * REDUNDANT_ROOK;
			imbal -= queens_b * rooks_b * REDUNDANT_QUEEN;
			imbal -= pawns_w;
		}
		
		// Give a bonus to knights for having more pawns
		imbal += knights_w * (5 - pawns_w) * KNIGHT_PAWN_SYNERGY;
		imbal -= knights_b * (5 - pawns_b) * KNIGHT_PAWN_SYNERGY;
		
		// Increase queen value for each enemy pawn
		if (queens_w >= 1) imbal += pawns_b * 3;
		if (queens_b >= 1) imbal -= pawns_w * 3;
		
		// Sum all the individual component scores
		double score_mg = mat_mg + psqt_mg + imbal + pawns_mg + pieces_mg + mob_mg + tempo;
		double score_eg = mat_eg + psqt_eg + imbal + pawns_eg + pieces_eg + mob_eg;

		// Scale down endgame score for bishops of opposite colors depending on the pawn
		// asymmetry (total number of unopposed pawns)
		if (npm_w == BISHOP_MG && npm_b == BISHOP_MG && bishopParity == 1) {
			int asymmetry = 0;
			for (int i = 0; i < 8; i++)
				if (   (pawn_file_w[i][0] == 0 && pawn_file_b[i][0] != 0)
					|| (pawn_file_b[i][0] == 0 && pawn_file_w[i][0] != 0))
					asymmetry++;
			score_eg *= (asymmetry * 0.0625 + 0.125);
		}
		
		// Scale down scores of likely draws due to insufficient material advantage. Draws that
		// are certain will be caught by a check during search.
		if (pawns_w == 0 && pawns_b == 0 && Math.abs(npm_w - npm_b) <= BISHOP_MG)
			score_eg /= 64;
		else {
			// If one side has no pawns and only a small material advantage, scale down the upper
			// bound of the eval.
			if (pawns_w == 0 && npm_w - npm_b <= BISHOP_MG) 
				score_eg = Math.min(score_eg, score_eg / 64);
			else if (pawns_b == 0 && npm_b - npm_w <= BISHOP_MG)
				score_eg = Math.max(score_eg, score_eg / 64);
		}

		assert(phase <= PHASE_MAX);
		
		// Calculate the middlegame and endgame weights (range 0 to 1)
		phase = Math.max(PHASE_EG, Math.min(phase, PHASE_MG));
		double weight_mg = (phase - PHASE_EG) / (double) (PHASE_MG - PHASE_EG);
		double weight_eg = 1 - weight_mg;
		
		// Calculate the tapered evaluation. This is the score interpolated between separate
		// middle and endgame scores, weighted by the phase, in order to transition smoothly
		// between middle and endgame.
		int score_tapered = (int) (score_mg * weight_mg + score_eg * weight_eg);
		
		// If the evaluation is 1 (or -1), change to 2 (or -2) since -1 is reserved specially
		// for path-dependent draw evaluations (see Types.java).
		if (Math.abs(score_tapered) == 1) score_tapered = (score_tapered == 1 ? 2 : -2);
		
		return score_tapered;
	}
	
	/**
	 * Returns the number of attacked squares not occupied by friendly pieces. Generate attacks
	 * through any "x-ray" pieces.
	 */
	private static int mobScan(int[] board, boolean[] excludedArea, int start, int[] delta,
			 				   boolean slider, String xray) {
		int count = 0;
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while (Position.isLegalIndex(target)) {
				int piece = board[target];
				if (!excludedArea[target]) count++;
				if (!slider ||   (piece != PIECE_NONE 
						       && xray.indexOf(PIECE_STR.charAt(piece + 6)) == -1))
					break;
				target += delta[i];
			}
		}
		return count;
	}
}