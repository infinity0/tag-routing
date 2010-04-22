// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.ui;

import tags.proto.AddressScheme;

import tags.util.Union.U2;
import tags.util.ProxyIterable;
import java.util.Collections;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
** A {@link QueryStateFormatter} that outputs plain text representations.
**
** DOCUMENT more detail here...
*/
public class QueryStateTextFormatter<T, A, W> implements QueryStateFormatter<T, A, W> {

	/**
	** {@inheritDoc}
	**
	** DOCUMENT
	*/
	public String formatResults(Map<A, W> results) {
		StringBuilder s = new StringBuilder();
		s.append("[ ");
		for (Map.Entry<A, W> en: results.entrySet()) {
			s.append(String.format("(%1$s:%2$.4s) ", en.getKey(), en.getValue()));
		}
		s.append(']');
		return s.toString();
	}

	/**
	** {@inheritDoc}
	**
	** DOCUMENT
	*/
	public String[] formatLookups(Map<A, Set<T>> lookups, Set<T> tags) {
		String[] out = new String[tags.size()+4];
		out[0] = String.format("Lookups over %d idx, %d tag", lookups.size(), tags.size());
		out[1] = "----";

		int w = maxSize(tags);
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

	/**
	** {@inheritDoc}
	**
	** DOCUMENT
	*/
	public String[] formatAddressScheme(AddressScheme<T, A, W> scheme) {
		final int columns = 5;
		int sz = scheme.indexMap().size();
		String[] out = new String[sz+2];
		out[0] = "Address scheme for " + scheme.seedTag();
		out[1] = "----";

		String[][] tab = new String[sz][];
		int i = 0;
		for (U2<T, A> node: scheme.nodeList()) {
			tab[i] = new String[] {
			  Integer.toString(i),
			  node.val.toString(),
			  String.format("%6.4s", scheme.arcAttrMap().get(node.val)),
			  join(",", scheme.indexes(scheme.incomingMap().get(node))),
			  join("->", scheme.indexes(scheme.pathMap().get(node)))
			};
			assert tab[i].length == columns;
			++i;
		}
		if (scheme.isIncomplete()) {
			char[] pad = new char[tab[sz-1][0].length()];
			Arrays.fill(pad, '#');
			tab[sz-1][0] = new String(pad);
		}

		int[] cols = colsize(Arrays.asList(tab), columns);
		for (i=0; i<sz; ++i) {
			out[i+2] = String.format("%"+cols[0]+"s | %-"+cols[1]+"s | %s | %-"+cols[3]+"s | %-"+cols[4]+"s", (Object[])tab[i]);
		}
		return out;
	}

	/**
	** Return the width of the biggest {@link Object#toString()} value in the
	** given iterable.
	*/
	public static <T> int maxSize(Iterable<T> it) {
		int max = 0;
		for (T o: it) {
			int sz = o.toString().length();
			if (sz > max) { max = sz; }
		}
		return max;
	}

	/**
	** Return the width of a positive integer's representation in base 10.
	*/
	public static int positiveNumberWidth(int i) {
		if (i < 0) { throw new IllegalArgumentException(); }
		return i < 10? 1: i < 100? 2: i < 1000? 3: i < 10000? 4: i < 100000? 5: 6;
	}

	/**
	** Return a string of characters representing the indicator function of a
	** subset of a parent set. Characters are ordered as per the iterator of
	** the parent set.
	**
	** @param slots The parent set
	** @param items The subset
	*/
	public static <T> char[] formatIndicatorVector(Set<T> slots, Set<T> items, char present, char absent) {
		char[] vec = new char[slots.size()];
		int i = 0;
		for (T item: slots) {
			vec[i] = items.contains(item)? present: absent;
			++i;
		}
		return vec;
	}

	public static String join(String sep, Iterable<?> items) {
		StringBuilder sb = new StringBuilder();
		for (Object item: items) {
			sb.append(item).append(sep);
		}
		int len = sb.length();
		return len == 0? "": sb.substring(0, len-sep.length());
	}

	public static int[] colsize(final Iterable<String[]> rect, int cols) {
		int[] col = new int[cols];
		for (int i=0; i<cols; ++i) {
			final int curcol = i;
			col[curcol] = maxSize(new ProxyIterable<String[], String>(rect) {
				@Override public String nextFor(String[] row) {
					return row[curcol];
				}
			});
		}
		return col;
	}

}
