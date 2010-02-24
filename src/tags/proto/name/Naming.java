// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.util.LayerInterfaceLo;
import tags.proto.route.Routing;
import tags.proto.cont.Contact;

import tags.proto.MultiParts;
import tags.util.Maps;
import java.util.Collections;

import tags.proto.DataSources;
import tags.proto.LocalTGraph;
import tags.proto.TGraph;
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
public class Naming<T, A, U, W, S> extends LayerService<Query<?, T>, StoreControl<?, T, A, U, W, S, ?>> implements
LayerInterfaceHi<Integer, Routing<T, A, W, S>>,
LayerInterfaceLo<Integer, Contact<?, A, S, ?>> {

	protected Routing<T, A, W, S> layer_hi;
	protected Contact<?, A, S, ?> layer_lo;

	final protected TGraphComposer<U, W, S> mod_tgr_cmp;
	final protected AddressSchemeBuilder<T, A, U, W> mod_asc_bld;

	final protected DataSources<A, LocalTGraph<T, A, U, W>, S> source;

	protected LocalTGraph<T, A, U, W> graph;
	protected AddressScheme<T, A, W> scheme;

	public Naming(
		Query<?, T> query,
		StoreControl<?, T, A, U, W, S, ?> sctl,
		TGraphComposer<U, W, S> mod_tgr_cmp,
		AddressSchemeBuilder<T, A, U, W> mod_asc_bld
	) {
		super(query, sctl);
		this.mod_tgr_cmp = mod_tgr_cmp;
		this.mod_asc_bld = mod_asc_bld;
		// TODO NOW
		this.source = null;
		this.graph = null;
		this.scheme = null;
	}

	@Override public void setLayerHi(Routing<T, A, W, S> layer_hi) {
		this.layer_hi = layer_hi;
	}

	@Override public Integer request() {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override public void setLayerLo(Contact<?, A, S, ?> layer_lo) {
		this.layer_lo = layer_lo;
	}

	@Override public void receive(Integer tkt) {
		throw new UnsupportedOperationException("not implemented");
	}

	public TGraph<T, A, U, W> getCompositeTGraph() {
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
	protected TGraph<T, A, U, W> composeTGraph() {
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

		// TODO HIGH this method needs to return a LocalTGraph
		return new TGraph<T, A, U, W>(node_map, arc_map);
	}

	/**
	** Make a new {@link #scheme} from {@link #graph}. To be called whenever
	** the latter changes, ie. after {@link #composeTGraph()}.
	*/
	protected AddressScheme<T, A, W> makeAddressScheme() {
		return mod_asc_bld.buildAddressScheme(graph, getCompletedTags(), query.seed_tag);
	}

}
