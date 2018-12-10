import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Stack;

/**
 * 
 * @author Dalton He
 * created 10-07-18
 * 
 */
public class Position implements Types {
    public int[] board;    // 0x88 board array of pieces
    //     A   B   C   D   E   F   G   H
    // 8   0   1   2   3   4   5   6   7 
    // 7   16  17  18  19  20  21  22  23
    // 6   32  33  34  35  36  37  38  39
    // 5   48  49  50  51  52  53  54  55
    // 4   64  65  66  67  68  69  70  71
    // 3   80  81  82  83  84  85  86  87
    // 2   96  97  98  99  100 101 102 103
    // 1   112 113 114 115 116 117 118 119
    public int sideToMove; // the side to move
    public int castling;   // castling rights for the position, stored as 4 bits (0bKQkq)
    public int enpassant;  // index of the enpassant square (-2 if none)
    public int fiftyMoves; // fifty moves rule half-move clock

    public long key;                    // zobrist key of the position
    public Stack<State> stateHist;      // previous state history
    public List<Integer> pieceList;     // board indices of all pieces
    public int king_pos_w;              // index of the white king
    public int king_pos_b;              // index of the black king
    public TranspositionTable reptable; // hash table for detecting repetitions
    public int nullCount;               // number of null moves made before this position
    public boolean nullAllowed;         // false if a null move was just made

    /**
     * Initializes the starting position.
     */
    public Position() {
        this(INITIAL_FEN);
    }

    /**
     * Initializes the position from the given FEN string.
     */
    public Position(String fen) {
        if (fen == null || fen.isBlank()) return;

        initDefaults();
        Scanner input = new Scanner(fen);
        String p = input.next();
        try {
            int index = 0;
            for (int i = 0; i < p.length(); i++) {
                char ch = p.charAt(i);
                if (Character.isDigit(ch))
                    index += Character.getNumericValue(ch);
                else if (ch == '/') index += 8;
                else {
                    if (PIECE_STR.indexOf(ch) == -1) continue;
                    if      (ch == 'K') king_pos_w = index;
                    else if (ch == 'k') king_pos_b = index;
                    board[index] = PIECE_STR.indexOf(ch) - 6;
                    pieceList.add(index);
                    index++;
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {};

        if (input.hasNext() && input.next().equalsIgnoreCase("b")) sideToMove = BLACK;
        if (input.hasNext()) {
            String c = input.next();
            if (c.contains("K")) castling |= W_SHORT_CASTLE;
            if (c.contains("Q")) castling |= W_LONG_CASTLE;
            if (c.contains("k")) castling |= B_SHORT_CASTLE;
            if (c.contains("q")) castling |= B_LONG_CASTLE;
        }
        if (input.hasNext())    enpassant  = algToIndex(input.next());
        if (input.hasNextInt()) fiftyMoves = input.nextInt();

        input.close();
        Zobrist.initialize();
        key = Zobrist.getKey(this);
    }

    /**
     * Sets fields to their default values.
     */
    public void initDefaults() {
        board       = new int[120];
        sideToMove  = WHITE;
        castling    = 0;
        enpassant   = SQ_NONE;
        fiftyMoves  = 0;
        key         = 0;
        stateHist   = new Stack<State>();
        pieceList   = new LinkedList<Integer>();
        king_pos_w  = SQ_NONE;
        king_pos_b  = SQ_NONE;
        nullCount   = 0;
        nullAllowed = true;
        reptable    = new TranspositionTable(HASH_SIZE_REP);
    }

    /**
     * Prints an ASCII board.
     */
    public void print() {
        for (int i = 0; i < 8; i++) {
            System.out.print(" " + (8 - i) + "| ");
            for (int j = 0; j < 8; j++)
                System.out.print(PIECE_STR.charAt(board[16 * i + j] + 6) + " ");
            System.out.println();
        }
        System.out.println("    ---------------\n    a b c d e f g h");
    }

    /**
     * Makes the given move on the board.
     */
    public void makeMove(Move move) {
        saveState();

        if (nullCount == 0) reptable.add(key);

        key ^= Zobrist.castling[castling];

        fiftyMoves++;

        if      (move.type == CASTLE_SHORT) castleShort();
        else if (move.type == CASTLE_LONG)  castleLong();
        else {
            board[move.target] = move.piece;
            board[move.start]  = PIECE_NONE;

            pieceList.remove((Integer) move.start);
            if (move.captured == PIECE_NONE || move.type == ENPASSANT)
                pieceList.add(move.target);

            key ^= Zobrist.pieces[move.piece + 6][move.target];
            key ^= Zobrist.pieces[move.type == PROMOTION ? 
                    PAWN * sideToMove + 6 : move.piece + 6][move.start];

            if (move.captured != PIECE_NONE) {
                if (move.type == ENPASSANT) {
                    int captureIndex = move.target + 16 * sideToMove;
                    board[captureIndex] = PIECE_NONE;
                    pieceList.remove((Integer) captureIndex);
                    key ^= Zobrist.pieces[PAWN * -sideToMove + 6][captureIndex];
                }
                else
                    key ^= Zobrist.pieces[move.captured + 6][move.target];
            }

            if      (move.piece == W_KING) king_pos_w = move.target;
            else if (move.piece == B_KING) king_pos_b = move.target;

            updateCastlingRights();
        }

        key ^= Zobrist.castling[castling];

        if (enpassant != SQ_NONE)
            key ^= Zobrist.enpassant[enpassant & 7];

        if (move.type == PAWN_TWO) {
            enpassant = move.target + 16 * sideToMove;
            key ^= Zobrist.enpassant[enpassant & 7];
        }
        else enpassant = SQ_NONE;

        if (move.captured != PIECE_NONE || move.type == PROMOTION || Math.abs(move.piece) == PAWN)
            fiftyMoves = 0;

        sideToMove *= -1;
        key ^= Zobrist.side;
    }

    /**
     * Unmakes the given move on the board.
     */
    public void unmakeMove(Move move) {
        revertState();

        sideToMove *= -1;
        
        if      (move.type == CASTLE_SHORT) uncastleShort();
        else if (move.type == CASTLE_LONG)  uncastleLong();
        else {
            board[move.start] = move.type == PROMOTION ? PAWN * sideToMove : move.piece;

            pieceList.add(move.start);
            if (move.captured == PIECE_NONE || move.type == ENPASSANT)
                pieceList.remove((Integer) move.target);

            if (move.type == ENPASSANT) {
                board[move.target] = PIECE_NONE;
                int captureIndex = move.target + 16 * sideToMove;
                board[captureIndex] = PAWN * -sideToMove;
                pieceList.add(captureIndex);
            }
            else board[move.target] = move.captured;

            if      (move.piece == W_KING) king_pos_w = move.start;
            else if (move.piece == B_KING) king_pos_b = move.start;
        }

        if (nullCount == 0) reptable.delete(key);
    }

    /**
     * Makes a null (passing) move.
     */
    public void makeNullMove() {
        saveState();
        sideToMove *= -1;
        key ^= Zobrist.side;
        if (enpassant != SQ_NONE) {
            key ^= Zobrist.enpassant[enpassant & 7];
            enpassant = SQ_NONE;
        }
        nullCount++;
        nullAllowed = false;
    }

    /**
     * Unmakes a null (passing) move.
     */
    public void unmakeNullMove() {
        revertState();
        sideToMove *= -1;
        nullCount--;
        nullAllowed = true;
    }

    /**
     * Kingside castles for the side to move.
     */
    public void castleShort() {
        if (sideToMove == WHITE) {
            board[SQ_e1] = PIECE_NONE;
            board[SQ_f1] = W_ROOK;
            board[SQ_g1] = W_KING;
            board[SQ_h1] = PIECE_NONE;
            pieceList.remove((Integer) SQ_e1);
            pieceList.add(SQ_f1);
            pieceList.add(SQ_g1);
            pieceList.remove((Integer) SQ_h1);
            key ^= Zobrist.w_short_castle;
            king_pos_w = SQ_g1;
            castling &= ~W_ALL_CASTLING;
        }
        else {
            board[SQ_e8] = PIECE_NONE;
            board[SQ_f8] = B_ROOK;
            board[SQ_g8] = B_KING;
            board[SQ_h8] = PIECE_NONE;
            pieceList.remove((Integer) SQ_e8);
            pieceList.add(SQ_f8);
            pieceList.add(SQ_g8);
            pieceList.remove((Integer) SQ_h8);
            key ^= Zobrist.b_short_castle;
            king_pos_b = SQ_g8;
            castling &= ~B_ALL_CASTLING;
        }
    }

    /**
     * Queenside castles for the side to move.
     */
    public void castleLong() {
        if (sideToMove == WHITE) {
            board[SQ_a1] = PIECE_NONE;
            board[SQ_c1] = W_KING;
            board[SQ_d1] = W_ROOK;
            board[SQ_e1] = PIECE_NONE;
            pieceList.remove((Integer) SQ_a1);
            pieceList.add(SQ_c1);
            pieceList.add(SQ_d1);
            pieceList.remove((Integer) SQ_e1);
            key ^= Zobrist.w_long_castle;
            king_pos_w = SQ_c1;
            castling &= ~W_ALL_CASTLING;
        }
        else {
            board[SQ_a8] = PIECE_NONE;
            board[SQ_c8] = B_KING;
            board[SQ_d8] = B_ROOK;
            board[SQ_e8] = PIECE_NONE;
            pieceList.remove((Integer) SQ_a8);
            pieceList.add(SQ_c8);
            pieceList.add(SQ_d8);
            pieceList.remove((Integer) SQ_e8);
            key ^= Zobrist.b_long_castle;
            king_pos_b = SQ_c8;
            castling &= ~B_ALL_CASTLING;
        }
    }

    /**
     * Undos kingside castling for the side to move.
     */
    public void uncastleShort() {
        if (sideToMove == WHITE) {
            board[SQ_e1] = W_KING;
            board[SQ_f1] = PIECE_NONE;
            board[SQ_g1] = PIECE_NONE;
            board[SQ_h1] = W_ROOK;
            pieceList.add(SQ_e1);
            pieceList.remove((Integer) SQ_f1);
            pieceList.remove((Integer) SQ_g1);
            pieceList.add(SQ_h1);
            king_pos_w = SQ_e1;
        }
        else {
            board[SQ_e8] = B_KING;
            board[SQ_f8] = PIECE_NONE;
            board[SQ_g8] = PIECE_NONE;
            board[SQ_h8] = B_ROOK;
            pieceList.add(SQ_e8);
            pieceList.remove((Integer) SQ_f8);
            pieceList.remove((Integer) SQ_g8);
            pieceList.add(SQ_h8);
            king_pos_b = SQ_e8;
        }
    }

    /**
     * Undos queenside castling for the side to move.
     */
    public void uncastleLong() {
        if (sideToMove == WHITE) {
            board[SQ_a1] = W_ROOK;
            board[SQ_c1] = PIECE_NONE;
            board[SQ_d1] = PIECE_NONE;
            board[SQ_e1] = W_KING;
            pieceList.add(SQ_a1);
            pieceList.remove((Integer) SQ_c1);
            pieceList.remove((Integer) SQ_d1);
            pieceList.add(SQ_e1);
            king_pos_w = SQ_e1;
        }
        else {
            board[SQ_a8] = B_ROOK;
            board[SQ_c8] = PIECE_NONE;
            board[SQ_d8] = PIECE_NONE;
            board[SQ_e8] = B_KING;
            pieceList.add(SQ_a8);
            pieceList.remove((Integer) SQ_c8);
            pieceList.remove((Integer) SQ_d8);
            pieceList.add(SQ_e8);
            king_pos_b = SQ_e8;
        }
    }

    /**
     * Checks the original squares of kings and rooks, and updates the castling rights
     * accordingly.
     */
    public void updateCastlingRights() {
        if (castling == 0) return;
        if (board[SQ_e1] != W_KING) castling &= ~W_ALL_CASTLING;
        else {
            if (board[SQ_h1] != W_ROOK) castling &= ~W_SHORT_CASTLE;
            if (board[SQ_a1] != W_ROOK) castling &= ~W_LONG_CASTLE;
        }
        if (board[SQ_e8] != B_KING) castling &= ~B_ALL_CASTLING;
        else {
            if (board[SQ_h8] != B_ROOK) castling &= ~B_SHORT_CASTLE;
            if (board[SQ_a8] != B_ROOK) castling &= ~B_LONG_CASTLE;
        }
    }

    /**
     * Returns a list of all legal moves that can be made from this position.
     */
    public ArrayList<Move> generateLegalMoves() {
        ArrayList<Move> legalMoves = new ArrayList<Move>();
        ArrayList<Move> moveList = generateMoves(GEN_ALL);
        for (Move move : moveList) {
            makeMove(move);
            if (!inCheck(-sideToMove)) legalMoves.add(move);
            unmakeMove(move);
        }
        return legalMoves;
    }

    /**
     * Returns a list of all pseudolegal moves (moves that follow the basic rules but may leave
     * the king in check) that can be made from this position.
     */
    public ArrayList<Move> generateMoves(int gen) {
        ArrayList<Move> moveList = new ArrayList<Move>();

        for (int index : pieceList) {
            int piece = board[index] * sideToMove;
            if (piece < 0) continue;

            switch (piece) {
            case PAWN:
                genPawn(index, gen, moveList);
                break;

            case KNIGHT:
                genNonslider(index, KNIGHT_DELTA, gen, moveList);
                break;

            case BISHOP:
                genSlider(index, BISHOP_DELTA, gen, moveList);
                break;

            case ROOK:
                genSlider(index, ROOK_DELTA, gen, moveList);
                break;

            case QUEEN:
                genSlider(index, QUEEN_DELTA, gen, moveList);
                break;

            case KING:
                if (gen != GEN_QSEARCH && castling != 0) genCastling(index, moveList);
                genNonslider(index, KING_DELTA, gen, moveList);
                break;
            }
        }
        return moveList;
    }
    
    /**
     * Adds all pseudolegal non-sliding moves to the given move list.
     */
    public void genNonslider(int start, int[] delta, int gen, ArrayList<Move> moveList) {
        for (int d : delta) {
            int target = start + d;
            if (isLegalIndex(target)) {
                int captured = board[target];
                if (   captured * sideToMove <= 0
                    && (gen != GEN_QSEARCH || captured != PIECE_NONE))
                    moveList.add(new Move(start, target, board[start], captured, NORMAL));
            }
        }
    }

    /**
     * Adds all pseudolegal sliding moves to the given move list.
     */
    public void genSlider(int start, int[] delta, int gen, ArrayList<Move> moveList) {
        for (int d : delta) {
            int target = start + d;
            while (isLegalIndex(target)) {
                int captured = board[target];
                if (captured * sideToMove > 0) break;
                if (gen != GEN_QSEARCH || captured != PIECE_NONE)
                    moveList.add(new Move(start, target, board[start], captured, NORMAL));
                if (captured != PIECE_NONE) break;
                target += d;
            }
        }
    }

    /**
     * Adds all pseudolegal pawn moves to the given move list.
     */
    public void genPawn(int start, int gen, ArrayList<Move> moveList) {
        for (int i = 0; i < 3; i++) {
            int target = start + PAWN_DELTA[i] * sideToMove;
            if (!isLegalIndex(target)) continue;

            if (   (i == 0 && board[target] == PIECE_NONE) 
                || (i != 0 && board[target] * sideToMove < 0)) {
                // promotion
                if (target <= SQ_h8 || target >= SQ_a1) {
                    moveList.add(new Move(start, target, QUEEN  * sideToMove, 
                            board[target], PROMOTION));
                    if (gen == GEN_ALL) {
                        moveList.add(new Move(start, target, KNIGHT * sideToMove, 
                                board[target], PROMOTION));
                        moveList.add(new Move(start, target, ROOK   * sideToMove, 
                                board[target], PROMOTION));
                        moveList.add(new Move(start, target, BISHOP * sideToMove, 
                                board[target], PROMOTION));
                    }
                }
                // push or capture
                else if (gen != GEN_QSEARCH || board[target] != PIECE_NONE)
                    moveList.add(new Move(start, target, PAWN * sideToMove,
                            board[target], NORMAL));
            } 

            // enpassant
            if (i != 0 && target == enpassant)
                moveList.add(new Move(start, enpassant, PAWN * sideToMove, PAWN * -sideToMove, 
                        ENPASSANT));

            // push two squares
            if (i == 0 && gen != GEN_QSEARCH && ((   sideToMove == WHITE && (start >> 4) == 6)
                                                  || sideToMove == BLACK && (start >> 4) == 1))
                if (board[target] == PIECE_NONE && board[target - 16 * sideToMove] == PIECE_NONE)
                    moveList.add(new Move(start, target - 16 * sideToMove, PAWN * sideToMove, 
                            PIECE_NONE,  PAWN_TWO));
        }
    }

    /**
     * Adds all pseudolegal castling moves to the given move list.
     */
    public void genCastling(int start, ArrayList<Move> moveList) {
        if (sideToMove == WHITE) {
            if (   canCastle(W_SHORT_CASTLE)
                && board[SQ_f1] == PIECE_NONE 
                && board[SQ_g1] == PIECE_NONE
                && !isAttacked(SQ_e1, BLACK) 
                && !isAttacked(SQ_f1, BLACK))
                moveList.add(new Move(SQ_e1, SQ_g1, W_KING, PIECE_NONE, CASTLE_SHORT));

            if (   canCastle(W_LONG_CASTLE)
                && board[SQ_d1] == PIECE_NONE 
                && board[SQ_c1] == PIECE_NONE
                && board[SQ_b1] == PIECE_NONE
                && !isAttacked(SQ_e1, BLACK) 
                && !isAttacked(SQ_d1, BLACK))
                moveList.add(new Move(SQ_e1, SQ_c1, W_KING, PIECE_NONE, CASTLE_LONG));
        }
        else {
            if (   canCastle(B_SHORT_CASTLE)
                && board[SQ_f8] == PIECE_NONE 
                && board[SQ_g8] == PIECE_NONE
                && !isAttacked(SQ_e8, WHITE) 
                && !isAttacked(SQ_f8, WHITE))
                moveList.add(new Move(SQ_e8, SQ_g8, B_KING, PIECE_NONE, CASTLE_SHORT));
            if (   canCastle(B_LONG_CASTLE)
                && board[SQ_d8] == PIECE_NONE 
                && board[SQ_c8] == PIECE_NONE 
                && board[SQ_b8] == PIECE_NONE 
                && !isAttacked(SQ_e8, WHITE) 
                && !isAttacked(SQ_d8, WHITE))
                moveList.add(new Move(SQ_e8, SQ_c8, B_KING, PIECE_NONE, CASTLE_LONG));
        }
    }

    /**
     * Returns {@code true} if the square given by index is attacked by the given side.
     */
    public boolean isAttacked(int index, int side) {
        if (side == WHITE) {
            if (isLegalIndex(index + 15) && board[index + 15] == W_PAWN) return true;
            if (isLegalIndex(index + 17) && board[index + 17] == W_PAWN) return true;
        }
        else {
            if (isLegalIndex(index - 17) && board[index - 17] == B_PAWN) return true;
            if (isLegalIndex(index - 15) && board[index - 15] == B_PAWN) return true;
        }
        if (nonsliderAttack(index, KNIGHT_DELTA, KNIGHT * side))            return true;
        if (sliderAttack(index, BISHOP_DELTA, BISHOP * side, QUEEN * side)) return true;
        if (sliderAttack(index, ROOK_DELTA, ROOK * side, QUEEN * side))     return true;
        if (nonsliderAttack(index, KING_DELTA, KING * side))                return true;
        return false;
    }
    
    /**
     * Returns {@code true} if the given delta contains the given attacker.
     */
    public boolean nonsliderAttack(int start, int[] delta, int attacker) { 
        for (int d : delta) {
            int target = start + d;
            if (isLegalIndex(target) && board[target] == attacker) return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if the given delta contains the given attackers.
     */
    public boolean sliderAttack(int start, int[] delta, int attacker1, int attacker2) {	
        for (int d : delta) {
            int target = start + d;
            while (isLegalIndex(target)) {
                int piece = board[target];
                if (piece != PIECE_NONE) {
                    if (piece == attacker1 || piece == attacker2) return true;
                    break;
                }
                target += d;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the given castleType is possible.
     */
    public boolean canCastle(int castleType) {
        return ((castling & castleType) != 0);
    }

    /**
     * Returns {@code true} if the given side is in check.
     */
    public boolean inCheck(int side) {
        int kingPos = (side == WHITE ? king_pos_w : king_pos_b);
        return isAttacked(kingPos, -side);
    }

    /**
     * Returns {@code true} if the given side has no pieces left except pawns.
     */
    public boolean hasOnlyPawns(int side) {
        if (pieceList.size() > 18) return false;
        for (int index : pieceList) {
            int piece = board[index] * side;
            if (piece > 0 && piece != PAWN && piece != KING) return false;
        }
        return true;
    }

    /**
     * Returns whether the position has been repeated.
     */
    public boolean isRepeat() {
        HashtableEntry rentry = reptable.get(key);
        return rentry != null && rentry.count >= 1;
    }

    /**
     * Returns whether the position is a three-fold repetition.
     */
    public boolean isThreefoldRep() {
        HashtableEntry rentry = reptable.get(key);
        return rentry != null && rentry.count >= 2;
    }

    /**
     * Returns whether there is insufficient mating material left.
     */
    public boolean insufficientMat() {
        int pieceCount = pieceList.size();
        // Too many pieces left
        if (pieceCount > 5) return false;

        // K vs K
        if (pieceCount == 2) return true;

        // Count pieces
        int[] pieces_w = new int[7], pieces_b = new int[7];
        int count_w = 0, count_b = 0;
        for (int index : pieceList) {
            int piece = board[index];

            // Non-minor piece left
            if (   Math.abs(piece) == PAWN 
                || Math.abs(piece) == ROOK 
                || Math.abs(piece) == QUEEN) return false;

            if (piece > 0) {
                pieces_w[piece]++;
                count_w++;
            }
            else {
                pieces_b[-piece]++;
                count_b++;
            }
        }

        // Km vs K
        if (pieceCount == 3) return true;

        if (pieceCount == 4) {
            // Km vs Km
            if (count_w == 2) return true;

            // KNN vs K
            if (pieces_w[KNIGHT] == 2 || pieces_b[KNIGHT] == 2) return true;

            // KBB vs K || KBN vs K
            return false;
        }

        assert(pieceCount == 5);

        // 3 minors vs 1
        if (count_w == 4 || count_b == 4) return false;

        // KBB vs KN
        if (   pieces_w[BISHOP] == 2 && pieces_b[BISHOP] == 0
            || pieces_b[BISHOP] == 2 && pieces_w[BISHOP] == 0) return false;

        // all other 2 minors vs 1 minor combinations
        return true;
    }

    /**
     * Flips the position with the black and white sides reversed. This is only useful for
     * debugging, e.g. for testing evaluation symmetry.
     */
    public void flip() {
        int[] board_f = new int[120];
        for (int i = 0; i < 8; i++) {
            board_f[SQ_a8 + i] = -board[SQ_a1 + i];
            board_f[SQ_a7 + i] = -board[SQ_a2 + i];
            board_f[SQ_a6 + i] = -board[SQ_a3 + i];
            board_f[SQ_a5 + i] = -board[SQ_a4 + i];
            board_f[SQ_a4 + i] = -board[SQ_a5 + i];
            board_f[SQ_a3 + i] = -board[SQ_a6 + i];
            board_f[SQ_a2 + i] = -board[SQ_a7 + i];
            board_f[SQ_a1 + i] = -board[SQ_a8 + i];
        }
        board = board_f;

        int castling_f = 0;
        if (canCastle(W_SHORT_CASTLE)) castling_f |= B_SHORT_CASTLE;
        if (canCastle(W_LONG_CASTLE))  castling_f |= B_LONG_CASTLE;
        if (canCastle(B_SHORT_CASTLE)) castling_f |= W_SHORT_CASTLE;
        if (canCastle(B_LONG_CASTLE))  castling_f |= W_LONG_CASTLE;
        castling = castling_f;

        if (enpassant != SQ_NONE) enpassant += 48 * sideToMove;

        sideToMove *= -1;

        pieceList.clear();
        for (int index = 0; index < 120; index++) {
            if (board[index] != PIECE_NONE) {
                pieceList.add(index);
                if (board[index] == W_KING) king_pos_w = index;
                if (board[index] == B_KING) king_pos_b = index;
            }
        }
    }

    /**
     * Returns the FEN string of the position.
     */
    public String getFEN() {
        String result = "";
        int index = 0;
        int emptySquares = 0;
        while (index <= SQ_h1) {
            if (board[index] == PIECE_NONE) emptySquares++;
            else {
                if (emptySquares > 0) {
                    result += "" + emptySquares;
                    emptySquares = 0;
                }
                result += PIECE_STR.charAt(board[index] + 6);
            }
            if (index % 8 == 7) {
                if (emptySquares > 0) {
                    result += "" + emptySquares;
                    emptySquares = 0;
                }
                if (index != SQ_h1) result += "/";
                index += 8;
            }
            index++;
        }
        result += (sideToMove == WHITE ? " w " : " b ");
        if (castling == 0) result += "-";
        else {
            result += (canCastle(W_SHORT_CASTLE) ? "K" : "");
            result += (canCastle(W_LONG_CASTLE)  ? "Q" : "");
            result += (canCastle(B_SHORT_CASTLE) ? "k" : "");
            result += (canCastle(B_LONG_CASTLE)  ? "q" : "");
        }
        result += " " + indexToAlg(enpassant);
        result += " " + fiftyMoves;
        result += " " + (stateHist.size() / 2 + 1); // move number
        return result;
    }

    /**
     * Stores information needed to restore a Position object to its previous state after
     * we unmake a move. Whenever a move is made on the board by calling makeMove() or 
     * makeNullMove(), a State object must be pushed to the stateHist stack.
     */
    private class State {
        long key;
        int  castling;
        int  enpassant;
        int  fiftyMoves;

        public State(Position pos) {
            this.key        = pos.key;
            this.castling   = pos.castling;
            this.enpassant  = pos.enpassant;
            this.fiftyMoves = pos.fiftyMoves;
        }
    }

    /**
     * Pushes a State object for the current position to the stateHist stack.
     */
    private void saveState() {
        stateHist.push(new State(this));
    }

    /**
     * Pop the last State off the stateHist stack and use to revert the position.
     */
    private void revertState() {
        State prev = stateHist.pop();
        key        = prev.key;
        castling   = prev.castling;
        enpassant  = prev.enpassant;
        fiftyMoves = prev.fiftyMoves;
    }

    /* STATIC HELPERS */

    /**
     * Returns {@code true} if index corresponds to a square on the board.
     */
    public static boolean isLegalIndex(int index) {
        return (index & 0x88) == 0;
    }

    /**
     * Returns the algebraic coordinate of the given board index.
     */
    public static String indexToAlg(int index) {
        if (!isLegalIndex(index)) return "-";
        return "" + "abcdefgh".charAt(index & 7) + (8 - (index >> 4));
    }

    /**
     * Returns the board index of the given algebraic coordinate.
     */
    public static int algToIndex(String coord) {
        if (coord.length() != 2) return SQ_NONE;
        coord = coord.toLowerCase();
        int index = 16 * (8 - coord.charAt(1)) + "abcdefgh".indexOf(coord.charAt(0));
        if (!isLegalIndex(index)) return SQ_NONE;
        return index;
    }

    /**
     * Returns the Chebyshev (chessboard) distance between two given indices.
     */
    public static int dist(int index1, int index2) {
        return DIST_LOOKUP[index1 - index2 + 0x77];
    }
}