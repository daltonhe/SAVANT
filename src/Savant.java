import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.Stack;
/**
 * Change log
 * 10-07: Started project
 * 		  Created position class and Definitions interface
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
 * 		  Reached 2400 blitz and 2250 bullet rating on Lichess
 * 		  Added penalty for pawns on the same color square as the bishop
 * 		  Added penalties for trapped bishop and trapped rook
 * 		  Added bonuses for rook and queen on the 7th rank
 * 		  Tempo bonus is no longer applied to endgame score
 * 		  Put a limit on the number of plies the search can be extended
 * 		  Added insufficient material check to search
 * 		  Opened GitHub repository
 * 	      Fixed a bug involving faulty repetition detection when null moving
 * 		  Fixed a bug where zobrist keys were initialized incorrectly. This was the actual root
 *          cause of several hash table bugs which I thought I fixed earlier.
 *        Added game over handling to UI
 *        Reached 2300 bullet rating on Lichess
 *        Reached 2450 blitz rating on Lichess (account was finally flagged for computer assistance)
 * 11-09: Added efficient zobrist key updating during makeMove() and unmakeMove()
 *        Reinstated underpromotions to move generation
 *        Changed castling rights storage from string to int
 *        Validated perft and sanity checked hash table
 *        Added node type parameter to search and made corrections to PVS implementation
 * 		  Added special evaluator for use in mating with KX vs K
 * 11-10: Added UCI support
 * 		  Fixed a bug where getMoveObject failed to recognize promotion inputs
 * 11-11: Changed formula for calculating aspiration windows
 * 		  Search is now terminated prematurely when there is only one legal move
 * 11-12: Added time control support to UCI
 *        Search is now hard stopped when time is up
 *        Implemented internal iterative deepening
 *        Added static eval restriction to null move pruning
 *        Added dynamic LMR reduction factor based on node type, ply, and moves searched
 *        Endgame eval score is now scaled down when draw due to insufficient material is likely
 *        Zobrist keys are now the same irrespective of the side to move (for the repetition
 *        	hash table only)
 * 11-13: Added bonuses for king proximity to passed pawns in the endgame
 * 11-14: Implemented in-tree repetition detection
 * 		  Repetition hash table is now stored as a field in the Position class
 *        Moved null move parameters to Position class
 *        Fixed a bug where the inCheck flag was set incorrectly during search
 *        Delta pruning margin is now calculated correctly for promotions
 *        Delta pruning is now switched off in the late endgame
 *        Implemented futility pruning and extended futility pruning
 *        Changed hash table sizes to prime numbers
 *        Added replace-by-age scheme to transposition table; tt is no longer cleared before
 *        	each search
 *        PV hash table is now cleared between iterative deepening searches
 *        Implemented limited razoring
 *        Added penalty for backward pawns
 *        More time allocated for early moves
 *        Search is now terminated prematurely when the time used for this depth is more than 
 *        	half the allocated time for this move
 *        Fixed a bug in which UCI PV lines were being sent in the wrong format
 *        Removed bonus to rook value for having more pawns
 * 11-15: Tested Stockfish material values and reverted to them
 * 		  Added lazy eval for positions with very large material difference 
 * 		  Added separate transposition table class
 * 		  Fixed issues with repetition draw scores and tt
 */

/**
 * @author Dalton He
 * created 10-07-18
 */
public class Savant implements Types {
	// TODO: fix opening book after user undo
	// TODO: blockage detection
	// TODO: regex input validation
	// TODO: null verification search
	// TODO: checks in quiescence
	// TODO: passed pawn pushes during quiescence
	// TODO: time management
	// TODO: download more UCI engines
	
	// TODO: mobility area
	// TODO: endgame
	// TODO: SEE
	// TODO: material imbalance
	// TODO: blocked pawns
	// TODO: passed pawn eval
	// TODO: king safety
	// TODO: piece lists
	// TODO: transposition table to separate class

	public static Position pos       = new Position("4rk2/RR6/8/8/8/8/3q4/7K");
	public static String openingLine = "";
	public static boolean inOpening  = true;
	
	/**
	 * The main method, calls console mode or UCI mode.
	 */
	public static void main(String[] args) throws IOException {
		//pos = new Position("1r2r3/p1p3k1/2qb1pN1/3p1p1Q/3P4/2pBP1P1/PK3PPR/7R");
		//pos = new Position("3r4/2P3p1/p4pk1/Nb2p1p1/1P1r4/P1R2P2/6PP/2R3K1 b - - 0 1");
		//pos = new Position("r1b4r/2nq1k1p/2n1p1p1/2B1Pp2/p1PP4/5N2/3QBPPP/R4RK1 w - -");
		
		//pos = new Position("k7/8/8/8/q7/8/8/1R3R1K w - - 0 1");
		//pos = new Position("5rk1/5Npp/8/3Q4/8/8/8/7K w - - 0 1");
		//pos = new Position("8/8/8/8/8/8/R7/R3K2k w Q - 0 1");
		//pos = new Position("7k/8/8/8/R2K3q/8/8/8 w - - 0 1");
		//pos = new Position("2k5/8/8/8/p7/8/8/4K3 b - - 0 1");
		
		if      (args.length == 0)         consoleMode();
		else if (args[0].charAt(0) == 'u') uciMode();
	}
	
	/**
	 * Initialization every time a new game starts.
	 */
	public static void initNewGame() {
		Engine.ttable = new TranspositionTable(HASH_SIZE_TT);
		inOpening     = true;
		openingLine   = "";
	}
	
	/**
	 * Run the program in UCI mode.
	 */
	public static void uciMode() throws IOException {
		Engine.uciMode = true;
		inOpening      = true;
		
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		while (true) {
			String command = input.readLine();
			
			if (command.equals("uci")) {
				System.out.println("id name SAVANT v1.0");
				System.out.println("id author Dalton He");
				System.out.println("uciok");
			}		
			
			if (command.equals("isready"))
				System.out.println("readyok");
			
			if (command.equals("quit"))
				System.exit(0);
			
			if (command.equals("ucinewgame"))
				initNewGame();
			
			if (command.startsWith("position")) {
				if (command.contains("startpos")) pos = new Position();
				else                              pos = new Position(extractFEN(command));
		
				String[] moveList = extractMoves(command);
				openingLine = "";
				if (moveList != null) {
					for (int i = 0; i < moveList.length; i++) {
						Move move = Engine.getMoveObject(pos, moveList[i]);
						if (move.captured != 0 || Math.abs(move.piece) == PAWN)
							pos.reptable.clear();
						
						pos.makeMove(move);
						openingLine += move + " ";
					}
				}
			}
			
			if (command.startsWith("go")) {
				String[] splitString = command.split(" ");
				
				int wtime = 0, btime = 0, winc = 0, binc = 0;
				for (int i = 0; i < splitString.length; i++) {
					try {
						if (splitString[i].equals("wtime"))
							wtime = Integer.parseInt(splitString[i + 1]);
						else if (splitString[i].equals("btime"))
							btime = Integer.parseInt(splitString[i + 1]);
						else if (splitString[i].equals("winc"))
							winc  = Integer.parseInt(splitString[i + 1]);
						else if (splitString[i].equals("binc"))
							binc  = Integer.parseInt(splitString[i + 1]);	
					}
					catch (ArrayIndexOutOfBoundsException ex) {}
					catch (NumberFormatException ex) {}
				}
				
				Engine.timeLeft  = (pos.sideToMove == WHITE ? wtime : btime);
				Engine.increment = (pos.sideToMove == WHITE ? winc  : binc);
				
				Move move = null;
				if (Engine.useBook && inOpening)
					move = Engine.getMoveObject(pos, Engine.getBookMove(openingLine));
				
				if (move == null) {
					inOpening = false;
					Engine.search(pos);
					move = Engine.bestMove;
				}
				
				System.out.println("bestmove " + move.longNotation());
			}
		}
	}
	
	/**
	 * Extracts the FEN string from the given UCI position command.
	 */
	private static String extractFEN(String command) {
		String[] splitString = command.split(" ");
		String fen = "";
		
		fen += splitString[2] + " "; // Pieces
		fen += splitString[3] + " "; // Side to move
		fen += splitString[4] + " "; // Castling rights
		fen += splitString[5] + " "; // Enpassant square
		fen += splitString[6] + " "; // Half moves
		fen += splitString[7];       // Full moves

		return fen;
	}
	
	/**
	 * Extracts the moves from the given UCI position command.
	 */
	private static String[] extractMoves(String command) {
		if (!command.contains("moves")) return null;
		return command.substring(command.indexOf("moves") + 6).split(" ");
	}
	
	/**
	 * Run the program in console mode.
	 */
	public static void consoleMode() throws FileNotFoundException {
		initNewGame();
		Stack<Move> moveHistory = new Stack<Move>();
		String gameOverMsg      = "";
		boolean engineWhite     = false;
		boolean engineBlack     = false;
		Scanner input           = new Scanner(System.in);	
		
		pos.print();
		System.out.println();
		
		// Game loop
		while (true) {
			
			// Check for mate/stalemate
			if (pos.filterLegal(pos.generateMoves(false)).isEmpty()) {
				if (pos.inCheck(pos.sideToMove))
					gameOverMsg = (pos.sideToMove == WHITE ? "Black" : "White") + 
								  " wins by checkmate.";
				else
					gameOverMsg = "Game drawn by stalemate.";
			}
			
			// Check for draw by insufficient material
			if (pos.insufficientMaterial())
				gameOverMsg = "Game drawn by insufficient material.";
			
			// Check for draw by repetition
			HashtableEntry rentry = pos.reptable.get(pos.repKey());
			if (rentry != null && rentry.count >= 3)
				gameOverMsg = "Game drawn by threefold repetition.";
				
			if (!gameOverMsg.isEmpty())
				break;
			
			boolean engineTurn =   (pos.sideToMove == WHITE && engineWhite)
								|| (pos.sideToMove == BLACK && engineBlack);
			if (!engineTurn)
				System.out.print((pos.sideToMove == WHITE ? "W" : "B") + ": ");
			
			String command = (engineTurn ? "go" : input.next()).toLowerCase();
			Move move = null;
			
			switch (command) {
			
			case "quit":
				System.exit(0);
				break;
				
			case "think":
				Engine.search(pos);
				break;
				
			case "go":
				engineTurn = true;
				if (Engine.useBook && inOpening)
					move = Engine.getMoveObject(pos, Engine.getBookMove(openingLine));
				
				if (move == null) {
					inOpening = false;
					Engine.search(pos);
					move = Engine.bestMove;
				}
				break;
				
			case "undo":
				pos.reptable.delete(pos.zobrist);
				if (!moveHistory.isEmpty()) {
					pos.unmakeMove(moveHistory.pop());
					pos.print();
				}
				engineWhite = false;
				engineBlack = false;
				break;
				
			case "playout":
				engineWhite = true;
				engineBlack = true;
				break;
				
			case "playwhite":
				engineWhite = true;
				break;
				
			case "playblack":
				engineBlack = true;
				break;
				
			case "fen":
				System.out.println(pos.getFEN());
				break;
				
			case "print":
				pos.print();
				break;
				
			default: // attempt to read move
				move = Engine.getMoveObject(pos, command);
				if (move == null) System.out.println("Invalid move.");
				break;
			}

			if (move != null) {
				pos.makeMove(move);
				moveHistory.push(move);
				if (inOpening) openingLine += move + " ";
				
				if (engineTurn) {
					if (!inOpening) System.out.println();
					System.out.println("SAVANT plays: " + move);
				}
				pos.print();
			}
			
			System.out.println();
		}
		
		System.out.println(gameOverMsg);
		input.close();
	}
}
