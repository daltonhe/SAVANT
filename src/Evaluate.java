/**
 * @author Dalton He
 * created 11-05-18
 */
public class Evaluate implements Definitions {
	
	/**
	 * Returns the position evaluation (in centipawns). Positive means good for White, negative
	 * means good for Black.
	 * @param board - The board state to be evaluated
	 * @return Position evaluation
	 */
	public static int staticEval(Board board) {
		// Initialize evaluation score components
		double phase    = 0;
		int material_mg = 0, material_eg = 0;
		int psqt_mg     = 0, psqt_eg     = 0;
		int pawns_mg    = 0, pawns_eg    = 0;
		int pieces_mg   = 0, pieces_eg   = 0;
		int mobility_mg = 0, mobility_eg = 0;
		int imbalance   = 0;	
		int tempo       = VALUE_TEMPO * board.sideToMove;
		
		int wKingPos = 0, bKingPos = 0;
		int[] pieces       = new int[13];   // pieces[pieceType + 6]
		int[][] whitePawns = new int[9][2]; // pawns[fileA..H][count|rank of least advanced pawn]
											// pawns[8][pawns on light squares|dark squares]
		int[][] blackPawns = new int[9][2];
		for (int i = 0; i < 8; i++)
			blackPawns[i][1] = 7;
		
		// First pass: count pieces, material, and phase.
		for (int index = a8; index <= h1; index++) {
			if ((index & 0x88) != 0)
				continue;

			int piece = board.board[index];
			
			if (piece == 0)
				continue;
			
			pieces[piece + 6]++;
			phase += PHASE_WEIGHT[Math.abs(piece)];
			int rank = index / 16, file = index % 16;
			
			if (piece > 0) {
				if (piece == W_PAWN) {
					whitePawns[file][0]++;
					whitePawns[8][(file + rank) % 2]++;
					if (rank > whitePawns[file][1])
						whitePawns[file][1] = rank;
				}
				else if (piece == W_KING)
					wKingPos = index;
				
				material_mg += PIECE_VALUE_MG[piece];
				material_eg += PIECE_VALUE_EG[piece];
				psqt_mg += PSQT_MG[piece][rank][file];
				psqt_eg += PSQT_EG[piece][rank][file];
			} 
			else if (piece < 0) {
				if (piece == B_PAWN) {
					blackPawns[file][0]++;
					blackPawns[8][(file + rank) % 2]++;
					if (rank < blackPawns[file][1])
						blackPawns[file][1] = rank;
				}
				else if (piece == B_KING)
					bKingPos = index;
				
				material_mg -= PIECE_VALUE_MG[-piece];
				material_eg -= PIECE_VALUE_EG[-piece];
				psqt_mg -= PSQT_MG[-piece][7 - rank][file];
				psqt_eg -= PSQT_EG[-piece][7 - rank][file];
			}
		}
		
		// Second pass: calculate mobility, and evaluate pieces and pawns.
		for (int index = a8; index <= h1; index++) {
			if ((index & 0x88) != 0)
				continue;
			
			int piece = board.board[index];	
			
			if (piece == 0)
				continue;
			
			int side = (piece > 0 ? WHITE : BLACK);
			int rank = index / 16, file = index % 16;
			int count = -1;
			
			switch (Math.abs(piece)) {
			
			case PAWN:
				boolean opposed, passed, supported, supported_twice, phalanx, connected,
				doubled, isolated;
				
				if (piece == W_PAWN) {
					// First flag the pawn
					opposed         =   blackPawns[file][0] > 0
							         && rank > blackPawns[file][1];
					passed          =   !opposed
									 && (file == 0 || rank <= blackPawns[file - 1][1])
				              		 && (file == 7 || rank <= blackPawns[file + 1][1]);
					supported       =   (file != 0 && board.board[index + 15] == W_PAWN)
								     || (file != 7 && board.board[index + 17] == W_PAWN);
					supported_twice =   (file != 0 && board.board[index + 15] == W_PAWN)
								     && (file != 7 && board.board[index + 17] == W_PAWN);
					phalanx         =   board.board[index - 1] == W_PAWN
							         || board.board[index + 1] == W_PAWN;
					connected       =   supported || phalanx;
					doubled         =   !supported && board.board[index + 16] == W_PAWN;
					isolated        =   (file == 0 || whitePawns[file - 1][0] == 0)
							         && (file == 7 || whitePawns[file + 1][0] == 0);
					
					// Bonus for passed pawns depending on rank and file. Any pawn which is
					// unopposed and cannot be contested by an enemy pawn is considered passed.
					if (passed) {
						pawns_mg += PASSED_PAWN_MG[rank][file];
						pawns_eg += PASSED_PAWN_EG[rank][file];
					}
					// Penalty for doubled pawns. Any pawn which has a friendly pawn directly
					// behind it and is not supported diagonally is considered doubled.
					if (doubled) {
						pawns_mg += VALUE_DOUBLED_PAWN_MG;
						pawns_eg += VALUE_DOUBLED_PAWN_EG;
					}
					// Penalty for isolated pawns. Any pawn which has no friendly pawn on an
					// adjacent file is considered isolated.
					if (isolated) {
						pawns_mg += VALUE_ISOLATED_PAWN_MG;
						pawns_eg += VALUE_ISOLATED_PAWN_EG;
					}
					// Bonus for connected pawns. Any pawn which is supported diagonally or
					// adjacent to a friendly pawn (phalanx) is considered connected. Bonus is
					// adjusted based on rank, whether the pawn is in a phalanx, whether the
					// pawn is opposed, and the number of supporting pawns.
					if (connected) {
						double connectedBonus = VALUE_CONNECTED_PAWN[rank];
						if (phalanx)
							connectedBonus *= 1.4;
						if (opposed)
							connectedBonus *= 0.5;
						if (supported) {
							connectedBonus += VALUE_SUPPORTED_PAWN;
							if (supported_twice)
								connectedBonus += VALUE_SUPPORTED_PAWN;
						}
						pawns_mg += (int) connectedBonus;
						// In the endgame only the 4th through 7th ranks receive a bonus.
						pawns_eg += (int) (connectedBonus * (5 - rank) / 4);
					}
				} 
				// Black Pawn
				else {
					opposed         =   whitePawns[file][0] > 0
					          		 && rank < whitePawns[file][1];
					passed          =   !opposed
									 && (file == 0 || rank >= whitePawns[file - 1][1])
					        	     && (file == 7 || rank >= whitePawns[file + 1][1]);
					supported       =   (file != 0 && board.board[index - 17] == B_PAWN)
								     || (file != 7 && board.board[index - 15] == B_PAWN);
					supported_twice =   (file != 0 && board.board[index - 17] == B_PAWN)
									 && (file != 7 && board.board[index - 15] == B_PAWN);
					phalanx         =   board.board[index - 1] == B_PAWN
					          		 || board.board[index + 1] == B_PAWN;
					connected       =   supported || phalanx;
					doubled         =   !supported && board.board[index - 16] == B_PAWN;
					isolated        =   (file == 0 || blackPawns[file - 1][0] == 0)
						    	     && (file == 7 || blackPawns[file + 1][0] == 0);
					
					if (passed) {
						pawns_mg -= PASSED_PAWN_MG[7 - rank][file];
						pawns_eg -= PASSED_PAWN_EG[7 - rank][file];
					}
					if (doubled) {
						pawns_mg -= VALUE_DOUBLED_PAWN_MG;
						pawns_eg -= VALUE_DOUBLED_PAWN_EG;
					}
					if (isolated) {
						pawns_mg -= VALUE_ISOLATED_PAWN_MG;
						pawns_eg -= VALUE_ISOLATED_PAWN_EG;
					}
					if (connected) {
						double connectedBonus = VALUE_CONNECTED_PAWN[7 - rank];
						if (phalanx)
							connectedBonus *= 1.4;
						if (opposed)
							connectedBonus *= 0.5;
						if (supported) {
							connectedBonus += VALUE_SUPPORTED_PAWN;
							if (supported_twice)
								connectedBonus += VALUE_SUPPORTED_PAWN;
						}
						pawns_mg -= (int) connectedBonus;
						pawns_eg -= (int) (connectedBonus * (rank - 2) / 4);
					}
				}
				break;
				
			case KNIGHT:
				count = mobilityDelta(board, side, index, DELTA_KNIGHT, false);
				break;
				
			case BISHOP:
				if (piece == W_BISHOP) {
					// Give a penalty for the number of pawns on the same color square
					// as the bishop
					int bishopPawns = whitePawns[8][(rank + file) % 2];
					pieces_mg += bishopPawns * VALUE_PAWN_ON_BISHOP_COLOR_MG;
					pieces_eg += bishopPawns * VALUE_PAWN_ON_BISHOP_COLOR_EG;
					// Give a penalty for a trapped bishop. This is to prevent the common
					// mistake of greedily capturing a pawn, e.g. Bxa7 b6, where the bishop is
					// then trapped.
					if (index == a7 || index == b8 || index == a6) {
						if (board.board[index + 17] == B_PAWN) {
							pieces_mg += VALUE_TRAPPED_BISHOP;
							pieces_eg += VALUE_TRAPPED_BISHOP;
						}
					}
					if (index == h7 || index == g8 || index == h6) {
						if (board.board[index + 15] == B_PAWN) {
							pieces_mg += VALUE_TRAPPED_BISHOP;
							pieces_eg += VALUE_TRAPPED_BISHOP;
						}
					}
				}
				// Black Bishop
				else {
					int bishopPawns = blackPawns[8][(rank + file) % 2];
					pieces_mg -= bishopPawns * VALUE_PAWN_ON_BISHOP_COLOR_MG;
					pieces_eg -= bishopPawns * VALUE_PAWN_ON_BISHOP_COLOR_EG;
					if (index == a2 || index == b1 || index == a3) {
						if (board.board[index - 15] == W_PAWN) {
							pieces_mg -= VALUE_TRAPPED_BISHOP;
							pieces_eg -= VALUE_TRAPPED_BISHOP;
						}
					}
					if (index == h2 || index == g1 || index == h3) {
						if (board.board[index - 17] == W_PAWN) {
							pieces_mg -= VALUE_TRAPPED_BISHOP;
							pieces_eg -= VALUE_TRAPPED_BISHOP;
						}
					}
					
				}
				count = mobilityDelta(board, side, index, DELTA_BISHOP, true);
				break;
				
			case ROOK:
				if (piece == W_ROOK) {
					// Give a bonus for rooks on open and semi-open files. A file without
					// any pawns of either color is considered open. A file with an enemy
					// pawn but no friendly pawn is considered semi-open.
					if (whitePawns[file][0] == 0) {
						if (blackPawns[file][0] == 0) {
							pieces_mg += VALUE_ROOK_OPEN_FILE_MG;
							pieces_eg += VALUE_ROOK_OPEN_FILE_EG;
						} else {
							pieces_mg += VALUE_ROOK_SEMIOPEN_FILE_MG;
							pieces_eg += VALUE_ROOK_SEMIOPEN_FILE_EG;
						}
					}
					// Give a bonus for a rook on the 7th rank with the enemy king on the
					// 8th rank
					if (rank == 1 && bKingPos / 16 == 0) {
						pieces_mg += VALUE_ROOK_ON_7TH_MG;
						pieces_eg += VALUE_ROOK_ON_7TH_EG;
					}
					// Give a penalty for a rook trapped by its own uncastled king
					if (index == a1 || index == a2 || index == b1) {
						if (wKingPos == c1 || wKingPos == b1)
							pieces_mg += VALUE_TRAPPED_ROOK;
					}
					if (index == h1 || index == h2 || index == g1) {
						if (wKingPos == g1 || wKingPos == f1)
							pieces_mg += VALUE_TRAPPED_ROOK;
					}
				}
				// Black Rook
				else {
					if (blackPawns[file][0] == 0) {
						if (whitePawns[file][0] == 0) {
							pieces_mg -= VALUE_ROOK_OPEN_FILE_MG;
							pieces_eg -= VALUE_ROOK_OPEN_FILE_EG;
						} else {
							pieces_mg -= VALUE_ROOK_SEMIOPEN_FILE_MG;
							pieces_eg -= VALUE_ROOK_SEMIOPEN_FILE_EG;
						}
					}
					if (rank == 6 && wKingPos / 16 == 7) {
						pieces_mg -= VALUE_ROOK_ON_7TH_MG;
						pieces_eg -= VALUE_ROOK_ON_7TH_EG;
					}
					if (index == a8 || index == a7 || index == b8) {
						if (bKingPos == c8 || bKingPos == b8)
							pieces_mg -= VALUE_TRAPPED_ROOK;
					}
					if (index == h8 || index == h7 || index == g8) {
						if (bKingPos == g8 || bKingPos == f8)
							pieces_mg -= VALUE_TRAPPED_ROOK;
					}
				}
				count = mobilityDelta(board, side, index, DELTA_ROOK, true);
				break;
				
			case QUEEN:
				if (piece == W_QUEEN) {
					// Give a bonus for a queen on the 7th rank with the enemy king on the
					// 8th rank
					if (rank == 1 && bKingPos / 16 == 0) {
						pieces_mg += VALUE_QUEEN_ON_7TH_MG;
						pieces_eg += VALUE_QUEEN_ON_7TH_EG;
					}
				}
				// Black Queen
				else {
					if (rank == 6 && wKingPos / 16 == 7) {
						pieces_mg -= VALUE_QUEEN_ON_7TH_MG;
						pieces_eg -= VALUE_QUEEN_ON_7TH_EG;
					}
				}
				count = mobilityDelta(board, side, index, DELTA_QUEEN, true);
				break;
			}
			
			if (count != -1) {
				mobility_mg += side * MOBILITY_MG[Math.abs(piece)][count];
				mobility_eg += side * MOBILITY_EG[Math.abs(piece)][count];
			}
		}
		
		// Drastically reduce the value of minor pieces if we have no pawns. This is used
		// to avoid situations with insufficient mating material.
		if (pieces[W_PAWN + 6] == 0)
			imbalance += (pieces[W_KNIGHT + 6] + pieces[W_BISHOP + 6]) * VALUE_MINOR_WITH_NO_PAWNS;
		if (pieces[B_PAWN + 6] == 0)
			imbalance -= (pieces[B_KNIGHT + 6] + pieces[B_BISHOP + 6]) * VALUE_MINOR_WITH_NO_PAWNS;
		
		// Give a bonus for having the bishop pair
		if (pieces[W_BISHOP + 6] == 2)
			imbalance += VALUE_BISHOP_PAIR;
		if (pieces[B_BISHOP + 6] == 2)
			imbalance -= VALUE_BISHOP_PAIR;
		
		// Give penalties for redundancy of major pieces
		if (pieces[W_ROOK + 6] >= 1) {
			imbalance += pieces[W_ROOK + 6] * VALUE_REDUNDANT_ROOK;
			imbalance += pieces[W_QUEEN + 6] * pieces[W_ROOK + 6] * VALUE_REDUNDANT_QUEEN;
		}
		if (pieces[B_ROOK + 6] >= 1) {
			imbalance -= pieces[B_ROOK + 6] * VALUE_REDUNDANT_ROOK;
			imbalance -= pieces[B_QUEEN + 6] * pieces[B_ROOK + 6] * VALUE_REDUNDANT_QUEEN;
		}
		
		// Give a bonus to Knights for having more pawns on the board, and a similar bonus to
		// Rooks for having fewer pawns on the board
		imbalance += pieces[W_KNIGHT + 6] * (pieces[W_PAWN + 6] - 5) * VALUE_KNIGHT_PAWN_SYNERGY;
		imbalance -= pieces[B_KNIGHT + 6] * (pieces[B_PAWN + 6] - 5) * VALUE_KNIGHT_PAWN_SYNERGY;
		imbalance += pieces[W_ROOK + 6]   * (pieces[W_PAWN + 6] - 5) * VALUE_ROOK_PAWN_SYNERGY;
		imbalance -= pieces[B_ROOK + 6]   * (pieces[B_PAWN + 6] - 5) * VALUE_ROOK_PAWN_SYNERGY;
		
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

		// Scale the phase from 0 (endgame) to 1 (middlegame)
		int minPhase = ENDGAME_PHASE_LIMIT, maxPhase = MIDGAME_PHASE_LIMIT;
		phase = Math.max(minPhase, Math.min(phase, maxPhase));
		phase = (phase - minPhase) / (maxPhase - minPhase);
		
		// Calculate the tapered evaluation. The final score is interpolated between separate
		// middle and endgame scores based on the phase.
		return (int) (score_mg * phase + score_eg * (1 - phase));	
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
	private static int mobilityDelta(Board board, int side, int start, int[] delta, boolean sliding) {
		int count = 0;
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while ((target & 0x88) == 0 && board.board[target] * side <= 0) {
				count++;
				if (!sliding || board.board[target] != 0) {
					break;
				}
				target += delta[i];
			}
		}
		return count;
	}
}
