/**
 * 
 * @author Dalton He
 * created 10-30-18
 * 
 */
public class HashtableEntry {
	public long zobrist; // zobrist key of the position
	public String move;  // best move
	public int depth;    // depth to which the position was evaluated
	public int eval;     // score of the position
	public int type;     // bound type of the eval
	public int count;    // number of times this position has been repeated
	public int age;      // number of searches ago this entry was from
	
	/**
	 * Repetition table entry
	 */
	public HashtableEntry(long zobrist) {
		this(zobrist, null, 0, 0, 0);
	}
	
	/**
	 * PV table entry
	 */
	public HashtableEntry(long zobrist, String move) {
		this(zobrist, move, 0, 0, 0);
	}

	/**
	 * Full transposition table entry
	 */
	public HashtableEntry(long zobrist, String move, int depth, int eval, int type) {
		this.zobrist = zobrist;
		this.move    = move;
		this.depth   = depth;
		this.eval    = eval;
		this.type    = type;
		this.count   = 1;
		this.age     = 0;
	}
}