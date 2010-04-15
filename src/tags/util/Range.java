// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Iterator;
import java.util.List;
import java.util.AbstractList;
import java.util.RandomAccess;

/**
** An efficient implementation of a list view over a range of integers. The
** collection is immutable.
*/
public class Range extends AbstractList<Integer> implements List<Integer>, RandomAccess {

	/** Low endpoint (inclusive). */
	final public int lo;
	/** High endpoint (exclusive). */
	final public int hi;
	/** Size of this range. */
	final public int size;

	/**
	** Creates a new range from {@code lo} (inclusive) to {@code hi}
	** (exclusive).
	**
	** @throws IllegalArgumentException if {@code lo} > {@code hi}
	*/
	public Range(int lo, int hi) {
		if (lo > hi) { throw new IllegalArgumentException("lo > hi: " + lo + " > " + hi); }
		this.lo = lo;
		this.hi = hi;
		this.size = hi - lo;
	}

	/**
	** Creates a new range from 0 to the given integer.
	*/
	public Range(int hi) {
		this(0, hi);
	}

	// AbstractCollection

	@Override public int size() {
		return size;
	}

	@Override public boolean isEmpty() {
		return size == 0;
	}

	@Override public boolean contains(Object o) {
		if (!(o instanceof Integer)) { return false; }
		return contains((Integer) o);
	}

	final public boolean contains(int i) {
		return lo <= i && i < hi;
	}

	// AbstractList

	@Override public Integer get(int index) {
		if (0 > index || index >= size) { throw new IndexOutOfBoundsException(); }
		return lo + index;
	}

	@Override public int indexOf(Object o) {
		if (!(o instanceof Integer)) { return -1; }
		return indexOf((Integer)o);
	}

	final public int indexOf(int i) {
		int index = i - lo;
		if (0 > index || index >= size) { return -1; }
		return index;
	}

	@Override public int lastIndexOf(Object o) {
		return indexOf(o);
	}

	@Override public List<Integer> subList(int fr, int to) {
		return new Range(get(fr), get(to-1)+1);
	}

}
