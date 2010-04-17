// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.proto.LocalTGraph;
import tags.proto.LocalIndex;
import tags.util.Maps.U2Map;
import tags.util.ProxyIterable;
import tags.util.Arc;
import java.util.Map;

/**
** Utility methods for grouping and collecting {@link PTable}, {@link TGraph},
** {@link Index}, and the components of these.
*/
public class MultiParts {

	public static <A, S> Iterable<Map<A, S>> iterTGraphs(Iterable<PTable<A, S>> tabs) {
		return new ProxyIterable<PTable<A, S>, Map<A, S>>(tabs) {
			@Override public Map<A, S> nextFor(PTable<A, S> tab) {
				return tab.getTGraphs();
			}
		};
	}

	public static <A, S> Iterable<Map<A, S>> iterIndexes(Iterable<PTable<A, S>> tabs) {
		return new ProxyIterable<PTable<A, S>, Map<A, S>>(tabs) {
			@Override public Map<A, S> nextFor(PTable<A, S> tab) {
				return tab.getIndexes();
			}
		};
	}

	public static <T, A, U, W> Iterable<U2Map<T, A, U>> iterTGraphNodeMaps(Iterable<LocalTGraph<T, A, U, W>> views) {
		return new ProxyIterable<LocalTGraph<T, A, U, W>, U2Map<T, A, U>>(views) {
			@Override public U2Map<T, A, U> nextFor(LocalTGraph<T, A, U, W> item) {
				return item.nodeMap();
			}
		};
	}

	public static <T, A, U, W> Iterable<U2Map<Arc<T, T>, Arc<T, A>, W>> iterTGraphArcMaps(Iterable<LocalTGraph<T, A, U, W>> views) {
		return new ProxyIterable<LocalTGraph<T, A, U, W>, U2Map<Arc<T, T>, Arc<T, A>, W>>(views) {
			@Override public U2Map<Arc<T, T>, Arc<T, A>, W> nextFor(LocalTGraph<T, A, U, W> item) {
				return item.arcMap();
			}
		};
	}

	public static <T, A, W> Iterable<U2Map<Arc<T, A>, Arc<T, A>, W>> iterIndexArcMaps(Iterable<LocalIndex<T, A, W>> views) {
		return new ProxyIterable<LocalIndex<T, A, W>, U2Map<Arc<T, A>, Arc<T, A>, W>>(views) {
			@Override public U2Map<Arc<T, A>, Arc<T, A>, W> nextFor(LocalIndex<T, A, W> item) {
				return item.arcMap();
			}
		};
	}

	/**
	** @deprecated No longer used
	*/
	@Deprecated public static <R, A, S> Map<Map<A, S>, R> viewTGraphs(Map<PTable<A, S>, R> src_score) {
		return new tags.util.SourceMap<PTable<A, S>, R, A, S>(src_score) {
			@Override public Map<A, S> mapFor(PTable<A, S> tab) {
				return tab.getTGraphs();
			}
		};
	}

	/**
	** @deprecated No longer used
	*/
	@Deprecated public static <R, A, S> Map<Map<A, S>, R> viewIndexes(Map<PTable<A, S>, R> src_score) {
		return new tags.util.SourceMap<PTable<A, S>, R, A, S>(src_score) {
			@Override public Map<A, S> mapFor(PTable<A, S> tab) {
				return tab.getIndexes();
			}
		};
	}

}
