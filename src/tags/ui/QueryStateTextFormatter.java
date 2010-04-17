// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.ui;

import tags.proto.AddressScheme;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
** A {@link QueryStateFormatter} that outputs plain text representations.
**
** DOCUMENT more detail here...
*/
public class QueryStateTextFormatter<T, A, W> implements QueryStateFormatter<T, A, W> {

	public String formatResults(Map<A, W> results) {
		StringBuilder s = new StringBuilder();
		s.append("[ ");
		for (Map.Entry<A, W> en: results.entrySet()) {
			s.append(String.format("(%1$s:%2$.4s) ", en.getKey(), en.getValue()));
		}
		s.append(']');
		return s.toString();
	}

	public String[] formatLookups(Map<A, Set<T>> lookups, Set<T> tags) {
		int w = maxSize(tags);
		String[] out = new String[tags.size()+4];
		out[0] = String.format("Lookups over %d idx, %d tag", lookups.size(), tags.size());
		out[1] = "----";

		char[][] tab = new char[lookups.size()][];
		int i = 0;
		for (Map.Entry<A, Set<T>> en: lookups.entrySet()) {
			tab[i] = formatIndicatorVector(tags, en.getValue(), '+', ' ');
			++i;
		}

		// transpose matrix
		i = 0;
		for (T tag: tags) {
			char[] line = new char[tab.length];
			for (int j=0; j<tab.length; ++j) {
				line[j] = tab[j][i];
			}
			out[i+2] = String.format("%-" + w + "s |%s|", tag, new String(line));
			++i;
		}

		out[i+2] = "----";
		out[i+3] = "idx: " + lookups.keySet().toString();
		return out;
	}

	public String[] formatAddressScheme(AddressScheme<T, A, W> scheme) {
		throw new UnsupportedOperationException("not implemented");
	}

	public static <T> int maxSize(Iterable<T> it) {
		int max = 0;
		for (T o: it) {
			int sz = o.toString().length();
			if (sz > max) { max = sz; }
		}
		return max;
	}

	public static <T> char[] formatIndicatorVector(Set<T> slots, Set<T> items, char present, char absent) {
		char[] vec = new char[slots.size()];
		int i = 0;
		for (T item: slots) {
			vec[i] = items.contains(item)? present: absent;
			++i;
		}
		return vec;
	}

}
