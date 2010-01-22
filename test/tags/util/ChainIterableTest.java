// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import junit.framework.TestCase;

import java.util.*;

public class ChainIterableTest extends TestCase {

	final public static int n = 0x10;

	public void testAll() {
		for (int x=0; x<n; ++x) {
			for (int y=0; y<n; ++y) {
				List<ArrayList<Integer>> ls = fillList(x, fullIntegerList(y));
				Iterable<Integer> ib = new ChainIterable<Integer>(false, ls);
				Iterator<Integer> it;
				int i = 0;

				// Test standard iteration
				for (it = ib.iterator(), i = 0; it.hasNext();) {
					it.next(); ++i;
				}
				checkNextException(it);
				assertTrue(i == x*y);

				// Test removal
				it = ib.iterator();
				checkRemoveException(it);
				for (i = 0; it.hasNext();) {
					it.next(); ++i;
					it.remove();
					checkRemoveException(it);
				}
				checkRemoveException(it);
				assertTrue(i == x*y);

				// No elements left
				it = ib.iterator();
				assertFalse(it.hasNext());
				checkNextException(it);
			}
		}
	}

	public void checkNextException(Iterator<?> it) {
		try { it.next(); fail(); }
		catch (Throwable t) { assertTrue(t instanceof NoSuchElementException); }
	}

	public void checkRemoveException(Iterator<?> it) {
		try { it.remove(); fail(); }
		catch (Throwable t) { assertTrue(t instanceof IllegalStateException); }
	}

	public ArrayList<Integer> fullIntegerList(int n) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i=0; i<n; ++i) {
			list.add(i);
		}
		return list;
	}

	public <T extends ArrayList> List<T> fillList(int n, T elem) {
		List<T> list = new ArrayList<T>();
		for (int i=0; i<n; ++i) {
			@SuppressWarnings("unchecked") boolean b = list.add((T)elem.clone());
		}
		return list;
	}

}
