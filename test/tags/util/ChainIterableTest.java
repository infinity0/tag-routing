// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import junit.framework.TestCase;

import java.util.*;

public class ChainIterableTest extends TestCase {

	final public static int n = 0x10;

	public void testAll() {
		for (int x=0; x<n; ++x) {
			for (int y=0; y<n; ++y) {
				List<List<Integer>> ls = fillList(x, fullIntegerList(y));
				Iterable<Integer> ib = new ChainIterable<Integer>(false, ls);
				int i = 0;
				for (Integer ii: ib) { ++i; }
				assertTrue(i == x*y);
				for (Iterator<Integer> it = ib.iterator(); it.hasNext();) { it.next(); it.remove(); }
			}
		}
	}

	public List<Integer> fullIntegerList(int n) {
		List<Integer> list = new ArrayList<Integer>();
		for (int i=0; i<n; ++i) {
			list.add(i);
		}
		return list;
	}

	public <T> List<T> fillList(int n, T elem) {
		List<T> list = new ArrayList<T>();
		for (int i=0; i<n; ++i) {
			list.add(elem);
		}
		return list;
	}

}
