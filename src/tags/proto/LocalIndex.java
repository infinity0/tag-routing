// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import tags.util.Maps;

import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import tags.util.Arc;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import java.io.IOException;

/**
** Local view of an {@link Index}, implemented on top of {@link FullIndex}.
**
** @param <T> Type of tag
** @param <A> Type of document/index address
** @param <W> Type of arc-attribute
*/
public class LocalIndex<T, A, W> extends FullIndex<T, A, W> {

	/**
	** Remote address of this local view.
	*/
	final public A addr;

	/**
	** The {@link DataSources} collection that this local view is part of.
	*/
	final protected DataSources<A, LocalIndex<T, A, W>, ?> src;

	/**
	** Constructs a new local view of the given remote address, attached to the
	** given {@link DataSources} collection.
	**
	** @throws NullPointerException if either parameter is {@code null}
	*/
	public LocalIndex(A addr, DataSources<A, LocalIndex<T, A, W>, ?> src) {
		if (addr == null || src == null) { throw new NullPointerException(); }
		this.addr = addr;
		this.src = src;
	}

	/**
	** Returns the results for a given tag, filtering out the indexes that are
	** in use as a data source.
	*/
	public U2Map<A, A, W> getOutgoingTLeaf(T src) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Load a given subject tag's outgoing arcs, and their weights.
	**
	** @param subj The tag
	** @param out A map of arc-targets to arc-weights
	** @throws IllegalStateException if the out-arcs are already loaded
	** @throws IOException if the remote data structure is inconsistent
	*/
	public void setOutgoingT(T subj, U2Map<A, A, W> out) throws IOException {
		if (outgoing.containsKey(subj)) { throw new IllegalStateException("already loaded out-arcs for tag " + subj); }
		if (out == null) {
			// TODO HIGH this needs to be put somewhere else too
			return;
		}

		outgoing.put(subj, out);
		if (src != null) { src.setOutgoing(addr, out.K1Map().keySet()); }

		// calculate incoming arcs
		for (Map.Entry<U2<A, A>, W> en: out.entrySet()) {
			U2<A, A> node = en.getKey();
			W wgt = en.getValue();
			Map<T, W> inc = incoming.get(node);
			if (inc == null) {
				incoming.put(node, inc = new HashMap<T, W>());
			}
			inc.put(subj, wgt);
		}
	}

	/**
	** Register an index as a data source. The index must already be an
	** outgoing target of some loaded tag.
	*/
	public void setDataSource(A index) {
		throw new UnsupportedOperationException("not implemented");
	}

	private static LocalViewFactory stdFactory = new LocalViewFactory() {
		@SuppressWarnings("unchecked")
		@Override public Object createLocalView(Object addr, DataSources src) {
			return new LocalIndex(addr, src);
		}
	};

	@SuppressWarnings("unchecked") public static <T, A, W> LocalViewFactory<A, LocalIndex<T, A, W>> getFactory() {
		return (LocalViewFactory<A, LocalIndex<T, A, W>>)stdFactory;
	}

}
