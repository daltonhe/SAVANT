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
	public static int minDepth = 1;
	public static int maxDepth = 100;
	public static int currentDepth;
	public static boolean pvNode;
	public static boolean nullSearching;
	public static ArrayList<Move> pv;
	public static int eval;
	public static long nodes;
	public static boolean timeControlOn = false;
	public static double timeControl = 1.0;
	public static boolean showThinking = true;
	public static boolean showBoard = true;
	public static boolean useBook = true;
	public static int[][][] historyMoves;
	public static HashtableEntry[] ttable = new HashtableEntry[HASH_SIZE_TT];
	public static HashtableEntry[] pvtable = new HashtableEntry[HASH_SIZE_PV];
	public static HashtableEntry[] reptable = new HashtableEntry[HASH_SIZE_REP];
	
	/**
	 * 
	 * @param openingLine
	 * @return
	 * @throws FileNotFoundException
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
	 * Gets the move object corresponding to the given algebraic string.
	 * @param algebraic
	 * @param board
	 * @return
	 */
	public static Move getMoveObject(String algebraic, Board board) {
		if (algebraic == null || algebraic.isBlank())
			return null;

		ArrayList<Move> moveList = board.filterLegal(board.generateMoves(false));
		for (Move move : moveList) {
			Engine.addAlgebraicModifier(move, moveList);
			String longNotation = Board.indexToAlgebraic(move.start) + 
					Board.indexToAlgebraic(move.target);
			if (algebraic.equals(longNotation) || algebraic.equalsIgnoreCase(move.toString()))
				return move;
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param depth
	 */
	public static void search(Board board) {
		// reset node count
		nodes = 0;
		
		// reset transposition table
		ttable = new HashtableEntry[HASH_SIZE_TT];
		
		// reset history moves array
		historyMoves = new int[2][13][120];
		
		// start timer
		long startTime = System.currentTimeMillis();
		
		// iterative deepening loop
		for (currentDepth = 1; currentDepth <= maxDepth; currentDepth++) {
			
			pvNode = true;
			
			if (currentDepth == 1)
				eval = alphaBeta(currentDepth, 0, -VALUE_INF, VALUE_INF, false, board);
			else { // aspiration search using last iteration's eval
				int delta = INITIAL_WINDOW_SIZE;
				int alpha = Math.max(eval - delta, -VALUE_INF);
				int beta  = Math.min(eval + delta, VALUE_INF);
				while (true) {
					eval = alphaBeta(currentDepth, 0, alpha, beta, false, board);
					
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
					
					delta *= 1.5; // increase width
					
					assert(alpha >= -VALUE_INF && beta <= VALUE_INF);
				}
			}
			
			// stop timer
			long endTime = System.currentTimeMillis();
			double timeElapsed = (endTime - startTime) / 1000.0;
			
			// update PV
			pv = extractPV(board);
			if (!pv.isEmpty()) {
			
				// display engine output
				if (showThinking) {
					String PVString = pv.toString().replace(",", "");
					PVString = PVString.substring(1, PVString.length() - 1);
					String evalString = "" + (eval / 100.0);
					System.out.println("(d=" + currentDepth + ") " + pv.get(0) +
							" [" + evalString + "] " + PVString + 
							" (n=" + nodes + " t=" + timeElapsed + "s)");
				}
			}
			
			// found mate, end search
			if (Math.abs(eval) > VALUE_MATE_THRESHOLD)
				break;

			// time up, end search
			if (currentDepth >= minDepth && timeControlOn && timeElapsed > timeControl)
				break;
		}
	}
	
	/**
	 * 
	 * @param board
	 * @return
	 */
	public static ArrayList<Move> extractPV(Board board) {
		ArrayList<Move> PV = new ArrayList<Move>();
		HashtableEntry hash = getEntry(board.zobrist, pvtable);
		int i = 0;
		while (i < currentDepth + 9 && hash != null && hash.move != null) {
			Move move = getMoveObject(hash.move, board);
			// in case of a key collision the stored move may be invalid
			if (move == null)
				break;

			PV.add(move);
			board.makeMove(move);

			hash = getEntry(board.zobrist, pvtable);
			i++;
		}
		
		for (int j = PV.size() - 1; j >= 0; j--)
			board.unmakeMove(PV.get(j));

		return PV;
	}

	/**
	 * The alpha-beta search.
	 * @param ply - Remaining number of plies to search
	 * @param ext - Number of plies we have extended the search
	 * @param alpha - Highest score we can guarantee
	 * @param beta - Lowest score our opponent can guarantee
	 * @param canNull - True if we should try a null move
	 * @param board
	 * @return
	 */
	private static int alphaBeta(int ply, int ext, int alpha, int beta, boolean canNull, Board board) {
		nodes++;
		
		int eval;
		boolean rootNode = (pvNode && ply == currentDepth);

		// Check for draw by 50 move rule
		if (board.fiftyMoves >= 100)
			return VALUE_DRAW;
		
		// Check for draw by repetition
		if (!rootNode && !nullSearching) {
			HashtableEntry rentry = getEntry(board.zobrist, Savant.reptable);
			if (rentry != null && rentry.zobrist == board.zobrist && rentry.count >= 2)
				return VALUE_DRAW;
		}
		
		// Check for draw by insufficient material
		if (board.insufficientMaterial())
			return VALUE_DRAW;
		
		// Mate distance pruning. If a shorter mate was found upward in the tree then there is
		// no need to search further because we can impossibly improve alpha. Same logic but
		// with reversed signs applies in the opposite condition of being mated. In this case
		// return a fail-high score.
		alpha = Math.max(matedScore(ply - ext), alpha);
		beta  = Math.min(mateScore(ply - ext), beta);
		if (alpha >= beta)
			return alpha;
		
		// At non-PV nodes check for an early transposition table cutoff
		HashtableEntry ttentry = getEntry(board.zobrist, ttable);
		if (!pvNode && ttentry != null && ttentry.depth >= (ply - ext)) {
			if ((ttentry.type == BOUND_UPPER && ttentry.eval * board.sideToMove < alpha) ||
			    (ttentry.type == BOUND_LOWER && ttentry.eval * board.sideToMove > beta))
				return ttentry.eval * board.sideToMove;
		}
		
		int kingPos = (board.sideToMove == WHITE ? board.wKing : board.bKing);
		boolean inCheck = (canNull ? board.isAttacked(kingPos, -board.sideToMove) : false);
		
		// Extend the search if we are in check
		if (!rootNode && inCheck && ext < currentDepth / 2) {
			ply++;
			ext++;
		}
		
		// Enter quiescence search we reach a leaf node
		if (ply <= 0)
			return quiescence(alpha, beta, board);
		
		assert(-VALUE_INF <= alpha && alpha < beta && beta <= VALUE_INF);
		
		// Null move pruning
		if (   canNull
			&& !pvNode
			&& !inCheck
			&& !board.isPawnEnding(board.sideToMove)) {
			int R = 2; // depth reduction factor
			board.makeNullMove();
			nullSearching = true;
			eval = -alphaBeta(ply - R - 1, ext, -beta, -beta + 1, false, board);
			nullSearching = false;
			board.makeNullMove();
			// Fail high
			if (eval >= beta) {
				// Update transposition table
				addEntry(board.zobrist, null, ply - ext, eval * board.sideToMove, BOUND_LOWER);
				return beta;
			}
		}
		
		// Generate moves and sort
		ArrayList<Move> moveList = board.generateMoves(false);
		sortMoves(moveList, board);

		boolean foundLegal = false;
		boolean foundPv = false;
		String bestMove = null;
		int movesTried = 0;
		
		// Loop through all the moves
		for (Move move : moveList) {
			
			// Make the move
			board.makeMove(move);
			
			// Check if the move is legal
			kingPos = (board.sideToMove == WHITE ? board.bKing : board.wKing);
			if (board.isAttacked(kingPos, board.sideToMove)) {
				board.unmakeMove(move);
				continue;
			}
			
			foundLegal = true;
			
			// Late move reductions
			if (   !pvNode
				&& !inCheck
				&& ply >= 3
			    && movesTried >= 4 
			    && move.captured == 0
			    && move.type != PROMOTION) {
				int R = 1;
				//int R = (int) Math.sqrt(2 * (ply - 1) + Math.sqrt(2 * (movesTried - 1)));
				eval = -alphaBeta(ply - R - 1, ext, -alpha - 1, -alpha, true, board);
			}
			else
				eval = alpha + 1;
			
			if (eval > alpha) {
				// Principal variation search
				if (foundPv) {
					eval = -alphaBeta(ply - 1, ext, -alpha - 1, -alpha, true, board);
					// Do a full width search
					if (eval > alpha && eval < beta)
						eval = -alphaBeta(ply - 1, ext, -beta, -alpha, true, board);
				} 
				// Do a full width search
				else
					eval = -alphaBeta(ply - 1, ext, -beta, -alpha, true, board);
			}
			
			// Unmake the move
			board.unmakeMove(move);
			
			assert(eval > -VALUE_INF && eval < VALUE_INF);
			
			// Fail high
			if (eval >= beta) {
				// Update transposition table
				addEntry(board.zobrist, move.longNotation(), ply - ext, eval * board.sideToMove, BOUND_LOWER);
				
				// Update history moves
				if (move.captured == 0) {
					int side = (board.sideToMove == WHITE ? 0 : 1);
					historyMoves[side][move.piece + 6][move.target] += ply * ply;
				}
				return beta;
			}
			
			// New best move
			if (eval > alpha) {
				foundPv = true;
				bestMove = move.longNotation();
				
				int hashKey = (int) (board.zobrist % HASH_SIZE_PV);
				pvtable[hashKey] = new HashtableEntry(board.zobrist, move.longNotation()); 
				
				alpha = eval;
			}
			
			movesTried++;
		}
		
		pvNode = false;
		
		assert(movesTried > 0 || !foundLegal);
		
		// Check for mate/stalemate
		if (!foundLegal) {
			if (inCheck)
				return matedScore(ply - ext);
			
			return VALUE_DRAW;
		}
		
		// Update transposition table
		if (foundPv)
			addEntry(board.zobrist, bestMove, ply - ext, alpha * board.sideToMove, BOUND_EXACT);
		else
			addEntry(board.zobrist, null, ply - ext, alpha * board.sideToMove, BOUND_UPPER);
		
		assert(alpha > -VALUE_INF && alpha < VALUE_INF);
		
		return alpha;
	}
	
	/**
	 * The quiescence search.
	 * @param alpha
	 * @param beta
	 * @param board
	 * @return
	 */
	private static int quiescence(int alpha, int beta, Board board) {
		
		assert(-VALUE_INF <= alpha && alpha < beta && beta <= VALUE_INF);
		
		// Get a static evaluation first
		int standPat = Evaluate.staticEval(board) * board.sideToMove;
		// Fail high
		if (standPat >= beta)
			return beta;

		if (standPat > alpha)
			alpha = standPat;

		// Generate captures and promotions only
		ArrayList<Move> moveList = board.generateMoves(true);
		sortMoves(moveList, board);
		
		// Loop through all the moves
		for (Move move : moveList) { 
			
			// Delta pruning: if the capture plus the delta margin can impossibly raise alpha,
			// prune it
			if (standPat + PIECE_VALUE_MG[Math.abs(move.captured)] + DELTA_MARGIN <= alpha)
				continue;
			
			// Make the move
			board.makeMove(move);
			
			assert(eval > -VALUE_INF && eval < VALUE_INF);
				
			// Check if the move is legal
			int kingPos = (board.sideToMove == WHITE ? board.bKing : board.wKing);
			if (board.isAttacked(kingPos, board.sideToMove)) {
				board.unmakeMove(move);
				continue;
			}
			
			int eval = -quiescence(-beta, -alpha, board);
			
			// Unmake the move
			board.unmakeMove(move);
			
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
	 * Orders the given move list for the alpha-beta search.
	 * @param moveList
	 */
	public static void sortMoves(ArrayList<Move> moveList, Board board) {
		// Get the hash move if it exists
		String hashMove = null;
		HashtableEntry hash = getEntry(board.zobrist, ttable);
		if (hash != null)
			hashMove = hash.move;
		
		for (Move move : moveList) {		
			if (hashMove != null && move.longNotation().equals(hashMove))
				move.priority = 121;
			else if (move.type == PROMOTION)
				move.priority = 120;
			// MVV/LVA
			else if (move.captured != 0)
				move.priority = Math.abs(move.captured) * 10 - Math.abs(move.piece);
			
			// History move ordering
			int side = (board.sideToMove == WHITE ? 0 : 1);
			move.history = historyMoves[side][move.piece + 6][move.target];
		}
		
		Collections.sort(moveList);
	}
	
	/**
	 * Returns the hashtable entry for the given zobrist key if it exists, otherwise
	 * return null.
	 * @param zobrist
	 * @return
	 */
	public static HashtableEntry getEntry(long zobrist, HashtableEntry[] hashTable) {
		int hashKey = (int) (zobrist % hashTable.length);
		HashtableEntry hash = hashTable[hashKey];
		if (hash != null && hash.zobrist == zobrist)
			return hash;
		
		return null;
	}

	/**
	 * 
	 * @param zobrist
	 * @param move
	 * @param depth
	 * @param eval
	 * @param type
	 */
	public static void addEntry(long zobrist, String move, int depth, int eval, int type) {
		int hashKey = (int) (zobrist % HASH_SIZE_TT);
		// if entry with higher depth exists, don't replace
		HashtableEntry hash = ttable[hashKey];
		if (hash != null && zobrist == hash.zobrist && depth < hash.depth)
			return;

		ttable[hashKey] = new HashtableEntry(zobrist, move, depth, eval, type);
	}
	
	/**
	 * Returns the value of mate in ply.
	 * @param ply
	 * @return
	 */
	public static int mateScore(int ply) {
		return VALUE_MATE - currentDepth + ply;
	}
	
	public static int matedScore(int ply) {
		return -mateScore(ply);
	}
	
	/**
	 * Adds a modifier to the algebraic notation of the given move if needed.
	 * @param move
	 * @param moveList
	 */
	public static void addAlgebraicModifier(Move move, ArrayList<Move> moveList) {
		if (Math.abs(move.piece) != KNIGHT && Math.abs(move.piece) != ROOK && Math.abs(move.piece) != QUEEN)
			return;

		for (Move m : moveList) {
			if (m.target == move.target && m.piece == move.piece && m.start != move.start) {
				if ((m.start % 16) != (move.start % 16))
					move.modifier = "" + "abcdefgh".charAt(move.start % 16);
				else
					move.modifier = "" + (8 - move.start / 16);
			}
		}
	}
	
}
