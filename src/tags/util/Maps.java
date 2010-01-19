// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Arrays;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;

/**
** Utilities for {@link Map}s.
*/
final public class Maps {

	private Maps() { }

	/**
	** @see #domain(Collection)
	*/
	public static <K, V> Set<K> domain(Map<K, V>... maps) {
		return domain(Arrays.asList(maps));
	}

	/**
	** Returns the union of all the domains of the given collection of maps.
	** Currently, this does '''not''' return a view of the given maps, and will
	** '''not''' appear to self-update when the maps change.
	**
	** TODO LOW making it a view seems quite complicated, so leave for now.
	*/
	public static <K, V> Set<K> domain(Iterable<Map<K, V>> maps) {
		int s = 0;
		for (Map m: maps) { s += m.size(); }
		Set<K> domain = new HashSet<K>(s<<1);
		for (Map m: maps) { domain.addAll(m.keySet()); }
		return domain;
	}

	/**
	** Returns the nodes which the given arcmap refers to. DOCUMENT explain
	** better.
	*/
	public static <K extends Arc, V> Set<Object> referent(Map<K, V> arcmap) {
		Set<Object> referent = new HashSet<Object>(arcmap.size()<<1);
		for (K arc: arcmap.keySet()) {
			referent.add(arc.src);
			referent.add(arc.dst);
		}
		return referent;
	}

}
