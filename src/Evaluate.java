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
		
		// Initialize score components
		int phase     = 0;
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
		
		int king_pos_w = 0, king_pos_b = 0;
		int npm_w = 0, npm_b = 0; // non-pawn material
		int bishopParity = 0; // used for determining opposite color bishops
		
		// pawn_file[file A..H][count | rank of least advanced pawn]
		int[][] pawn_file_w = new int[8][2]; 
		int[][] pawn_file_b = new int[8][2];
		for (int i = 0; i < 8; i++) // initialize
			pawn_file_b[i][1] = 7;
		
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
		int blockedPawns_w = 0, blockedPawns_b = 0;
		
		// First pass: piece count, material, phase, mobility area
		for (int index : pos.pieceList) {
			
			int piece = board[index];
			
			phase += PHASE_WEIGHT[Math.abs(piece)];
			int rank = index / 16, file = index % 16;
			
			switch (piece) {
			
			case W_PAWN:
				pawns_w++;
				pawn_file_w[file][0]++;
				if (rank > pawn_file_w[file][1])
					pawn_file_w[file][1] = rank;
				pawn_color_w[(file + rank) % 2]++;
				if (file > 1 && file < 6 && board[index - 16] != PIECE_NONE)
					blockedPawns_w++;
				if (file != 0) excluded_area_b[index - 17] = true;
				if (file != 7) excluded_area_b[index - 15] = true;
				if (rank == 6 || rank == 5 || board[index - 16] != PIECE_NONE)
					excluded_area_w[index] = true;
				break;
				
			case B_PAWN:
				pawns_b++;
				pawn_file_b[file][0]++;
				if (rank < pawn_file_b[file][1])
					pawn_file_b[file][1] = rank;
				pawn_color_b[(file + rank) % 2]++;
				if (file > 1 && file < 6 && board[index + 16] != PIECE_NONE)
					blockedPawns_b++;
				if (file != 0) excluded_area_w[index + 15] = true;
				if (file != 7) excluded_area_w[index + 17] = true;
				if (rank == 1 || rank == 2 || board[index + 16] != PIECE_NONE)
					excluded_area_b[index] = true;
				break;
				
			case W_KNIGHT:
				knights_w++;
				break;
				
			case B_KNIGHT:
				knights_b++;
				break;
				
			case W_BISHOP:
				bishops_w++;
				bishopParity += (file + rank) % 2;
				break;
				
			case B_BISHOP:
				bishops_b++;
				bishopParity += (file + rank) % 2;
				break;
				
			case W_ROOK:
				rooks_w++;
				break;
				
			case B_ROOK:
				rooks_b++;
				break;
				
			case W_QUEEN:
				queens_w++;
				excluded_area_w[index] = true;
				break;
				
			case B_QUEEN:
				queens_b++;
				excluded_area_b[index] = true;
				break;
			
			case W_KING:
				king_pos_w = index;
				excluded_area_w[index] = true;
				break;
			
			case B_KING:
				king_pos_b = index;
				excluded_area_b[index] = true;
				break;
			}
			
			if (piece > 0) {
				mat_mg  += PIECE_VALUE_MG[piece];
				mat_eg  += PIECE_VALUE_EG[piece];
				psqt_mg += PSQT_MG[piece][rank][file];
				psqt_eg += PSQT_EG[piece][rank][file];
				npm_w   += PIECE_UNIT_VALUE[piece];
			}
			else {
				mat_mg  -= PIECE_VALUE_MG[-piece];
				mat_eg  -= PIECE_VALUE_EG[-piece];
				psqt_mg -= PSQT_MG[-piece][7 - rank][file];
				psqt_eg -= PSQT_EG[-piece][7 - rank][file];
				npm_b   += PIECE_UNIT_VALUE[-piece];
			}
		}
		
		int pieces_w = knights_w + bishops_w + rooks_w + queens_w;
		int pieces_b = knights_b + bishops_b + rooks_b + queens_b;
		
		// Mate with KX vs K. Give a bonus for driving the enemy king to the edge of board and
		// for keeping distance between the two kings small.
		if (pawns_b == 0 && pieces_b == 0 && pieces_w > 0) {
			int cornerProximity = EDGE_PROXIMITY[king_pos_b / 16][king_pos_b % 16];
			int kingDist = Position.distance(king_pos_w, king_pos_b);
			int kingProximity = KINGS_PROXIMITY[kingDist];
			return mat_mg + (cornerProximity + kingProximity) * 10;
		}
		if (pawns_w == 0 && pieces_w == 0 && pieces_b > 0) {
			int cornerProximity = EDGE_PROXIMITY[king_pos_w / 16][king_pos_w % 16];
			int kingDist = Position.distance(king_pos_w, king_pos_b);
			int kingProximity = KINGS_PROXIMITY[kingDist];
			return mat_mg - (cornerProximity + kingProximity) * 10;
		}
		
		// Return a lazy eval if the score is high
		int score_lazy = (mat_mg + mat_eg + psqt_mg + psqt_eg) / 2;
		if (Math.abs(score_lazy) > LAZY_THRESHOLD)
			return score_lazy;
		
		// Second pass: calculate mobility, and evaluate pieces and pawns.
		for (int index : pos.pieceList) {
			
			int piece = board[index];
			
			int side = (piece > 0 ? WHITE : BLACK);
			int rank = index / 16, file = index % 16;
			int squares = 0;
			
			switch (piece) {
			case W_PAWN:
				boolean opposed, passed, phalanx, doubled, isolated, backward;
				int supporters;

				// First flag the pawn
				opposed    =   pawn_file_b[file][0] > 0
						    && rank > pawn_file_b[file][1];
				passed     =   !opposed
						    && (file == 0 || rank <= pawn_file_b[file - 1][1])
			              	&& (file == 7 || rank <= pawn_file_b[file + 1][1]);
				phalanx    =   board[index - 1] == W_PAWN
						    || board[index + 1] == W_PAWN;
				doubled    =   pawn_file_w[file][0] >= 2;
				isolated   =   (file == 0 || pawn_file_w[file - 1][0] == 0)
						    && (file == 7 || pawn_file_w[file + 1][0] == 0);
				backward   =   (file == 0 || rank > pawn_file_w[file - 1][1])
						    && (file == 7 || rank > pawn_file_w[file + 1][1])
							&& (   (board[index - 16] == B_PAWN)
								|| (file == 0 || board[index - 17] == B_PAWN)
								|| (file == 7 || board[index - 15] == B_PAWN));
				supporters = ((file != 0 && board[index + 15] == W_PAWN) ? 1 : 0) +
							 ((file != 7 && board[index + 17] == W_PAWN) ? 1 : 0);
						
				// Bonus for passed pawns depending on rank and file, and king proximity.
				// Any pawn which is unopposed and cannot be contested by an enemy pawn is 
				// considered passed.
				if (passed) {
					pawns_mg += PASSED_PAWN_MG[rank][file];
					pawns_eg += PASSED_PAWN_EG[rank][file];
					
					int rankBonus = PASSED_DANGER[rank];
					// distance from king to block square of pawn, capped at 5
					int kingDist_w = Math.min(Position.distance(king_pos_w, index - 16), 5);
					int kingDist_b = Math.min(Position.distance(king_pos_b, index - 16), 5);
					pawns_eg += kingDist_b * 5 * rankBonus;
					pawns_eg -= kingDist_w * 2 * rankBonus;
					
					// If the block square is not the queening square, consider a second push
					if (rank > 1) {
						int kingDist2_w = Math.min(Position.distance(king_pos_w, index - 32), 5);
						pawns_eg -= kingDist2_w * rankBonus;
					}
					
					// If the pawn is free to advance, increase the bonus
					if (board[index - 16] == PIECE_NONE) {
						pawns_mg += 5 * rankBonus;
						pawns_eg += 5 * rankBonus;
					}
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
					double connectedBonus = CONNECTED_PAWN[rank];
					if (phalanx) connectedBonus += PAWN_PHALANX[rank];
					if (opposed) connectedBonus /= 2;
					connectedBonus += supporters *= SUPPORTED_PAWN;
					pawns_mg += (int) connectedBonus;
					
					// In the endgame only the 4th through 7th ranks receive a bonus.
					pawns_eg += (int) (connectedBonus * (5 - rank) / 4);
				}
				break;
				
			case B_PAWN:
				opposed    =   pawn_file_w[file][0] > 0
				            && rank < pawn_file_w[file][1];
				passed     =   !opposed
							&& (file == 0 || rank >= pawn_file_w[file - 1][1])
				        	&& (file == 7 || rank >= pawn_file_w[file + 1][1]);
				phalanx    =   board[index - 1] == B_PAWN
				          	|| board[index + 1] == B_PAWN;
				doubled    =   pawn_file_b[file][0] >= 2;
				isolated   =   (file == 0 || pawn_file_b[file - 1][0] == 0)
					    	&& (file == 7 || pawn_file_b[file + 1][0] == 0);
				backward   =   (file == 0 || rank < pawn_file_b[file - 1][1])
							&& (file == 7 || rank < pawn_file_b[file + 1][1])
							&& (   (board[index + 16] == W_PAWN)
							    || (file == 0 || board[index + 15] == W_PAWN)
							    || (file == 7 || board[index + 17] == W_PAWN));
				supporters =   ((file != 0 && board[index - 17] == B_PAWN) ? 1 : 0) +
					     	   ((file != 7 && board[index - 15] == B_PAWN) ? 1 : 0);
				
				if (passed) {
					pawns_mg -= PASSED_PAWN_MG[7 - rank][file];
					pawns_eg -= PASSED_PAWN_EG[7 - rank][file];
					
					int rankBonus = PASSED_DANGER[7 - rank];
					int kingDist_b = Math.min(Position.distance(king_pos_b, index + 16), 5);
					int kingDist_w = Math.min(Position.distance(king_pos_w, index + 16), 5);
					pawns_eg -= kingDist_w * rankBonus * 5;
					pawns_eg += kingDist_b * rankBonus * 2;
					
					if (rank < 6) {
						int kingDist2_b = Math.min(Position.distance(king_pos_b, index + 32), 5);
						pawns_eg += kingDist2_b * rankBonus;
					}
					
					if (board[index + 16] == PIECE_NONE) {
						pawns_mg -= 5 * rankBonus;
						pawns_eg -= 5 * rankBonus;
					}
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
					double connectedBonus = CONNECTED_PAWN[7 - rank];
					if (phalanx) connectedBonus += PAWN_PHALANX[7 - rank];
					if (opposed) connectedBonus /= 2;
					connectedBonus += supporters * SUPPORTED_PAWN;
					pawns_mg -= (int) connectedBonus;
					pawns_eg -= (int) (connectedBonus * (rank - 2) / 4);
				}
				break;
				
			case W_KNIGHT:
				// Knight mobility
				squares = mobScan(board, excluded_area_w, side, index, MOVE_DELTA[KNIGHT], false, "");
				mob_mg += MOB_MG[KNIGHT][squares];
				mob_eg += MOB_EG[KNIGHT][squares];
				break;
				
			case B_KNIGHT:
				squares = mobScan(board, excluded_area_b, side, index, MOVE_DELTA[KNIGHT], false, "");
				mob_mg -= MOB_MG[KNIGHT][squares];
				mob_eg -= MOB_EG[KNIGHT][squares];
				break;
				
			case W_BISHOP:
				// Give a penalty for a trapped bishop. This is to prevent a pawn grab such as
				// Bxa7, whereupon b6 immediately traps the bishop.
				if (index == SQ_a7 && board[SQ_b6] == B_PAWN && board[SQ_c7] == B_PAWN)
					pieces_mg += TRAPPED_BISHOP;
				if (index == SQ_h7 && board[SQ_g6] == B_PAWN && board[SQ_f7] == B_PAWN)
					pieces_mg += TRAPPED_BISHOP;
				
				// Give a penalty for the number of pawns on the same color square
				// as the bishop. The penalty is increased for each blocked pawn on the
				// 4 center files (C, D, E, and F).
				int bishopPawns = pawn_color_w[(rank + file) % 2];
				pieces_mg += bishopPawns * (blockedPawns_w + 1) * BAD_BISHOP_PAWN[MG];
				pieces_eg += bishopPawns * (blockedPawns_w + 1) * BAD_BISHOP_PAWN[EG];
				
				// Bishop mobility
				squares = mobScan(board, excluded_area_w, side, index, MOVE_DELTA[BISHOP], true, "Qq");
				mob_mg += MOB_MG[BISHOP][squares];
				mob_eg += MOB_EG[BISHOP][squares];
				break;
				
			case B_BISHOP:
				if (index == SQ_a2 && board[SQ_b3] == W_PAWN && board[SQ_c2] == W_PAWN)
					pieces_mg -= TRAPPED_BISHOP;
				if (index == SQ_h2 && board[SQ_g3] == W_PAWN && board[SQ_f2] == W_PAWN)
					pieces_mg -= TRAPPED_BISHOP;
				
				bishopPawns = pawn_color_b[(rank + file) % 2];
				pieces_mg -= bishopPawns * (blockedPawns_b + 1) * BAD_BISHOP_PAWN[MG];
				pieces_eg -= bishopPawns * (blockedPawns_b + 1) * BAD_BISHOP_PAWN[EG];
				
				squares = mobScan(board, excluded_area_b, side, index, MOVE_DELTA[BISHOP], true, "Qq");
				mob_mg -= MOB_MG[BISHOP][squares];
				mob_eg -= MOB_EG[BISHOP][squares];
				break;
				
			case W_ROOK:
				// Give a bonus for rooks on open and semi-open files. A file without
				// any pawns of either color is considered open. A file with an enemy
				// pawn but no friendly pawn is considered semi-open.
				if (pawn_file_w[file][0] == 0) {
					if (pawn_file_b[file][0] == 0) {
						pieces_mg += ROOK_OPEN_FILE[MG];
						pieces_eg += ROOK_OPEN_FILE[EG];
					}
					else {
						pieces_mg += ROOK_SEMIOPEN_FILE[MG];
						pieces_eg += ROOK_SEMIOPEN_FILE[EG];
					}
				}
				
				// Give a bonus for a rook on the 7th rank with the enemy king on the 8th rank
				if (rank == 1 && king_pos_b / 16 == 0) {
					pieces_mg += ROOK_ON_7TH[MG];
					pieces_eg += ROOK_ON_7TH[EG];
				}
				
				// Give a penalty for a rook trapped by its own uncastled king
				if (   (index == SQ_a1 || index == SQ_a2 || index == SQ_b1)
					&& (king_pos_w == SQ_c1 || king_pos_w == SQ_b1))
						pieces_mg += TRAPPED_ROOK;
				if (   (index == SQ_h1 || index == SQ_h2 || index == SQ_g1)
					&& (king_pos_w == SQ_g1 || king_pos_w == SQ_f1))
						pieces_mg += TRAPPED_ROOK;
				
				// Rook mobility
				squares = mobScan(board, excluded_area_w, side, index, MOVE_DELTA[ROOK], true, "QqR");
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
						pieces_mg -= ROOK_SEMIOPEN_FILE[MG];
						pieces_eg -= ROOK_SEMIOPEN_FILE[EG];
					}
				}
				
				if (rank == 6 && king_pos_w / 16 == 7) {
					pieces_mg -= ROOK_ON_7TH[MG];
					pieces_eg -= ROOK_ON_7TH[EG];
				}
				
				if (   (index == SQ_a8 || index == SQ_a7 || index == SQ_b8)
					&& (king_pos_b == SQ_c8 || king_pos_b == SQ_b8))
						pieces_mg -= TRAPPED_ROOK;
				if (   (index == SQ_h8 || index == SQ_h7 || index == SQ_g8)
					&& (king_pos_b == SQ_g8 || king_pos_b == SQ_f8))
						pieces_mg -= TRAPPED_ROOK;
				
				squares = mobScan(board, excluded_area_b, side, index, MOVE_DELTA[ROOK], true, "Qqr");
				mob_mg -= MOB_MG[ROOK][squares];
				mob_eg -= MOB_EG[ROOK][squares];break;
				
			case W_QUEEN:
				// Give a bonus for a queen on the 7th rank with the enemy king on the
				// 8th rank
				if (rank == 1 && king_pos_b / 16 == 0) {
					pieces_mg += QUEEN_ON_7TH[MG];
					pieces_eg += QUEEN_ON_7TH[EG];
				}
				
				// Queen mobility
				squares = mobScan(board, excluded_area_w, side, index, MOVE_DELTA[QUEEN], true, "");
				mob_mg += MOB_MG[QUEEN][squares];
				mob_eg += MOB_EG[QUEEN][squares];
				break;
				
			case B_QUEEN:
				if (rank == 6 && king_pos_w / 16 == 7) {
					pieces_mg -= QUEEN_ON_7TH[MG];
					pieces_eg -= QUEEN_ON_7TH[EG];
				}
				
				squares = mobScan(board, excluded_area_b, side, index, MOVE_DELTA[QUEEN], true, "");
				mob_mg -= MOB_MG[QUEEN][squares];
				mob_eg -= MOB_EG[QUEEN][squares];
				break;
			}
		}
		
		// Give a penalty for rook pawns, which are of diminished value since they can only
		// capture in one direction.
		imbal += (pawn_file_w[0][0] + pawn_file_w[7][0]) * ROOK_PAWN;
		imbal -= (pawn_file_b[0][0] + pawn_file_b[7][0]) * ROOK_PAWN;
		
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
		if (queens_w >= 1)
			imbal += pawns_b * 3;
		if (queens_b >= 1)
			imbal -= pawns_w * 3;
		
		// Sum all the individual component scores
		int score_mg = mat_mg + psqt_mg + imbal + pawns_mg + pieces_mg + mob_mg + tempo;
		int score_eg = mat_eg + psqt_eg + imbal + pawns_eg + pieces_eg + mob_eg;

		// Scale down endgame score for bishops of opposite colors depending on the pawn
		// asymmetry (total number of unopposed pawns)
		if (npm_w == 3 && npm_b == 3 && bishops_w == 1 && bishops_b == 1 && bishopParity == 1) {
			int asymmetry = 0;
			for (int i = 0; i < 8; i++)
				if (   (pawn_file_w[i][0] == 0 && pawn_file_b[i][0] != 0)
					|| (pawn_file_b[i][0] == 0 && pawn_file_w[i][0] != 0))
					asymmetry++;
			score_eg *= (0.125 + asymmetry * 0.0625);
		}
		
		// Check for likely draw due to insufficient material advantage
		// note: does not cover the KNN v K case
		if (pawns_w == 0 && pawns_b == 0 && Math.abs(npm_w - npm_b) < 4) score_eg = 0;
		
		// If one side has no pawns and insufficient material advantage, the upper bound of
		// the eval is a draw
		if      (pawns_w == 0 && npm_w - npm_b < 4) score_eg = Math.min(VALUE_DRAW, score_eg);
		else if (pawns_b == 0 && npm_b - npm_w < 4) score_eg = Math.max(VALUE_DRAW, score_eg);

		// Calculate the middle and endgame weights
		int minPhase = PHASE_MAX_EG, maxPhase = PHASE_MAX_MG;
		phase = Math.max(minPhase, Math.min(phase, maxPhase));
		double weight_mg = (phase - minPhase) / (double) (maxPhase - minPhase);
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
	private static int mobScan(int[] board, boolean[] excludedArea, int side, int start,
							   int[] delta, boolean slider, String xray) {
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