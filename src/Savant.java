import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import java.util.Stack;

/**
 * 
 * @author Dalton He
 * created 10-07-18
 * 
 */
public class Savant implements Types {
    // TODO: download more UCI engines
    // TODO: fix opening book after user undo
    // TODO: blockage detection
    // TODO: regex input validation
    // TODO: null verification search
    // TODO: checks in quiescence
    // TODO: passed pawn push extension
    // TODO: time management
    // TODO: special endgame evaluators
    // TODO: SEE
    // TODO: passed pawn eval
    // TODO: king safety
    // TODO: piece lists index board
    // TODO: rook on pawn bonus
    // TODO: contempt factor
    // TODO: in-check special move gen
    // TODO: move ordering for captures
    // TODO: move gen stages
    // TODO: test eval symmetry

    public static Position pos;
    public static String movesString;
    public static boolean inOpening;
    public static boolean debug;

    /**
     * The main method, calls console mode or UCI mode.
     */
    public static void main(String[] args) throws IOException {
        if      (args.length == 0)         consoleMode();
        else if (args[0].charAt(0) == 'u') UCI.main();
    }

    /**
     * Initialization every time a new game starts.
     */
    public static void initNewGame() {
        pos               = new Position();
        //pos = new Position("1r2r3/p1p3k1/2qb1pN1/3p1p1Q/3P4/2pBP1P1/PK3PPR/7R");
        //pos = new Position("3r4/2P3p1/p4pk1/Nb2p1p1/1P1r4/P1R2P2/6PP/2R3K1 b - - 0 1");
        //pos = new Position("r1b4r/2nq1k1p/2n1p1p1/2B1Pp2/p1PP4/5N2/3QBPPP/R4RK1 w - -");

        //pos = new Position("k7/8/8/8/q7/8/8/1R3R1K w - - 0 1");
        //pos = new Position("5rk1/5Npp/8/3Q4/8/8/8/7K w - - 0 1");
        //pos = new Position("8/8/8/8/8/8/R7/R3K2k w Q - 0 1");
        //pos = new Position("7k/8/8/8/R2K3q/8/8/8 w - - 0 1");
        //pos = new Position("2k5/8/8/8/p7/8/8/4K3 b - - 0 1");

        Engine.ttable    = new TranspositionTable(HASH_SIZE_TT);
        Engine.ttable_qs = new TranspositionTable(HASH_SIZE_TT);
        inOpening        = true;
        movesString      = "";
    }

    /**
     * Run the program in console mode.
     */
    public static void consoleMode() throws FileNotFoundException {
        initNewGame();
        Stack<Move> moveHist = new Stack<Move>();
        String gameOverMsg   = "";
        boolean engineWhite  = false;
        boolean engineBlack  = false;
        Scanner input        = new Scanner(System.in);	

        System.out.println("SAVANT, a UCI-compatible chess engine");
        System.out.println("Written by Dalton He\n");
        System.out.println("Type \"help\" for a list of commands.\n");
        pos.print();
        System.out.println();

        // Game loop
        while (true) {

            // Check for mate/stalemate
            if (pos.generateLegalMoves().isEmpty()) {
                if (pos.inCheck(pos.sideToMove))
                    gameOverMsg = (pos.sideToMove == WHITE ? "Black" : "White") + 
                    " wins by checkmate.";
                else
                    gameOverMsg = "Game drawn by stalemate.";
            }

            // Check for draw by insufficient material
            if (pos.insufficientMat())
                gameOverMsg = "Game drawn by insufficient material.";

            // Check for draw by 50 moves rule
            if (pos.fiftyMoves >= 100)
                gameOverMsg = "Game drawn by 50 moves rule.";

            // Check for draw by three-fold repetition
            if (pos.isThreefoldRep())
                gameOverMsg = "Game drawn by threefold repetition.";

            if (!gameOverMsg.isEmpty()) break;

            boolean engineTurn =    (pos.sideToMove == WHITE && engineWhite)
                    || (pos.sideToMove == BLACK && engineBlack);

            if (!engineTurn) System.out.print(">");

            String command = (engineTurn ? "go" : input.nextLine()).toLowerCase();
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
                    move = Engine.getMoveObject(pos, Engine.getBookMove(movesString));

                if (move == null) {
                    inOpening = false;
                    Engine.search(pos);
                    move = Engine.bestMove;
                }
                break;

            case "undo":
                pos.deleteRep();
                if (!moveHist.isEmpty()) {
                    pos.unmakeMove(moveHist.pop());
                    pos.print();
                }
                engineWhite = false;
                engineBlack = false;
                break;

            case "play w":
                engineWhite = true;
                break;

            case "play b":
                engineBlack = true;
                break;

            case "playout":
                engineWhite = true;
                engineBlack = true;
                break;

            case "fen":
                System.out.println(pos.getFEN());
                break;

            case "print":
                pos.print();
                break;

            case "help":
                System.out.println("quit    - Exit the program");
                System.out.println("think   - Tell the engine to think");
                System.out.println("go      - Tell the engine to make a move");
                System.out.println("undo    - Take back the last move played");
                System.out.println("play w  - Tell the engine to play white");
                System.out.println("play b  - Tell the engine to play black");
                System.out.println("playout - Engine will move for both sides");
                System.out.println("fen     - Display the FEN string of the position");
                System.out.println("print   - Display the board");
                break;

            default: // attempt to read move
                move = Engine.getMoveObject(pos, command);
                if (move == null) System.out.println("Invalid command.");
                break;
            }

            if (move != null) {
                pos.makeMove(move);
                moveHist.push(move);
                if (inOpening) movesString += move + " ";

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
