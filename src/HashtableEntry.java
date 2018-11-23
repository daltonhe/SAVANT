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
	public byte type;    // bound type of the eval
	public byte count;   // number of times this position has been repeated
	public byte age;     // number of searches ago this entry was from
	
	// memory usage: 56 bytes
	//
	// zobrist  64 bit
	// move     40 byte
	// depth    8  bit
	// eval     32 bit
	// type     8  bit
	// count    8  bit
	// age      8  bit
	
	/**
	 * Repetition table entry
	 */
	public HashtableEntry(long zobrist) {
		this(zobrist, null, (byte) 0, 0, (byte) 0);
	}
	
	/**
	 * PV table entry
	 */
	public HashtableEntry(long zobrist, String move) {
		this(zobrist, move, (byte) 0, 0, (byte) 0);
	}

	/**
	 * Full transposition table entry
	 */
	public HashtableEntry(long zobrist, String move, byte depth, int eval, byte type) {
		this.zobrist = zobrist;
		this.move    = move;
		this.depth   = depth;
		this.eval    = eval;
		this.type    = type;
		this.count   = 1;
		this.age     = 0;
	}
}