// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.CompositeIterable;

import java.util.Map;

/**
** Utility methods for grouping and collecting {@link PTable}, {@link TGraph},
** {@link Index}, and the components of these.
*/
public class MultiParts {

	public static <A, S> Iterable<Map<A, S>> iterTGraphs(Iterable<PTable<A, S>> tabs) {
		return new CompositeIterable<PTable<A, S>, Map<A, S>>(tabs) {
			@Override public Map<A, S> nextFor(PTable<A, S> tab) {
				return tab.getTGraphs();
			}
		};
	}

	public static <A, S> Iterable<Map<A, S>> iterIndexes(Iterable<PTable<A, S>> tabs) {
		return new CompositeIterable<PTable<A, S>, Map<A, S>>(tabs) {
			@Override public Map<A, S> nextFor(PTable<A, S> tab) {
				return tab.getIndexes();
			}
		};
	}

}
