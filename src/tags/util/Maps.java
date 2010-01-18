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
		Set<K> domain = new HashSet<K>(s);
		for (Map m: maps) { domain.addAll(m.keySet()); }
		return domain;
	}

}
