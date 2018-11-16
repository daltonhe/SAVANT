/**
 * @author Dalton He
 * created 11-15-18
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
			if (table[i] != null) {
				table[i].age++;
				// delete old entries
				if (table[i].age >= HASH_MAX_AGE) table[i] = null;
			}
		}
	}
	
	/**
	 * Returns the hash table entry for the given zobrist key, or null if not found.
	 */
	public HashtableEntry get(long zobrist) {
		int hashKey = (int) (zobrist % size);
		HashtableEntry entry = table[hashKey];
		if (entry != null && zobrist == entry.zobrist) return entry;
		return null;
	}
	
	/**
	 * Deletes the hash table entry for the given zobrist key, or decrements its count
	 * if count > 1.
	 */
	public void delete(long zobrist) {
		int hashKey = (int) (zobrist % size);
		if (table[hashKey] == null)
			return;
		
		if (table[hashKey].count > 1)
			table[hashKey].count--;
		else
			table[hashKey] = null;
	}
	
	/**
	 * Adds a repetition table entry
	 */
	public void add(long zobrist) {
		int hashKey = (int) (zobrist % size);
		if (table[hashKey] == null) {
			table[hashKey] = new HashtableEntry(zobrist);
		} else
			table[hashKey].count++;
	}
	
	/**
	 * Adds a PV table entry
	 */
	public void add(long zobrist, String move) {
		int hashKey = (int) (zobrist % size);
		table[hashKey] = new HashtableEntry(zobrist, move);
	}
	
	/**
	 * Adds a TT entry
	 */
	public void add(long zobrist, String move, int depth, int eval, int type) {
		assert(depth > 0);
		
		// Do not save path-dependent draw evaluations arising from three-fold repetition or
		// 50 moves rule, since these evaluations may not hold for other branches of the search.
		if (Math.abs(eval) == VALUE_PATH_DRAW) return;
		
		int hashKey = (int) (zobrist % size);
		HashtableEntry entry = table[hashKey];	
		
		// If an entry for the same position exists, replace if the search depth was higher.
		// If an entry exists but for a different position, replace if it was from an old search.
		boolean replace;
		if (entry != null) {
			if (zobrist == entry.zobrist) {
				replace = (depth > entry.depth);
				if (entry.move == null && move != null)
					table[hashKey].move = move;
			}
			else replace = (entry.age > 0);
		}
		else replace = true;
		
		if (replace) table[hashKey] = new HashtableEntry(zobrist, move, depth, eval, type);
	}
	

}
