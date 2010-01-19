// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.CompositeIterable;
import tags.util.SourceMap;

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

	public static <R, A, S> Map<Map<A, S>, R> viewTGraphs(Map<PTable<A, S>, R> src_score) {
		return new SourceMap<PTable<A, S>, R, A, S>(src_score) {
			@Override public Map<A, S> mapFor(PTable<A, S> tab) {
				return tab.getTGraphs();
			}
		};
	}

	public static <R, A, S> Map<Map<A, S>, R> viewIndexes(Map<PTable<A, S>, R> src_score) {
		return new SourceMap<PTable<A, S>, R, A, S>(src_score) {
			@Override public Map<A, S> mapFor(PTable<A, S> tab) {
				return tab.getIndexes();
			}
		};
	}

}
