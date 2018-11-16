import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.Stack;

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
	// TODO: passed pawn eval
	// TODO: king safety
	// TODO: piece lists

	public static Position pos       = new Position("8/2p5/1p4k1/3P2P1/2P3pp/P7/3BpP1K/8 b");
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
			
			System.out.println(Evaluate.staticEval(pos));
			
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
