/**
 * 
 * @author Dalton He
 * created 10-30-18
 * 
 */
public class HashtableEntry {
    public long key;    // zobrist key of the position
    public String move; // best move
    public int depth;   // depth to which the position was evaluated
    public int eval;    // score of the position
    public int type;    // bound type of the eval
    public int count;   // number of times this position has been repeated
    public int age;     // number of searches ago this entry was from

    // memory usage: 68 bytes
    //
    // key    64 bit
    // move   40 byte
    // depth  32 bit
    // eval   32 bit
    // type   32 bit
    // count  32 bit
    // age    32 bit

    /**
     * Repetition table entry
     */
    public HashtableEntry(long key) {
        this(key, null, 0, 0, 0);
    }

    /**
     * PV table entry
     */
    public HashtableEntry(long key, String move, int depth) {
        this(key, move, 0, 0, 0);
    }

    /**
     * Full transposition table entry
     */
    public HashtableEntry(long key, String move, int depth, int eval, int type) {
        this.key   = key;
        this.move  = move;
        this.depth = depth;
        this.eval  = eval;
        this.type  = type;
        this.count = 1;
        this.age   = 0;
    }
}