import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;

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
    
    public int toMove;     // the side to move
    public int castling;   // castling rights for the position, stored as 4 bits (0bKQkq)
    public int enpassant;  // enpassant square index (-2 if none)
    public int fiftyMoves; // fifty moves half-move clock

    public long key;              // zobrist hash key of the position
    public List<State> stateHist; // previous state history, used for unmaking moves
    
    public List<Integer> pieces;  // indices of all the pieces
    public int[] indexBoard;      // piece list index lookup
    
    public int w_king;            // index of the white king
    public int b_king;            // index of the black king
    public boolean nullAllowed;   // false if the last move was a null move

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
                if (Character.isDigit(ch)) index += Character.getNumericValue(ch);
                else if (ch == '/')        index += 8;
                else {
                    if (PIECE_STR.indexOf(ch) == -1) continue;
                    if      (ch == 'K') w_king = index;
                    else if (ch == 'k') b_king = index;
                    board[index] = PIECE_STR.indexOf(ch) - 6;
                    addPiece(index);
                    index++;
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {};

        if (input.hasNext() && input.next().equalsIgnoreCase("b")) toMove = BLACK;
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
        toMove      = WHITE;
        castling    = 0;
        enpassant   = SQ_NONE;
        fiftyMoves  = 0;
        key         = 0;
        stateHist   = new ArrayList<State>();
        pieces      = new ArrayList<Integer>(32);
        indexBoard  = new int[120];
        w_king      = SQ_NONE;
        b_king      = SQ_NONE;
        nullAllowed = true;
    }

    /**
     * Prints an ASCII board.
     */
    public void print() {
        for (int r = 0; r < 8; r++) {
            System.out.print(" " + (8 - r) + "| ");
            for (int f = 0; f < 8; f++)
                System.out.print(PIECE_STR.charAt(board[16 * r + f] + 6) + " ");
            System.out.println();
        }
        System.out.println("    ---------------\n    a b c d e f g h");
    }

    /**
     * Makes the given move on the board.
     */
    public void makeMove(Move move) {
        saveState();
        key ^= Zobrist.castling[castling];
        if (enpassant != SQ_NONE) {
            key ^= Zobrist.enpassant[enpassant & 7];
            enpassant = SQ_NONE;
        }
        if (move.type != NORMAL || move.captured != 0 || Math.abs(move.piece) == PAWN)
            fiftyMoves = 0;
        else fiftyMoves++;
        
        if      (move.type == CASTLE_SHORT) castleShort();
        else if (move.type == CASTLE_LONG)  castleLong();
        else {
            board[move.start]  = 0;
            board[move.target] = move.piece;
            
            if (move.type == NORMAL) {
                key ^= Zobrist.moves[move.piece + 6][move.start][move.target];
                if (move.captured == 0) movePiece(move.start, move.target);
                else {
                    key ^= Zobrist.pieces[move.captured + 6][move.target];
                    removePiece(move.start);
                }
                if      (move.piece == W_KING) w_king = move.target;
                else if (move.piece == B_KING) b_king = move.target;
                if (castling != 0) updateCastlingRights();
            }
            else if (move.type == PAWN_TWO) {
                key ^= Zobrist.moves[move.piece + 6][move.start][move.target];
                movePiece(move.start, move.target);
                enpassant = move.target + 16 * toMove;
                key ^= Zobrist.enpassant[enpassant & 7];
            }
            else if (move.type == ENPASSANT) {
                int captureIndex = move.target + 16 * toMove;
                board[captureIndex] = 0;
                key ^= Zobrist.moves[move.piece + 6][move.start][move.target];
                key ^= Zobrist.pieces[PAWN * -toMove + 6][captureIndex];      
                movePiece(move.start, move.target);
                removePiece(captureIndex);
            }
            else { // move.type == PROMOTION
                key ^= Zobrist.moves[PAWN * toMove + 6][move.start][move.target];
                if (move.captured == 0) movePiece(move.start, move.target);
                else {
                    key ^= Zobrist.pieces[move.captured + 6][move.target];
                    removePiece(move.start);
                }
                if (castling != 0) updateCastlingRights();
            }
        }
        key ^= Zobrist.castling[castling];
        toMove *= -1;
        key ^= Zobrist.side;
    }

    /**
     * Unmakes the given move on the board.
     */
    public void unmakeMove(Move move) {
        revertState();
        toMove *= -1;
        
        if      (move.type == CASTLE_SHORT) uncastleShort();
        else if (move.type == CASTLE_LONG)  uncastleLong();
        else {
            if (move.type == NORMAL) {
                board[move.start]  = move.piece;
                board[move.target] = move.captured;
                if (move.captured == 0) movePiece(move.target, move.start);
                else addPiece(move.start);
                if      (move.piece == W_KING) w_king = move.start;
                else if (move.piece == B_KING) b_king = move.start;
            }
            else if (move.type == PAWN_TWO) {
                board[move.start]  = move.piece;
                board[move.target] = 0;
                movePiece(move.target, move.start);
            }
            else if (move.type == ENPASSANT) {
                int captureIndex = move.target + 16 * toMove;
                board[move.start]   = move.piece;
                board[move.target]  = 0;
                board[captureIndex] = PAWN * -toMove;
                movePiece(move.target, move.start);
                addPiece(captureIndex);
            }
            else { // move.type == PROMOTION
                board[move.start]  = PAWN * toMove;
                board[move.target] = move.captured;
                if (move.captured == 0) movePiece(move.target, move.start);
                else addPiece(move.start);
            }
        }
    }
    
    /**
     * Makes a null (passing) move.
     */
    public void makeNullMove() {
        saveState();
        if (enpassant != SQ_NONE) {
            key ^= Zobrist.enpassant[enpassant & 7];
            enpassant = SQ_NONE;
        }
        toMove *= -1;
        key ^= Zobrist.side;
        fiftyMoves = 0;
        nullAllowed = false;
    }

    /**
     * Unmakes a null (passing) move.
     */
    public void unmakeNullMove() {
        revertState();
        toMove *= -1;
        nullAllowed = true;
    }
    
    /**
     * Changes start index to target index in the piece list.
     */
    public void movePiece(int start, int target) {
        pieces.set(indexBoard[start], target);
        indexBoard[target] = indexBoard[start];
    }
    
    /**
     * Adds the given index to the piece list.
     */
    public void addPiece(int index) {
        indexBoard[index] = pieces.size();
        pieces.add(index);
    }
    
    /**
     * Removes the given index from the piece list.
     */
    public void removePiece(int index) {
        int last = pieces.get(pieces.size() - 1);
        pieces.set(indexBoard[index], last);
        indexBoard[last] = indexBoard[index];
        pieces.remove(pieces.size() - 1);
    }
    
    /**
     * Kingside castles for the side to move.
     */
    public void castleShort() {
        if (toMove == WHITE) {
            board[SQ_e1] = 0;
            board[SQ_f1] = W_ROOK;
            board[SQ_g1] = W_KING;
            board[SQ_h1] = 0;
            movePiece(SQ_e1, SQ_g1);
            movePiece(SQ_h1, SQ_f1);
            key ^= Zobrist.w_short_castle;
            w_king = SQ_g1;
            castling &= ~W_ALL_CASTLING;
        }
        else {
            board[SQ_e8] = 0;
            board[SQ_f8] = B_ROOK;
            board[SQ_g8] = B_KING;
            board[SQ_h8] = 0;
            movePiece(SQ_e8, SQ_g8);
            movePiece(SQ_h8, SQ_f8);
            key ^= Zobrist.b_short_castle;
            b_king = SQ_g8;
            castling &= ~B_ALL_CASTLING;
        }
    }

    /**
     * Queenside castles for the side to move.
     */
    public void castleLong() {
        if (toMove == WHITE) {
            board[SQ_a1] = 0;
            board[SQ_c1] = W_KING;
            board[SQ_d1] = W_ROOK;
            board[SQ_e1] = 0;
            movePiece(SQ_e1, SQ_c1);
            movePiece(SQ_a1, SQ_d1);
            key ^= Zobrist.w_long_castle;
            w_king = SQ_c1;
            castling &= ~W_ALL_CASTLING;
        }
        else {
            board[SQ_a8] = 0;
            board[SQ_c8] = B_KING;
            board[SQ_d8] = B_ROOK;
            board[SQ_e8] = 0;
            movePiece(SQ_e8, SQ_c8);
            movePiece(SQ_a8, SQ_d8);
            key ^= Zobrist.b_long_castle;
            b_king = SQ_c8;
            castling &= ~B_ALL_CASTLING;
        }
    }

    /**
     * Undos kingside castling for the side to move.
     */
    public void uncastleShort() {
        if (toMove == WHITE) {
            board[SQ_e1] = W_KING;
            board[SQ_f1] = 0;
            board[SQ_g1] = 0;
            board[SQ_h1] = W_ROOK;
            movePiece(SQ_g1, SQ_e1);
            movePiece(SQ_f1, SQ_h1);
            w_king = SQ_e1;
        }
        else {
            board[SQ_e8] = B_KING;
            board[SQ_f8] = 0;
            board[SQ_g8] = 0;
            board[SQ_h8] = B_ROOK;
            movePiece(SQ_g8, SQ_e8);
            movePiece(SQ_f8, SQ_h8);
            b_king = SQ_e8;
        }
    }

    /**
     * Undos queenside castling for the side to move.
     */
    public void uncastleLong() {
        if (toMove == WHITE) {
            board[SQ_a1] = W_ROOK;
            board[SQ_c1] = 0;
            board[SQ_d1] = 0;
            board[SQ_e1] = W_KING;
            movePiece(SQ_c1, SQ_e1);
            movePiece(SQ_d1, SQ_a1);
            w_king = SQ_e1;
        }
        else {
            board[SQ_a8] = B_ROOK;
            board[SQ_c8] = 0;
            board[SQ_d8] = 0;
            board[SQ_e8] = B_KING;
            movePiece(SQ_c8, SQ_e8);
            movePiece(SQ_d8, SQ_a8);
            b_king = SQ_e8;
        }
    }

    /**
     * Checks the original squares of kings and rooks, and updates the castling rights
     * accordingly.
     */
    public void updateCastlingRights() {
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
    public List<Move> genLegalMoves() {
        List<Move> legalMoves = new ArrayList<Move>();
        List<Move> moveList = genPseudoMoves(GEN_ALL);
        for (Move move : moveList) {
            makeMove(move);
            if (!inCheck(-toMove)) legalMoves.add(move);
            unmakeMove(move);
        }
        return legalMoves;
    }

    /**
     * Returns a list of all pseudolegal moves (moves that follow the basic rules but may leave
     * the king in check) that can be made from this position.
     */
    public List<Move> genPseudoMoves(int gen) {
        List<Move> moveList = new ArrayList<Move>();

        for (int index : pieces) {
            int piece = board[index] * toMove;
            if (piece < 0) continue;

            if      (piece == PAWN)   genPawn(index, gen, moveList);
            else if (piece == KING) {
                if (gen != GEN_QSEARCH && castling != 0) genCastling(index, moveList);
                genNonslider(index, KING_DELTA, gen, moveList);
            }
            else if (piece == KNIGHT) genNonslider(index, KNIGHT_DELTA, gen, moveList);
            else                      genSlider(index, PIECE_DELTA[piece], gen, moveList);
        }
        return moveList;
    }
    
    /**
     * Adds all pseudolegal non-sliding moves to the given move list.
     */
    public void genNonslider(int start, int[] delta, int gen, List<Move> moveList) {
        for (int d : delta) {
            int target = start + d;
            if (isLegalIndex(target)) {
                if (gen == GEN_QSEARCH ? board[target] * toMove < 0
                                       : board[target] * toMove <= 0)
                    moveList.add(new Move(start, target, board[start], board[target], NORMAL));
            }
        }
    }

    /**
     * Adds all pseudolegal sliding moves to the given move list.
     */
    public void genSlider(int start, int[] delta, int gen, List<Move> moveList) {
        for (int d : delta) {
            int target = start + d;
            while (isLegalIndex(target)) {
                int captured = board[target];
                if (captured * toMove <= 0) {
                    if (gen != GEN_QSEARCH || captured != 0)
                        moveList.add(new Move(start, target, board[start], board[target], NORMAL));
                }
                if (captured != 0) break;
                target += d;
            }
        }
    }

    /**
     * Adds all pseudolegal pawn moves to the given move list.
     */
    public void genPawn(int start, int gen, List<Move> moveList) {
        for (int i = 0; i < 3; i++) {
            int target = start - PAWN_DELTA[i] * toMove;
            if (!isLegalIndex(target)) continue;

            if (i == 0 ? board[target] == 0 : board[target] * toMove < 0) {
                if (target <= SQ_h8 || target >= SQ_a1) { // promotion
                    moveList.add(new Move(start, target, QUEEN * toMove, board[target], PROMOTION));
                    if (gen == GEN_ALL) { // underpromotion
                        moveList.add(new Move(start, target, KNIGHT * toMove, board[target], PROMOTION));
                        moveList.add(new Move(start, target, ROOK   * toMove, board[target], PROMOTION));
                        moveList.add(new Move(start, target, BISHOP * toMove, board[target], PROMOTION));
                    }
                }
                // push or capture
                else if (gen != GEN_QSEARCH || board[target] != 0)
                    moveList.add(new Move(start, target, PAWN * toMove, board[target], NORMAL));
            } 

            // enpassant
            if (i != 0 && target == enpassant)
                moveList.add(new Move(start, enpassant, PAWN * toMove, PAWN * -toMove, ENPASSANT));

            // push two squares
            if (   i == 0 && gen != GEN_QSEARCH && board[target] == 0 
                && (toMove == WHITE ? start >= SQ_a2 : start <= SQ_h7)) {
                target -= 16 * toMove;
                if (board[target] == 0) 
                    moveList.add(new Move(start, target, PAWN * toMove, 0, PAWN_TWO));
            }
        }
    }

    /**
     * Adds all pseudolegal castling moves to the given move list.
     */
    public void genCastling(int start, List<Move> moveList) {
        if (toMove == WHITE) {
            if (   canCastle(W_SHORT_CASTLE)
                && board[SQ_f1] == 0 && board[SQ_g1] == 0
                && !isAttacked(SQ_e1, BLACK) && !isAttacked(SQ_f1, BLACK))
                moveList.add(new Move(SQ_e1, SQ_g1, W_KING, 0, CASTLE_SHORT));

            if (   canCastle(W_LONG_CASTLE)
                && board[SQ_d1] == 0 && board[SQ_c1] == 0 && board[SQ_b1] == 0
                && !isAttacked(SQ_e1, BLACK) && !isAttacked(SQ_d1, BLACK))
                moveList.add(new Move(SQ_e1, SQ_c1, W_KING, 0, CASTLE_LONG));
        }
        else {
            if (   canCastle(B_SHORT_CASTLE)
                && board[SQ_f8] == 0 && board[SQ_g8] == 0
                && !isAttacked(SQ_e8, WHITE) && !isAttacked(SQ_f8, WHITE))
                moveList.add(new Move(SQ_e8, SQ_g8, B_KING, 0, CASTLE_SHORT));
            
            if (   canCastle(B_LONG_CASTLE)
                && board[SQ_d8] == 0 && board[SQ_c8] == 0 && board[SQ_b8] == 0 
                && !isAttacked(SQ_e8, WHITE) && !isAttacked(SQ_d8, WHITE))
                moveList.add(new Move(SQ_e8, SQ_c8, B_KING, 0, CASTLE_LONG));
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
        if (nonsliderAttack(index, KING_DELTA, KING * side))                return true;
        if (nonsliderAttack(index, KNIGHT_DELTA, KNIGHT * side))            return true;
        if (sliderAttack(index, BISHOP_DELTA, BISHOP * side, QUEEN * side)) return true;
        if (sliderAttack(index, ROOK_DELTA, ROOK * side, QUEEN * side))     return true;
        
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
                if (piece != 0) {
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
        return isAttacked((side == WHITE ? w_king : b_king), -side);
    }

    /**
     * Returns {@code true} if the given side has no pieces left except pawns.
     */
    public boolean hasOnlyPawns(int side) {
        if (pieces.size() > 18) return false;
        for (int index : pieces) {
            int piece = board[index] * side;
            if (piece > 0 && piece != PAWN && piece != KING) return false;
        }
        return true;
    }

    /**
     * Returns {@code true} if the current position is a repetition.
     */
    public boolean isRepeat() {
        if (fiftyMoves < 4) return false;
        for (int i = 4; i <= fiftyMoves; i += 2)
            if (key == stateHist.get(stateHist.size() - i).key) return true;
        return false;
    }

    /**
     * Returns {@code true} if the current position is a three-fold repetition.
     */
    public boolean isThreefold() {
        if (fiftyMoves < 4) return false;
        int count = 0;
        for (int i = 4; i <= fiftyMoves; i += 2)
            if (key == stateHist.get(stateHist.size() - i).key) count++;
        return (count >= 2);
    }

    /**
     * Returns {@code true} if there is insufficient mating material.
     */
    public boolean insufficientMat() {
        if (pieces.size() > 3)  return false; // Too many pieces left
        if (pieces.size() == 2) return true;  // K vs K
        for (int index : pieces) {
            int piece = board[index];
            // Non-minor piece left
            if (   Math.abs(piece) == PAWN 
                || Math.abs(piece) == ROOK 
                || Math.abs(piece) == QUEEN) return false;
        }
        return true;
    }

    /**
     * Flips the position with the black and white sides reversed. This is only useful for
     * debugging purposes, i.e. testing evaluation symmetry.
     */
    public void flip() {
        int[] fboard = new int[120];
        for (int f = 0; f < 8; f++) {
            fboard[SQ_a8 + f] = -board[SQ_a1 + f];
            fboard[SQ_a7 + f] = -board[SQ_a2 + f];
            fboard[SQ_a6 + f] = -board[SQ_a3 + f];
            fboard[SQ_a5 + f] = -board[SQ_a4 + f];
            fboard[SQ_a4 + f] = -board[SQ_a5 + f];
            fboard[SQ_a3 + f] = -board[SQ_a6 + f];
            fboard[SQ_a2 + f] = -board[SQ_a7 + f];
            fboard[SQ_a1 + f] = -board[SQ_a8 + f];
        }
        board = fboard;

        int fcastling = 0;
        if (canCastle(W_SHORT_CASTLE)) fcastling |= B_SHORT_CASTLE;
        if (canCastle(W_LONG_CASTLE))  fcastling |= B_LONG_CASTLE;
        if (canCastle(B_SHORT_CASTLE)) fcastling |= W_SHORT_CASTLE;
        if (canCastle(B_LONG_CASTLE))  fcastling |= W_LONG_CASTLE;
        castling = fcastling;

        if (enpassant != SQ_NONE) enpassant += 48 * toMove;

        toMove *= -1;

        pieces.clear();
        for (int index = 0; index < 120; index++) {
            if (board[index] != 0) {
                addPiece(index);
                if (board[index] == W_KING) w_king = index;
                if (board[index] == B_KING) b_king = index;
            }
        }
    }

    /**
     * Returns the FEN string of the position.
     */
    public String fen() {
        String result = "";
        int index = 0;
        int emptySquares = 0;
        while (index <= SQ_h1) {
            if (board[index] == 0) emptySquares++;
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
        result += (toMove == WHITE ? " w " : " b ");
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
        stateHist.add(new State(this));
    }

    /**
     * Pop the last State off the stateHist stack and use to revert the position.
     */
    private void revertState() {
        State prev = stateHist.remove(stateHist.size() - 1);
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