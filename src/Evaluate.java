/**
 * @author Dalton He
 * created 11-05-18
 */
public class Evaluate implements Definitions {
	
	/**
	 * Returns the static position evaluation in centipawns. Positive scores are good for White,
	 * while negative scores are good for black.
	 * @param board - The board state to be evaluated
	 * @return        Score of the position
	 */
	public static int staticEval(int[] board, int sideToMove) {
		// Initialize evaluation score components
		int phase        = 0;
		int material_mg  = 0, material_eg  = 0;
		int psqt_mg      = 0, psqt_eg      = 0;
		int pawns_mg     = 0, pawns_eg     = 0;
		int pieces_mg    = 0, pieces_eg    = 0;
		int mobility_mg  = 0, mobility_eg  = 0;
		int imbalance    = 0;	
		int tempo        = TEMPO * sideToMove;
		
		int whiteKingPos = 0, blackKingPos = 0;
		int whitePawns   = 0, blackPawns   = 0;
		int whiteKnights = 0, blackKnights = 0;
		int whiteBishops = 0, blackBishops = 0;
		int whiteRooks   = 0, blackRooks   = 0;
		int whiteQueens  = 0, blackQueens  = 0;
		// pawns[fileA..H][count|rank of least advanced pawn]
		// pawns[8][pawns on light squares|dark squares]
		int[][] whitePawnFile = new int[9][2]; 
		int[][] blackPawnFile = new int[9][2];
		for (int i = 0; i < 8; i++)
			blackPawnFile[i][1] = 7;
		
		// First pass: count pieces, material, and phase.
		for (int index = SQ_a8; index <= SQ_h1; index++) {
			if ((index & 0x88) != 0)
				continue;

			int piece = board[index];
			
			if (piece == 0)
				continue;
			
			phase += PHASE_WEIGHT[Math.abs(piece)];
			int rank = index / 16, file = index % 16;
			
			switch (piece) {
			
			case W_PAWN:
				whitePawns++;
				whitePawnFile[file][0]++;
				whitePawnFile[8][(file + rank) % 2]++;
				if (rank > whitePawnFile[file][1])
					whitePawnFile[file][1] = rank;
				break;
				
			case W_KNIGHT:
				whiteKnights++;
				break;
				
			case W_BISHOP:
				whiteBishops++;
				break;
				
			case W_ROOK:
				whiteRooks++;
				break;
				
			case W_QUEEN:
				whiteQueens++;
				break;
			
			case W_KING:
				whiteKingPos = index;
				break;
			
			case B_PAWN:
				blackPawns++;
				blackPawnFile[file][0]++;
				blackPawnFile[8][(file + rank) % 2]++;
				if (rank < blackPawnFile[file][1])
					blackPawnFile[file][1] = rank;
				break;
			
			case B_KNIGHT:
				blackKnights++;
				break;
			
			case B_BISHOP:
				blackBishops++;
				break;
			
			case B_ROOK:
				blackRooks++;
				break;
			
			case B_QUEEN:
				blackQueens++;
				break;
			
			case B_KING:
				blackKingPos = index;
				break;
			}
			
			if (piece > 0) {
				material_mg += PIECE_VALUE_MG[piece];
				material_eg += PIECE_VALUE_EG[piece];
				psqt_mg     += PSQT_MG[piece][rank][file];
				psqt_eg     += PSQT_EG[piece][rank][file];
			}
			else {
				material_mg -= PIECE_VALUE_MG[-piece];
				material_eg -= PIECE_VALUE_EG[-piece];
				psqt_mg     -= PSQT_MG[-piece][7 - rank][file];
				psqt_eg     -= PSQT_EG[-piece][7 - rank][file];
			}
		}
		
		int whitePieces = whiteKnights + whiteBishops + whiteRooks + whiteQueens;
		int blackPieces = blackKnights + blackBishops + blackRooks + blackQueens;
		
		// In KX vs K positions, the only terms that matter are enemy king proximity to the
		// corner, and proximity between the two kings.
		if (blackPawns + blackPieces == 0 && whitePieces > 0) {
			int cornered = CORNER_PROXIMITY[blackKingPos / 16][blackKingPos % 16];
			int kingDist = Math.max(Math.abs(blackKingPos / 16 - whiteKingPos / 16),
		            Math.abs(blackKingPos % 16 - whiteKingPos % 16));
			int kingProximity = KING_PROXIMITY[kingDist];
			return material_mg + (cornered + kingProximity) * 10;
		}
		if (whitePawns + whitePieces == 0 && blackPieces > 0) {
			int cornered = CORNER_PROXIMITY[whiteKingPos / 16][whiteKingPos % 16];
			int kingDist = Math.max(Math.abs(blackKingPos / 16 - whiteKingPos / 16),
						            Math.abs(blackKingPos % 16 - whiteKingPos % 16));
			int kingProximity = KING_PROXIMITY[kingDist];
			return material_mg - (cornered + kingProximity) * 10;
		}
		
		// Second pass: calculate mobility, and evaluate pieces and pawns.
		for (int index = SQ_a8; index <= SQ_h1; index++) {
			if ((index & 0x88) != 0)
				continue;
			
			int piece = board[index];	
			
			if (piece == 0)
				continue;
			
			int side = (piece > 0 ? WHITE : BLACK);
			int rank = index / 16, file = index % 16;
			int squares;
			
			switch (piece) {
			case W_PAWN:
				boolean opposed, passed, supported, supported_twice, phalanx, connected,
				doubled, isolated;

				// First flag the pawn
				opposed         =   blackPawnFile[file][0] > 0
						         && rank > blackPawnFile[file][1];
				passed          =   !opposed
								 && (file == 0 || rank <= blackPawnFile[file - 1][1])
			              		 && (file == 7 || rank <= blackPawnFile[file + 1][1]);
				supported       =   (file != 0 && board[index + 15] == W_PAWN)
							     || (file != 7 && board[index + 17] == W_PAWN);
				supported_twice =   (file != 0 && board[index + 15] == W_PAWN)
							     && (file != 7 && board[index + 17] == W_PAWN);
				phalanx         =   board[index - 1] == W_PAWN
						         || board[index + 1] == W_PAWN;
				connected       =   supported || phalanx;
				doubled         =   !supported && board[index + 16] == W_PAWN;
				isolated        =   (file == 0 || whitePawnFile[file - 1][0] == 0)
						         && (file == 7 || whitePawnFile[file + 1][0] == 0);
				
				// Bonus for passed pawns depending on rank and file. Any pawn which is
				// unopposed and cannot be contested by an enemy pawn is considered passed.
				if (passed) {
					pawns_mg += PASSED_PAWN_MG[rank][file];
					pawns_eg += PASSED_PAWN_EG[rank][file];
				}
				// Penalty for doubled pawns. Any pawn which has a friendly pawn directly
				// behind it and is not supported diagonally is considered doubled.
				if (doubled) {
					pawns_mg += DOUBLED_PAWN_MG;
					pawns_eg += DOUBLED_PAWN_EG;
				}
				// Penalty for isolated pawns. Any pawn which has no friendly pawn on an
				// adjacent file is considered isolated.
				if (isolated) {
					pawns_mg += ISOLATED_PAWN_MG;
					pawns_eg += ISOLATED_PAWN_EG;
				}
				// Bonus for connected pawns. Any pawn which is supported diagonally or
				// adjacent to a friendly pawn (phalanx) is considered connected. Bonus is
				// adjusted based on rank, whether the pawn is in a phalanx, whether the
				// pawn is opposed, and the number of supporting pawns.
				if (connected) {
					double connectedBonus = CONNECTED_PAWN[rank];
					if (phalanx)
						connectedBonus *= 1.4;
					if (opposed)
						connectedBonus *= 0.5;
					if (supported) {
						connectedBonus += SUPPORTED_PAWN;
						if (supported_twice)
							connectedBonus += SUPPORTED_PAWN;
					}
					pawns_mg += (int) connectedBonus;
					// In the endgame only the 4th through 7th ranks receive a bonus.
					pawns_eg += (int) (connectedBonus * (5 - rank) / 4);
				}
				break;
				
			case B_PAWN:
				opposed         =   whitePawnFile[file][0] > 0
				          		 && rank < whitePawnFile[file][1];
				passed          =   !opposed
								 && (file == 0 || rank >= whitePawnFile[file - 1][1])
				        	     && (file == 7 || rank >= whitePawnFile[file + 1][1]);
				supported       =   (file != 0 && board[index - 17] == B_PAWN)
							     || (file != 7 && board[index - 15] == B_PAWN);
				supported_twice =   (file != 0 && board[index - 17] == B_PAWN)
								 && (file != 7 && board[index - 15] == B_PAWN);
				phalanx         =   board[index - 1] == B_PAWN
				          		 || board[index + 1] == B_PAWN;
				connected       =   supported || phalanx;
				doubled         =   !supported && board[index - 16] == B_PAWN;
				isolated        =   (file == 0 || blackPawnFile[file - 1][0] == 0)
					    	     && (file == 7 || blackPawnFile[file + 1][0] == 0);
				
				if (passed) {
					pawns_mg -= PASSED_PAWN_MG[7 - rank][file];
					pawns_eg -= PASSED_PAWN_EG[7 - rank][file];
				}
				if (doubled) {
					pawns_mg -= DOUBLED_PAWN_MG;
					pawns_eg -= DOUBLED_PAWN_EG;
				}
				if (isolated) {
					pawns_mg -= ISOLATED_PAWN_MG;
					pawns_eg -= ISOLATED_PAWN_EG;
				}
				if (connected) {
					double connectedBonus = CONNECTED_PAWN[7 - rank];
					if (phalanx)
						connectedBonus *= 1.4;
					if (opposed)
						connectedBonus *= 0.5;
					if (supported) {
						connectedBonus += SUPPORTED_PAWN;
						if (supported_twice)
							connectedBonus += SUPPORTED_PAWN;
					}
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
				// as the bishop
				int bishopPawns = whitePawnFile[8][(rank + file) % 2];
				pieces_mg += bishopPawns * PAWN_ON_BISHOP_COLOR_MG;
				pieces_eg += bishopPawns * PAWN_ON_BISHOP_COLOR_EG;
				// Give a penalty for a trapped bishop. This is to prevent the common
				// mistake of greedily capturing a pawn, e.g. Bxa7 b6, where the bishop is
				// then trapped.
				/*if (   (index == SQ_a7 || index == SQ_b8 || index == SQ_a6)
					&& board[index + 17] == B_PAWN) {
					pieces_mg += TRAPPED_BISHOP;
					pieces_eg += TRAPPED_BISHOP;
				}
				if (   (index == SQ_h7 || index == SQ_g8 || index == SQ_h6)
					&& board[index + 15] == B_PAWN) {
					pieces_mg += TRAPPED_BISHOP;
					pieces_eg += TRAPPED_BISHOP;
				}*/
				// Bishop mobility
				squares = mobilityDelta(board, side, index, DELTA_BISHOP, true);
				mobility_mg += (squares - BISHOP_MAX_SQUARES / 2) * BISHOP_MOBILITY_MG;
				mobility_eg += (squares - BISHOP_MAX_SQUARES / 2) * BISHOP_MOBILITY_EG;
				break;
				
			case B_BISHOP:
				bishopPawns = blackPawnFile[8][(rank + file) % 2];
				pieces_mg -= bishopPawns * PAWN_ON_BISHOP_COLOR_MG;
				pieces_eg -= bishopPawns * PAWN_ON_BISHOP_COLOR_EG;
				/*if (   (index == SQ_a2 || index == SQ_b1 || index == SQ_a3)
					&& (board[index - 15] == W_PAWN)) {
					pieces_mg -= TRAPPED_BISHOP;
					pieces_eg -= TRAPPED_BISHOP;
				}
				if (   (index == SQ_h2 || index == SQ_g1 || index == SQ_h3)
					&& board[index - 17] == W_PAWN) {
					pieces_mg -= TRAPPED_BISHOP;
					pieces_eg -= TRAPPED_BISHOP;
				}*/
				squares = mobilityDelta(board, side, index, DELTA_BISHOP, true);
				mobility_mg -= (squares - BISHOP_MAX_SQUARES / 2) * BISHOP_MOBILITY_MG;
				mobility_eg -= (squares - BISHOP_MAX_SQUARES / 2) * BISHOP_MOBILITY_EG;
				break;
				
			case W_ROOK:
				// Give a bonus for rooks on open and semi-open files. A file without
				// any pawns of either color is considered open. A file with an enemy
				// pawn but no friendly pawn is considered semi-open.
				if (whitePawnFile[file][0] == 0) {
					if (blackPawnFile[file][0] == 0) {
						pieces_mg += ROOK_OPEN_FILE_MG;
						pieces_eg += ROOK_OPEN_FILE_EG;
					} else {
						pieces_mg += ROOK_SEMIOPEN_FILE_MG;
						pieces_eg += ROOK_SEMIOPEN_FILE_EG;
					}
				}
				// Give a bonus for a rook on the 7th rank with the enemy king on the
				// 8th rank
				if (rank == 1 && blackKingPos / 16 == 0) {
					pieces_mg += ROOK_ON_7TH_MG;
					pieces_eg += ROOK_ON_7TH_EG;
				}
				// Give a penalty for a rook trapped by its own uncastled king
				/*if (   (index == SQ_a1 || index == SQ_a2 || index == SQ_b1)
					&& (whiteKingPos == SQ_c1 || whiteKingPos == SQ_b1))
						pieces_mg += TRAPPED_ROOK;
				if (   (index == SQ_h1 || index == SQ_h2 || index == SQ_g1)
					&& (whiteKingPos == SQ_g1 || whiteKingPos == SQ_f1))
						pieces_mg += TRAPPED_ROOK;*/
				// Rook mobility
				squares = mobilityDelta(board, side, index, DELTA_ROOK, true);
				mobility_mg += (squares - ROOK_MAX_SQUARES / 2) * ROOK_MOBILITY_MG;
				mobility_eg += (squares - ROOK_MAX_SQUARES / 2) * ROOK_MOBILITY_EG;
				break;
				
			case B_ROOK:
				if (blackPawnFile[file][0] == 0) {
					if (whitePawnFile[file][0] == 0) {
						pieces_mg -= ROOK_OPEN_FILE_MG;
						pieces_eg -= ROOK_OPEN_FILE_EG;
					} else {
						pieces_mg -= ROOK_SEMIOPEN_FILE_MG;
						pieces_eg -= ROOK_SEMIOPEN_FILE_EG;
					}
				}
				if (rank == 6 && whiteKingPos / 16 == 7) {
					pieces_mg -= ROOK_ON_7TH_MG;
					pieces_eg -= ROOK_ON_7TH_EG;
				}
				/*if (   (index == SQ_a8 || index == SQ_a7 || index == SQ_b8)
					&& (blackKingPos == SQ_c8 || blackKingPos == SQ_b8))
						pieces_mg -= TRAPPED_ROOK;
				if (   (index == SQ_h8 || index == SQ_h7 || index == SQ_g8)
					&& (blackKingPos == SQ_g8 || blackKingPos == SQ_f8))
						pieces_mg -= TRAPPED_ROOK;*/
				squares = mobilityDelta(board, side, index, DELTA_ROOK, true);
				mobility_mg -= (squares - ROOK_MAX_SQUARES / 2) * ROOK_MOBILITY_MG;
				mobility_eg -= (squares - ROOK_MAX_SQUARES / 2) * ROOK_MOBILITY_EG;
				break;
				
			case W_QUEEN:
				// Give a bonus for a queen on the 7th rank with the enemy king on the
				// 8th rank
				if (rank == 1 && blackKingPos / 16 == 0) {
					pieces_mg += QUEEN_ON_7TH_MG;
					pieces_eg += QUEEN_ON_7TH_EG;
				}
				// Queen mobility
				squares = mobilityDelta(board, side, index, DELTA_QUEEN, true);
				mobility_mg += (squares - QUEEN_MAX_SQUARES / 2) * QUEEN_MOBILITY_MG;
				mobility_eg += (squares - QUEEN_MAX_SQUARES / 2) * QUEEN_MOBILITY_EG;
				break;
				
			case B_QUEEN:
				if (rank == 6 && whiteKingPos / 16 == 7) {
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
		imbalance += (whitePawnFile[0][0] + whitePawnFile[7][0]) * ROOK_PAWN;
		imbalance -= (blackPawnFile[0][0] + blackPawnFile[7][0]) * ROOK_PAWN;
		
		// Give a bonus for having the bishop pair. If the bishop pair is unopposed, i.e.
		// the opponent has no minor pieces to contest the bishops, the bonus is larger.
		if (whiteBishops >= 2)
			imbalance += (blackKnights + blackBishops == 0 ? UNOPPOSED_BISHOP_PAIR : BISHOP_PAIR);
		
		if (blackBishops >= 2) {
			imbalance -= (whiteKnights + whiteBishops == 0 ? UNOPPOSED_BISHOP_PAIR : BISHOP_PAIR);
		}
		// Give a small penalty for having the knight pair
		if (whiteKnights >= 2)
			imbalance += KNIGHT_PAIR;
		if (blackKnights >= 2)
			imbalance -= KNIGHT_PAIR;
		
		// Give penalties for redundant major pieces
		if (whiteRooks >= 1) {
			imbalance += (whiteRooks - 1) * REDUNDANT_ROOK;
			imbalance += whiteQueens * whiteRooks * REDUNDANT_QUEEN;
		}
		if (blackRooks >= 1) {
			imbalance -= (blackRooks - 1) * REDUNDANT_ROOK;
			imbalance -= blackQueens * blackRooks * REDUNDANT_QUEEN;
		}
		
		// Give a bonus to Knights for having more pawns on the board, and a similar bonus to
		// Rooks for having fewer pawns on the board
		imbalance += whiteKnights * (whitePawns - 5) * KNIGHT_PAWN_SYNERGY;
		imbalance -= blackKnights * (blackPawns - 5) * KNIGHT_PAWN_SYNERGY;
		imbalance += whiteRooks   * (whitePawns - 5) * ROOK_PAWN_SYNERGY;
		imbalance -= blackRooks   * (blackPawns - 5) * ROOK_PAWN_SYNERGY;
		
		// Sum all the individual component scores
		int score_mg =  material_mg 
				      + psqt_mg 
				      + imbalance 
				      + pawns_mg 
				      + pieces_mg 
				      + mobility_mg 
				      + tempo;
		int score_eg =  material_eg 
				      + psqt_eg 
				      + imbalance
				      + pawns_eg 
				      + pieces_eg
				      + mobility_eg;

		// Calculate the middle and endgame weights
		int minPhase = ENDGAME_PHASE_LIMIT, maxPhase = MIDGAME_PHASE_LIMIT;
		phase = Math.max(minPhase, Math.min(phase, maxPhase));
		double weight_mg = (phase - minPhase) / (double) (maxPhase - minPhase);
		double weight_eg = 1 - weight_mg;
		
		// Calculate the tapered evaluation. This is the score interpolated between separate
		// middle and endgame scores, weighted by the phase, in order to transition smoothly
		// between middle and endgame.
		return (int) (score_mg * weight_mg + score_eg * weight_eg);

	}
	
	/**
	 * Returns the number of attacked squares not occupied by friendly pieces.
	 * @param side
	 * @param board
	 * @param mobArea
	 * @param start
	 * @param delta
	 * @param sliding
	 * @return
	 */
	private static int mobilityDelta(int[] board, int side, int start, int[] delta, boolean sliding) {
		int count = 0;
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while ((target & 0x88) == 0 && board[target] * side <= 0) {
				count++;
				if (!sliding || board[target] != 0) {
					break;
				}
				target += delta[i];
			}
		}
		return count;
	}
}
