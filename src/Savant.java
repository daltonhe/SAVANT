import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Stack;
/**
 * Change log
 * 10-07: Started project
 * 		  Created Board class and Definitions interface
 * 10-08: Created Move class
 * 10-22: Finished make and unmake move
 * 		  Finished legal move generation and Perft tested, confirmed bug-free
 * 10-23: Created Engine class
 * 		  Created skeleton alpha-beta search
 * 		  Added basic material evaluator
 * 		  Added iterative deepening framework
 * 		  Added PV extraction
 * 		  Implemented quiescence search for captures/promotions
 * 		  Added basic MVV/LVA sorting scheme
 * 		  Removed rook/bishop underpromotions from move generation
 * 		  Created basic UI scheme
 * 		  Played first online test games (chess24 rating ~1700)
 * 		  Added delta pruning to quiescence search
 * 		  Implemented aspiration windows
 * 		  Last iteration's PV move is now searched first in subsequent iterations
 * 		  Moves are now checked for legality during search instead of during move generation to 
 * 			save computation
 *        Added mobility to evaluation
 *        Added tempo to evaluation
 *        Implemented history heuristic
 *        Implemented principal variation search
 *        Implemented crude repetition detection using FEN history
 * 10-26: Fixed bug where quiescence search was generating non-captures/promotions
 * 		  Ran Bratko-Kopec test: score=14 (T=9, L=5)
 * 		  Implemented basic time control
 * 10-27: Added basic opening book with ~120 most popular variations from master play
 * 		  Cleaned up user interface and added option for engine to play white/black
 *        Fixed algebraic notation for ambiguous knight/rook moves (e.g. Nbd7)
 * 		  Added crude king safety metrics to evaluation
 * 10-28: Implemented null move pruning
 * 		  Added check evasion extension
 * 10-29: Reached 2350 blitz rating on Lichess (100+ games)
 * 		  Implemented tapered evaluation
 *		  Added middle and endgame piece-square tables
 * 10-30: Implemented transposition table with zobrist hashing
 * 		  Implemented new repetition detection scheme using small zobrist hash table
 * 		  PV is now extracted from transposition table following search
 * 		  Updated mobility evaluation
 * 10-31: Changed mobility area to exclude blocked pawns, pawns on the 2nd and 3rd ranks, 
 * 			and king and queen
 * 		  Updated material values to reflect the ideas in Larry Kaufmann's paper
 * 	      Added material imbalances (bishop pair bonus, piece redundancy penalties)
 * 		  Implemented late move reductions (LMR)
 * 11-01: Changed material values to more closely match Stockfish
 * 	      Updated piece square table values to match Stockfish
 * 	      Added major piece redundancy to imbalance evaluation
 * 		  Simplified mobility evaluation to include all attacked squares not occupied by 
 * 			friendly pieces
 * 		  Updated mobility bonus values to match Stockfish
 * 		  Added grain size for more precise eval score
 * 		  Displayed node count is now cumulative across all search depths
 * 11-02: Reverted to old material values (Kaufmann 2012)
 * 11-05: Reverted to Stockfish material values
 * 		  Split Engine and Evaluate classes
 * 		  Changed phase weights to roughly match material values (phase is approximately
 * 			proportional to non-pawn material)
 * 		  Aspiration window width is now incrementally adjusted 
 * 		  Fixed a transposition table bug where evals were being stored incorrectly
 * 		  Fixed a transposition table bug concerning faulty ply values as result of check 
 * 			extensions
 * 		  Added various material imbalance bonuses to evaluation
 * 11-06: Fixed a transposition table bug where TT values were being erroneously used at 
 * 			PV nodes
 * 		  Implemented mate distance pruning
 * 	      Null move, LMR, and TT cutoffs are now only used in non-PV nodes
 * 		  Node count is no longer incremented in quiescence search
 * 	      Pawn moves to the 7th rank are now checked in quiescence search
 * 		  Fixed a bug where castling moves were being generated during quiescence search
 * 		  Check extensions are no longer applied at the root node
 * 		  Added passed pawn evaluation
 * 		  Added doubled and isolated pawn penalties to evaluation
 * 		  Added bonuses for rook on open/semi-open file
 * 11-07: Added bonuses for connected pawns
 * 		  Reverted to old material values (Kaufmann 2012) and old phase values (Fruit)
 * 		  Tweaked imbalance evaluation
 * 		  Reached 2442 blitz and 2267 bullet rating on Lichess
 * 		  Added penalty for pawns on the same color square as the bishop
 * 		  Added penalties for trapped bishop and trapped rook
 * 		  Added bonuses for rook and queen on the 7th rank
 * 		  Tempo bonus is no longer applied to endgame score
 * 		  Added insufficient material check to search
 * 		  Opened GitHub repository
 */

/**
 * @author Dalton He
 */
public class Savant implements Definitions {
	// TODO: fix opening book after user undo
	// TODO: in-tree repetition detection
	// TODO: efficient zobrist update
	// TODO: expand opening book
	// TODO: tt verification
	// TODO: mobility area
	// TODO: backward pawns
	// TODO: blockage detection
	// TODO: unstoppable passers
	// TODO: king safety
	
	public static HashtableEntry[] reptable = new HashtableEntry[HASH_SIZE_REP];
	
	public static void main(String[] args) throws FileNotFoundException {
		
		Board board = new Board();
		//board = new Board("1r2r3/p1p3k1/2qb1pN1/3p1p1Q/3P4/2pBP1P1/PK3PPR/7R");
		//board = new Board("3r4/2P3p1/p4pk1/Nb2p1p1/1P1r4/P1R2P2/6PP/2R3K1 b - - 0 1");
		//board = new Board("r1b4r/2nq1k1p/2n1p1p1/2B1Pp2/p1PP4/5N2/3QBPPP/R4RK1 w - -");
		
		//board = new Board("k7/8/8/8/q7/8/8/1R3R1K w - - 0 1");
		//board = new Board("5rk1/5Npp/8/3Q4/8/8/8/7K w - - 0 1");
		//board = new Board("8/8/8/8/8/8/R7/R3K2k w Q - 0 1");
		//board = new Board("7k/8/8/8/R2K3q/8/8/8 w - - 0 1");
		//board = new Board("2k5/8/8/8/p7/8/8/4K3 b - - 0 1");
		
		//board = new Board("k7/P7/8/K7/8/8/8/8 w - - 0 1");
		//board = new Board("1k6/1P6/8/1K6/8/8/8/8 w - - 0 1");	
		
		Engine.minDepth      = 7;
		Engine.maxDepth      = 25;
		Engine.timeControlOn = true;
		Engine.timeControl   = 0.15;
		Engine.showThinking  = true;
		Engine.showBoard     = true;
		Engine.useBook       = (board.getFEN().equals(INITIAL_FEN));
		
		Stack<Move> moveHistory = new Stack<Move>();
		String openingLine      = "";
		boolean inOpening       = true;
		boolean gameOver        = false;
		boolean playWhite       = false;
		boolean playBlack       = false;
		Scanner scan            = new Scanner(System.in);	
		
		board.print();
		System.out.println();
		
		// Game loop
		while (!gameOver) {

			// Check for mate/stalemate and draw by insufficient material
			if (   board.filterLegal(board.generateMoves(false)).isEmpty()
				|| board.insufficientMaterial()) {
				gameOver = true;
				continue;
			}
			
			/*if (board.moveNumber >= 15) {
				Engine.timeControl = 1.5;
				if (board.moveNumber >= 30) {
					Engine.timeControl = 1.0;
					if (board.moveNumber >= 45)
						Engine.timeControl = 0.7;
				}
			}*/
			
			boolean engineTurn =   (board.sideToMove == WHITE && playWhite)
								|| (board.sideToMove == BLACK && playBlack);
			if (!engineTurn)
				System.out.print((board.sideToMove == WHITE ? "W" : "B") + ": ");
			
			String input = (engineTurn ? "go" : scan.next()).toLowerCase();
			Move move = null;
			
			switch (input) {
			
			case "end":
				gameOver = true;
				break;
				
			case "think":
				Engine.search(board);
				break;
				
			case "go":
				engineTurn = true;
				if (Engine.useBook && inOpening)
					move = Engine.getMoveObject(Engine.getBookMove(openingLine), board);
				if (move == null) {
					inOpening = false;
					//Engine.reptable = reptable;
					Engine.search(board);
					move = Engine.pv.get(0);
				}
				break;
				
			case "undo":
				int hashKey = (int) (board.zobrist % HASH_SIZE_REP);
				reptable[hashKey] = null;
				if (!moveHistory.isEmpty()) {
					board.unmakeMove(moveHistory.pop());
					board.print();
				}
				playWhite = false;
				playBlack = false;
				break;
				
			case "playout":
				playWhite = true;
				playBlack = true;
				break;
				
			case "playwhite":
			case "playw":
				playWhite = true;
				break;
				
			case "playblack":
			case "playb":
				playBlack = true;
				break;
				
			case "fen":
				System.out.println(board.getFEN());
				break;
				
			case "display":
			case "disp":
			case "print":
				board.print();
				break;
				
			default: // attempt to read move
				move = Engine.getMoveObject(input, board);
				if (move == null)
					System.out.println("Invalid move.");
				break;
			}

			if (move != null) {
				board.makeMove(move);
				moveHistory.push(move);
				if (inOpening)
					openingLine += move + " ";
				int hashKey = (int) (board.zobrist % HASH_SIZE_REP);
				reptable[hashKey] = new HashtableEntry(board.zobrist); 
				
				if (engineTurn) {
					if (Engine.showThinking && !inOpening)
						System.out.println();
					System.out.print("SAVANT plays: ");
					System.out.println(move + (Engine.showThinking ? "" : " [" + (Engine.eval / 100.0) + "]"));
				}
				
				if (Engine.showBoard)
					board.print();
			}
			System.out.println();
		}
		scan.close();
	}
	
	/**
	 * Perft checker
	 * @param board
	 * @param depth
	 * @return
	 */
	@SuppressWarnings("unused")
	private static long miniMax(Board board, int depth) {
	  long nodes = 0;
	  if (depth == 0) {
		  return 1;
	  }
	  ArrayList<Move> moveList = board.generateMoves(false);
	  moveList = board.filterLegal(moveList);
	  for (Move move : moveList) {
	    board.makeMove(move);
	    nodes += miniMax(board, depth - 1);
	    board.unmakeMove(move);
	  }
	  
	  return nodes;
	}
	
	// Perft divider
	/*ArrayList<Move> moveList = board.generateMoves(false);
	for (Move move : moveList) {
		System.out.print(move + ": ");
		board.makeMove(move);

		System.out.println(miniMax(board, 1));
		board.unmakeMove(move);
	}*/

}
