/**
 * 
 * @author Dalton He
 * created 11-15-18
 * 
 */
public class TranspositionTable implements Types {
    private HashtableEntry[] table;
    private int size;

    /**
     * Creates a hash table with the given size.
     */
    public TranspositionTable(int size) {
        this.size = size;
        clear();
    }

    /**
     * Clears the hash table.
     */
    public void clear() {
        table = new HashtableEntry[size];
    }

    /**
     * Increments the age of all existing entries.
     */
    public void update() {
        for (int i = 0; i < HASH_SIZE_TT; i++) {
            if (table[i] != null) table[i].age++;
        }
    }

    /**
     * Returns the hash table entry for the given zobrist key, or null if not found.
     */
    public HashtableEntry get(long key) {
        int hashKey = (int) (key % size);
        HashtableEntry entry = table[hashKey];
        if (entry != null && key == entry.key) return entry;
        return null;
    }

    /**
     * Deletes a repetition
     */
    public void delete(long key) {
        int hashKey = (int) (key % size);
        HashtableEntry entry = table[hashKey];
        if (entry == null || key != entry.key) return;
        if (entry.count > 1) table[hashKey].count--;
        else table[hashKey] = null;
    }

    /**
     * Adds a repetition table entry
     */
    public void add(long key) {
        int hashKey = (int) (key % size);
        HashtableEntry entry = table[hashKey];
        if (entry == null || key != entry.key)
            table[hashKey] = new HashtableEntry(key);
        else table[hashKey].count++;
    }

    /**
     * Adds a PV table entry
     */
    public void add(long key, String move, int depth) {
        int hashKey = (int) (key % size);
        HashtableEntry entry = table[hashKey];
        if (entry == null || depth > entry.depth)
            table[hashKey] = new HashtableEntry(key, move, depth);
    }

    /**
     * Adds a TT entry
     */
    public void add(long key, String move, int depth, int eval, int type) {
        if (depth == DEPTH_QS) {
            // Always replace
            int hashKey = (int) (key % size);
            table[hashKey] = new HashtableEntry(key, move, DEPTH_QS, eval, type);
        }
        else {
            assert(depth > 0);

            // Do not store path-dependent draw evaluations arising from three-fold repetition or
            // 50 moves rule, since these evaluations may not hold for other branches of the search.
            if (Math.abs(eval) == VALUE_PATH_DRAW) return;

            int hashKey = (int) (key % size);
            HashtableEntry entry = table[hashKey];	

            // If an entry for the same position exists, replace if the search depth was higher.
            // If an entry exists but for a different position, replace if it was from an old search.
            boolean replace;
            if (entry == null) replace = true;
            else {
                if (key == entry.key) {
                    replace = depth > entry.depth;
                    if (entry.move == null && move != null)
                        table[hashKey].move = move;
                }
                else replace = depth > entry.depth - entry.age * 3;
            }

            if (replace) table[hashKey] = new HashtableEntry(key, move, depth, eval, type);
        }
    }
}
