import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

import org.w3c.dom.Node;

/**
 * 
 * @author Dalton He
 * created 10-22-18
 */
public class Engine implements Definitions {
	public static int maxDepth       = 100;   // max depth to search
	public static boolean uciMode    = false; // true if we are in UCI mode
	public static double timeLeft    = TIME_INF; // total time remaining
	public static double increment   = 1000;  // increment per move
	public static double timeForMove = 100;   // time for this move
	public static boolean useBook    = true;  // true if using native opening book
	
	public static int currentDepth;           // current depth of search
	public static long startTime;             // time the search was started
	public static boolean abortedSearch;      // true if the search was ended early
	
	public static int[][] historyScore;       // history heuristic move scores
	public static HashtableEntry[] ttable;	  // transposition table
	public static HashtableEntry[] pvtable;   // hash table to store PV moves

	public static ArrayList<Move> pv;         // the principal variation
	public static Move bestMove;              // best move so far
	public static Move prevBestMove;          // last iteration's best move
	public static int eval;                   // score of the position
	public static long nodes;                 // number of nodes searched

	/**
	 * Resets engine fields in preparation for a new search.
	 */
	public static void initializeSearch() {
		currentDepth   = 0;
		abortedSearch  = false;
		historyScore   = new int[13][120];
		pvtable        = new HashtableEntry[HASH_SIZE_PV];
		pv             = new ArrayList<Move>();
		bestMove       = null;
		prevBestMove   = null;
		eval           = 0;
		nodes          = 0;
		updateTT();
	}
	
	/**
	 * Searches the given position and reports the expected line and score (stored as static
	 * fields pv and eval).
	 */
	public static void search(Position pos) {
		initializeSearch();
		
		// Calculate the time to use for this move
		if (timeLeft > increment) {
			timeForMove = timeLeft / 40 + increment;
			if (pos.moveNumber <= 20)
				timeForMove *= 2;
		}
		else
			timeForMove = timeLeft / 5;
		
		// Start the timer
		startTime = System.currentTimeMillis();
		
		// The iterative deepening loop
		for (currentDepth = 1; currentDepth <= maxDepth; currentDepth++) {
			
			// Clear the PV hash table
			pvtable = new HashtableEntry[HASH_SIZE_PV];
			
			// For the first few depths, start with an infinite search window.
			if (currentDepth < 5)
				eval = alphaBeta(pos, currentDepth, 0, -VALUE_INF, VALUE_INF, true, NODE_PV);
			
			// For later depths, try an aspirated search with a window around the previous
			// iteration's eval.
			else {
				int delta = INITIAL_WINDOW_SIZE;
				int alpha = Math.max(eval - delta, -VALUE_INF);
				int beta  = Math.min(eval + delta, VALUE_INF);
				
				// Start with a small aspiration window. If we fail high/low, re-search with a
				// bigger window until we succeed.
				while (true) {
					
					eval = alphaBeta(pos, currentDepth, 0, alpha, beta, true, NODE_PV);

					// Use last iteration's move if the search was terminated early
					if (abortedSearch) {
						bestMove = prevBestMove;
						break;
					}
					
					// In case of failing low/high increase aspiration window and re-search,
					// otherwise exit the loop.
					if (eval <= alpha) {
						beta = (alpha + beta) / 2;
						alpha = Math.max(eval - delta, -VALUE_INF);
					}
					else if (eval >= beta)
						beta = Math.min(eval + delta, VALUE_INF);
					else
						break;
					
					// Increase window width
					delta += delta / 4 + 5;
					
					assert(alpha >= -VALUE_INF && beta <= VALUE_INF);
				}
			}
			
			// Stop searching if time is up
			if (abortedSearch)
				break;
			
			// Check the timer
			long endTime = System.currentTimeMillis();
			double timeElapsed = endTime - startTime;
			double decimalTime = timeElapsed / 1000.0;
			
			// Allocate more time if the best move is not the same as last iteration's best move
			//if (currentDepth > 5 && !bestMove.equals(prevBestMove))
				//timeForMove = Math.min(timeForMove * 1.2, timeLeft / 5);
			
			// Update the pv and previous best move
			pv = extractPV(pos);
			prevBestMove = bestMove;
			
			// Update the GUI
			String pvString;
			if (uciMode) {
				pvString = "";
				for (Move move : pv)
					pvString += move.longNotation() + " ";
				System.out.println(  "info score cp " + eval
								   + " depth "        + currentDepth
								   + " nodes "        + nodes
								   + " nps "          + 0
								   + " time "         + (int) timeElapsed
								   + " pv "           + pvString);
			}
			else {
				pvString = pv.toString().replace(",", "");
				pvString = pvString.substring(1, pvString.length() - 1);
				System.out.println(  "(d=" + currentDepth + ") "
								   + bestMove
								   + " [" + eval / 100.0 + "] "
								   + pvString 
								   + " (n=" + nodes + " t=" + decimalTime + "s)");
			}
			
			// Stop searching if a forced mate was found, or we have only one legal move,
			// or the time left is likely not enough to search the next depth
			if (   Math.abs(eval) > VALUE_MATE_THRESHOLD
				|| pos.filterLegal(pos.generateMoves(false)).size() == 1
				|| timeElapsed >= timeForMove / 2)
				break;
		}
	}
	
	/**
	 * Extracts the principal variation from the hash table and returns it as list of moves.
	 */
	public static ArrayList<Move> extractPV(Position pos) {
		ArrayList<Move> PV = new ArrayList<Move>();
		HashtableEntry hash = getEntry(pos.zobrist, pvtable);
		while (hash != null && hash.move != null) {
			Move move = getMoveObject(pos, hash.move);
			// In the rare case of a key collision the stored move may be invalid
			if (move == null)
				break;
			
			PV.add(move);
			pos.makeMove(move);
			
			// Check for a repetition cycle
			long zobrist = pos.zobrist;
			if (pos.sideToMove == BLACK)
				zobrist ^= Zobrist.side;
			HashtableEntry rentry = getEntry(zobrist, pos.reptable);
			if (rentry != null && rentry.count >= 2)
				break;

			hash = getEntry(pos.zobrist, pvtable);
		}
		
		// Unmake all the moves
		for (int j = PV.size() - 1; j >= 0; j--)
			pos.unmakeMove(PV.get(j));

		return PV;
	}

	/**
	 * The alpha-beta recursive search.
	 * @param pos     - The position we are searching
	 * @param ply     - Remaining number of plies to search
	 * @param ext     - Number of plies we have extended the search
	 * @param alpha   - Highest score that we have found (lower bound)
	 * @param beta    - Lowest score that our opponent can guarantee (upper bound)
	 * @param canNull - true if we should try a null move, false if the last move was a null move
	 * @return          The score of the position from the perspective of the side to move
	 */
	private static int alphaBeta(Position pos,
								 int ply,
								 int ext,
								 int alpha,
								 int beta,
								 boolean canNull,
								 int nodeType) {
		
		assert(nodeType == NODE_PV || nodeType == NODE_CUT || nodeType == NODE_ALL);
		
		// Check if time is up every 1000 nodes
		if (abortedSearch)
			return 0;
		
		if (nodes % 1000 == 0) {
			if ((System.currentTimeMillis() - startTime) > timeForMove) {
				abortedSearch = true;
				return 0;
			}
		}

		// Enter quiescence search once we reach a leaf node
		if (ply <= DEPTH_HORIZON)
			return quiescence(pos, alpha, beta);
		
		// Increment node count
		nodes++;
		
		boolean rootNode = (nodeType == NODE_PV && ply == currentDepth);
		int eval = 0;

		if (!rootNode) {
			// Check for draw by repetition or 50 move rule
			if (pos.nullCount == 0) {				
				if (pos.fiftyMoves >= 100)
					return VALUE_DRAW;
				
				long zobrist = pos.zobrist;
				
				// fix parity issue so that zobrist keys are the same irrespective of the 
				// side to move
				if (pos.sideToMove == BLACK)
					zobrist ^= Zobrist.side;
				
				HashtableEntry rentry = getEntry(zobrist, pos.reptable);
				if (rentry != null && rentry.count >= 2)
					return VALUE_DRAW;
			}
			
			// Check for draw by insufficient material
			if (pos.insufficientMaterial())
				return VALUE_DRAW;
			
			// Mate distance pruning. If a shorter mate was found upward in the tree then there is
			// no need to search further because we can impossibly improve alpha. Same logic but
			// with reversed signs applies in the opposite condition of being mated. In this case
			// return a fail-high score.
			alpha = Math.max(matedScore(ply - ext), alpha);
			beta  = Math.min(mateScore(ply - ext), beta);
			if (alpha >= beta)
				return alpha;
		}
				
		// At non-PV nodes check for an early transposition table cutoff
		HashtableEntry ttentry = getEntry(pos.zobrist, ttable);
		if (   nodeType != NODE_PV 
			&& ttentry != null 
			&& ttentry.depth >= ply) {
			if (    ttentry.type == BOUND_EXACT
				|| (ttentry.type == BOUND_UPPER && ttentry.eval * pos.sideToMove <= alpha)
				|| (ttentry.type == BOUND_LOWER && ttentry.eval * pos.sideToMove >= beta))
				return ttentry.eval * pos.sideToMove;
		}
		
		// Extend the search if we are in check
		boolean inCheck = pos.inCheck(pos.sideToMove);
		//int maxExt = currentDepth / 4 + 5;
		if (ply < currentDepth && inCheck) {
			ply++;
			ext++;
		}
		
		// Static evaluation of the position
		int standPat = Evaluate.staticEval(pos) * pos.sideToMove;
		
		// Razoring
		/*if (   ply < 2
			&& !inCheck
			&& standPat <= alpha - FUTILITY_MARGIN)
			return quiescence(pos, alpha, beta);*/
		
		// Reverse futility pruning
		if (   !rootNode
			&& !inCheck
			&& standPat < VALUE_KNOWN_WIN) { // Do not return unproven wins
			if (   ply == DEPTH_FRONTIER && standPat - FUTILITY_MARGIN >= beta
				|| ply == DEPTH_PRE_FRONTIER && standPat - FUTILITY_EXT_MARGIN >= beta)
				return standPat;
		}
		
		// Limited razoring
		if (   !rootNode
			&& !inCheck
			&& pos.pieceCount > 6
			&& standPat < VALUE_KNOWN_WIN
			&& ply == DEPTH_PRE_PRE_FRONTIER
			&& standPat <= alpha - RAZOR_MARGIN)
			ply--;
		
		// Null move pruning
		if (   canNull
			&& nodeType != NODE_PV
			&& !inCheck
			&& beta < VALUE_MATE_THRESHOLD
			&& !pos.isPawnEnding(pos.sideToMove)
			&& standPat >= beta) {
			
			pos.makePassingMove();
			pos.nullCount++;
			
			int R = (ply > 6 ? 3 : 2);
			eval = -alphaBeta(pos, ply - R - 1, ext, -beta, -beta + 1, false, -nodeType);
			
			pos.makePassingMove();
			pos.nullCount--;
			
			// Fail high
			if (eval >= beta) {
				// Do not return unproven mate scores
				if (eval >= VALUE_MATE_THRESHOLD)
					eval = beta;
				
				if (Math.abs(beta) < VALUE_KNOWN_WIN) {
					// Update transposition table
					addTTEntry(pos.zobrist, null, ply, eval * pos.sideToMove, BOUND_LOWER);	
					
					return beta;
				}
			}
		}
		
		// Internal iterative deepening at PV nodes if we have no hash move
		if (   nodeType == NODE_PV
			&& ply >= 3 
			&& (ttentry == null || ttentry.move == null)) {
			eval = alphaBeta(pos, ply - 2, 0, alpha, beta, false, nodeType);
			// Fail low
			if (eval <= alpha)
				eval = alphaBeta(pos, ply - 2, 0, -VALUE_INF, VALUE_INF, false, nodeType);
			ttentry = getEntry(pos.zobrist, ttable);
		}
		
		assert(-VALUE_INF <= alpha && alpha < beta && beta <= VALUE_INF);
		
		// Generate moves and sort
		ArrayList<Move> moveList = pos.generateMoves(false);
		sortMoves(pos, moveList, ttentry);

		String bestMove = null;
		int bestEval = -VALUE_INF;
		int moveCount = 0;
		
		// Loop through all the moves
		for (Move move : moveList) {

			// Make the move
			pos.makeMove(move);
			
			// Check if the move leaves our king in check
			if (pos.inCheck(-pos.sideToMove)) {
				pos.unmakeMove(move);
				continue;
			}
			
			// Increment move count
			moveCount++;
			
			boolean doFullDepthSearch = false;
			boolean givesCheck = pos.inCheck(pos.sideToMove);
			
			// Futility pruning
			if (   !inCheck 
				&& !givesCheck
				&& move.type != PROMOTION
				&& move.captured == PIECE_NONE
				&& Math.abs(alpha) < VALUE_KNOWN_WIN
				&& Math.abs(beta) < VALUE_KNOWN_WIN) {
				if (   (ply == DEPTH_FRONTIER && standPat <= alpha - FUTILITY_MARGIN)
					|| (ply == DEPTH_PRE_FRONTIER && standPat <= alpha - FUTILITY_EXT_MARGIN)) {
					pos.unmakeMove(move);
					continue;
				}
			}
			
			// Late move reductions
			if (   nodeType != NODE_PV
				&& !inCheck
				&& !givesCheck
				&& ply >= 3
			    && moveCount > 1 
			    && move.type != PROMOTION
			    && move.captured == PIECE_NONE) {

				// Increase reduction for later moves
				int R = (moveCount <= 7 ? 1 : ply / 3);
				
				// Increase reduction for cut nodes
				if (nodeType == NODE_CUT)
					R++;
				
				// Increase reduction if hash move is a capture
				/*if (ttentry != null && ttentry.move != null && ttentry.move.contains("x"))
					R++;*/
			
				eval = -alphaBeta(pos, ply - R - 1, ext, -alpha - 1, -alpha, true, NODE_CUT);
				
				doFullDepthSearch = (eval > alpha);
			}
			else
				doFullDepthSearch = true;
			
			if (doFullDepthSearch) {
				// Search first move with full width
				if (nodeType != NODE_PV || moveCount == 1)
					eval = -alphaBeta(pos, ply - 1, ext, -beta, -alpha, true, -nodeType);
				else {
					// Principal variation search
					eval = -alphaBeta(pos, ply - 1, ext, -alpha - 1, -alpha, true, NODE_CUT);
					
					// PVS failed high; do a re-search if eval < beta, otherwise let the
					// parent node fail low with value <= alpha. Re-search is done as a 
					// PV node.
					if (eval > alpha && eval < beta)
						eval = -alphaBeta(pos, ply - 1, ext, -beta, -alpha, true, NODE_PV);
				} 
			}
			
			// Unmake the move
			pos.unmakeMove(move);
			
			assert(eval > -VALUE_INF && eval < VALUE_INF);
			
			// Check for a new best move
			if (eval > bestEval) {
				bestEval = eval;
				
				if (eval > alpha) {
					bestMove = move.longNotation();
					if (rootNode)
						Engine.bestMove = move;
					
					// Update PV hash table
					if (nodeType == NODE_PV) {
						int hashKey = (int) (pos.zobrist % HASH_SIZE_PV);
						pvtable[hashKey] = new HashtableEntry(pos.zobrist, move.longNotation());
					}
					
					if (eval < beta) // Update alpha
						alpha = eval;
					else { // Fail high
						// Update history score
						if (move.captured == PIECE_NONE) {
							historyScore[move.piece + 6][move.target] += ply * ply;
							
							// Prevent history overflow; also has the effect of weighing recently
							// searched moves more heavily during move ordering
							if (historyScore[move.piece + 6][move.target] >= HISTORY_MAX) {
								for (int i = 0; i < 13; i++)
									for (int j = 0; j < 120; j++)
										historyScore[i][j] /= 2;
							}
						}
						break;
					}
				}	
			}
			
			// Switch node type
			if (nodeType == NODE_CUT)
				nodeType = NODE_ALL;
		}
		
		// No legal moves found, return mate/stalemate score
		if (moveCount == 0) {
			assert(pos.filterLegal(pos.generateMoves(false)).isEmpty());
			return (inCheck ? matedScore(ply - ext) : VALUE_DRAW);
		}
		
		// Update transposition table
		addTTEntry(pos.zobrist,
				   bestMove,
				   ply,
				   bestEval * pos.sideToMove,
				   bestEval >= beta ? BOUND_LOWER : bestMove != null ? BOUND_EXACT : BOUND_UPPER);
		
		assert(alpha > -VALUE_INF && alpha < VALUE_INF);
		
		return bestEval;
	}
	
	/**
	 * The quiescence search.
	 */
	private static int quiescence(Position pos, int alpha, int beta) {
		
		assert(-VALUE_INF <= alpha && alpha < beta && beta <= VALUE_INF);
		
		// Get a standing evaluation first
		int standPat = Evaluate.staticEval(pos) * pos.sideToMove;
		
		// Fail high
		if (standPat >= beta)
			return beta;

		if (standPat > alpha)
			alpha = standPat;

		// Generate captures and promotions only
		ArrayList<Move> moveList = pos.generateMoves(true);
		sortMoves(pos, moveList, null);
		
		// Loop through all the moves
		for (Move move : moveList) {
			
			// Delta pruning
			if (pos.pieceCount > 6) {
				int materialGain = PIECE_VALUE_EG[Math.abs(move.captured)];
				if (move.type == PROMOTION)
					materialGain += (PIECE_VALUE_EG[Math.abs(move.piece)] - PAWN_EG);
				if (standPat + materialGain <= alpha - DELTA_MARGIN)
					continue;
			}
			
			// Make the move
			pos.makeMove(move);
			
			assert(eval > -VALUE_INF && eval < VALUE_INF);
				
			// Check if the move leaves our king in check
			if (pos.inCheck(-pos.sideToMove)) {
				pos.unmakeMove(move);
				continue;
			}
			
			int eval = -quiescence(pos, -beta, -alpha);
			
			// Unmake the move
			pos.unmakeMove(move);
			
			// Fail high
			if (eval >= beta)
				return beta;

			// New best move
			if (eval > alpha) 
				alpha = eval;
		}
		
		assert(alpha > -VALUE_INF && alpha < VALUE_INF);
		
		return alpha;
	}

	/**
	 * Sorts the given move list to improve pruning by the alpha-beta search.
	 */
	private static void sortMoves(Position pos, ArrayList<Move> moveList, HashtableEntry ttentry) {
		String hashMove = (ttentry != null ? ttentry.move : null);
		
		// Go through the move list and assign priorities
		for (Move move : moveList) {		
			if (hashMove != null && move.longNotation().equals(hashMove))
				move.priority = 121;
			else if (move.type == PROMOTION) {
				if (Math.abs(move.piece) == QUEEN)
					move.priority = 120;
				else
					move.priority = 4;
			}
				
			// MVV/LVA
			else if (move.captured != PIECE_NONE)
				move.priority = Math.abs(move.captured) * 10 - Math.abs(move.piece);
			else if (move.type == CASTLE_SHORT || move.type == CASTLE_LONG)
				move.priority = 3;
			
			// History heuristic
			move.historyScore = historyScore[move.piece + 6][move.target];
		}
		
		Collections.sort(moveList);
	}
	
	/**
	 * Returns the hash table entry for the given zobrist key (null if there is none).
	 */
	public static HashtableEntry getEntry(long zobrist, HashtableEntry[] hashtable) {
		int hashKey = (int) (zobrist % hashtable.length);
		HashtableEntry hash = hashtable[hashKey];
		if (hash != null && hash.zobrist == zobrist)
			return hash;
		
		return null;
	}

	/**
	 * Adds an entry to the transposition table.
	 */
	public static void addTTEntry(long zobrist, String move, int depth, int eval, int type) {
		assert(depth > DEPTH_HORIZON);
		
		int hashKey = (int) (zobrist % HASH_SIZE_TT);
		HashtableEntry hash = ttable[hashKey];	
		
		// If an entry for the same position exists, replace if the search depth was higher.
		// If an entry exists but for a different position, replace if it was from an old search.
		boolean replace;
		if (hash != null) {
			if (zobrist == hash.zobrist) {
				replace = (depth > hash.depth);
				if (hash.move == null && move != null)
					ttable[hashKey].move = move;
			}
			else
				replace = (hash.age > 0);
		}
		else
			replace = true;
		
		if (replace)
			ttable[hashKey] = new HashtableEntry(zobrist, move, depth, eval, type);
	}
	
	/**
	 * Increment the age of all existing transposition table entries.
	 */
	private static void updateTT() {
		for (int i = 0; i < HASH_SIZE_TT; i++) {
			if (ttable[i] != null) {
				ttable[i].age++;
				if (ttable[i].age >= 5) // delete old entries
					ttable[i] = null;
			}
		}
	}
	
	/**
	 * Returns the value of mate in ply.
	 */
	private static int mateScore(int ply) {
		return VALUE_MATE - currentDepth + ply;
	}
	
	/**
	 * Returns the value of being mated in ply.
	 */
	private static int matedScore(int ply) {
		return -mateScore(ply);
	}
	
	/**
	 * Gets a move from the opening book (returns null if no move).
	 */
	public static String getBookMove(String openingLine) throws FileNotFoundException {
		Scanner book = new Scanner(new File("book.txt"));
		ArrayList<String> variations = new ArrayList<String>();
		
		while (book.hasNextLine()) {
			String line = book.nextLine().trim();
			openingLine = openingLine.trim();
			if (line.startsWith(openingLine) && !line.equals(openingLine)) {
				Scanner continuation = new Scanner(line.substring(openingLine.length()));
				variations.add(continuation.next());
				continuation.close();
			}
		}
		
		book.close();
		if (variations.isEmpty())
			return null;
			
		return variations.get(new Random().nextInt(variations.size()));
	}
	
	/**
	 * Gets the move object with the given algebraic notation (case insensitive, accepts both
	 * short and long algebraic forms). Returns null if the move is not found.
	 */
	public static Move getMoveObject(Position pos, String notation) {
		if (notation == null || notation.isBlank())
			return null;

		ArrayList<Move> moveList = pos.filterLegal(pos.generateMoves(false));
		for (Move move : moveList) {
			addAlgebraicModifier(move, moveList);
			if (   notation.equalsIgnoreCase(move.longNotation()) 
				|| notation.equalsIgnoreCase(move.toString()))
				return move;
		}
		return null;
	}
	
	/**
	 * Adds a modifier to the algebraic notation of the given move if needed (e.g. Nbd7).
	 */
	public static void addAlgebraicModifier(Move move, ArrayList<Move> moveList) {
		int piece = Math.abs(move.piece);
		if (piece == PAWN || piece == BISHOP || piece == KING)
			return;

		for (Move m : moveList) {
			if (   m.piece  == move.piece 
				&& m.target == move.target 
				&& m.start  != move.start) {
				if (m.start % 16 != move.start % 16)
					move.modifier = "" + "abcdefgh".charAt(move.start % 16);
				else
					move.modifier = "" + (8 - move.start / 16);
			}
		}
	}
	
	/**
	 * Perft checker for the move generation.
	 */
	public static long perft(Position pos, int depth, boolean useHash) {
	  int nodes = 0;
	  
	  //"r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -"
	  //"8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -"
	  //"r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq -"
	  //"r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ -"
	  //"rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -"
	  //"r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - -"
	  
	  // normal
	  /*if (depth == 0)
		  return 1;*/
	  
	  if (useHash) {
		  HashtableEntry ttentry = getEntry(pos.zobrist, ttable);
		  if (ttentry != null && ttentry.depth == depth)
			  return ttentry.eval;
	  }

	  ArrayList<Move> moveList = pos.filterLegal(pos.generateMoves(false));
	  
	  // bulk count
	  if (depth == 1)
		  return moveList.size();
	  
	  for (Move move : moveList) {
		  pos.makeMove(move);
		  nodes += perft(pos, depth - 1, useHash);
		  pos.unmakeMove(move);
	  }
	  
	  if (useHash)
		  addTTEntry(pos.zobrist, null, depth, nodes, 0);
	  
	  return nodes;
	}

}
