// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import junit.framework.TestCase;

import tags.util.Union.U2;
import tags.util.Tuple.X2;
import tags.util.Maps.U2Map;
import tags.util.Maps.MapX2;

import java.util.*;

public class MapsTest extends TestCase {

	final public static int nn = 0x10;

	public static String nstr(int n) {
		char[] s = new char[n];
		Arrays.fill(s, ' ');
		return new String(s);
	}

	public void testAllSizes() {
		for (int s0=0; s0<nn; ++s0) {
			for (int s1=0; s1<nn; ++s1) {
				testUniteDisjoint(s0, s1);
				testConvoluteStrict(s0, s1);
				testViewSubMap(s0, s1);
			}
		}
	}

	public void testUniteDisjoint(int s0, int s1) {
		Map<String, Boolean> m0 = new HashMap<String, Boolean>();
		Map<Integer, Boolean> m1 = new HashMap<Integer, Boolean>();
		U2Map<String, Integer, Boolean> mm = Maps.uniteDisjoint(m0, m1);

		for (int i=0; i<4; ++i) {
			assertTrue(mm.size() == 0);
			for (int j=0; j<s0; ++j) {
				m0.put(nstr(j), true);
				assertTrue(mm.containsKey(Union.<String, Integer>U2_0(nstr(j))));
				assertFalse(mm.containsKey(Union.<String, Integer>U2_0(nstr(j+1))));
			}
			assertTrue(mm.size() == s0);
			for (int j=0; j<s1; ++j) {
				m1.put(j, true);
				assertTrue(mm.containsKey(Union.<String, Integer>U2_1(j)));
				assertFalse(mm.containsKey(Union.<String, Integer>U2_1(j+1)));
			}
			assertTrue(mm.size() == s0+s1);
			CollectionTests.testIterable(mm.keySet(), s0+s1);
			assertTrue(m0.isEmpty());
			assertTrue(m1.isEmpty());
			assertTrue(mm.size() == 0);
		}

		for (int i=0; i<4; ++i) {
			assertTrue(mm.size() == 0);
			for (int j=0; j<s0+s1; ++j) {
				mm.put(Union.<String, Integer>U2_0(nstr(j)), true);
				assertTrue(m0.containsKey(nstr(j)));
				assertFalse(m0.containsKey(nstr(j+1)));
				assertTrue(mm.containsKey(Union.<String, Integer>U2_0(nstr(j))));
				assertFalse(mm.containsKey(Union.<String, Integer>U2_0(nstr(j+1))));
				mm.put(Union.<String, Integer>U2_1(j), true);
				assertTrue(m1.containsKey(j));
				assertFalse(m1.containsKey(j+1));
				assertTrue(mm.containsKey(Union.<String, Integer>U2_1(j)));
				assertFalse(mm.containsKey(Union.<String, Integer>U2_1(j+1)));
			}
			assertTrue(mm.size() == (s0+s1)*2);
			CollectionTests.testIterable(m0.keySet(), s0+s1);
			assertTrue(mm.size() == (s0+s1));
			CollectionTests.testIterable(m1.keySet(), s0+s1);
			assertTrue(mm.size() == 0);
		}
	}

	public void testConvoluteStrict(int s0, int s1) {
		Map<Long, String> m0 = new HashMap<Long, String>();
		Map<Long, Integer> m1 = new HashMap<Long, Integer>();
		MapX2<Long, String, Integer, Map<Long, String>, Map<Long, Integer>> mm = Maps.convoluteStrict(m0, m1, Maps.BaseMapX2.Inclusion.SUB0SUP1);

		for (int i=0; i<4; ++i) {
			assertTrue(mm.size() == 0);
			for (int j=0; j<s0+s1; ++j) {
				m1.put((long)j, j);
				assertFalse(mm.containsKey((long)j));
				assertTrue(mm.get((long)j) == null);
			}
			assertTrue(m1.size() == s0+s1);
			assertTrue(mm.size() == 0);
			for (int j=0; j<s0; ++j) {
				m0.put((long)j, nstr(j));
				assertTrue(mm.containsKey((long)j));
				assertTrue(mm.get((long)j).equals(Tuple.X2(nstr(j), j)));
			}
			assertTrue(mm.size() == s0);
			CollectionTests.testIterable(mm.keySet(), s0);
			assertTrue(mm.size() == 0);
			assertTrue(m0.isEmpty());
			assertTrue(m1.size() == s1);
			m1.clear();
		}

		for (int i=0; i<4; ++i) {
			assertTrue(mm.size() == 0);
			assertTrue(m0.isEmpty());
			assertTrue(m1.isEmpty());
			for (int j=0; j<s0; ++j) {
				mm.put((long)j, Tuple.X2(nstr(j), j));
				assertTrue(mm.get((long)j).equals(Tuple.X2(nstr(j), j)));
				assertTrue(m0.get((long)j).equals(nstr(j)));
				assertTrue(m1.get((long)j).equals(j));
			}
			assertTrue(mm.size() == s0);
			CollectionTests.testIterable(mm.keySet(), s0);
			assertTrue(mm.size() == 0);
			assertTrue(m0.isEmpty());
			assertTrue(m1.isEmpty());
		}
	}

	public void testViewSubMap(int s0, int s1) {
		Map<Integer, Boolean> parent = new HashMap<Integer, Boolean>();
		Set<Integer> keys = new HashSet<Integer>();

		for (int i=0; i<4; ++i) {
			for (int j=0; j<s0+s1; j+=2) { parent.put(j, true); }
			for (int j=0; j<s0; ++j) { keys.add(j); }
			Map<Integer, Boolean> subview = Maps.viewSubMap(parent, keys);
			assertTrue(parent.size() == (s0+s1+1)>>1);
			assertTrue(subview.size() == (s0+1)>>1);
			CollectionTests.testIterable(subview.keySet(), (s0+1)>>1);
			assertTrue(subview.size() == 0);
			assertTrue(parent.size() == ((s0+s1+1)>>1)-((s0+1)>>1));
			parent.clear();
			for (int j=0; j<s0; ++j) { parent.put(j, true); }
			assertTrue(subview.size() == s0);
			subview.clear();
			assertTrue(parent.size() == 0);
			assertTrue(subview.size() == 0);
		}
	}

}
