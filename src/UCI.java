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
                if (moveList != null) {
                    for (String move : moveList)
                        pos.makeMove(Engine.getMoveObject(pos, move));
                }
            }

            if (command.startsWith("go")) {
                String[] split = command.split(" ");
                int wtime = 0, btime = 0, winc = 0, binc = 0;
                for (int i = 0; i < split.length; i++) {
                    if      (split[i].equals("wtime")) wtime = Integer.parseInt(split[i+1]);
                    else if (split[i].equals("btime")) btime = Integer.parseInt(split[i+1]);
                    else if (split[i].equals("winc"))  winc  = Integer.parseInt(split[i+1]);
                    else if (split[i].equals("binc"))  binc  = Integer.parseInt(split[i+1]);
                }
                Engine.timeLeft  = (pos.toMove == WHITE ? wtime : btime);
                Engine.increment = (pos.toMove == WHITE ? winc  : binc);
                Engine.search(pos);
                
                System.out.println("bestmove " + Engine.bestMove.longNot());
            }
        }
    }

    /**
     * Extracts the FEN string from the given UCI position command.
     */
    private static String extractFEN(String command) {
        String[] split = command.split(" ");
        String fen = "";
        fen += split[2] + " "; // Pieces
        fen += split[3] + " "; // Side to move
        fen += split[4] + " "; // Castling rights
        fen += split[5] + " "; // Enpassant square
        fen += split[6] + " "; // Half moves
        fen += split[7];       // Full moves
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
