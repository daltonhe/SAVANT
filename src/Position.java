import java.util.ArrayList;
import java.util.Scanner;

/**
 * @author Dalton He
 * created 10-07-18
 */
public class Position implements Types {
	public int[] board;    // 0x88 board array of pieces
	public int sideToMove; // the side to move
	public int castling;   // castling rights for the position, stored as 4 bits (0bKQkq)
	public int enpassant;  // index of the enpassant square (-2 if none)
	public int fiftyMoves; // fifty moves rule half-move clock
	public int moveNumber; // full-move count
	public int kingPos_w;  // index of the white king
	public int kingPos_b;  // index of the black king
	public int pieceCount; // total number of pieces left on board
	public long zobrist;   // zobrist key of the position
	
	public HashtableEntry[] reptable; // hash table of previous positions
	public int nullCount;             // number of null moves made to this position
	
	/**
	 * Initializes the starting position.
	 */
	public Position() {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}
	
	/**
	 * Initializes the position from the given FEN string.
	 */
	public Position(String fen) {
		setDefaults();
		if (fen == null || fen.isBlank())
			return;
		
		Scanner input = new Scanner(fen);
		String pieces = input.next();
		
		try {
			int index = 0;
			for (int i = 0; i < pieces.length(); i++) {
				char ch = pieces.charAt(i);
				if (Character.isDigit(ch))
					index += Character.getNumericValue(ch);
				else if (ch == '/')
					index += 8;
				else {
					if (PIECE_STR.indexOf(ch) == -1) continue;
					
					if      (ch == 'K') kingPos_w = index;
					else if (ch == 'k') kingPos_b = index;
	
					board[index] = PIECE_STR.indexOf(ch) - 6;
					pieceCount++;
					
					index++;
				}
			}
		} catch (ArrayIndexOutOfBoundsException ex) {};
		
		if (input.hasNext())
			if (input.next().toLowerCase().equals("b"))
				sideToMove = BLACK;
		
		if (input.hasNext()) {
			String castl = input.next();
			if (castl.contains("K")) castling |= W_SHORT_CASTLE;
			if (castl.contains("Q")) castling |= W_LONG_CASTLE;
			if (castl.contains("k")) castling |= B_SHORT_CASTLE;
			if (castl.contains("q")) castling |= B_LONG_CASTLE;
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
	public void setDefaults() {
		board      = new int[128];
		sideToMove = WHITE;
		castling   = 0;
		enpassant  = SQ_NONE;
		fiftyMoves = 0;
		moveNumber = 1;
		kingPos_w  = SQ_NONE;
		kingPos_b  = SQ_NONE;
		pieceCount = 0;
		zobrist    = 0;
		reptable   = new HashtableEntry[HASH_SIZE_REP];
		nullCount  = 0;
	}
	
	/**
	 * Returns true if index corresponds to a square on the board.
	 */
	public static boolean isLegalIndex(int index) {
		return (index & 0x88) == 0;
	}
	
	/**
	 * Makes a null (passing) move.
	 */
	public void makePassingMove() {
		sideToMove *= -1;
		zobrist    ^= Zobrist.side;
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
			board[move.start]  = 0;
			
			zobrist ^= Zobrist.pieces[move.piece + 6][move.target];
			if (move.type == PROMOTION)
				zobrist ^= Zobrist.pieces[PAWN * sideToMove + 6][move.start];
			else
				zobrist ^= Zobrist.pieces[move.piece + 6][move.start];
			
			if (move.captured != 0) {
				pieceCount--;
				
				if (move.type == ENPASSANT) {
					int captureIndex = move.target + 16 * sideToMove;
					board[captureIndex] = 0;
					zobrist ^= Zobrist.pieces[PAWN * -sideToMove + 6][captureIndex];
				}
				else
					zobrist ^= Zobrist.pieces[move.captured + 6][move.target];
			}

			if      (move.piece == W_KING) kingPos_w = move.target;
			else if (move.piece == B_KING) kingPos_b = move.target;
			
			if (castling != 0) updateCastlingRights();
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

		if (move.captured != 0 || Math.abs(move.piece) == PAWN)
			fiftyMoves = 0;
		else
			fiftyMoves++;

		sideToMove *= -1;
		zobrist    ^= Zobrist.side;
	}
	
	/**
	 * Unmakes the given move on the board.
	 */
	public void unmakeMove(Move move) {	
		sideToMove *= -1;
		zobrist    ^= Zobrist.side;
		zobrist    ^= Zobrist.castling[castling];
		castling    = move.prevCastling;
		zobrist    ^= Zobrist.castling[castling];
		
		if (enpassant != SQ_NONE)
			zobrist ^= Zobrist.enpassant[enpassant % 16];
		enpassant   = move.prevEnpassant;
		if (enpassant != SQ_NONE)
			zobrist ^= Zobrist.enpassant[enpassant % 16];
		
		fiftyMoves  = move.prevFiftyMoves;
		
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
			
			zobrist ^= Zobrist.pieces[move.piece + 6][move.target];
			if (move.type == ENPASSANT) {
				board[move.target] = 0;
				int captureIndex = move.target + 16 * sideToMove;
				board[captureIndex] = PAWN * -sideToMove;
				zobrist ^= Zobrist.pieces[PAWN * -sideToMove + 6][captureIndex];
			}
			else {
				board[move.target] = move.captured;
				if (move.captured != 0)
					zobrist ^= Zobrist.pieces[move.captured + 6][move.target];
			}
			
			if (move.captured != 0) pieceCount++;

			if      (move.piece == W_KING) kingPos_w = move.start;
			else if (move.piece == B_KING) kingPos_b = move.start;
		}
		
		// Remove the current position from the repetition hash table
		if (nullCount == 0) deleteRep();
	}
	
	/**
	 * Kingside castles for the side to move.
	 */
	public void castleShort() {
		if (sideToMove == WHITE) {
			board[SQ_e1] = 0;
			board[SQ_f1] = W_ROOK;
			board[SQ_g1] = W_KING;
			board[SQ_h1] = 0;
			zobrist     ^= Zobrist.pieces[W_KING + 6][SQ_e1];
			zobrist     ^= Zobrist.pieces[W_KING + 6][SQ_g1];
			zobrist     ^= Zobrist.pieces[W_ROOK + 6][SQ_f1];
			zobrist     ^= Zobrist.pieces[W_ROOK + 6][SQ_h1];
			kingPos_w = SQ_g1;
			castling    &= ~W_ALL_CASTLING;
		}
		else {
			board[SQ_e8] = 0;
			board[SQ_f8] = B_ROOK;
			board[SQ_g8] = B_KING;
			board[SQ_h8] = 0;
			zobrist     ^= Zobrist.pieces[B_KING + 6][SQ_e8];
			zobrist     ^= Zobrist.pieces[B_KING + 6][SQ_g8];
			zobrist     ^= Zobrist.pieces[B_ROOK + 6][SQ_f8];
			zobrist     ^= Zobrist.pieces[B_ROOK + 6][SQ_h8];
			kingPos_b = SQ_g8;
			castling    &= ~B_ALL_CASTLING;
		}
	}
	
	/**
	 * Queenside castles for the side to move.
	 */
	public void castleLong() {
		if (sideToMove == WHITE) {
			board[SQ_a1] = 0;
			board[SQ_c1] = W_KING;
			board[SQ_d1] = W_ROOK;
			board[SQ_e1] = 0;
			zobrist     ^= Zobrist.pieces[W_KING + 6][SQ_e1];
			zobrist     ^= Zobrist.pieces[W_KING + 6][SQ_c1];
			zobrist     ^= Zobrist.pieces[W_ROOK + 6][SQ_a1];
			zobrist     ^= Zobrist.pieces[W_ROOK + 6][SQ_d1];
			kingPos_w = SQ_c1;
			castling    &= ~W_ALL_CASTLING;
		}
		else {
			board[SQ_a8] = 0;
			board[SQ_c8] = B_KING;
			board[SQ_d8] = B_ROOK;
			board[SQ_e8] = 0;
			zobrist     ^= Zobrist.pieces[B_KING + 6][SQ_e8];
			zobrist     ^= Zobrist.pieces[B_KING + 6][SQ_c8];
			zobrist     ^= Zobrist.pieces[B_ROOK + 6][SQ_a8];
			zobrist     ^= Zobrist.pieces[B_ROOK + 6][SQ_d8];
			kingPos_b = SQ_c8;
			castling    &= ~B_ALL_CASTLING;
		}
	}
	
	/**
	 * Undos kingside castling for the side to move.
	 */
	public void uncastleShort() {
		if (sideToMove == WHITE) {
			board[SQ_e1] = W_KING;
			board[SQ_f1] = 0;
			board[SQ_g1] = 0;
			board[SQ_h1] = W_ROOK;
			zobrist     ^= Zobrist.pieces[W_KING + 6][SQ_e1];
			zobrist     ^= Zobrist.pieces[W_KING + 6][SQ_g1];
			zobrist     ^= Zobrist.pieces[W_ROOK + 6][SQ_f1];
			zobrist     ^= Zobrist.pieces[W_ROOK + 6][SQ_h1];
			kingPos_w = SQ_e1;
		}
		else {
			board[SQ_e8] = B_KING;
			board[SQ_f8] = 0;
			board[SQ_g8] = 0;
			board[SQ_h8] = B_ROOK;
			zobrist     ^= Zobrist.pieces[B_KING + 6][SQ_e8];
			zobrist     ^= Zobrist.pieces[B_KING + 6][SQ_g8];
			zobrist     ^= Zobrist.pieces[B_ROOK + 6][SQ_f8];
			zobrist     ^= Zobrist.pieces[B_ROOK + 6][SQ_h8];
			kingPos_b = SQ_e8;
		}
	}
	
	/**
	 * Undos queenside castling for the side to move.
	 */
	public void uncastleLong() {
		if (sideToMove == WHITE) {
			board[SQ_a1] = W_ROOK;
			board[SQ_c1] = 0;
			board[SQ_d1] = 0;
			board[SQ_e1] = W_KING;
			zobrist     ^= Zobrist.pieces[W_KING + 6][SQ_e1];
			zobrist     ^= Zobrist.pieces[W_KING + 6][SQ_c1];
			zobrist     ^= Zobrist.pieces[W_ROOK + 6][SQ_a1];
			zobrist     ^= Zobrist.pieces[W_ROOK + 6][SQ_d1];
			kingPos_w = SQ_e1;
		}
		else {
			board[SQ_a8] = B_ROOK;
			board[SQ_c8] = 0;
			board[SQ_d8] = 0;
			board[SQ_e8] = B_KING;
			zobrist     ^= Zobrist.pieces[B_KING + 6][SQ_e8];
			zobrist     ^= Zobrist.pieces[B_KING + 6][SQ_c8];
			zobrist     ^= Zobrist.pieces[B_ROOK + 6][SQ_a8];
			zobrist     ^= Zobrist.pieces[B_ROOK + 6][SQ_d8];
			kingPos_b = SQ_e8;
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
	 * Adds an entry for the current position to the repetition hash table.
	 */
	public void saveRep() {
		// fix parity issue so that zobrist keys are the same irrespective of the side to move
		long rzobrist = zobrist;
		if (sideToMove == BLACK) rzobrist ^= Zobrist.side;
		
		int hashKey = (int) (rzobrist % HASH_SIZE_REP);
		if (reptable[hashKey] == null || rzobrist != reptable[hashKey].zobrist)
			reptable[hashKey] = new HashtableEntry(rzobrist);
		else
			reptable[hashKey].count++;
	}
	
	/**
	 * Deletes an entry for the current position from the repetition hash table.
	 */
	public void deleteRep() {
		// fix parity issue so that zobrist keys are the same irrespective of the side to move
		long rzobrist = zobrist;
		if (sideToMove == BLACK) rzobrist ^= Zobrist.side;
		
		int hashKey = (int) (rzobrist % HASH_SIZE_REP);
		if (reptable[hashKey] == null || rzobrist != reptable[hashKey].zobrist)
			return;
		
		if (reptable[hashKey].count > 1)
			reptable[hashKey].count--;
		else
			reptable[hashKey] = null;
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
		int kingPos = (side == WHITE ? kingPos_w : kingPos_b);
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
		
		for (int index = SQ_a8; index <= SQ_h1; index++) {
			if (!isLegalIndex(index)) continue;

			int piece = board[index] * sideToMove;
			if (piece == 0) continue;

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
				genDelta(index, PIECE_DELTAS[piece], isSlider[piece], skipQuiets, moveList);
				break;
			}
		}
		return moveList;
	}
	
	/**
	 * Filters out all moves from the given move list that leave the king in check. Returns the
	 * new move list.
	 */
	public ArrayList<Move> filterLegal(ArrayList<Move> moveList) {
		ArrayList<Move> legalMoves = new ArrayList<Move>();
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
		for (int i = 0; i < DELTA_PAWN.length; i++) {
			int target = start + DELTA_PAWN[i] * sideToMove;
			if (!isLegalIndex(target)) continue;

			if ((i == 1 && board[target] == 0) || (i != 1 && board[target] * sideToMove < 0)) {
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
				else if (!qs || board[target] != 0 || target <= SQ_h7 || target >= SQ_a2)
					moveList.add(new Move(start, target, PAWN * sideToMove, board[target],
							castling, enpassant, fiftyMoves, NORMAL));
			} 
			
			// enpassant
			if (i != 1 && target == enpassant)
				moveList.add(new Move(start, enpassant, PAWN * sideToMove, PAWN * -sideToMove,
						castling, enpassant, fiftyMoves, ENPASSANT));
			
			// push two squares
			if (   !qs && i == 1 && ((sideToMove == WHITE && start / 16 == 6)
			    || sideToMove == BLACK && start / 16 == 1))
				if (board[target] == 0 && board[target - 16 * sideToMove] == 0)
					moveList.add(new Move(start, target - 16 * sideToMove, PAWN * sideToMove, 0,
							castling, enpassant, fiftyMoves, PAWN_TWO));
		}
	}
	
	/**
	 * Adds all legal castling moves to the move list.
	 */
	public void genCastling(int start, ArrayList<Move> moveList) {
		if (sideToMove == WHITE) {
			if (   canCastle(W_SHORT_CASTLE)
				&& board[SQ_f1] == 0 
				&& board[SQ_g1] == 0
			    && !isAttacked(SQ_e1, BLACK) 
			    && !isAttacked(SQ_f1, BLACK))
				moveList.add(new Move(
						SQ_e1, SQ_g1, W_KING, 0, castling, enpassant, fiftyMoves, CASTLE_SHORT));
			
			if (   canCastle(W_LONG_CASTLE)
				&& board[SQ_d1] == 0 
				&& board[SQ_c1] == 0 
				&& board[SQ_b1] == 0
				&& !isAttacked(SQ_e1, BLACK) 
				&& !isAttacked(SQ_d1, BLACK))
				moveList.add(new Move(
						SQ_e1, SQ_c1, W_KING, 0, castling, enpassant, fiftyMoves, CASTLE_LONG));
		}
		else {
			if (   canCastle(B_SHORT_CASTLE)
				&& board[SQ_f8] == 0 
				&& board[SQ_g8] == 0
				&& !isAttacked(SQ_e8, WHITE) 
				&& !isAttacked(SQ_f8, WHITE))
				moveList.add(new Move(
						SQ_e8, SQ_g8, B_KING, 0, castling, enpassant, fiftyMoves, CASTLE_SHORT));
			
			if (   canCastle(B_LONG_CASTLE)
				&& board[SQ_d8] == 0 
				&& board[SQ_c8] == 0 
				&& board[SQ_b8] == 0 
				&& !isAttacked(SQ_e8, WHITE) 
				&& !isAttacked(SQ_d8, WHITE))
				moveList.add(new Move(
						SQ_e8, SQ_c8, B_KING, 0, castling, enpassant, fiftyMoves, CASTLE_LONG));
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
	public void genDelta(int start, int[] delta, boolean slider, boolean qs, ArrayList<Move> moveList) {
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while (isLegalIndex(target) && board[target] * sideToMove <= 0) {
				int captured = board[target];
				if (!qs || captured != 0)
					moveList.add(new Move(
							start, target, board[start], captured, castling, enpassant, fiftyMoves, NORMAL));

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
			if (index % 16 == 0 || board[index + 15] == W_PAWN) return true;
			if (index % 16 == 7 || board[index + 17] == W_PAWN) return true;
		}
		else {
			if (index % 16 == 0 || board[index - 17] == B_PAWN) return true;
			if (index % 16 == 7 || board[index - 15] == B_PAWN) return true;
		}

		if (attackDelta(index, DELTA_KNIGHT, side == WHITE ? "N"  : "n",  false)) return true;
		if (attackDelta(index, DELTA_BISHOP, side == WHITE ? "BQ" : "bq", true))  return true;
		if (attackDelta(index, DELTA_ROOK,   side == WHITE ? "RQ" : "rq", true))  return true;
		if (attackDelta(index, DELTA_KING,   side == WHITE ? "K"  : "k",  false)) return true;

		return false;
	}
	
	/**
	 * Scans the given piece delta for an attacker and returns whether one was found.
	 */
	public boolean attackDelta(int start, int[] delta, String attackers, boolean slider) {	
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while (isLegalIndex(target)) {
				int captured = board[target];
				if (captured != PIECE_NONE) {
					char piece = PIECE_STR.charAt(captured + 6);
					if (attackers.indexOf(piece) != -1)
						return true;
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
		for (int index = SQ_a8; index <= SQ_h1; index++) {
			if (!isLegalIndex(index)) continue;

			int piece = board[index] * side;
			if (piece > 0 && piece != PAWN && piece != KING)
				return false;
		}
		return true;
	}
	
	/**
	 * Returns whether there is insufficient mating material left.
	 */
	public boolean insufficientMaterial() {
		// Too many pieces left
		if (pieceCount > 5)
			return false;
		// K vs K
		if (pieceCount == 2)
			return true;
		
		// Count pieces
		int[] pieces_w = new int[7], pieces_b = new int[7];
		int count_w = 0, count_b = 0;
		for (int index = SQ_a8; index <= SQ_h1; index++) {
			if (!isLegalIndex(index)) continue;

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
}