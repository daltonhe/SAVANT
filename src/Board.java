import java.util.ArrayList;
import java.util.Scanner;

/**
 * 
 * @author Dalton He
 * created 10-07-18
 */
public class Board implements Definitions {
	public int[] board;
	public int sideToMove;
	public String castling; // stored using FEN format
	public int enpassant;
	public int fiftyMoves;
	public int moveNumber;
	public int wKing;
	public int bKing;
	public int pieceCount;
	public int nullMovesMade;
	public long zobrist;
	
	/**
	 * Initializes the board to the starting position.
	 */
	public Board() {
		this("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
	}
	
	/**
	 * Initializes the board to the given FEN string.
	 * @param FEN - FEN String
	 */
	public Board(String FEN) {
		setDefaults();
		Scanner input = new Scanner(FEN);
		String temp = input.next();
		int index = 0;
		for (int i = 0; i < temp.length(); i++) {
			char ch = temp.charAt(i);
			if (Character.isDigit(ch))
				index += Character.getNumericValue(ch);
			else if (ch == '/')
				index += 8;
			else {
				if (ch == 'K')
					wKing = index;
				else if (ch == 'k')
					bKing = index;

				board[index] = PIECE_STR.indexOf(ch) - 6;
				pieceCount++;
				
				index++;
			}
		}
		
		if (input.hasNext()) {
			if (input.next().toLowerCase().equals("b"))
				sideToMove = BLACK;
		}
		
		if (input.hasNext()) {
			castling = input.next();
			if (castling.equals("-"))
				castling = "";
		}
		
		if (input.hasNext())
			enpassant = algebraicToIndex(input.next());

		if (input.hasNextInt())
			fiftyMoves = input.nextInt();

		if (input.hasNextInt())
			moveNumber = input.nextInt();

		input.close();
		Zobrist.initialize();
		zobrist = Zobrist.getKey(this);
	}
	
	/**
	 * Sets all fields to their default values.
	 */
	public void setDefaults() {
		board = new int[128];
		sideToMove    = WHITE;
		castling      = "";
		enpassant     = -2;
		moveNumber    = 1;
		fiftyMoves    = 0;
		nullMovesMade = 0;
	}
	
	/**
	 * Makes a passing move.
	 */
	public void makeNullMove(boolean unmaking) {
		sideToMove *= -1;
		zobrist ^= Zobrist.SIDE;
		if (unmaking)
			nullMovesMade--;
		else
			nullMovesMade++;
	}

	
	/**
	 * Makes the given move on the board.
	 * @param move
	 */
	public void makeMove(Move move) {
		if (move.type == CASTLE_SHORT)
			castleShort();
		else if (move.type == CASTLE_LONG)
			castleLong();
		else {
			board[move.target] = move.piece;
			board[move.start] = 0;
			if (move.type == ENPASSANT)
				board[enpassant + 16 * sideToMove] = 0;

			if (move.piece == W_KING)
				wKing = move.target;
			else if (move.piece == B_KING)
				bKing = move.target;

			if (!castling.isEmpty())
				updateCastling();
		}
		
		enpassant = (move.type == PAWN_TWO ? move.target + 16 * sideToMove : -2);
		if (sideToMove == BLACK)
			moveNumber++;

		if (move.captured == 0 && Math.abs(move.piece) != PAWN)
			fiftyMoves++;
		else
			fiftyMoves = 0;

		if (move.captured != 0)
			pieceCount--;
		
		sideToMove *= -1;
		zobrist = Zobrist.getKey(this);
	}
	
	/**
	 * Unmakes the given move on the board.
	 * @param move
	 */
	public void unmakeMove(Move move) {	
		sideToMove *= -1;
		castling = move.prevCastling;
		enpassant = move.prevEnpassant;
		fiftyMoves = move.prevFiftyMoves;
		if (sideToMove == BLACK)
			moveNumber--;

		if (move.type == CASTLE_SHORT)
			uncastleShort();
		else if (move.type == CASTLE_LONG)
			uncastleLong();
		else {
			board[move.start] = (move.type == PROMOTION ? PAWN * sideToMove : move.piece);
			if (move.type == ENPASSANT) {
				board[move.target] = 0;
				board[move.target + 16 * sideToMove] = PAWN * -sideToMove;
			}
			else
				board[move.target] = move.captured;

			if (move.piece == W_KING)
				wKing = move.start;
			else if (move.piece == B_KING)
				bKing = move.start;
		}
		
		if (move.captured != 0)
			pieceCount++;
		
		zobrist = Zobrist.getKey(this);
	}
	
	public void castleShort() {
		if (sideToMove == WHITE) {
			board[e1] = 0;
			board[f1] = W_ROOK;
			board[g1] = W_KING;
			board[h1] = 0;
			wKing     = g1;
			castling = castling.replace("K", "");
			castling = castling.replace("Q", "");
		} else {
			board[e8] = 0;
			board[f8] = B_ROOK;
			board[g8] = B_KING;
			board[h8] = 0;
			bKing     = g8;
			castling = castling.replace("k", "");
			castling = castling.replace("q", "");
		}
	}
	
	public void uncastleShort() {
		if (sideToMove == WHITE) {
			board[e1] = W_KING;
			board[f1] = 0;
			board[g1] = 0;
			board[h1] = W_ROOK;
			wKing     = e1;
		} else {
			board[e8] = B_KING;
			board[f8] = 0;
			board[g8] = 0;
			board[h8] = B_ROOK;
			bKing     = e8;
		}
	}
	
	public void castleLong() {
		if (sideToMove == WHITE) {
			board[a1] = 0;
			board[c1] = W_KING;
			board[d1] = W_ROOK;
			board[e1] = 0;
			castling = castling.replace("K", "");
			castling = castling.replace("Q", "");
		} else {
			board[a8] = 0;
			board[c8] = B_KING;
			board[d8] = B_ROOK;
			board[e8] = 0;
			castling = castling.replace("k", "");
			castling = castling.replace("q", "");
		}
	}
	
	public void uncastleLong() {
		if (sideToMove == WHITE) {
			board[a1] = W_ROOK;
			board[c1] = 0;
			board[d1] = 0;
			board[e1] = W_KING;
			wKing     = e1;
		} else {
			board[a8] = B_ROOK;
			board[c8] = 0;
			board[d8] = 0;
			board[e8] = B_KING;
			bKing     = e8;
		}
	}
	
	/**
	 * Checks the original squares of the Kings and Rooks, and updates the castling rights
	 * accordingly.
	 */
	public void updateCastling() {
		if (board[e1] != W_KING) {
			castling = castling.replace("K", "");
			castling = castling.replace("Q", "");
		} 
		else {
			if (board[h1] != W_ROOK)
				castling = castling.replace("K", "");
			
			if (board[a1] != W_ROOK)
				castling = castling.replace("Q", "");
		}

		if (board[e8] != B_KING) {
			castling = castling.replace("k", "");
			castling = castling.replace("q", "");
		} 
		else {
			if (board[h8] != B_ROOK)
				castling = castling.replace("k", "");
				
			if (board[a8] != B_ROOK)
				castling = castling.replace("q", "");
		}
	}
	
	/**
	 * Returns a list of all pseudolegal moves that can be made from this position, i.e.,
	 * moves may leave the king in check. Such moves are only filtered out during search, in order
	 * to save computation.
	 * @param qs - True if generating moves for quiescence search
	 * @return Move list
	 */
	public ArrayList<Move> generateMoves(boolean skipQuiets) {
		ArrayList<Move> moveList = new ArrayList<Move>();
		for (int index = a8; index <= h1; index++) {
			if ((index & 0x88) != 0)
				continue;

			int piece = board[index] * sideToMove;
			if (piece == 0)
				continue;

			switch (piece) {

			case PAWN:
				genPawnMoves(index, skipQuiets, moveList);
				break;
				
			case KNIGHT:
				genDelta(index, DELTA_KNIGHT, false, skipQuiets, moveList);
				break;
				
			case BISHOP:
				genDelta(index, DELTA_BISHOP, true, skipQuiets, moveList);
				break;
				
			case ROOK:
				genDelta(index, DELTA_ROOK, true, skipQuiets, moveList);
				break;
				
			case QUEEN:
				genDelta(index, DELTA_QUEEN, true, skipQuiets, moveList);
				break;
				
			case KING:
				genDelta(index, DELTA_QUEEN, false, skipQuiets, moveList);
				if (!skipQuiets && !castling.isEmpty())
					genCastling(index, moveList);
				break;
			}
		}
		return moveList;
	}
	
	/**
	 * Filters out all moves that leave the king in check from the given move list. Returns
	 * the new move list.
	 * @param moveList
	 * @return
	 */
	public ArrayList<Move> filterLegal(ArrayList<Move> moveList) {
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		for (Move move : moveList) {
			makeMove(move);
			int kingPos = (sideToMove == WHITE ? bKing : wKing);
			if (!isAttacked(kingPos, sideToMove))
				legalMoves.add(move);
			
			unmakeMove(move);
		}
		return legalMoves;
	}
	
	/**
	 * Adds all pseudolegal pawn moves to the move list.
	 * @param start - Index of pawn
	 * @param moveList - The list of moves
	 */
	public void genPawnMoves(int start, boolean qs, ArrayList<Move> moveList) {
		for (int i = 0; i < DELTA_PAWN.length; i++) {
			int target = start + DELTA_PAWN[i] * sideToMove;
			// Note: i = 1 means forward delta
			if ((target & 0x88) != 0)
				continue;

			if ((i == 1 && board[target] == 0) || (i != 1 && board[target] * sideToMove < 0)) {
				if (target <= h8 || target >= a1) // promotion
					moveList.add(new Move(start, target, QUEEN * sideToMove, board[target],
							castling, enpassant, fiftyMoves, PROMOTION));
				else { // push or capture
					if (!qs || board[target] != 0 || target <= h7 || target >= a2)
						moveList.add(new Move(start, target, PAWN * sideToMove, board[target],
								castling, enpassant, fiftyMoves, NORMAL));
				}
			} 
			// enpassant
			if (i != 1 && target == enpassant)
				moveList.add(new Move(start, enpassant, PAWN * sideToMove, PAWN * -sideToMove,
						castling, enpassant, fiftyMoves, ENPASSANT));
			
			// push two squares
			if (   !qs && i == 1 && ((sideToMove == WHITE && start / 16 == 6)
			    || sideToMove == BLACK && start / 16 == 1)) {
				if (board[target] == 0 && board[target - 16 * sideToMove] == 0)
					moveList.add(new Move(start, target - 16 * sideToMove, PAWN * sideToMove, 0,
							castling, enpassant, fiftyMoves, PAWN_TWO));
			}
		}
	}
	
	/**
	 * Adds all legal castling moves to the move list.
	 * @param start - Index of king
	 * @param moveList - The list of moves
	 */
	public void genCastling(int start, ArrayList<Move> moveList) {
		if (sideToMove == WHITE) {
			if (   castling.contains("K") && board[f1] == 0 && board[g1] == 0
			    && !isAttacked(e1, BLACK) && !isAttacked(f1, BLACK))
				moveList.add(new Move(e1, g1, W_KING, 0, castling, enpassant, fiftyMoves, CASTLE_SHORT));
			
			if (   castling.contains("Q") && board[d1] == 0 && board[c1] == 0 && board[b1] == 0
				&& !isAttacked(e1, BLACK) && !isAttacked(d1, BLACK))
				moveList.add(new Move(e1, c1, W_KING, 0, castling, enpassant, fiftyMoves, CASTLE_LONG));
		}
		else {
			if (   castling.contains("k") && board[f8] == 0 && board[g8] == 0
				&& !isAttacked(e8, WHITE) && !isAttacked(f8, WHITE))
				moveList.add(new Move(e8, g8, B_KING, 0, castling, enpassant, fiftyMoves, CASTLE_SHORT));
			
			if (   castling.contains("q") && board[d8] == 0 && board[c8] == 0 && board[b8] == 0 
				&& !isAttacked(e8, WHITE) && !isAttacked(d8, WHITE))
				moveList.add(new Move(e8, c8, B_KING, 0, castling, enpassant, fiftyMoves, CASTLE_LONG));
		}
	}
	
	/**
	 * Generates moves using piece deltas and adds them to the move list.
	 * @param start - Index of piece
	 * @param delta - Piece delta for calculating target indices
	 * @param sliding - True if the piece is a Bishop, Rook, or Queen
	 * @param qs - True if only captures should be generated
	 * @param moveList - The list of moves
	 */
	public void genDelta(int start, int[] delta, boolean sliding, boolean qs, ArrayList<Move> moveList) {
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while ((target & 0x88) == 0 && board[target] * sideToMove <= 0) {
				int captured = board[target];
				if (!qs || captured != 0)
					moveList.add(new Move(start, target, board[start], captured, castling, enpassant, fiftyMoves, NORMAL));

				if (!sliding || captured != 0)
					break;

				target += delta[i];
			}
		}
	}
	
	/**
	 * Returns whether the given index is attacked by the given player.
	 * @param index
	 * @param attackingSide
	 * @return
	 */
	public boolean isAttacked(int index, int attackingSide) {
		int[] pawnDelta = (attackingSide == WHITE ? DELTA_W_PAWN : DELTA_B_PAWN);
		String[] attackLookup = (attackingSide == WHITE ? ATTACK_LOOKUP_W : ATTACK_LOOKUP_B);
		
		if (attackDelta(index, pawnDelta, attackLookup[0], false))
			return true;

		if (attackDelta(index, DELTA_KNIGHT, attackLookup[1], false))
			return true;

		if (attackDelta(index, DELTA_BISHOP, attackLookup[2], true))
			return true;

		if (attackDelta(index, DELTA_ROOK, attackLookup[3], true))
			return true;

		if (attackDelta(index, DELTA_QUEEN, attackLookup[4], false))
			return true;

		return false;
	}
	
	/**
	 * Scans the piece delta for an attacker and returns whether one was found.
	 * @param start
	 * @param delta
	 * @param sliding
	 * @return
	 */
	public boolean attackDelta(int start, int[] delta, String attackers, boolean sliding) {	
		for (int i = 0; i < delta.length; i++) {
			int target = start + delta[i];
			while ((target & 0x88) == 0) {
				int captured = board[target];
				if (captured != 0) {
					String piece = Character.toString(PIECE_STR.charAt(captured + 6));
					if (attackers.contains(piece))
						return true;
				}
				if (!sliding || captured != 0)
					break;

				target += delta[i];
			}
		}
		return false;
	}
	
	/**
	 * Returns whether the given side has any pieces left (NBRQ).
	 * @param side
	 * @return
	 */
	public boolean isPawnEnding(int side) {
		for (int index = a8; index <= h1; index++) {
			if ((index & 0x88) != 0)
				continue;

			int piece = board[index] * side;
			if (piece > 0 && piece != PAWN && piece != KING)
				return false;
		}
		return true;
	}
	
	/**
	 * Returns whether there is insufficient mating material left
	 * @return
	 */
	public boolean insufficientMaterial() {
		// Too many pieces left
		if (pieceCount > 5)
			return false;
		// K vs K
		if (pieceCount == 2)
			return true;
		
		// Count pieces
		int[] whitePieces = new int[7], blackPieces = new int[7];
		int whiteCount = 0, blackCount = 0;
		for (int index = a8; index <= h1; index++) {
			if ((index & 0x88) != 0)
				continue;

			int piece = board[index];
			
			// Non-minor piece left
			if (Math.abs(piece) == PAWN || Math.abs(piece) == ROOK || Math.abs(piece) == QUEEN)
				return false;
			
			if (piece > 0) {
				whitePieces[piece]++;
				whiteCount++;
			}
			else {
				blackPieces[-piece]++;
				blackCount++;
			}
		}
		
		// Km vs K
		if (pieceCount == 3)
			return true;

		if (pieceCount == 4) {
			// Km vs Km
			if (whiteCount == 2)
				return true;
			// KNN vs K
			if (whitePieces[KNIGHT] == 2 || blackPieces[KNIGHT] == 2)
				return true;
			// KBB vs K || KBN vs K
			return false;
		}
		
		assert(pieceCount == 5);
		
		// 3 minors vs 1
		if (whiteCount == 4 || blackCount == 4)
			return false;
		// KBB vs KN
		if (   whitePieces[BISHOP] == 2 && blackPieces[BISHOP] == 0
			|| blackPieces[BISHOP] == 2 && whitePieces[BISHOP] == 0)
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
		while (index <= h1) {
			if (board[index] == 0)
				emptySquares++;
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
				if (index != h1)
					result += "/";
					
				index += 8;
			}
			index++;
		}
		result += (sideToMove == WHITE ? " w" : " b");
		result += (castling.isEmpty() ? " -" : " " + castling);
		result += " " + indexToAlgebraic(enpassant);
		result += " " + fiftyMoves;
		result += " " + moveNumber;
		return result;
	}
	
	/**
	 * Returns the algebraic coordinate of the given board index.
	 * @param index - Board index of the square
	 * @return Algebraic coordinate of the square
	 */
	public static String indexToAlgebraic(int index) {
		if (index == -2)
			return "-";

		return "" + "abcdefgh".charAt(index % 16) + (8 - index / 16);
	}
	
	/**
	 * Returns the board index of the given algebraic coordinate.
	 * @param coord - Algebraic coordinate of the square
	 * @return Board index of the square
	 */
	public static int algebraicToIndex(String coord) {
		if (coord.equals("-"))
			return -2;

		coord = coord.toLowerCase();
		return 16 * (8 - Character.getNumericValue(coord.charAt(1))) + 
				"abcdefgh".indexOf(coord.charAt(0));
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