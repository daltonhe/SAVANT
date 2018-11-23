import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
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
	public int sideToMove; // the side to move
	public int castling;   // castling rights for the position, stored as 4 bits (0bKQkq)
	public int enpassant;  // index of the enpassant square (-2 if none)
	public int fiftyMoves; // fifty moves rule half-move clock
	public int moveNumber; // full-move count
	
	public List<Integer> pieceList;     // board indices of all pieces
	public int king_pos_w;              // index of the white king
	public int king_pos_b;              // index of the black king
	public long zobrist;                // zobrist key of the position
	public TranspositionTable reptable; // hash table of previous positions
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
		
		if (input.hasNext() && input.next().toLowerCase().equals("b"))
			sideToMove = BLACK;
		
		if (input.hasNext()) {
			String c = input.next();
			if (c.contains("K")) castling |= W_SHORT_CASTLE;
			if (c.contains("Q")) castling |= W_LONG_CASTLE;
			if (c.contains("k")) castling |= B_SHORT_CASTLE;
			if (c.contains("q")) castling |= B_LONG_CASTLE;
		}
		
		if (input.hasNext())    enpassant = algebraicToIndex(input.next());
		if (input.hasNextInt()) fiftyMoves = input.nextInt();
		if (input.hasNextInt()) moveNumber = input.nextInt();

		input.close();
		Zobrist.initialize();
		zobrist = Zobrist.getKey(this);
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
		moveNumber  = 1;
		king_pos_w  = SQ_NONE;
		king_pos_b  = SQ_NONE;
		zobrist     = 0;
		pieceList   = new LinkedList<Integer>();
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
		// Save the current position in the repetition hash table
		if (nullCount == 0) saveRep();
		
		zobrist ^= Zobrist.castling[castling];
		
		if      (move.type == CASTLE_SHORT) castleShort();
		else if (move.type == CASTLE_LONG)  castleLong();
		else {
			board[move.target] = move.piece;
			board[move.start]  = PIECE_NONE;
			
			pieceList.remove((Integer) move.start);
			if (move.captured == PIECE_NONE || move.type == ENPASSANT)
				pieceList.add(move.target);
			
			zobrist ^= Zobrist.pieces[move.piece + 6][move.target];
			zobrist ^= Zobrist.pieces[move.type == PROMOTION ? 
					PAWN * sideToMove + 6 : move.piece + 6][move.start];
			
			if (move.captured != PIECE_NONE) {
				if (move.type == ENPASSANT) {
					int captureIndex = move.target + 16 * sideToMove;
					board[captureIndex] = PIECE_NONE;
					pieceList.remove((Integer) captureIndex);
					zobrist ^= Zobrist.pieces[PAWN * -sideToMove + 6][captureIndex];
				}
				else
					zobrist ^= Zobrist.pieces[move.captured + 6][move.target];
			}
			
			if      (move.piece == W_KING) king_pos_w = move.target;
			else if (move.piece == B_KING) king_pos_b = move.target;
			
			updateCastlingRights();
		}
		
		zobrist ^= Zobrist.castling[castling];
		
		if (enpassant != SQ_NONE)
			zobrist ^= Zobrist.enpassant[enpassant % 16];
		
		if (move.type == PAWN_TWO) {
			enpassant = move.target + 16 * sideToMove;
			zobrist ^= Zobrist.enpassant[enpassant % 16];
		}
		else
			enpassant = SQ_NONE;
		
		if (sideToMove == BLACK) moveNumber++;

		boolean irreversible = move.captured != PIECE_NONE || Math.abs(move.piece) == PAWN;
		fiftyMoves = irreversible ? 0 : fiftyMoves + 1; 

		sideToMove *= -1;
		zobrist    ^= Zobrist.side;
	}
	
	/**
	 * Makes a null (passing) move.
	 */
	public void makeNullMove() {
		sideToMove *= -1;
		zobrist    ^= Zobrist.side;
		nullCount++;
		nullAllowed = false;
	}
	
	/**
	 * Unmakes a null (passing) move.
	 */
	public void unmakeNullMove() {
		sideToMove *= -1;
		zobrist    ^= Zobrist.side;
		nullCount--;
		nullAllowed = true;
	}
	
	/**
	 * Unmakes the given move on the board.
	 */
	public void unmakeMove(Move move) {	
		sideToMove *= -1;
		zobrist    ^= Zobrist.side;
		
		zobrist  ^= Zobrist.castling[castling];
		castling  = move.prevCastling;
		zobrist  ^= Zobrist.castling[castling];
		
		if (enpassant != SQ_NONE) zobrist ^= Zobrist.enpassant[enpassant % 16];
		enpassant = move.prevEnpassant;
		if (enpassant != SQ_NONE) zobrist ^= Zobrist.enpassant[enpassant % 16];
		
		fiftyMoves = move.prevFiftyMoves;
		
		if (sideToMove == BLACK) moveNumber--;

		if      (move.type == CASTLE_SHORT) uncastleShort();
		else if (move.type == CASTLE_LONG)  uncastleLong();
		else {
			if (move.type == PROMOTION) {
				board[move.start] = PAWN * sideToMove;
				zobrist ^= Zobrist.pieces[PAWN * sideToMove + 6][move.start];
			}
			else {
				board[move.start] = move.piece;
				zobrist ^= Zobrist.pieces[move.piece + 6][move.start];
			}
			
			pieceList.add(move.start);
			if (move.captured == PIECE_NONE || move.type == ENPASSANT)
				pieceList.remove((Integer) move.target);
			
			zobrist ^= Zobrist.pieces[move.piece + 6][move.target];
			if (move.type == ENPASSANT) {
				board[move.target] = PIECE_NONE;
				int captureIndex = move.target + 16 * sideToMove;
				board[captureIndex] = PAWN * -sideToMove;
				pieceList.add(captureIndex);
				zobrist ^= Zobrist.pieces[PAWN * -sideToMove + 6][captureIndex];
			}
			else {
				board[move.target] = move.captured;
				if (move.captured != PIECE_NONE)
					zobrist ^= Zobrist.pieces[move.captured + 6][move.target];
			}

			if      (move.piece == W_KING) king_pos_w = move.start;
			else if (move.piece == B_KING) king_pos_b = move.start;
		}
		
		// Remove the current position from the repetition hash table
		if (nullCount == 0) deleteRep();
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
			zobrist ^= Zobrist.pieces[W_KING + 6][SQ_e1];
			zobrist ^= Zobrist.pieces[W_KING + 6][SQ_g1];
			zobrist ^= Zobrist.pieces[W_ROOK + 6][SQ_f1];
			zobrist ^= Zobrist.pieces[W_ROOK + 6][SQ_h1];
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
			zobrist ^= Zobrist.pieces[B_KING + 6][SQ_e8];
			zobrist ^= Zobrist.pieces[B_KING + 6][SQ_g8];
			zobrist ^= Zobrist.pieces[B_ROOK + 6][SQ_f8];
			zobrist ^= Zobrist.pieces[B_ROOK + 6][SQ_h8];
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
			zobrist ^= Zobrist.pieces[W_KING + 6][SQ_e1];
			zobrist ^= Zobrist.pieces[W_KING + 6][SQ_c1];
			zobrist ^= Zobrist.pieces[W_ROOK + 6][SQ_a1];
			zobrist ^= Zobrist.pieces[W_ROOK + 6][SQ_d1];
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
			zobrist ^= Zobrist.pieces[B_KING + 6][SQ_e8];
			zobrist ^= Zobrist.pieces[B_KING + 6][SQ_c8];
			zobrist ^= Zobrist.pieces[B_ROOK + 6][SQ_a8];
			zobrist ^= Zobrist.pieces[B_ROOK + 6][SQ_d8];
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
			zobrist ^= Zobrist.pieces[W_KING + 6][SQ_e1];
			zobrist ^= Zobrist.pieces[W_KING + 6][SQ_g1];
			zobrist ^= Zobrist.pieces[W_ROOK + 6][SQ_f1];
			zobrist ^= Zobrist.pieces[W_ROOK + 6][SQ_h1];
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
			zobrist ^= Zobrist.pieces[B_KING + 6][SQ_e8];
			zobrist ^= Zobrist.pieces[B_KING + 6][SQ_g8];
			zobrist ^= Zobrist.pieces[B_ROOK + 6][SQ_f8];
			zobrist ^= Zobrist.pieces[B_ROOK + 6][SQ_h8];
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
			zobrist ^= Zobrist.pieces[W_KING + 6][SQ_e1];
			zobrist ^= Zobrist.pieces[W_KING + 6][SQ_c1];
			zobrist ^= Zobrist.pieces[W_ROOK + 6][SQ_a1];
			zobrist ^= Zobrist.pieces[W_ROOK + 6][SQ_d1];
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
			zobrist ^= Zobrist.pieces[B_KING + 6][SQ_e8];
			zobrist ^= Zobrist.pieces[B_KING + 6][SQ_c8];
			zobrist ^= Zobrist.pieces[B_ROOK + 6][SQ_a8];
			zobrist ^= Zobrist.pieces[B_ROOK + 6][SQ_d8];
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
	 * Returns the parity-corrected zobrist key for use in repetition detection.
	 */
	public long repKey() {
		long rzobrist = zobrist;
		if (sideToMove == BLACK)  rzobrist ^= Zobrist.side;
		if (enpassant != SQ_NONE) rzobrist ^= Zobrist.enpassant[enpassant % 16];
		rzobrist ^= Zobrist.castling[castling];
		return rzobrist;
	}
	
	/**
	 * Adds an entry for the current position to the repetition hash table.
	 */
	public void saveRep() {
		reptable.add(repKey());
	}
	
	/**
	 * Deletes an entry for the current position from the repetition hash table.
	 */
	public void deleteRep() {
		reptable.delete(repKey());
	}
	
	/**
	 * Returns the castling right flag for castleType.
	 */
	public boolean canCastle(int castleType) {
		return ((castling & castleType) != 0);
	}
	
	/**
	 * Returns whether the given side is in check.
	 */
	public boolean inCheck(int side) {
		int kingPos = (side == WHITE ? king_pos_w : king_pos_b);
		return isAttacked(kingPos, -side);
	}
	
	/**
	 * Returns a list of all pseudolegal moves that can be made from this position (moves may
	 * leave the king in check).
	 * @param qs - True if generating moves for quiescence search, false otherwise
	 * @return     List of pseudolegal moves
	 */
	public ArrayList<Move> generateMoves(boolean skipQuiets) {
		ArrayList<Move> moveList = new ArrayList<Move>();
		boolean[] isSlider = {false, false, false, true, true, true, false};
		
		for (int index : pieceList) {
			int piece = board[index] * sideToMove;

			switch (piece) {

			case PAWN:
				genPawnMoves(index, skipQuiets, moveList);
				break;
				
			case KING:
				if (!skipQuiets && castling != 0)
					genCastling(index, moveList);
			case KNIGHT:
			case BISHOP:
			case ROOK:
			case QUEEN:
				genPieceMoves(index, MOVE_DELTA[piece], isSlider[piece], skipQuiets, moveList);
				break;
			}
		}
		return moveList;
	}
	
	/**
	 * Returns a list of all legal moves that can be made from this position.
	 */
	public ArrayList<Move> generateLegalMoves() {
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		ArrayList<Move> moveList = generateMoves(false);
		for (Move move : moveList) {
			makeMove(move);
			if (!inCheck(-sideToMove)) legalMoves.add(move);
			unmakeMove(move);
		}
		return legalMoves;
	}
	
	/**
	 * Adds all pseudolegal pawn moves to the move list.
	 */
	public void genPawnMoves(int start, boolean qs, ArrayList<Move> moveList) {
		for (int i = 0; i < MOVE_DELTA[PAWN].length; i++) {
			int target = start + MOVE_DELTA[PAWN][i] * sideToMove;
			if (!isLegalIndex(target)) continue;

			if (   (i == 1 && board[target] == PIECE_NONE) 
				|| (i != 1 && board[target] * sideToMove < 0)) {
				// promotion
				if (target <= SQ_h8 || target >= SQ_a1) {
					moveList.add(new Move(start, target, QUEEN * sideToMove, board[target],
							castling, enpassant, fiftyMoves, PROMOTION));
					moveList.add(new Move(start, target, ROOK * sideToMove, board[target],
							castling, enpassant, fiftyMoves, PROMOTION));
					moveList.add(new Move(start, target, BISHOP * sideToMove, board[target],
							castling, enpassant, fiftyMoves, PROMOTION));
					moveList.add(new Move(start, target, KNIGHT * sideToMove, board[target],
							castling, enpassant, fiftyMoves, PROMOTION));
				}
				// push or capture
				else if (!qs || board[target] != PIECE_NONE || target <= SQ_h7 || target >= SQ_a2)
					moveList.add(new Move(start, target, PAWN * sideToMove, board[target],
							castling, enpassant, fiftyMoves, NORMAL));
			} 
			
			// enpassant
			if (nullAllowed && i != 1 && target == enpassant)
				moveList.add(new Move(start, enpassant, PAWN * sideToMove, PAWN * -sideToMove,
						castling, enpassant, fiftyMoves, ENPASSANT));
			
			// push two squares
			if (   !qs && i == 1 && ((sideToMove == WHITE && start / 16 == 6)
			    || sideToMove == BLACK && start / 16 == 1))
				if (board[target] == PIECE_NONE && board[target - 16 * sideToMove] == PIECE_NONE)
					moveList.add(new Move(start, target - 16 * sideToMove, PAWN * sideToMove, 
							PIECE_NONE, castling, enpassant, fiftyMoves, PAWN_TWO));
		}
	}
	
	/**
	 * Adds all legal castling moves to the move list.
	 */
	public void genCastling(int start, ArrayList<Move> moveList) {
		if (sideToMove == WHITE) {
			if (   canCastle(W_SHORT_CASTLE)
				&& board[SQ_f1] == PIECE_NONE 
				&& board[SQ_g1] == PIECE_NONE
			    && !isAttacked(SQ_e1, BLACK) 
			    && !isAttacked(SQ_f1, BLACK))
				moveList.add(new Move(SQ_e1, SQ_g1, W_KING, PIECE_NONE, 
						castling, enpassant, fiftyMoves, CASTLE_SHORT));
			
			if (   canCastle(W_LONG_CASTLE)
				&& board[SQ_d1] == PIECE_NONE 
				&& board[SQ_c1] == PIECE_NONE 
				&& board[SQ_b1] == PIECE_NONE
				&& !isAttacked(SQ_e1, BLACK) 
				&& !isAttacked(SQ_d1, BLACK))
				moveList.add(new Move(SQ_e1, SQ_c1, W_KING, PIECE_NONE, 
						castling, enpassant, fiftyMoves, CASTLE_LONG));
		}
		else {
			if (   canCastle(B_SHORT_CASTLE)
				&& board[SQ_f8] == PIECE_NONE 
				&& board[SQ_g8] == PIECE_NONE
				&& !isAttacked(SQ_e8, WHITE) 
				&& !isAttacked(SQ_f8, WHITE))
				moveList.add(new Move(SQ_e8, SQ_g8, B_KING, PIECE_NONE, 
						castling, enpassant, fiftyMoves, CASTLE_SHORT));
			
			if (   canCastle(B_LONG_CASTLE)
				&& board[SQ_d8] == PIECE_NONE 
				&& board[SQ_c8] == PIECE_NONE 
				&& board[SQ_b8] == PIECE_NONE 
				&& !isAttacked(SQ_e8, WHITE) 
				&& !isAttacked(SQ_d8, WHITE))
				moveList.add(new Move(SQ_e8, SQ_c8, B_KING, PIECE_NONE, 
						castling, enpassant, fiftyMoves, CASTLE_LONG));
		}
	}
	
	/**
	 * Generates moves using piece deltas and adds them to the move list.
	 * @param start    - Index of piece
	 * @param delta    - Piece delta for calculating target indices
	 * @param slider   - True if the piece is a bishop, rook, or queen
	 * @param qs       - True if only captures should be generated
	 * @param moveList - The list of moves
	 */
	public void genPieceMoves(int start, int[] delta, boolean slider, boolean qs, 
							  ArrayList<Move> moveList) {
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while (isLegalIndex(target) && board[target] * sideToMove <= 0) {
				int captured = board[target];
				if (!qs || captured != PIECE_NONE)
					moveList.add(new Move(start, target, board[start], captured, 
							castling, enpassant, fiftyMoves, NORMAL));

				if (!slider || captured != PIECE_NONE) break;

				target += delta[i];
			}
		}
	}
	
	/**
	 * Returns whether the given index is attacked by the given side.
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

		if (attackScan(index, MOVE_DELTA[KNIGHT], side == WHITE ? "N"  : "n",  false)) return true;
		if (attackScan(index, MOVE_DELTA[BISHOP], side == WHITE ? "BQ" : "bq", true))  return true;
		if (attackScan(index, MOVE_DELTA[ROOK],   side == WHITE ? "RQ" : "rq", true))  return true;
		if (attackScan(index, MOVE_DELTA[KING],   side == WHITE ? "K"  : "k",  false)) return true;

		return false;
	}
	
	/**
	 * Scans the given piece delta for an attacker and returns whether one was found.
	 */
	public boolean attackScan(int start, int[] delta, String attackers, boolean slider) {	
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while (isLegalIndex(target)) {
				int captured = board[target];
				if (captured != PIECE_NONE) {
					char piece = PIECE_STR.charAt(captured + 6);
					if (attackers.indexOf(piece) != -1) return true;
				}
				
				if (!slider || captured != PIECE_NONE) break;

				target += delta[i];
			}
		}
		return false;
	}
	
	/**
	 * Returns whether the given side has any pieces left (NBRQ).
	 */
	public boolean isPawnEnding(int side) {
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
		HashtableEntry rentry = reptable.get(repKey());
		return rentry != null && rentry.count >= 1;
	}
	
	/**
	 * Returns whether the position is a three-fold repetition.
	 */
	public boolean threefoldRep() {
		HashtableEntry rentry = reptable.get(repKey());
		return rentry != null && rentry.count >= 3;
	}
	
	/**
	 * Returns whether there is insufficient mating material left.
	 */
	public boolean insufficientMat() {
		int pieceCount = pieceList.size();
		// Too many pieces left
		if (pieceCount > 5)
			return false;
		// K vs K
		if (pieceCount == 2)
			return true;
		
		// Count pieces
		int[] pieces_w = new int[7], pieces_b = new int[7];
		int count_w = 0, count_b = 0;
		for (int index : pieceList) {
			int piece = board[index];
			
			// Non-minor piece left
			if (Math.abs(piece) == PAWN || Math.abs(piece) == ROOK || Math.abs(piece) == QUEEN)
				return false;
			
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
		if (pieceCount == 3)
			return true;

		if (pieceCount == 4) {
			// Km vs Km
			if (count_w == 2)
				return true;
			// KNN vs K
			if (pieces_w[KNIGHT] == 2 || pieces_b[KNIGHT] == 2)
				return true;
			// KBB vs K || KBN vs K
			return false;
		}
		
		assert(pieceCount == 5);
		
		// 3 minors vs 1
		if (count_w == 4 || count_b == 4)
			return false;
		// KBB vs KN
		if (   pieces_w[BISHOP] == 2 && pieces_b[BISHOP] == 0
			|| pieces_b[BISHOP] == 2 && pieces_w[BISHOP] == 0)
			return false;
		// all other 2 minors vs 1 minor combinations
		return true;
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
		result += " " + indexToAlgebraic(enpassant);
		result += " " + fiftyMoves;
		result += " " + moveNumber;
		return result;
	}
	
	/* HELPERS */
	
	/**
	 * Returns the algebraic coordinate of the given board index.
	 */
	public static String indexToAlgebraic(int index) {
		if (!isLegalIndex(index)) return "-";
		return "" + "abcdefgh".charAt(index % 16) + (8 - index / 16);
	}
	
	/**
	 * Returns the board index of the given algebraic coordinate.
	 */
	public static int algebraicToIndex(String coord) {
		if (coord.length() != 2) return SQ_NONE;
		
		coord = coord.toLowerCase();
		int index = 16 * (8 - Character.getNumericValue(coord.charAt(1))) + 
				"abcdefgh".indexOf(coord.charAt(0));
		if (!isLegalIndex(index)) return SQ_NONE;
		
		return index;
	}
	
	/**
	 * Returns true if index corresponds to a square on the board.
	 */
	public static boolean isLegalIndex(int index) {
		return (index & 0x88) == 0;
	}
	
	/**
	 * Returns the Chebyshev (chessboard) distance between two given indices.
	 */
	public static int distance(int index1, int index2) {
		return Math.max(Math.abs(index1 / 16 - index2 / 16), Math.abs(index1 % 16 - index2 % 16));
	}
}