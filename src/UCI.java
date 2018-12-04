import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 * @author Dalton He
 * created 11-22-18
 * 
 */
public class UCI implements Types{
    public static Position pos;
    public static String movesString;
    public static boolean inOpening;

    /**
     * Run the program in UCI mode.
     */
    public static void main() throws IOException {
        Engine.uciMode = true;		
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
                Savant.initNewGame();

            if (command.startsWith("position")) {
                if (command.contains("startpos")) pos = new Position();
                else                              pos = new Position(extractFEN(command));

                String[] moveList = extractMoves(command);
                movesString = "";
                if (moveList != null) {
                    for (int i = 0; i < moveList.length; i++) {
                        Move move = Engine.getMoveObject(pos, moveList[i]);
                        if (move.captured != 0 || Math.abs(move.piece) == PAWN)
                            pos.reptable.clear();

                        pos.makeMove(move);
                        movesString += move + " ";
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
                    move = Engine.getMoveObject(pos, Engine.getBookMove(movesString));

                if (move == null) {
                    inOpening = false;
                    Engine.search(pos);
                    move = Engine.bestMove;
                }

                System.out.println("bestmove " + move.longNot());
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
}
