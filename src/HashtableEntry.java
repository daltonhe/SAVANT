/**
 * @author Dalton He
 * created 10-30-18
 */
public class HashtableEntry {
	public long zobrist;
	public int depth;
	public int type;
	public int eval;
	public String move;
	public int count = 1;
	
	public HashtableEntry(long zobrist) {
		this.zobrist = zobrist;
	}
	
	public HashtableEntry(long zobrist, String move) {
		this.zobrist = zobrist;
		this.move = move;
	}

	/**
	 * 
	 * @param zobrist
	 * @param move
	 * @param depth
	 * @param eval
	 * @param type
	 */
	public HashtableEntry(long zobrist, String move, int depth, int eval, int type) {
		this.zobrist = zobrist;
		this.move = move;
		this.depth = depth;
		this.eval = eval;
		this.type = type;
	}
}