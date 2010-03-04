// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.proto.QueryProcessor;
import tags.util.exec.MessageReceiver;
import tags.util.exec.MessageRejectedException;

import tags.proto.MultiParts;
import tags.util.Maps;
import java.util.Collections;

import tags.proto.DataSources;
import tags.proto.LocalTGraph;
import tags.proto.FullTGraph;
import tags.proto.AddressScheme;
import tags.util.Maps.U2Map;
import tags.util.Union.U2;
import tags.util.Arc;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class Naming<T, A, U, W, S> extends LayerService<Query<?, T>, QueryProcessor<?, T, A, U, W, S, ?>>
implements MessageReceiver<Naming.MSG_I> {

	public static enum MSG_I { REQ_MORE_DATA, RECV_SEED_G }

	final protected TGraphComposer<T, A, U, W, S> mod_tgr_cmp;
	final protected AddressSchemeBuilder<T, A, U, W> mod_asc_bld;

	final protected DataSources<A, LocalTGraph<T, A, U, W>, S> source;

	protected LocalTGraph<T, A, U, W> graph;
	protected AddressScheme<T, A, W> scheme;

	public Naming(
		Query<?, T> query,
		QueryProcessor<?, T, A, U, W, S, ?> proc,
		TGraphComposer<T, A, U, W, S> mod_tgr_cmp,
		AddressSchemeBuilder<T, A, U, W> mod_asc_bld
	) {
		super(query, proc);
		if (mod_tgr_cmp == null) { throw new NullPointerException(); }
		if (mod_asc_bld == null) { throw new NullPointerException(); }
		this.mod_tgr_cmp = mod_tgr_cmp;
		this.mod_asc_bld = mod_asc_bld;
		// TODO NOW
		this.source = null;
		this.graph = null;
		this.scheme = null;
	}

	@Override public synchronized void recv(MSG_I msg) throws MessageRejectedException {
		switch (msg) {
		case REQ_MORE_DATA: // request for more data, from Routing

			// if no seeds, or no tgraphs to add, or some other heuristic,
			// - pass request onto contact layer

			// otherwise,
			// - complete the next tag in the address scheme, or
			// - add a tgraph as a data source

			//throw new UnsupportedOperationException("not implemented");

			break;
		case RECV_SEED_G: // receive seed tgraphs, from Contact

			//
			//throw new UnsupportedOperationException("not implemented");

			break;
		}
		assert false;
	}

	public FullTGraph<T, A, U, W> getCompositeTGraph() {
		throw new UnsupportedOperationException("not implemented");
	}

	public AddressScheme<T, A, W> getAddressScheme() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Get the set of tags which have been completed in all the tgraphs that we
	** are using as a data source (ie. source.localMap().values()).
	*/
	protected Set<T> getCompletedTags() {
		// TODO HIGH need to make LocalTGraph.getCompletedTags() return absent tags too
		if (source.localMap().isEmpty()) { return Collections.emptySet(); }

		// return intersection of all sources.getCompletedTags(),
		Iterator<LocalTGraph<T, A, U, W>> it = source.localMap().values().iterator();
		assert it.hasNext();
		Set<T> complete = new HashSet<T>(it.next().getCompletedTags());

		while (it.hasNext()) {
			complete.retainAll(it.next().getCompletedTags());
		}

		return complete;
	}

	/**
	** Make a new {@link #graph} from {@link #source}. To be called whenever
	** the {@linkplain #getCompletedTags() completed set} changes.
	*/
	protected FullTGraph<T, A, U, W> composeTGraph() {
		// iterates through all nodes present in every source
		U2Map<T, A, U> node_map = Maps.uniteDisjoint(new HashMap<T, U>(), new HashMap<A, U>());
		for (U2<T, A> node: Maps.domain(MultiParts.iterTGraphNodeMaps(source.localMap().values()))) {
			node_map.put(node, mod_tgr_cmp.composeNode(source.localScoreMap(), node));
		}

		// iterates through all arcs present in every source
		U2Map<Arc<T, T>, Arc<T, A>, W> arc_map = Maps.uniteDisjoint(new HashMap<Arc<T, T>, W>(), new HashMap<Arc<T, A>, W>());
		for (U2<Arc<T, T>, Arc<T, A>> arc: Maps.domain(MultiParts.iterTGraphArcMaps(source.localMap().values()))) {
			arc_map.put(arc, mod_tgr_cmp.composeArc(source.localScoreMap(), arc));
		}

		return new FullTGraph<T, A, U, W>(node_map, arc_map);
	}

	/**
	** Make a new {@link #scheme} from {@link #graph}. To be called whenever
	** the latter changes, ie. after {@link #composeTGraph()}.
	*/
	protected AddressScheme<T, A, W> makeAddressScheme() {
		return mod_asc_bld.buildAddressScheme(graph, getCompletedTags(), query.seed_tag);
	}

}
