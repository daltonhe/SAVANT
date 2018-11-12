import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

/**
 * 
 * @author Dalton He
 * created 10-22-18
 */
public class Engine implements Definitions {
	public static int minDepth             = 1;
	public static int maxDepth             = 100;
	public static boolean uciMode          = false;
	public static boolean timeControlOn    = false;
	public static double timeControl       = 1.0;
	public static boolean useBook          = true;
	public static int currentDepth;
	public static int nullMovesMade;
	public static int[][][] historyMoves;
	public static HashtableEntry[] ttable;
	public static HashtableEntry[] pvtable;
	
	public static ArrayList<Move> pv;
	public static Move bestMove;
	public static int eval;
	public static long nodes;

	/**
	 * Resets engine fields in preparation for a new search.
	 */
	public static void initializeSearch() {
		currentDepth  = 0;
		nullMovesMade = 0;
		historyMoves  = new int[2][13][120];
		ttable        = new HashtableEntry[HASH_SIZE_TT];
		pvtable       = new HashtableEntry[HASH_SIZE_PV];
		pv            = new ArrayList<Move>();
		bestMove      = null;
		eval          = 0;
		nodes         = 0;
	}
	
	/**
	 * Searches the given position and reports the expected line and score (stored as static
	 * fields pv and eval).
	 */
	public static void search(Position pos) {
		initializeSearch();
		
		// Start the timer
		long startTime = System.currentTimeMillis();
		
		// The iterative deepening loop
		for (currentDepth = 1; currentDepth <= maxDepth; currentDepth++) {
			
			// For the first few depths, start with an infinite search window.
			if (currentDepth < 5)
				eval = alphaBeta(currentDepth, 0, -VALUE_INF, VALUE_INF, false, NODE_PV, pos);
			
			// For later depths, try an aspirated search with a window around the previous
			// iteration's eval.
			else {
				int delta = INITIAL_WINDOW_SIZE;
				int alpha = Math.max(eval - delta, -VALUE_INF);
				int beta  = Math.min(eval + delta, VALUE_INF);
				// Start with a small aspiration window. If we fail high/low, re-search with a
				// bigger window until we succeed.
				while (true) {
					eval = alphaBeta(currentDepth, 0, alpha, beta, false, NODE_PV, pos);
					
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
					
					delta += delta / 4 + 5; // increase width
					
					assert(alpha >= -VALUE_INF && beta <= VALUE_INF);
				}
			}
			
			// Stop the timer
			long endTime = System.currentTimeMillis();
			double timeElapsed = (endTime - startTime);
			double decimalTime = timeElapsed / 1000.0;
			
			// Update the pv
			pv = extractPV(pos);
			
			// Update the GUI
			String pvString = pv.toString().replace(",", "");
			pvString        = pvString.substring(1, pvString.length() - 1);
			if (uciMode)
				System.out.println(  "info score cp " + eval
								   + " depth "        + currentDepth
								   + " nodes "        + nodes
								   + " nps "          + 0
								   + " time "         + (int) timeElapsed
								   + " pv "           + pvString);
			else
				System.out.println(  "(d=" + currentDepth + ") "
								   + pv.get(0)
								   + " [" + eval / 100.0 + "] "
								   + pvString 
								   + " (n=" + nodes + " t=" + decimalTime + "s)");
			
			// Stop searching if:
			// 1) A forced mate was found
			// 2) We have only one legal move
			// 3) Time is up and we have searched to the minDepth
			if (   (Math.abs(eval) > VALUE_MATE_THRESHOLD)
				|| (pos.filterLegal(pos.generateMoves(false)).size() == 1)
				|| (timeControlOn && decimalTime > timeControl && currentDepth >= minDepth))
				break;
		}
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
	public static Move getMoveObject(String notation, Position pos) {
		if (notation == null || notation.isBlank())
			return null;

		ArrayList<Move> moveList = pos.filterLegal(pos.generateMoves(false));
		for (Move move : moveList) {
			Engine.addAlgebraicModifier(move, moveList);
			if (   notation.equalsIgnoreCase(move.longNotation()) 
				|| notation.equalsIgnoreCase(move.toString()))
				return move;
		}
		return null;
	}
	
	/**
	 * Extracts the principal variation from the hash table and returns it as a move list.
	 */
	public static ArrayList<Move> extractPV(Position pos) {
		ArrayList<Move> PV = new ArrayList<Move>();
		HashtableEntry hash = getEntry(pos.zobrist, pvtable);
		int i = 0;
		while (i < currentDepth + 9 && hash != null && hash.move != null) {
			Move move = getMoveObject(hash.move, pos);
			// in case of a key collision the stored move may be invalid
			if (move == null)
				break;

			PV.add(move);
			pos.makeMove(move);

			hash = getEntry(pos.zobrist, pvtable);
			i++;
		}
		
		for (int j = PV.size() - 1; j >= 0; j--)
			pos.unmakeMove(PV.get(j));

		return PV;
	}

	/**
	 * The alpha-beta recursive search.
	 * @param ply     - Remaining number of plies to search
	 * @param ext     - Number of plies we have extended the search
	 * @param alpha   - Highest score that we have found (lower bound)
	 * @param beta    - Lowest score that our opponent can guarantee (upper bound)
	 * @param canNull - true if we should try a null move, false if the last move was a null move
	 * @param pos     - The position object
	 * @return          The score of the position from the perspective of the side to move
	 */
	private static int alphaBeta(int ply,
								 int ext,
								 int alpha,
								 int beta,
								 boolean canNull,
								 int nodeType,
								 Position pos) {

		// Enter quiescence search once we reach a leaf node
		
		if (ply <= 0)
			return quiescence(alpha, beta, pos);
		
		nodes++;
		
		int eval = 0;
		boolean rootNode = (nodeType == NODE_PV && ply == currentDepth);

		if (!rootNode) {
			// Check for draw by 50 move rule
			if (pos.fiftyMoves >= 100)
				return VALUE_DRAW;
			
			// Check for draw by repetition
			if (nullMovesMade == 0) {
				HashtableEntry rentry = getEntry(pos.zobrist, Savant.reptable);
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
		if (nodeType != NODE_PV && ttentry != null && ttentry.depth >= (ply - ext)) {
			if (    ttentry.type == BOUND_EXACT
				|| (ttentry.type == BOUND_UPPER && ttentry.eval * pos.sideToMove <= alpha)
				|| (ttentry.type == BOUND_LOWER && ttentry.eval * pos.sideToMove >= beta))
				return ttentry.eval * pos.sideToMove;
		}
		
		// Extend the search if we are in check
		boolean inCheck = (canNull ? pos.inCheck(pos.sideToMove) : false);
		int maxExt = currentDepth / 4 + 3;
		if (!rootNode && inCheck && ext < maxExt) {
			ply++;
			ext++;
		}
		
		assert(-VALUE_INF <= alpha && alpha < beta && beta <= VALUE_INF);
		
		// Use null move pruning if we are not in a PV node, the side to move is not in check,
		// and the side to move has non-pawns left. Obviously do not allow more than one null
		// move in a row.
		if (   canNull
			&& nodeType != NODE_PV
			&& ply >= 2
			&& !inCheck
			&& beta < VALUE_MATE_THRESHOLD
			&& !pos.isPawnEnding(pos.sideToMove)) {
			int R = 3; // depth reduction factor
			pos.makePassingMove();
			nullMovesMade++;
			eval = -alphaBeta(ply - R - 1, ext, -beta, -beta + 1, false, NODE_CUT, pos);
			pos.makePassingMove();
			nullMovesMade--;
			
			// Fail high
			if (eval >= beta) {
				// Do not return unproven mate scores
				if (eval >= VALUE_MATE_THRESHOLD)
					eval = beta;
				
				// Update transposition table
				addEntry(pos.zobrist, null, ply - ext, eval * pos.sideToMove, BOUND_LOWER);
				return beta;
			}
		}
		
		// Generate moves and sort
		ArrayList<Move> moveList = pos.generateMoves(false);
		sortMoves(moveList, pos);

		boolean foundLegal = false;
		String bestMove = null;
		int movesTried = 0;
		
		// Loop through all the moves
		for (Move move : moveList) {
			
			// Make the move
			pos.makeMove(move);
			
			// Check if the move leaves our king in check
			if (pos.inCheck(-pos.sideToMove)) {
				pos.unmakeMove(move);
				continue;
			}
			
			foundLegal = true;
			boolean doFullSearch = false;
			
			// Late move reductions
			if (   nodeType != NODE_PV
				&& !inCheck
				&& ply >= 3
			    && movesTried >= 1 
			    && move.captured == 0
			    && move.type != PROMOTION) {
				int R = 1;
				//int R = (int) Math.sqrt(2 * (ply - 1) + Math.sqrt(2 * (movesTried - 1)));
				eval = -alphaBeta(ply - R - 1, ext, -alpha - 1, -alpha, true, NODE_CUT, pos);
				doFullSearch = (eval > alpha);
			}
			else
				doFullSearch = true;
			
			if (doFullSearch) {
				// Search first move to full depth
				if (nodeType != NODE_PV || movesTried == 0)
					eval = -alphaBeta(ply - 1, ext, -beta, -alpha, true, nodeType, pos);
				else {
					// Principal variation search
					eval = -alphaBeta(ply - 1, ext, -alpha - 1, -alpha, true, NODE_CUT, pos);
					// Do a full width search
					if (eval > alpha && eval < beta)
						eval = -alphaBeta(ply - 1, ext, -beta, -alpha, true, NODE_PV, pos);
				} 
			}
			
			// Unmake the move
			pos.unmakeMove(move);
			
			assert(eval > -VALUE_INF && eval < VALUE_INF);
			
			// Fail high
			if (eval >= beta) {
				// Update transposition table
				addEntry(pos.zobrist, move.longNotation(), ply - ext, eval * pos.sideToMove, BOUND_LOWER);
				
				// Update history moves
				if (move.captured == 0) {
					int side = (pos.sideToMove == WHITE ? 0 : 1);
					historyMoves[side][move.piece + 6][move.target] += ply * ply;
				}
				return beta;
			}
			
			// New best move
			if (eval > alpha) {
				bestMove = move.longNotation();
				if (rootNode)
					Engine.bestMove = move;
				
				int hashKey = (int) (pos.zobrist % HASH_SIZE_PV);
				pvtable[hashKey] = new HashtableEntry(pos.zobrist, move.longNotation()); 
				
				alpha = eval;
			}
			
			movesTried++;
		}
		
		assert(movesTried > 0 || !foundLegal);
		
		// Check for mate/stalemate
		if (!foundLegal) {
			if (inCheck)
				return matedScore(ply - ext);

			return VALUE_DRAW;
		}
		
		// Update transposition table
		if (bestMove != null)
			addEntry(pos.zobrist, bestMove, ply - ext, alpha * pos.sideToMove, BOUND_EXACT);
		else
			addEntry(pos.zobrist, null, ply - ext, alpha * pos.sideToMove, BOUND_UPPER);
		
		assert(alpha > -VALUE_INF && alpha < VALUE_INF);
		
		return alpha;
	}
	
	/**
	 * The quiescence search.
	 */
	private static int quiescence(int alpha, int beta, Position pos) {
		
		assert(-VALUE_INF <= alpha && alpha < beta && beta <= VALUE_INF);
		
		// Get a static evaluation first
		int standPat = Evaluate.staticEval(pos.board, pos.sideToMove) * pos.sideToMove;
		// Fail high
		if (standPat >= beta)
			return beta;
		
		// Delta pruning: if the capture plus the delta margin can impossibly raise alpha,
		// prune it
		if (standPat + QUEEN_MG <= alpha)
			return alpha;

		if (standPat > alpha)
			alpha = standPat;

		// Generate captures and promotions only
		ArrayList<Move> moveList = pos.generateMoves(true);
		sortMoves(moveList, pos);
		
		// Loop through all the moves
		for (Move move : moveList) { 
			
			// Make the move
			pos.makeMove(move);
			
			assert(eval > -VALUE_INF && eval < VALUE_INF);
				
			// Check if the move leaves our king in check
			if (pos.inCheck(-pos.sideToMove)) {
				pos.unmakeMove(move);
				continue;
			}
			
			int eval = -quiescence(-beta, -alpha, pos);
			
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
	public static void sortMoves(ArrayList<Move> moveList, Position pos) {
		// Get the hash move if it exists
		String hashMove = null;
		HashtableEntry hash = getEntry(pos.zobrist, ttable);
		if (hash != null)
			hashMove = hash.move;
		
		for (Move move : moveList) {		
			if (hashMove != null && move.longNotation().equals(hashMove))
				move.priority = 121;
			else if (move.type == PROMOTION && move.piece * pos.sideToMove == QUEEN)
				move.priority = 120;
			// MVV/LVA
			else if (move.captured != 0)
				move.priority = Math.abs(move.captured) * 10 - Math.abs(move.piece);
			else if (move.type == CASTLE_SHORT || move.type == CASTLE_LONG)
				move.priority = 4;
			// History move ordering
			int side = (pos.sideToMove == WHITE ? 0 : 1);
			move.historyCount = historyMoves[side][move.piece + 6][move.target];
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
	public static void addEntry(long zobrist, String move, int depth, int eval, int type) {
		int hashKey = (int) (zobrist % HASH_SIZE_TT);
		HashtableEntry hash = ttable[hashKey];
		
		// If an entry exists with higher depth, do not replace
		if (hash != null && zobrist == hash.zobrist && depth < hash.depth)
			return;

		ttable[hashKey] = new HashtableEntry(zobrist, move, depth, eval, type);
	}
	
	/**
	 * Returns the value of mate in ply.
	 */
	public static int mateScore(int ply) {
		return VALUE_MATE - currentDepth + ply;
	}
	
	/**
	 * Returns the value of being mated in ply.
	 */
	public static int matedScore(int ply) {
		return -mateScore(ply);
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
		  addEntry(pos.zobrist, null, depth, nodes, 0);
	  
	  return nodes;
	}

}
