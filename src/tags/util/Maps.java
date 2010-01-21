// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Arrays;

import tags.util.Union.U2;
import tags.util.Tuple.$2;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;

/**
** Utilities for {@link Map}s.
*/
final public class Maps {

	private Maps() { }

	/**
	** @see #domain(Iterable)
	*/
	public static <K, V> Set<K> domain(Map<K, V>... maps) {
		return domain(Arrays.asList(maps));
	}

	/**
	** Returns the union of all the domains of the given collection of maps.
	**
	** Currently, this does '''not''' return a view of the given maps, and will
	** '''not''' appear to self-update when the maps change.
	**
	** OPTIMISE NORM making it a view seems quite complicated...
	*/
	public static <K, V> Set<K> domain(Iterable<Map<K, V>> maps) {
		int s = 0;
		for (Map<K, V> m: maps) { s += m.size(); }
		Set<K> domain = new HashSet<K>(s<<1);
		for (Map<K, V> m: maps) { domain.addAll(m.keySet()); }
		return domain;
	}

	/**
	** Returns the nodes which the given arcmap refers to. DOCUMENT explain
	** better.
	**
	** Currently, this does '''not''' return a view of the given map, and will
	** '''not''' appear to self-update when the map changes.
	**
	** OPTIMISE NORM making it a view seems quite complicated...
	*/
	public static <K extends Arc, V> Set<Object> referent(Map<K, V> arcmap) {
		Set<Object> referent = new HashSet<Object>(arcmap.size()<<1);
		for (K arc: arcmap.keySet()) {
			referent.add(arc.src);
			referent.add(arc.dst);
		}
		return referent;
	}

	/**
	** A {@link Map} which can have two different types of key.
	**
	** @param <K0> Type of key 0
	** @param <K1> Type of key 1
	** @param <V> Type of value
	*/
	public static interface U2Map<K0, K1, V> extends Map<U2<K0, K1>, V> {

		/**
		** Return a view of the map containing only keys of type {@code K0}.
		*/
		public Map<K0, V> K0Map();

		/**
		** Return a view of the map containing only keys of type {@code K1}.
		*/
		public Map<K1, V> K1Map();

	}

	/**
	** A {@link Map} which has two values for each key.
	**
	** @param <K> Type of key
	** @param <V0> Type of value 0
	** @param <V1> Type of value 1
	*/
	public static interface Map$2<K, V0, V1, M0 extends Map<K, V0>, M1 extends Map<K, V1>> extends Map<K, $2<V0, V1>> {

		/**
		** Return a view of the map containing only values of type {@code V0}.
		*/
		public M0 MapV0();

		/**
		** Return a view of the map containing only values of type {@code V1}.
		*/
		public M1 MapV1();

	}

	/**
	** DOCUMENT.
	**
	** TODO LOW possibly make this also {@code extend U2Map<K0, K1, $2<V0, V1>, Map$2<K0, V0, V1>, Map$2<K1, V0, V1>>}
	*/
	public static interface U2Map$2<K0, K1, V0, V1> extends Map$2<U2<K0, K1>, V0, V1, U2Map<K0, K1, V0>, U2Map<K0, K1, V1>> {
		// for convience, like "typedef"
	}

}
