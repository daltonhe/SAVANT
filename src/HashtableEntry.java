/**
 * 
 * @author Dalton He
 * created 10-30-18
 * 
 */
public class HashtableEntry {
    public long  key;   // zobrist key
    public short move;  // best move
    public byte  depth; // search depth
    public short eval;  // position score (from white's perspective)
    public byte  type;  // bound type of the eval
    public byte  age;   // incremented after every search

    // memory usage per entry: 16 bytes
    //     key    64 bits
    //     move   16 bits
    //     depth   8 bits
    //     eval   16 bits
    //     type    8 bits
    //     count   8 bits
    //     age     8 bits
    
    /**
     * PV table entry
     */
    public HashtableEntry(long key, int move) {
        this.key  = key;
        this.move = (short) move;
    }

    /**
     * Full transposition table entry
     */
    public HashtableEntry(long key, int move, int depth, int eval, int type) {
        this.key   = key;
        this.move  = (short) move;
        this.depth = (byte)  depth;
        this.eval  = (short) eval;
        this.type  = (byte)  type;
    }
}