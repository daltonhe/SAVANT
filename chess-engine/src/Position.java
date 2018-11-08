/**
 * Represents a chess position.
 * @author Dalton He
 * created 6-10-2018
 */
import java.util.ArrayList;
import java.util.Scanner;

public class Position implements Definitions {

//0x88 board representation; empty squares are assigned a value of 0,
//white pieces positive integers, and black pieces negative integers
	public int[] board = new int[128];
//player whose turn it is to move
	public int sideToMove = WHITE;
//castling rights for both sides
	public byte castlingRights = 0b0000;
//index of the enpassant square (-1 if none)
	public int enpassantSquare = NONE;
//half move clock for the fifty moves rule
	public int fiftyMoveCount = 0;
//total number of half moves made
	public int halfMoveCount = 0;
//indices of both kings
	public int[] kingIndex = {E1, NONE, E8};

	/**
	 * Creates the initial position.
	 */
	public Position() {
		this(INITIAL_POSITION_FEN);
	}

	/**
	 * Creates the position specified by FENString.
	 * @param FENString FEN string of the position to initialize
	 */
	public Position(String FENString) {
		Scanner fen = new Scanner(FENString.trim());
		String s = fen.next();
		int index = 112;
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (ch == '/') {
				index -= 24; //go to next rank
			} else if (Character.isDigit(ch)) {
				index += Character.getNumericValue(ch);
			} else if (PIECES.contains(Character.toString(ch))) {
				int piece = toNumber(ch);
				if (piece == KING) {
					kingIndex[0] = index;
				} else if (piece == BLACK_KING) {
					kingIndex[2] = index;
				}
				board[index] = piece;
				index++;
			}
		}
		if (fen.next().equals("b")) {
			sideToMove = BLACK;
		}
		s = fen.next();
		if (s.contains("K")) {
			castlingRights |= 0b1000;
		}
		if (s.contains("Q")) {
			castlingRights |= 0b0100;
		}
		if (s.contains("k")) {
			castlingRights |= 0b0010;
		}
		if (s.contains("q")) {
			castlingRights |= 0b0001;
		}
    s = fen.next();
    if (!s.equals("-")) {
    	enpassantSquare = toIndex(s);
    }
		if (fen.hasNext()) {
			fiftyMoveCount = fen.nextInt();
		}
		if (fen.hasNext()) {
			halfMoveCount = (fen.nextInt() - 1) * 2;
			if (sideToMove == BLACK) {
				halfMoveCount++;
			}
		}
		fen.close();
	}

	/**
	 * Makes the specified move on the board.
	 * @param move the move to be made
	 */
	public void makeMove(Move move) {
		board[move.startIndex] = EMPTY;
		board[move.endIndex] = move.movingPiece;
		enpassantSquare = NONE;
		switch (move.moveType) {
		case PAWN_TWO_SQUARES:
			enpassantSquare = move.endIndex - sideToMove * 16;
			break;
		case ENPASSANT:
			board[move.endIndex - sideToMove * 16] = EMPTY;
			break;
		case CASTLE_SHORT:
			board[sideToMove == WHITE ? H1 : H8] = EMPTY;
			board[sideToMove == WHITE ? F1 : F8] = ROOK * sideToMove;
			break;
		case CASTLE_LONG:
			board[sideToMove == WHITE ? A1 : A8] = EMPTY;
			board[sideToMove == WHITE ? D1 : D8] = ROOK * sideToMove;
			break;
		case PROMOTION_QUEEN:
			board[move.endIndex] = QUEEN * sideToMove;
			break;
		case PROMOTION_ROOK:
			board[move.endIndex] = ROOK * sideToMove;
			break;
		case PROMOTION_BISHOP:
			board[move.endIndex] = BISHOP * sideToMove;
			break;
		case PROMOTION_KNIGHT:
			board[move.endIndex] = KNIGHT * sideToMove;
			break;
		}
		updateCastlingRights();
		if (move.movingPiece * sideToMove == KING) {
			kingIndex[1 - sideToMove] = move.endIndex;
		}
		if (move.movingPiece * sideToMove == PAWN || move.capturedPiece != EMPTY) {
			fiftyMoveCount = 0;
		} else {
			fiftyMoveCount++;
		}
		halfMoveCount++;
		sideToMove *= -1;
	}

	/**
	 * Unmakes the specified move on the board.
	 * @param move the move to undo
	 */
	public void unmakeMove(Move move) {
		sideToMove *= -1;
		halfMoveCount--;
		fiftyMoveCount = move.prevFiftyMoveCount;
		enpassantSquare = move.prevEnpassantSquare;
		castlingRights = move.prevCastlingRights;
		if (move.movingPiece * sideToMove == KING) {
			kingIndex[1 - sideToMove] = move.startIndex;
		}
		board[move.startIndex] = move.movingPiece;
		if (move.moveType == ENPASSANT) {
			board[move.endIndex] = EMPTY;
			board[move.endIndex - sideToMove * 16] = move.capturedPiece;
		} else {
			board[move.endIndex] = move.capturedPiece;
		}
		if (move.moveType == CASTLE_SHORT) {
			board[sideToMove == WHITE ? H1 : H8] = ROOK * sideToMove;
			board[sideToMove == WHITE ? F1 : F8] = EMPTY;
		} else if (move.moveType == CASTLE_LONG) {
			board[sideToMove == WHITE ? A1 : A8] = ROOK * sideToMove;
			board[sideToMove == WHITE ? D1 : D8] = EMPTY;
		}
	}

	/**
	 * Updates the castling rights for this position.
	 */
	public void updateCastlingRights() {
		if (castlingRights == 0) {
			return;
		}
		if (board[E1] != KING) {
			castlingRights &= 0b0011;
		} else {
			if (board[H1] != ROOK) {
				castlingRights &= 0b0111;
			}
			if (board[A1] != ROOK) {
				castlingRights &= 0b1011;
			}
		}
		if (board[E8] != BLACK_KING) {
			castlingRights &= 0b1100;
		} else {
			if (board[H8] != BLACK_ROOK) {
				castlingRights &= 0b1101;
			}
			if (board[A8] != BLACK_ROOK) {
				castlingRights &= 0b1110;
			}
		}
	}

	/**
	 * Returns a list of all legal moves that can be made from this position.
	 * @return move list
	 */
	public ArrayList<Move> generateMoves() {
		ArrayList<Move> moves = new ArrayList<Move>();
		for (int i = 0; i < 64; i++) {
			int index = INDEX_0x88[i];
			if (board[index] * sideToMove > 0) {
				switch (board[index] * sideToMove) {
				case PAWN:
					generatePawnMoves(moves, index);
					break;
				case KNIGHT:
					generatePieceMoves(moves, index, KNIGHT_DELTA, false);
					break;
				case BISHOP:
					generatePieceMoves(moves, index, BISHOP_DELTA, true);
					break;
				case ROOK:
					generatePieceMoves(moves, index, ROOK_DELTA, true);
					break;
				case QUEEN:
					generatePieceMoves(moves, index, QUEEN_DELTA, true);
					break;
				case KING:
					generatePieceMoves(moves, index, QUEEN_DELTA, false);
					generateCastlingMoves(moves, index);
					break;
				}
			}
		}
	//filter out moves that leave the king in check
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		for (Move move : moves) {
			makeMove(move);
			if (!isInCheck(-sideToMove)) {
				legalMoves.add(move);
			}
			unmakeMove(move);
		}
		return legalMoves;
	}

	/**
	 * Returns a list of all legal captures that can be made from this position.
	 * @return move list
	 */
	public ArrayList<Move> generateCaptures() {
		ArrayList<Move> moves = new ArrayList<Move>();
		for (int i = 0; i < 64; i++) {
			int index = INDEX_0x88[i];
			if (board[index] * sideToMove > 0) {
				switch (board[index] * sideToMove) {
				case PAWN:
					generatePawnCaptures(moves, index);
					break;
				case KNIGHT:
					generatePieceCaptures(moves, index, KNIGHT_DELTA, false);
					break;
				case BISHOP:
					generatePieceCaptures(moves, index, BISHOP_DELTA, true);
					break;
				case ROOK:
					generatePieceCaptures(moves, index, ROOK_DELTA, true);
					break;
				case QUEEN:
					generatePieceCaptures(moves, index, QUEEN_DELTA, true);
					break;
				case KING:
					generatePieceCaptures(moves, index, QUEEN_DELTA, false);
					break;
				}
			}
		}
	//filter out moves that leave the king in check
		ArrayList<Move> legalMoves = new ArrayList<Move>();
		for (Move move : moves) {
			makeMove(move);
			if (!isInCheck(-sideToMove)) {
				legalMoves.add(move);
			}
			unmakeMove(move);
		}
		return legalMoves;
	}

	/**
	 * Adds all pseudolegal moves that can be made by the pawn on index to moves.
	 * @param moves move list
	 * @param index index of pawn
	 */
	public void generatePawnMoves(ArrayList<Move> moves, int index) {
		int targetIndex = index + sideToMove * 16;
		if (board[targetIndex] == EMPTY) {
			if (getRank(index) == (sideToMove == WHITE ? 7 : 2)) {
				addMove(moves, PROMOTION_QUEEN, index, targetIndex);
				addMove(moves, PROMOTION_ROOK, index, targetIndex);
				addMove(moves, PROMOTION_BISHOP, index, targetIndex);
				addMove(moves, PROMOTION_KNIGHT, index, targetIndex);
			} else {
				addMove(moves, NORMAL_MOVE, index, targetIndex);
				if (getRank(index) == (sideToMove == WHITE ? 2 : 7)) {
					targetIndex = index + sideToMove * 32;
					if (board[targetIndex] == EMPTY) {
						addMove(moves, PAWN_TWO_SQUARES, index, targetIndex);
					}
				}
			}
		}
		for (int i = 0; i < PAWN_DELTA.length; i++) {
			targetIndex = index + PAWN_DELTA[i] * sideToMove;
			if (isOnBoard(targetIndex)) {
				if (board[targetIndex] * sideToMove < 0) {
					if (getRank(index) == (sideToMove == WHITE ? 7 : 2)) {
						addMove(moves, PROMOTION_QUEEN, index, targetIndex);
						addMove(moves, PROMOTION_ROOK, index, targetIndex);
						addMove(moves, PROMOTION_BISHOP, index, targetIndex);
						addMove(moves, PROMOTION_KNIGHT, index, targetIndex);
					} else {
						addMove(moves, NORMAL_MOVE, index, targetIndex);
					}
				} else if (targetIndex == enpassantSquare) {
					moves.add(new Move(ENPASSANT, PAWN * sideToMove, PAWN *
							-sideToMove, index, targetIndex, castlingRights,
							enpassantSquare, fiftyMoveCount));
				}
			}
		}
	}

	/**
	 * Adds all pseudolegal captures that can be made by the pawn on index to
	 * moves.
	 * @param moves move list
	 * @param index index of pawn
	 */
	public void generatePawnCaptures(ArrayList<Move> moves, int index) {
		for (int i = 0; i < PAWN_DELTA.length; i++) {
			int targetIndex = index + PAWN_DELTA[i] * sideToMove;
			if (isOnBoard(targetIndex)) {
				if (board[targetIndex] * sideToMove < 0) {
					if (getRank(index) == (sideToMove == WHITE ? 7 : 2)) {
						addMove(moves, PROMOTION_QUEEN, index, targetIndex);
						addMove(moves, PROMOTION_ROOK, index, targetIndex);
						addMove(moves, PROMOTION_BISHOP, index, targetIndex);
						addMove(moves, PROMOTION_KNIGHT, index, targetIndex);
					} else {
						addMove(moves, NORMAL_MOVE, index, targetIndex);
					}
				}
			}
		}
	}

	/**
	 * Adds all pseudolegal moves that can be made by the piece on index to
	 * moves.
	 * @param moves move list
	 * @param index index of piece
	 * @param delta delta array of piece
	 * @param isSlidingPiece true if piece is a bishop, rook, or queen; false if
	 * it is a knight or king
	 */
	public void generatePieceMoves(ArrayList<Move> moves, int index,
			int[] delta, boolean isSlidingPiece) {
		for (int i = 0; i < delta.length; i++) {
			int targetIndex = index + delta[i];
			while (isOnBoard(targetIndex) &&
					board[targetIndex] * sideToMove <= 0) {
				addMove(moves, NORMAL_MOVE, index, targetIndex);
				if (!isSlidingPiece || board[targetIndex] * sideToMove != EMPTY) {
					break;
				}
				targetIndex += delta[i];
			}
		}
	}

	/**
	 * Adds all pseudolegal captures that can be made by the piece on index to
	 * moves.
	 * @param moves move list
	 * @param index index of piece
	 * @param delta delta array of piece
	 * @param isSlidingPiece true if piece is a bishop, rook, or queen; false if
	 * it is a knight or king
	 */
	public void generatePieceCaptures(ArrayList<Move> moves, int index,
			int[] delta, boolean isSlidingPiece) {
		for (int i = 0; i < delta.length; i++) {
			int targetIndex = index + delta[i];
			while (isOnBoard(targetIndex) &&
					board[targetIndex] * sideToMove <= 0) {
				if (board[targetIndex] * sideToMove != EMPTY) {
					addMove(moves, NORMAL_MOVE, index, targetIndex);
					break;
				}
				if (!isSlidingPiece) {
					break;
				}
				targetIndex += delta[i];
			}
		}
	}

	/**
	 * Adds all legal castling moves that can be made by the king on index to
	 * moves.
	 * @param moves move list
	 * @param index index of king
	 */
	public void generateCastlingMoves(ArrayList<Move> moves, int index) {
		if (castlingRights == 0) {
			return;
		}
		if ((sideToMove == WHITE && (castlingRights & 0b1000) != 0) ||
				(sideToMove == BLACK && (castlingRights & 0b0010) != 0)) {
			if (board[index + 1] == EMPTY && board[index + 2] == EMPTY) {
				if (!isInCheck(sideToMove) && !isAttacked(index + 1, -sideToMove)) {
					addMove(moves, CASTLE_SHORT, index, index + 2);
				}
			}
		}
		if ((sideToMove == WHITE && (castlingRights & 0b0100) != 0) ||
				(sideToMove == BLACK && (castlingRights & 0b0001) != 0)) {
			if (board[index - 1] == EMPTY && board[index - 2] == EMPTY &&
					board[index - 3] == EMPTY) {
				if (!isInCheck(sideToMove) && !isAttacked(index - 1, -sideToMove)) {
					addMove(moves, CASTLE_LONG, index, index - 2);
				}
			}
		}
	}

	/**
	 * Adds the specified move to moves.
	 * @param moves move list
	 * @param moveType type of move
	 * @param startIndex index of starting square
	 * @param endIndex index of ending square
	 */
	public void addMove(ArrayList<Move> moves, int moveType, int startIndex,
			int endIndex) {
		moves.add(new Move(moveType, board[startIndex], board[endIndex],
				startIndex, endIndex, castlingRights, enpassantSquare,
				fiftyMoveCount));
	}

	/**
	 * Returns whether the 0x88 index is on the board.
	 * @param index index of square
	 * @return true if index is on the board; false otherwise
	 */
	public static boolean isOnBoard (int index) {
		return (index & 0x88) == 0;
	}

	/**
	 * Returns whether the specified side is in check.
	 * @param side side to check
	 * @return true if side is in check; false if it is not
	 */
	public boolean isInCheck(int side) {
		return isAttacked(kingIndex[1 - side], -side);
	}

	/**
	 * Returns whether the square represented by index is attacked by the
	 * specified side
	 * @param index index of square
	 * @param side attacking side
	 * @return true if square is attacked; false if it is not
	 */
	public boolean isAttacked(int index, int side) {
		for (int i = 0; i < PAWN_DELTA.length; i++) {
			int targetIndex = index - PAWN_DELTA[i] * side;
			if (isOnBoard(targetIndex) &&
					board[targetIndex] == PAWN * side) {
				return true;
			}
		}
		for (int i = 0; i < KNIGHT_DELTA.length; i++) {
			int targetIndex = index + KNIGHT_DELTA[i];
			if (isOnBoard(targetIndex) &&
					board[targetIndex] == KNIGHT * side) {
				return true;
			}
			targetIndex = index + QUEEN_DELTA[i];
			if (isOnBoard(targetIndex) &&
					board[targetIndex] == KING * side) {
				return true;
			}
		}
		for (int i = 0; i < ROOK_DELTA.length; i++) {
			int targetIndex = index + ROOK_DELTA[i];
			while (isOnBoard(targetIndex)) {
				int piece = board[targetIndex];
				if (piece == EMPTY) {
					targetIndex += ROOK_DELTA[i];
				} else {
					if (piece == ROOK * side || piece == QUEEN * side) {
						return true;
					}
					break;
				}
			}
		}
		for (int i = 0; i < BISHOP_DELTA.length; i++) {
			int targetIndex = index + BISHOP_DELTA[i];
			while (isOnBoard(targetIndex)) {
				int piece = board[targetIndex];
				if (piece == EMPTY) {
					targetIndex += BISHOP_DELTA[i];
				} else {
					if (piece == BISHOP * side || piece == QUEEN * side) {
						return true;
					}
					break;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the algebraic notation of the square represented by index.
	 * @param index index of square
	 * @return algebraic notation of square
	 */
	public static String toSquare (int index) {
		if (index == NONE) {
			return "-";
		}
		return "" + getFile(index) + getRank(index);
	}

	/**
	 * Returns the file (a-h) of the square represented by index.
	 * @param index index of square
	 * @return file of square
	 */
	public static char getFile (int index) {
		return FILES.charAt(index & 7);
	}

	/**
	 * Returns the rank (1-8) of the square represented by index.
	 * @param index index of square
	 * @return rank of square
	 */
	public static int getRank (int index) {
		return (index >> 4) + 1;
	}

	/**
	 * Returns the index associated with square.
	 * @param square algebraic notation of square
	 * @return index of square
	 */
	public static int toIndex(String square) {
		if (square == "") {
			return NONE;
		}
		String file = square.substring(0, 1).toLowerCase();
		String rank = square.substring(1, 2);
		return 16 * (Integer.parseInt(rank) - 1) + FILES.indexOf(file);
	}

	/**
	 * Returns the letter of the piece associated with the specified number.
	 * @param number piece number
	 * @return piece letter
	 */
	public static char toLetter (int number) {
		return PIECES.charAt(number + 6);
	}

	/**
	 * Returns the number of the piece associated with the specified letter.
	 * @param letter piece letter
	 * @return piece number
	 */
	public static int toNumber(char letter) {
		return PIECES.indexOf(letter) - 6;
	}

	/**
	 * Returns an ASCII representation of the board.
	 */
	@Override
	public String toString() {
		String result = "   A B C D E F G H\n";
		for (int rank = 7; rank >= 0; rank--) {
			result += (1 + rank + "| ");
			for (int file = 0; file < 8; file++) {
				int index8x8 = rank * 8 + file;
				result += (toLetter(board[INDEX_0x88[index8x8]]) + " ");
			}
			result += "|" + (1 + rank) + "\n";
		}
		result += "   A B C D E F G H\n\n";
		result += (sideToMove == WHITE ? "w" : "b");
		result += " ";
		if (castlingRights == 0) {
			result += "-";
		} else {
			result += ((castlingRights & 0b1000) != 0 ? "K" : "");
			result += ((castlingRights & 0b0100) != 0 ? "Q" : "");
			result += ((castlingRights & 0b0010) != 0 ? "k" : "");
			result += ((castlingRights & 0b0001) != 0 ? "q" : "");
		}
		result += " ";
		result += toSquare(enpassantSquare) + " ";
		result += fiftyMoveCount + " ";
		result += (1 + halfMoveCount / 2) + "\n";
		return result;
	}

}