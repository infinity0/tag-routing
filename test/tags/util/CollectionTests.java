// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import junit.framework.TestCase;

import java.util.*;

final public class CollectionTests extends TestCase {

	private CollectionTests() { }

	public static void testIterable(Iterable<?> ib, int sz) {
		testIterable(ib, sz, true);
	}

	public static void testIterable(Iterable<?> ib, int sz, boolean testrem) {
		Iterator<?> it;
		int i = 0;

		// Test standard iteration
		for (it = ib.iterator(), i = 0; it.hasNext();) {
			it.next(); ++i;
		}
		checkNextException(it);
		assertTrue(i == sz);

		if (!testrem) { return; }

		// Test removal
		it = ib.iterator();
		checkRemoveException(it);
		for (i = 0; it.hasNext();) {
			it.next(); ++i;
			it.remove();
			checkRemoveException(it);
		}
		checkRemoveException(it);
		assertTrue(i == sz);

		// No elements left
		it = ib.iterator();
		assertFalse(it.hasNext());
		checkNextException(it);
	}

	public static void checkNextException(Iterator<?> it) {
		try { it.next(); fail(); }
		catch (Throwable t) { assertTrue(t instanceof NoSuchElementException); }
	}

	public static void checkRemoveException(Iterator<?> it) {
		try { it.remove(); fail(); }
		catch (Throwable t) { assertTrue(t instanceof IllegalStateException); }
	}

}
