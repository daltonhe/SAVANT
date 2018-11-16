/**
 * @author Dalton He
 * created 11-05-18
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
		int phase        = 0;
		int material_mg  = 0, material_eg  = 0;
		int psqt_mg      = 0, psqt_eg      = 0;
		int pawns_mg     = 0, pawns_eg     = 0;
		int pieces_mg    = 0, pieces_eg    = 0;
		int mobility_mg  = 0, mobility_eg  = 0;
		int imbalance    = 0;	
		int tempo        = TEMPO * pos.sideToMove;
		
		// Initialize piece counts
		int kingPos_w = 0, kingPos_b = 0;
		int pawns_w   = 0, pawns_b   = 0;
		int knights_w = 0, knights_b = 0;
		int bishops_w = 0, bishops_b = 0;
		int rooks_w   = 0, rooks_b   = 0;
		int queens_w  = 0, queens_b  = 0;
		int npm_w     = 0, npm_b     = 0; // non-pawn material
		
		int bishopParity = 0; // used for determining opposite color bishops
		
		// pawnFile[fileA..H][count|rank of least advanced pawn]
		int[][] pawnFile_w = new int[8][2]; 
		int[][] pawnFile_b = new int[8][2];
		for (int i = 0; i < 8; i++) // initialize
			pawnFile_b[i][1] = 7;
		
		// pawnColor[pawns on light squares|dark squares]
		int[] pawnColor_w = new int[2];
		int[] pawnColor_b = new int[2];
		
		// number of blocked pawns on the 4 center files (C, D, E, and F)
		int blockedPawns_w = 0, blockedPawns_b = 0;
		
		// First pass: count pieces, material, and phase.
		for (int index = SQ_a8; index <= SQ_h1; index++) {
			if (!Position.isLegalIndex(index)) continue;

			int piece = board[index];
			if (piece == PIECE_NONE) continue;
			
			phase += PHASE_WEIGHT[Math.abs(piece)];
			int rank = index / 16, file = index % 16;
			
			switch (piece) {
			
			case W_PAWN:
				pawns_w++;
				pawnFile_w[file][0]++;
				if (rank > pawnFile_w[file][1])
					pawnFile_w[file][1] = rank;
				pawnColor_w[(file + rank) % 2]++;
				if (file > 1 && file < 6 && board[index - 16] != PIECE_NONE)
					blockedPawns_w++;
				break;
				
			case B_PAWN:
				pawns_b++;
				pawnFile_b[file][0]++;
				if (rank < pawnFile_b[file][1])
					pawnFile_b[file][1] = rank;
				pawnColor_b[(file + rank) % 2]++;
				if (file > 1 && file < 6 && board[index + 16] != PIECE_NONE)
					blockedPawns_b++;
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
				break;
				
			case B_QUEEN:
				queens_b++;
				break;
			
			case W_KING:
				kingPos_w = index;
				break;
			
			case B_KING:
				kingPos_b = index;
				break;
			}
			
			if (piece > 0) {
				material_mg += PIECE_VALUE_MG[piece];
				material_eg += PIECE_VALUE_EG[piece];
				psqt_mg     += PSQT_MG[piece][rank][file];
				psqt_eg     += PSQT_EG[piece][rank][file];
				npm_w       += PIECE_UNIT_VALUE[piece];
			}
			else {
				material_mg -= PIECE_VALUE_MG[-piece];
				material_eg -= PIECE_VALUE_EG[-piece];
				psqt_mg     -= PSQT_MG[-piece][7 - rank][file];
				psqt_eg     -= PSQT_EG[-piece][7 - rank][file];
				npm_b       += PIECE_UNIT_VALUE[-piece];
			}
		}
		
		int pieces_w = knights_w + bishops_w + rooks_w + queens_w;
		int pieces_b = knights_b + bishops_b + rooks_b + queens_b;
		
		// Mate with KX vs K. In this case, give a bonus for driving the enemy king to the corner,
		// and for keeping distance between the two kings small.
		if (pawns_b == 0 && pieces_b == 0 && pieces_w > 0) {
			int cornered = CORNER_PROXIMITY[kingPos_b / 16][kingPos_b % 16];
			int kingDist = Math.max(Math.abs(kingPos_b / 16 - kingPos_w / 16),
		            				Math.abs(kingPos_b % 16 - kingPos_w % 16));
			int kingProximity = KINGS_PROXIMITY[kingDist];
			return material_mg + (cornered + kingProximity) * 10;
		}
		if (pawns_w == 0 && pieces_w == 0 && pieces_b > 0) {
			int cornered = CORNER_PROXIMITY[kingPos_w / 16][kingPos_w % 16];
			int kingDist = Math.max(Math.abs(kingPos_b / 16 - kingPos_w / 16),
						            Math.abs(kingPos_b % 16 - kingPos_w % 16));
			int kingProximity = KINGS_PROXIMITY[kingDist];
			return material_mg - (cornered + kingProximity) * 10;
		}
		
		// Return a lazy eval if the score is high
		int score_lazy = (material_mg + material_eg + psqt_mg + psqt_eg) / 2;
		if (Math.abs(score_lazy) > LAZY_THRESHOLD)
			return score_lazy;
		
		// Second pass: calculate mobility, and evaluate pieces and pawns.
		for (int index = SQ_a8; index <= SQ_h1; index++) {
			if (!Position.isLegalIndex(index)) continue;
			
			int piece = board[index];
			if (piece == PIECE_NONE) continue;
			
			int side = (piece > 0 ? WHITE : BLACK);
			int rank = index / 16, file = index % 16;
			int squares;
			
			switch (piece) {
			case W_PAWN:
				boolean opposed, passed, phalanx, doubled, isolated, backward;
				int supporters;

				// First flag the pawn
				opposed    =   pawnFile_b[file][0] > 0
						    && rank > pawnFile_b[file][1];
				passed     =   !opposed
						    && (file == 0 || rank <= pawnFile_b[file - 1][1])
			              	&& (file == 7 || rank <= pawnFile_b[file + 1][1]);
				phalanx    =   board[index - 1] == W_PAWN
						    || board[index + 1] == W_PAWN;
				doubled    =   pawnFile_w[file][0] >= 2;
				isolated   =   (file == 0 || pawnFile_w[file - 1][0] == 0)
						    && (file == 7 || pawnFile_w[file + 1][0] == 0);
				backward   =   (file == 0 || rank > pawnFile_w[file - 1][1])
						    && (file == 7 || rank > pawnFile_w[file + 1][1])
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
					int kingDist_our = Math.min(Position.distance(kingPos_w, index - 16), 5);
					int kingDist_opp = Math.min(Position.distance(kingPos_b, index - 16), 5);
					pawns_eg += kingDist_opp * 5 * rankBonus;
					pawns_eg -= kingDist_our * 2 * rankBonus;
					// If the block square is not the queening square, consider a second push
					if (rank > 1) {
						int kingDist_our2 = Math.min(Position.distance(kingPos_w, index - 32), 5);
						pawns_eg -= kingDist_our2 * rankBonus;
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
					pawns_mg += DOUBLED_PAWN_MG;
					pawns_eg += DOUBLED_PAWN_EG;
				}
				// Penalty for isolated pawns. Any pawn which has no friendly pawn on an
				// adjacent file is considered isolated.
				if (isolated) {
					pawns_mg += ISOLATED_PAWN_MG;
					pawns_eg += ISOLATED_PAWN_EG;
				}
				// Penalty for backward pawns. Any pawn which is behind all pawns of the same
				// color on adjacent files and cannot be safely advanced is considered backward.
				if (backward) {
					pawns_mg += BACKWARD_PAWN_MG;
					pawns_eg += BACKWARD_PAWN_EG;
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
				opposed    =   pawnFile_w[file][0] > 0
				            && rank < pawnFile_w[file][1];
				passed     =   !opposed
							&& (file == 0 || rank >= pawnFile_w[file - 1][1])
				        	&& (file == 7 || rank >= pawnFile_w[file + 1][1]);
				phalanx    =   board[index - 1] == B_PAWN
				          	|| board[index + 1] == B_PAWN;
				doubled    =   pawnFile_b[file][0] >= 2;
				isolated   =   (file == 0 || pawnFile_b[file - 1][0] == 0)
					    	&& (file == 7 || pawnFile_b[file + 1][0] == 0);
				backward   =   (file == 0 || rank < pawnFile_b[file - 1][1])
							&& (file == 7 || rank < pawnFile_b[file + 1][1])
							&& (   (board[index + 16] == W_PAWN)
							    || (file == 0 || board[index + 15] == W_PAWN)
							    || (file == 7 || board[index + 17] == W_PAWN));
				supporters =   ((file != 0 && board[index - 17] == B_PAWN) ? 1 : 0) +
					     	   ((file != 7 && board[index - 15] == B_PAWN) ? 1 : 0);
				
				if (passed) {
					pawns_mg -= PASSED_PAWN_MG[7 - rank][file];
					pawns_eg -= PASSED_PAWN_EG[7 - rank][file];
					
					int rankBonus = PASSED_DANGER[7 - rank];
					int kingDist_our = Math.min(Position.distance(kingPos_b, index + 16), 5);
					int kingDist_opp = Math.min(Position.distance(kingPos_w, index + 16), 5);
					pawns_eg -= kingDist_opp * rankBonus * 5;
					pawns_eg += kingDist_our * rankBonus * 2;
					if (rank < 6) {
						int kingDist_our2 = Math.min(Position.distance(kingPos_b, index + 32), 5);
						pawns_eg += kingDist_our2 * rankBonus;
					}
					
					if (board[index + 16] == PIECE_NONE) {
						pawns_mg -= 5 * rankBonus;
						pawns_eg -= 5 * rankBonus;
					}
				}
				if (doubled && supporters == 0) {
					pawns_mg -= DOUBLED_PAWN_MG;
					pawns_eg -= DOUBLED_PAWN_EG;
				}
				if (isolated) {
					pawns_mg -= ISOLATED_PAWN_MG;
					pawns_eg -= ISOLATED_PAWN_EG;
				}
				if (backward) {
					pawns_mg -= BACKWARD_PAWN_MG;
					pawns_eg -= BACKWARD_PAWN_EG;
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
				squares = mobilityDelta(board, side, index, DELTA_KNIGHT, false);
				mobility_mg += (squares - KNIGHT_MAX_SQUARES / 2) * KNIGHT_MOBILITY_MG;
				mobility_eg += (squares - KNIGHT_MAX_SQUARES / 2) * KNIGHT_MOBILITY_EG;
				break;
				
			case B_KNIGHT:
				squares = mobilityDelta(board, side, index, DELTA_KNIGHT, false);
				mobility_mg -= (squares - KNIGHT_MAX_SQUARES / 2) * KNIGHT_MOBILITY_MG;
				mobility_eg -= (squares - KNIGHT_MAX_SQUARES / 2) * KNIGHT_MOBILITY_EG;
				break;
				
			case W_BISHOP:
				// Give a penalty for the number of pawns on the same color square
				// as the bishop. The penalty is increased for each blocked pawn on the
				// 4 center files (C, D, E, and F).
				int bishopPawns = pawnColor_w[(rank + file) % 2];
				pieces_mg += bishopPawns * (blockedPawns_w + 1) * PAWN_ON_BISHOP_COLOR_MG;
				pieces_eg += bishopPawns * (blockedPawns_w + 1) * PAWN_ON_BISHOP_COLOR_EG;
				// Bishop mobility
				squares = mobilityDelta(board, side, index, DELTA_BISHOP, true);
				mobility_mg += (squares - BISHOP_MAX_SQUARES / 2) * BISHOP_MOBILITY_MG;
				mobility_eg += (squares - BISHOP_MAX_SQUARES / 2) * BISHOP_MOBILITY_EG;
				break;
				
			case B_BISHOP:
				bishopPawns = pawnColor_b[(rank + file) % 2];
				pieces_mg -= bishopPawns * (blockedPawns_b + 1) * PAWN_ON_BISHOP_COLOR_MG;
				pieces_eg -= bishopPawns * (blockedPawns_b + 1) * PAWN_ON_BISHOP_COLOR_EG;
				squares = mobilityDelta(board, side, index, DELTA_BISHOP, true);
				mobility_mg -= (squares - BISHOP_MAX_SQUARES / 2) * BISHOP_MOBILITY_MG;
				mobility_eg -= (squares - BISHOP_MAX_SQUARES / 2) * BISHOP_MOBILITY_EG;
				break;
				
			case W_ROOK:
				// Give a bonus for rooks on open and semi-open files. A file without
				// any pawns of either color is considered open. A file with an enemy
				// pawn but no friendly pawn is considered semi-open.
				if (pawnFile_w[file][0] == 0) {
					if (pawnFile_b[file][0] == 0) {
						pieces_mg += ROOK_OPEN_FILE_MG;
						pieces_eg += ROOK_OPEN_FILE_EG;
					}
					else {
						pieces_mg += ROOK_SEMIOPEN_FILE_MG;
						pieces_eg += ROOK_SEMIOPEN_FILE_EG;
					}
				}
				// Give a bonus for a rook on the 7th rank with the enemy king on the
				// 8th rank
				if (rank == 1 && kingPos_b / 16 == 0) {
					pieces_mg += ROOK_ON_7TH_MG;
					pieces_eg += ROOK_ON_7TH_EG;
				}
				// Give a penalty for a rook trapped by its own uncastled king
				if (   (index == SQ_a1 || index == SQ_a2 || index == SQ_b1)
					&& (kingPos_w == SQ_c1 || kingPos_w == SQ_b1))
						pieces_mg += TRAPPED_ROOK;
				if (   (index == SQ_h1 || index == SQ_h2 || index == SQ_g1)
					&& (kingPos_w == SQ_g1 || kingPos_w == SQ_f1))
						pieces_mg += TRAPPED_ROOK;
				// Rook mobility
				squares = mobilityDelta(board, side, index, DELTA_ROOK, true);
				mobility_mg += (squares - ROOK_MAX_SQUARES / 2) * ROOK_MOBILITY_MG;
				mobility_eg += (squares - ROOK_MAX_SQUARES / 2) * ROOK_MOBILITY_EG;
				break;
				
			case B_ROOK:
				if (pawnFile_b[file][0] == 0) {
					if (pawnFile_w[file][0] == 0) {
						pieces_mg -= ROOK_OPEN_FILE_MG;
						pieces_eg -= ROOK_OPEN_FILE_EG;
					}
					else {
						pieces_mg -= ROOK_SEMIOPEN_FILE_MG;
						pieces_eg -= ROOK_SEMIOPEN_FILE_EG;
					}
				}
				if (rank == 6 && kingPos_w / 16 == 7) {
					pieces_mg -= ROOK_ON_7TH_MG;
					pieces_eg -= ROOK_ON_7TH_EG;
				}
				if (   (index == SQ_a8 || index == SQ_a7 || index == SQ_b8)
					&& (kingPos_b == SQ_c8 || kingPos_b == SQ_b8))
						pieces_mg -= TRAPPED_ROOK;
				if (   (index == SQ_h8 || index == SQ_h7 || index == SQ_g8)
					&& (kingPos_b == SQ_g8 || kingPos_b == SQ_f8))
						pieces_mg -= TRAPPED_ROOK;
				squares = mobilityDelta(board, side, index, DELTA_ROOK, true);
				mobility_mg -= (squares - ROOK_MAX_SQUARES / 2) * ROOK_MOBILITY_MG;
				mobility_eg -= (squares - ROOK_MAX_SQUARES / 2) * ROOK_MOBILITY_EG;
				break;
				
			case W_QUEEN:
				// Give a bonus for a queen on the 7th rank with the enemy king on the
				// 8th rank
				if (rank == 1 && kingPos_b / 16 == 0) {
					pieces_mg += QUEEN_ON_7TH_MG;
					pieces_eg += QUEEN_ON_7TH_EG;
				}
				// Queen mobility
				squares = mobilityDelta(board, side, index, DELTA_QUEEN, true);
				mobility_mg += (squares - QUEEN_MAX_SQUARES / 2) * QUEEN_MOBILITY_MG;
				mobility_eg += (squares - QUEEN_MAX_SQUARES / 2) * QUEEN_MOBILITY_EG;
				break;
				
			case B_QUEEN:
				if (rank == 6 && kingPos_w / 16 == 7) {
					pieces_mg -= QUEEN_ON_7TH_MG;
					pieces_eg -= QUEEN_ON_7TH_EG;
				}
				squares = mobilityDelta(board, side, index, DELTA_QUEEN, true);
				mobility_mg -= (squares - QUEEN_MAX_SQUARES / 2) * QUEEN_MOBILITY_MG;
				mobility_eg -= (squares - QUEEN_MAX_SQUARES / 2) * QUEEN_MOBILITY_EG;
				break;
			}
		}
		
		// Give a penalty for rook pawns, which are of diminished value since they can only
		// capture in one direction.
		imbalance += (pawnFile_w[0][0] + pawnFile_w[7][0]) * ROOK_PAWN;
		imbalance -= (pawnFile_b[0][0] + pawnFile_b[7][0]) * ROOK_PAWN;
		
		// Give a bonus for having the bishop pair. If the bishop pair is unopposed, i.e.
		// the opponent has no minor pieces to contest the bishops, the bonus is larger.
		if (bishops_w >= 2) {
			imbalance += BISHOP_PAIR;
			// The bishop pair is worth less with the queen on board
			if (queens_w >= 1) imbalance -= 6;
			// But more with the enemy queen on board
			//if (queens_b >= 1) imbalance += 3;
			// Give a tiny bonus for each pawn on the board
			//imbalance += pawns_w + pawns_b;
		}
		if (bishops_b >= 2) {
			imbalance -= BISHOP_PAIR;
			if (queens_b >= 1) imbalance += 6;
			//if (queens_w >= 1) imbalance -= 3;
			//imbalance -= pawns_w + pawns_b;
		}
		
		// Give a small penalty for having the knight pair
		if (knights_w >= 2) imbalance += KNIGHT_PAIR;
		if (knights_b >= 2) imbalance -= KNIGHT_PAIR;
		
		// Give penalties for redundant major pieces
		if (rooks_w >= 1) {
			imbalance += (rooks_w - 1) * REDUNDANT_ROOK;
			imbalance += queens_w * rooks_w * REDUNDANT_QUEEN;
			// Increase rook value for each enemy pawn
			imbalance += pawns_b;
		}
		if (rooks_b >= 1) {
			imbalance -= (rooks_b - 1) * REDUNDANT_ROOK;
			imbalance -= queens_b * rooks_b * REDUNDANT_QUEEN;
			imbalance -= pawns_w;
		}
		
		// Give a big bonus to knights for having more pawns, and a smaller bonus to bishops
		// Also give a smaller bonus for enemy pawns
		imbalance += knights_w * (5 - pawns_w) * KNIGHT_PAWN_SYNERGY;
		imbalance -= knights_b * (5 - pawns_b) * KNIGHT_PAWN_SYNERGY;
		//imbalance += bishops_w * pawns_w * BISHOP_PAWN_SYNERGY;
		//imbalance -= bishops_b * pawns_b * BISHOP_PAWN_SYNERGY;
		//imbalance += (knights_w + bishops_w) * pawns_b * 2;
		//imbalance -= (knights_b + bishops_b) * pawns_w * 2;
		
		// Increase queen value for each enemy pawn, bishop, and rook, and for each friendly minor
		if (queens_w >= 1) {
			imbalance += pawns_b * 3;
			//imbalance += bishops_b * 4;
			//imbalance += rooks_b * 8; 
			//imbalance += (knights_w + bishops_w) * 4;
		}
		if (queens_b >= 1) {
			imbalance -= pawns_w * 3;
			//imbalance -= bishops_b * 4;
			//imbalance -= rooks_w * 8; 
			//imbalance -= (knights_b + bishops_b) * 4;
		}
		
		// Sum all the individual component scores
		int score_mg = material_mg + psqt_mg + imbalance + pawns_mg + pieces_mg + mobility_mg + tempo;
		int score_eg = material_eg + psqt_eg + imbalance + pawns_eg + pieces_eg + mobility_eg;

		// Scale down endgame score for bishops of opposite colors
		if (npm_w == 3 && npm_b == 3 && bishops_w == 1 && bishops_b == 1 && bishopParity == 1) {
			// Count pawn asymmetry, i.e., number of unopposed pawns
			int asymmetry = 0;
			for (int i = 0; i < 8; i++)
				if (   (pawnFile_w[i][0] == 0 && pawnFile_b[i][0] != 0)
					|| (pawnFile_b[i][0] == 0 && pawnFile_w[i][0] != 0))
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
		int minPhase = ENDGAME_PHASE_LIMIT, maxPhase = MIDGAME_PHASE_LIMIT;
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
	 * Returns the number of attacked squares not occupied by friendly pieces.
	 */
	private static int mobilityDelta(int[] board, int side, int start, int[] delta, boolean slider) {
		int count = 0;
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while (Position.isLegalIndex(target) && board[target] * side <= 0) {
				count++;
				if (!slider || board[target] != PIECE_NONE) break;
				target += delta[i];
			}
		}
		return count;
	}
}