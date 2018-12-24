import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * 
 * @author Dalton He
 * created 10-22-18
 * 
 */
public class Engine implements Types {
    public static boolean uciMode;               // true if in UCI mode
    public static boolean useBook;               // true if using native opening book
    public static double timeLeft  = 10000;      // total time remaining
    public static double increment = 0;          // increment per move
    public static double timeForMove;            // time for this move

    public static int currentDepth;              // current search depth
    public static long startTime;                // time the search was started
    public static boolean abortedSearch;         // true if the search was ended early

    public static int[][] quietHistory;          // history heuristic move scores 
        // quietHistory[pieceType][toIndex]
    public static int[][] depthReduction;        // precomputed R values
        // depthReduction[ply][moveCount]
    public static TranspositionTable main_TT;    // transposition table for main search
    public static TranspositionTable qsearch_TT; // transposition table for quiescence search
    public static TranspositionTable pv_TT;      // hash table for PV moves

    public static List<Move> pv;                 // principal variation
    public static Move bestMove;                 // best move so far
    public static Move prevBestMove;             // last iteration's best move
    public static int eval;                      // score of the position
    public static long nodes;                    // number of nodes searched
    
    /**
     * Initialization at the start of a new game.
     */
    public static void initialize() {
        // Initialize LMR reduction factor table
        depthReduction = new int[64][64];
        for (int p = 1; p <= 63; p++)
            for (int mc = 1; mc <= 63; mc++)
                depthReduction[p][mc] = (int) Math.round(Math.log(p) * Math.log(mc / 2));
        // Initialize hash tables
        main_TT    = new TranspositionTable(HASH_SIZE_TT);
        qsearch_TT = new TranspositionTable(HASH_SIZE_TT);
        pv_TT      = new TranspositionTable(HASH_SIZE_PV);
    }
    
    /**
     * Initialization at the start of a new search.
     */
    public static void initSearch() {
        abortedSearch = false;
        quietHistory  = new int[13][120];
        pv            = new ArrayList<Move>();
        bestMove      = null;
        prevBestMove  = null;
        eval          = 0;
        nodes         = 0;
        main_TT.update();
        pv_TT.clear();
    }

    /**
     * Searches the given position and reports the expected line and score.
     */
    public static void search(Position pos) {
        initSearch();

        // If we have only one legal move, just play it and don't bother searching
        if (pos.genLegalMoves().size() == 1) {
            bestMove = pos.genLegalMoves().get(0);
            return;
        }

        // Calculate the time to use for this move
        if (timeLeft > increment) {
            // Spend more time out of the opening to figure out the position
            timeForMove = (pos.stateHist.size() > 40 ? timeLeft / 20 : timeLeft / 10);
            timeForMove += increment;
        }
        else timeForMove = timeLeft / 5;

        // Start the timer
        startTime = System.currentTimeMillis();

        // The iterative deepening loop
        for (currentDepth = 1; currentDepth <= DEPTH_MAX; currentDepth++) {

            // For the first few depths, start with an infinite search window.
            if (currentDepth < 5)
                eval = alphaBeta(pos, currentDepth, 0, -VALUE_INF, VALUE_INF, NODE_PV);

            // For later depths, try an aspirated search with a window around the previous
            // iteration's eval.
            else {
                int delta = INITIAL_WINDOW;
                int alpha = Math.max(eval - delta, -VALUE_INF);
                int beta  = Math.min(eval + delta, VALUE_INF);

                // Start with a small aspiration window. If we fail high/low, re-search with a
                // bigger window until we succeed.
                while (true) {
                    eval = alphaBeta(pos, currentDepth, 0, alpha, beta, NODE_PV);

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

                    // Increase the window width
                    delta += delta / 4 + 5;			
                    assert(alpha >= -VALUE_INF && beta <= VALUE_INF);
                }
            }

            // Stop searching if time is up
            if (abortedSearch) break;

            // Check the timer
            long endTime = System.currentTimeMillis();
            double timeElapsed = endTime - startTime;
            double decimalTime = timeElapsed / 1000.0;

            // Update the pv and previous best move
            pv = extractPV(pos);
            //assert(bestMove.equals(pv.get(0)));
            prevBestMove = bestMove;

            // Update the GUI
            String pvString = "";
            if (uciMode) {
                for (Move move : pv) pvString += move.longNot() + " ";
                pvString = pvString.trim();
                System.out.println(  "info score cp " + eval
                                   + " depth " + currentDepth
                                   + " nodes " + nodes
                                   + " time "  + (int) timeElapsed
                                   + " pv "    + pvString);
            }
            else {
                for (Move move : pv) pvString += move.shortNot(pos.genLegalMoves()) + " ";
                pvString = pvString.trim();
                System.out.println(  "(d=" + currentDepth + ") "
                                   + bestMove.shortNot(pos.genLegalMoves())
                                   + " [" + eval / 100.0 + "] "
                                   + pvString 
                                   + " (n=" + nodes + " t=" + decimalTime + "s)");
            }

            // Stop searching if a forced mate was found or if the time left is probably not 
            // enough to search the next depth
            if (Math.abs(eval) > VALUE_MATE_THRESH || timeElapsed >= timeForMove / 4) break;
        }
    }

    /**
     * The alpha-beta recursive search.
     * @param pos      - The position we are searching
     * @param ply      - Remaining number of plies to search
     * @param height   - Number of plies from the root position
     * @param alpha    - Highest score so far (lower bound)
     * @param beta     - Lowest score that the opponent can guarantee (upper bound)
     * @param nodeType - Expected node type
     * @return           Score of the position from the for the side to move
     */
    private static int alphaBeta(Position pos,
                                 int ply,
                                 int height,
                                 int alpha,
                                 int beta,
                                 int nodeType) {        

        assert(-VALUE_INF <= alpha && alpha < beta && beta <= VALUE_INF);
        assert(nodeType == NODE_PV || nodeType == NODE_CUT || nodeType == NODE_ALL);

        // Check if time is up
        if (abortedSearch) return 0;
        if (nodes % 1000 == 0) {
            if ((System.currentTimeMillis() - startTime) > timeForMove) {
                abortedSearch = true;
                return 0;
            }
        }

        // Increment the node count
        nodes++;

        // Enter quiescence search when we reach a leaf node
        if (ply <= 0) return quiescence(pos, alpha, beta);

        boolean rootNode = (nodeType == NODE_PV && height == 0);
        int eval = 0;

        if (!rootNode) {
            // Check for draw
            if (   pos.fiftyMoves >= 100
                || pos.isRepeat()
                || pos.insufficientMat())
                return VALUE_DRAW;

            // Mate distance pruning. If a shorter mate was found upward in the tree then there is
            // no need to search further because we cannot possibly improve alpha. Same logic
            // applies in the opposite condition of being mated (but with reversed signs).
            // In this case return a fail-high score.
            alpha = Math.max(matedScore(height), alpha);
            beta  = Math.min(mateScore(height), beta);
            if (alpha >= beta) return alpha;
        }

        // At non-PV nodes check for an early transposition table cutoff
        HashtableEntry ttentry = main_TT.get(pos.key);
        if (nodeType != NODE_PV && ttentry != null && ttentry.depth >= ply) {
            if (    ttentry.type == BOUND_EXACT
                || (ttentry.type == BOUND_UPPER && ttentry.eval * pos.sideToMove <= alpha)
                || (ttentry.type == BOUND_LOWER && ttentry.eval * pos.sideToMove >= beta))
                return ttentry.eval * pos.sideToMove;
        }

        // Extend the search if the side to move is in check
        boolean inCheck = pos.inCheck(pos.sideToMove);
        if (!rootNode && inCheck) ply++;

        int standPat = 0;
        if (!rootNode && !inCheck) {
            // Static evaluation of the position
            standPat = Evaluate.staticEval(pos) * pos.sideToMove;

            // Reverse futility pruning (RFP)
            if (   (ply == 1 && standPat >= beta + FUTILITY_MARGIN)
                || (ply == 2 && standPat >= beta + EXT_FUTILITY_MARGIN))
                return standPat;

            // Limited razoring
            if (   ply == 3
                && standPat <= alpha - RAZOR_MARGIN
                && pos.pieceList.size() > 6)
                ply--;

            // Null move pruning
            if (   pos.nullAllowed
                && nodeType != NODE_PV
                && standPat >= beta
                && !pos.hasOnlyPawns(pos.sideToMove)) {

                pos.makeNullMove();
                
                // Dynamic reduction factor based on ply and static evaluation
                int R = (3 + ply / 4 + Math.min(2, (standPat - beta) / 200));
                eval = -alphaBeta(pos, ply-R-1, height+1, -beta, -beta+1, -nodeType);

                pos.unmakeNullMove();

                // Fail-high
                if (eval >= beta) {
                    // Do not return unproven mate scores
                    if (eval >= VALUE_MATE_THRESH) eval = beta;
                    
                    if (Math.abs(beta) < VALUE_MATE_THRESH) return eval;
                }
            }
        }

        // Internal iterative deepening if we have no hash move
        if (ply >= 6 && (ttentry == null || ttentry.move == MOVE_NONE)) {
            eval = alphaBeta(pos, ply-5, height, alpha, beta, nodeType);
            ttentry = main_TT.get(pos.key);
        }

        // Generate all moves and sort
        List<Move> moveList = pos.genPseudoMoves(GEN_SEARCH);
        sortMoves(pos, moveList, (ttentry == null ? MOVE_NONE : ttentry.move));

        Move bestMove  = null;
        int  bestEval  = -VALUE_INF;
        int  moveCount = 0;

        // Move loop
        for (Move move : moveList) {
            pos.makeMove(move);

            // Check for legality
            if (pos.inCheck(-pos.sideToMove)) {
                pos.unmakeMove(move);
                continue;
            }

            moveCount++;

            boolean doFullDepthSearch;
            boolean pruningOk =   (!rootNode
                                && !inCheck
                                && !pos.inCheck(pos.sideToMove) 
                                && move.type != PROMOTION 
                                && move.captured == PIECE_NONE);			   

            // Futility pruning
            if (   pruningOk
                && Math.abs(alpha) < VALUE_MATE_THRESH 
                && Math.abs(beta)  < VALUE_MATE_THRESH
                && (   (ply == 1 && standPat <= alpha - FUTILITY_MARGIN)
                    || (ply == 2 && standPat <= alpha - EXT_FUTILITY_MARGIN))) {
                pos.unmakeMove(move);
                continue;
            }

            // Late move reductions (LMR)
            if (pruningOk && ply >= 3 && moveCount > 1) {

                // Dynamic reduction factor based on move count and ply
                int R = depthReduction[Math.min(63, ply)][Math.min(63, moveCount)];

                // Increase/decrease reduction based on node type
                if      (nodeType == NODE_CUT) R += 2;
                else if (nodeType == NODE_PV)  R--;
                
                R = Math.max(0, R);
                eval = -alphaBeta(pos, ply-R-1, height+1, -alpha-1, -alpha, NODE_CUT);

                doFullDepthSearch = (eval > alpha && R != 0);
            }
            else doFullDepthSearch = (nodeType != NODE_PV || moveCount > 1);

            // Full-depth PVS when LMR is skipped or fails high
            if (doFullDepthSearch)
                eval = -alphaBeta(pos, ply-1, height+1, -alpha-1, -alpha, 
                                  nodeType == NODE_PV ? NODE_CUT : -nodeType);

            // For PV nodes only, do a full-width search on the first move or after a fail-high
            if (nodeType == NODE_PV && (moveCount == 1 || (eval > alpha && eval < beta)))
                eval = -alphaBeta(pos, ply-1, height+1, -beta, -alpha, NODE_PV);

            pos.unmakeMove(move);

            // Check for a new best move
            if (eval > bestEval) {
                bestEval = eval;

                if (eval > alpha) {
                    bestMove = move;
                    if (rootNode) Engine.bestMove = move;

                    // Update PV hash table at PV nodes even after fail-high
                    if (nodeType == NODE_PV) pv_TT.add(pos.key, bestMove.toInt());

                    if (nodeType == NODE_PV && eval < beta)
                        alpha = eval; // Update alpha. Always alpha < beta
                    else {
                        assert(eval >= beta); // Fail-high

                        // Update quiet move sorting heuristics
                        if (move.captured == PIECE_NONE) {
                            
                            // History heuristic
                            quietHistory[move.piece + 6][move.target] += ply * ply;

                            // Prevent history overflow; also has the effect of weighing recently
                            // searched moves more heavily during move ordering
                            if (quietHistory[move.piece + 6][move.target] >= HISTORY_MAX) {
                                for (int i = 0; i < 13; i++)
                                    for (int j = 0; j < 120; j++)
                                        quietHistory[i][j] >>= 1;
                            }
                        }
                        break; // cutoff
                    }
                }	
            }
        }

        // No legal moves were found: return mate/stalemate score
        if (moveCount == 0) bestEval = (inCheck ? matedScore(height) : VALUE_DRAW);

        // If we pruned all moves without searching, return a fail-low score
        if (bestEval == -VALUE_INF) bestEval = alpha;

        // Update the transposition table
        main_TT.add(pos.key, 
                    (bestMove == null ? MOVE_NONE : bestMove.toInt()), 
                    ply, 
                    bestEval * pos.sideToMove, 
                    (bestEval >= beta ? BOUND_LOWER 
                                      : (nodeType == NODE_PV && bestMove != null) ? BOUND_EXACT 
                                                                                  : BOUND_UPPER));

        assert(bestEval > -VALUE_INF && bestEval < VALUE_INF);

        return bestEval;
    }

    /**
     * The quiescence search.
     */
    private static int quiescence(Position pos, int alpha, int beta) {
        assert(-VALUE_INF <= alpha && alpha < beta && beta <= VALUE_INF);

        // Check for draw
        if (pos.insufficientMat()) return VALUE_DRAW;

        // Transposition table lookup
        HashtableEntry ttentry = qsearch_TT.get(pos.key);
        if (ttentry != null) {
            if (    ttentry.type == BOUND_EXACT
                || (ttentry.type == BOUND_UPPER && ttentry.eval * pos.sideToMove <= alpha)
                || (ttentry.type == BOUND_LOWER && ttentry.eval * pos.sideToMove >= beta))
                return ttentry.eval * pos.sideToMove;
        }

        // Get a standing evaluation first
        int standPat = Evaluate.staticEval(pos) * pos.sideToMove;
        if (standPat >= beta) {
            qsearch_TT.add(pos.key, MOVE_NONE, DEPTH_QS, standPat * pos.sideToMove, BOUND_LOWER);
            return standPat;
        }
        if (standPat > alpha) alpha = standPat;

        // Generate captures and promotions only
        List<Move> moveList = pos.genPseudoMoves(GEN_QSEARCH);
        sortMoves(pos, moveList);

        int bestMove = MOVE_NONE;

        // Loop through all the moves
        for (Move move : moveList) {

            assert(move.captured != PIECE_NONE || move.type == PROMOTION);

            // Delta pruning
            if (pos.pieceList.size() > 6) {
                int materialGain = VALUE_PIECE[Math.abs(move.captured)];
                if (move.type == PROMOTION) materialGain += VALUE_PROMOTION;
                if (standPat + materialGain <= alpha - DELTA_MARGIN) continue;
            }

            pos.makeMove(move);

            // Check if the move leaves our king in check
            if (pos.inCheck(-pos.sideToMove)) {
                pos.unmakeMove(move);
                continue;
            }

            int eval = -quiescence(pos, -beta, -alpha);

            pos.unmakeMove(move);

            if (eval >= beta) {
                qsearch_TT.add(pos.key, move.toInt(), DEPTH_QS, eval * pos.sideToMove, BOUND_LOWER);
                return eval;
            }
            if (eval > alpha) {
                bestMove = move.toInt();
                alpha = eval;
            }
        }
        // Update the transposition table
        qsearch_TT.add(pos.key, bestMove, DEPTH_QS, alpha * pos.sideToMove, 
                       (bestMove != MOVE_NONE ? BOUND_EXACT : BOUND_UPPER));

        assert(alpha > -VALUE_INF && alpha < VALUE_INF);

        return alpha;
    }

    /**
     * Sorts the given move list in order of most promising move to least promising move,
     * in order to improve the performance of alpha-beta search.
     */
    private static void sortMoves(Position pos, List<Move> moveList, int hashMove) {
        // Go through the move list and assign priorities
        for (Move move : moveList) {
            if (move.toInt() == hashMove)
                move.score = PRIORITY_HASH_MOVE;
            else if (move.type == PROMOTION)
                // range: 100 to 105
                move.score = PRIORITY_PROMOTION + Math.abs(move.captured);
            else if (move.captured != PIECE_NONE) {
                // range: 54 (KxP) to 99 (PxQ)
                int victim = Math.abs(move.captured);
                if (victim == 3) victim = 2;
                int attacker = Math.abs(move.piece);
                move.score = PRIORITY_CAPTURE + 10 * victim - attacker; // MVV/LVA
            }
            else // History heuristic
                move.hscore = quietHistory[move.piece + 6][move.target];
        }
        Collections.sort(moveList);
    }
    
    /**
     * Move sort for quiescence search.
     */
    private static void sortMoves(Position pos, List<Move> moveList) {
        // Go through the move list and assign priorities
        for (Move move : moveList) {
            if (move.type == PROMOTION)
                // range: 100 to 105
                move.score = PRIORITY_PROMOTION + Math.abs(move.captured);
            else {
                // range: 54 (KxP) to 99 (PxQ)
                int victim = Math.abs(move.captured);
                if (victim == 3) victim = 2;
                int attacker = Math.abs(move.piece);
                move.score = PRIORITY_CAPTURE + 10 * victim - attacker; // MVV/LVA
            }
        }
        Collections.sort(moveList);
    }

    /**
     * Returns the value of mate in ply moves from the root.
     */
    private static int mateScore(int ply) {
        return VALUE_MATE - ply;
    }

    /**
     * Returns the value of being mated in ply moves from the root.
     */
    private static int matedScore(int ply) {
        return -(VALUE_MATE - ply);
    }
    
    /**
     * Extracts the principal variation from the hash table and returns it as list of moves.
     */
    public static List<Move> extractPV(Position pos) {
        List<Move> PV = new ArrayList<Move>();
        HashtableEntry entry = pv_TT.get(pos.key);
        while (entry != null && entry.move != MOVE_NONE && PV.size() < DEPTH_MAX) {
            Move move = getMoveObject(pos, entry.move);
            // In the rare case of a key collision the stored move may be invalid
            if (move == null) break;

            PV.add(move);
            pos.makeMove(move);

            // Check for a repetition cycle
            if (pos.isThreefold()) break;

            entry = pv_TT.get(pos.key);
        }

        // Unmake all the moves
        for (int i = PV.size() - 1; i >= 0; i--)
            pos.unmakeMove(PV.get(i));

        return PV;
    }

    /**
     * Gets a move from the opening book (returns {@code null} if no move).
     */
    public static String getBookMove(String movesString) throws FileNotFoundException {
        Scanner book = new Scanner(new File("book.txt"));
        List<String> variations = new ArrayList<String>();
        while (book.hasNextLine()) {
            String line = book.nextLine().trim();
            movesString = movesString.trim();
            if (line.startsWith(movesString) && !line.equals(movesString)) {
                Scanner continuation = new Scanner(line.substring(movesString.length()));
                variations.add(continuation.next());
                continuation.close();
            }
        }
        book.close();
        if (variations.isEmpty()) return null;
        return variations.get(new Random().nextInt(variations.size()));
    }

    /**
     * Gets the move object with the given algebraic notation (case insensitive, accepts both
     * short and long algebraic forms). Returns {@code null} if the move is not found.
     */
    public static Move getMoveObject(Position pos, String notation) {
        if (notation == null || notation.isBlank()) return null;
        List<Move> moveList = pos.genLegalMoves();
        for (Move move : moveList) {
            if (   notation.equalsIgnoreCase(move.longNot()) 
                || notation.equalsIgnoreCase(move.shortNot(moveList))) return move;
        }
        return null;
    }
    
    /**
     * Gets the move object with the given integer value. Returns {@code null} if no move is found.
     */
    public static Move getMoveObject(Position pos, int moveInt) {
        if (moveInt == MOVE_NONE) return null;
        List<Move> moveList = pos.genLegalMoves();
        for (Move move : moveList) {
            if (moveInt == move.toInt()) return move;
        }
        return null;
    }

    /**
     * Perft checker for the move generation.
     */
    public static long perft(Position pos, int depth) {
        int nodes = 0;

        // test positions from CPW
        //"r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq -"
        //"8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - -"
        //"r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq -"
        //"r2q1rk1/pP1p2pp/Q4n2/bbp1p3/Np6/1B3NBn/pPPP1PPP/R3K2R b KQ -"
        //"rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ -"
        //"r4rk1/1pp1qppp/p1np1n2/2b1p1B1/2B1P1b1/P1NP1N2/1PP1QPPP/R4RK1 w - -"

        // normal perft
        //if (depth == 0) return 1;*/

        List<Move> moveList = pos.genLegalMoves();

        // bulk count
        if (depth == 1) return moveList.size();

        for (Move move : moveList) {
            pos.makeMove(move);
            nodes += perft(pos, depth - 1);
            pos.unmakeMove(move);
        }

        return nodes;
    }
}
