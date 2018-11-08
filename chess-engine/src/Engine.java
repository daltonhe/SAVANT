import java.util.ArrayList;
import java.util.Collections;

/**
 * Contains the search and evaluation functions.
 * @author Dalton
 * created 6-18-2018
 */
public class Engine implements Definitions {
	public static int searchDepth = 0;
	public static int nodes = 0;
	public static int qnodes = 0;

	/**
	 * Searches to depth for the best move in position and displays the results.
	 * @param position position to search
	 * @param depth depth to search
	 */
	public static void search(Position position, int depth) {
		searchDepth = depth;
		for (int currentDepth = 1; currentDepth <= depth; currentDepth++) {
			nodes = 0;
			qnodes = 0;
			ArrayList<Move> pv = new ArrayList<Move>();
			long startTime = System.nanoTime();
			int eval = alphaBeta(position, currentDepth, -INFINITY, INFINITY, pv);
			eval *= position.sideToMove; //adjust evaluation to white's perspective
			long endTime = System.nanoTime();
			double elapsedSeconds = (endTime - startTime) / 1.0e9;
			elapsedSeconds = Math.round(1000 * elapsedSeconds) / 1000.;
			String result = "";
			result += currentDepth + " ";
			result += "[" + (eval >= 0 ? "+" : "") + eval + "] ";
			for (Move move : pv) {
				result += move + " ";
			}
			result += "\n       (" + elapsedSeconds + "s ";
			result += "nodes=" + (nodes + qnodes);
			result += " nps=" + (long) ((nodes + qnodes) / elapsedSeconds) + ")";
			System.out.println(result);
		}
	}

	/**
	 * Returns the evaluation of position from the perspective of moving side.
	 * @param position current position
	 * @param ply remaining plies of search
	 * @param alpha highest guaranteed score for the maximizing player (white)
	 * @param beta lowest guaranteed score for the minimizing player (black)
	 * @param pv current principal variation
	 * @return evaluation of the position
	 */
	public static int alphaBeta(Position position, int ply, int alpha,
			int beta, ArrayList<Move> pv) {
		nodes++;
		if (ply == 0) {
			return quiescence(position, alpha, beta);
		}
		ArrayList<Move> localpv = new ArrayList<Move>();
		ArrayList<Move> moves = position.generateMoves();
		if (moves.isEmpty()) {
			pv.clear();
			if (position.isInCheck(position.sideToMove)) { //checkmate
				return -(MATE_VALUE - searchDepth + ply);
			} else { //stalemate
				return DRAW_VALUE;
			}
		}
		Collections.sort(moves);
		for (Move move : moves) {
			position.makeMove(move);
			int eval = -alphaBeta(position, ply - 1, -beta, -alpha, localpv);
			position.unmakeMove(move);
			if (eval >= beta) { //beta cutoff; move is too good
				return beta;
			}
			if (eval > alpha) { //new best move
			//update principal variation
				pv.clear();
				pv.add(move);
				pv.addAll(localpv);
				alpha = eval;
			}
		}
		return alpha;
	}

	/**
	 * Returns the evaluation of position after it is quiet (no captures are
	 * available).
	 * @param position current position
	 * @param alpha highest guaranteed score for the maximizing player (white)
	 * @param beta lowest guaranteed score for the minimizing player (black)
	 * @return evaluation of the position
	 */
	public static int quiescence(Position position, int alpha, int beta) {
		qnodes++;
		int standingPat = evaluate(position) * position.sideToMove;
		if (standingPat >= beta) {
			return beta;
		}
		if (standingPat > alpha) {
			alpha = standingPat;
		}
		ArrayList<Move> moves = position.generateCaptures();
		Collections.sort(moves);
		for (Move move : moves) {
			position.makeMove(move);
			int eval = -quiescence(position, -beta, -alpha);
			position.unmakeMove(move);
			if (eval >= beta) {
				return beta;
			}
			if (eval > alpha) {
				alpha = eval;
			}
		}
		return alpha;
	}

	/**
	 * Returns the static evaluation of position from white's perspective.
	 * @param position position to evaluate
	 * @return evaluation of the position
	 */
	public static int evaluate(Position position) {
		int materialScore = 0;
		for (int i = 0; i < 64; i++) {
			int index = INDEX_0x88[i];
			materialScore += MATERIAL_VALUES[position.board[index] + 6];
		}
		return materialScore;
	}

	/**
	 * Returns the perft results for the position at the specified depth.
	 * @param position
	 * @param depth
	 */
	public static void perft(Position position, int depth) {
		long startTime = System.nanoTime();
		long nodes = miniMax(position, depth);
		long endTime = System.nanoTime();
		double elapsedTime = (endTime - startTime) / 1.0e9;
		System.out.println("Seconds:\t" + elapsedTime);
		System.out.println("Nodes:\t\t" + nodes);
		System.out.println("NPS:\t\t" + (long) (nodes / elapsedTime));
		System.out.println(position);
		System.out.println(Engine.evaluate(position));
	}

	/**
	 * Returns the number of child nodes at the specified depth from position.
	 * @param position
	 * @param depth
	 * @return number of nodes
	 */
	public static long miniMax(Position position, int depth) {
	  long nodes = 0;
	  if (depth == 0) {
	  	return 1;
	  }
	  ArrayList<Move> moves = position.generateMoves();
	  for (Move move : moves) {
	    position.makeMove(move);
	    nodes += miniMax(position, depth - 1);
	    position.unmakeMove(move);
	  }
	  return nodes;
	}

}
