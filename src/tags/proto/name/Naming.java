// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.util.LayerInterfaceLo;
import tags.proto.route.Routing;
import tags.proto.cont.Contact;

import tags.util.Maps;

import tags.proto.DataSources;
import tags.proto.LocalTGraph;
import tags.proto.TGraph;
import tags.proto.AddressScheme;
import tags.util.CompositeIterable;
import tags.util.Maps.U2Map;
import tags.util.Union.U2;
import tags.util.Tuple.X2;
import tags.util.Arc;
import java.util.Map;
import java.util.Set;
import java.util.Queue;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Comparator;

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

	final protected DistanceMetric<?, U, W> mod_dmtr;
	final protected TGraphComposer<U, W, S> mod_tgr_cmp;

	final protected DataSources<A, LocalTGraph<T, A, U, W>, S> source;
	final protected LocalTGraph<T, A, U, W> graph;
	final protected AddressScheme<T, A> scheme;

	public Naming(
		Query<?, T> query,
		StoreControl<?, T, A, U, W, S, ?> sctl,
		DistanceMetric<?, U, W> mod_dmtr,
		TGraphComposer<U, W, S> mod_tgr_cmp
	) {
		super(query, sctl);
		this.mod_dmtr = mod_dmtr;
		this.mod_tgr_cmp = mod_tgr_cmp;
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

	public AddressScheme<T, A> getAddressScheme() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Get the set of tags which have been completed in all the tgraphs that we
	** are using as a data source (ie. source.localMap().values()).
	*/
	protected Set<T> getCompletedTags() {
		throw new UnsupportedOperationException("not implemented");
		// need to make LocalTGraph.getCompletedTags() return absent tags too
		// return intersection of all sources.getCompletedTags(),
	}

	/**
	** Make a new {@link #graph} from {@link #source}. To be called whenever
	** the {@linkplain #getCompletedTags() completed set} changes.
	*/
	protected TGraph<T, A, U, W> composeTGraph() {
		U2Map<T, A, U> node_map = Maps.uniteDisjoint(new HashMap<T, U>(), new HashMap<A, U>());
		U2Map<Arc<T, T>, Arc<T, A>, W> arc_map = Maps.uniteDisjoint(new HashMap<Arc<T, T>, W>(), new HashMap<Arc<T, A>, W>());
		// TODO HIGH probably move the CompositeIterable shit into MultiParts

		// iterates through all nodes present in every source
		for (U2<T, A> node: Maps.domain(new CompositeIterable<LocalTGraph<T, A, U, W>, U2Map<T, A, U>>(source.localMap().values()) {
			@Override public U2Map<T, A, U> nextFor(LocalTGraph<T, A, U, W> item) {
				return item.nodeMap();
			}
		})) {
			node_map.put(node, mod_tgr_cmp.composeTGraphNode(source.localScoreMap(), node));
		}

		// iterates through all arcs present in every source
		for (U2<Arc<T, T>, Arc<T, A>> arc: Maps.domain(new CompositeIterable<LocalTGraph<T, A, U, W>, U2Map<Arc<T, T>, Arc<T, A>, W>>(source.localMap().values()) {
			@Override public U2Map<Arc<T, T>, Arc<T, A>, W> nextFor(LocalTGraph<T, A, U, W> item) {
				return item.arcMap();
			}
		})) {
			arc_map.put(arc, mod_tgr_cmp.composeTGraphArc(source.localScoreMap(), arc));
		}

		return new TGraph<T, A, U, W>(node_map, arc_map);
	}

	/**
	** Make a new {@link #scheme} from {@link #graph}. To be called whenever
	** the latter changes, ie. after {@link #composeTGraph()}.
	*/
	protected AddressScheme<T, A> makeAddressScheme() {
		return makeAddressScheme(this.mod_dmtr);
	}

	/**
	** Main worker method for {@link #makeAddressScheme()}. This allows us to
	** avoid having a {@code <D>} type parameter in the class definition.
	**
	** OPT NORM use a FibonnacciHeap instead of PriorityQueue. JGraphT has
	** an implementation.
	*/
	protected <D> AddressScheme<T, A> makeAddressScheme(final DistanceMetric<D, U, W> mod_dmtr) {
		AddressScheme<T, A> scheme_new = new AddressScheme<T, A>(query.seed_tag);
		Set<T> completed = getCompletedTags();

		// Dijkstra's algorithm

		class DijkstraNode {

			final public U2<T, A> node;
			protected D dist;

			public DijkstraNode(U2<T, A> node) {
				this.node = node;
				this.dist = (query.seed_tag.equals(node.val))? mod_dmtr.getMinElement(): mod_dmtr.getMaxElement();
			}

		}

		// 1. Init
		Queue<DijkstraNode> queue = new PriorityQueue<DijkstraNode>(graph.nodeMap().size(), new Comparator<DijkstraNode>() {
			@Override public int compare(DijkstraNode d1, DijkstraNode d2) {
				return mod_dmtr.compare(d1.dist, d2.dist);
			}
		});
		Map<U2<T, A>, DijkstraNode> dmap = new HashMap<U2<T, A>, DijkstraNode>();
		for (U2<T, A> u: graph.nodeMap().keySet()) {
			DijkstraNode n = new DijkstraNode(u);
			dmap.put(u, n);
			queue.add(n);
		}

		// 2. Loop
		// TODO HIGH need to get rid of tgraph nodes already in use as a data source
		while (queue.isEmpty()) {
			DijkstraNode cur = queue.poll();
			U2<T, A> node = cur.node;
			assert(dmap.get(node) == cur);
			dmap.remove(node);

			if (node.isT1()) {
				scheme_new.pushNode(node, null);
				continue;
			}
			T tag = node.getT0();

			// tag is not fully loaded, set as incomplete
			if (!completed.contains(tag)) {
				scheme_new.pushNode(node, null);
				scheme_new.setIncomplete();
				break;
			}

			// "relax" all out-neighbours
			U srcw = graph.nodeMap().K0Map().get(tag);
			for (Map.Entry<U2<T, A>, X2<U, W>> en: graph.getOutgoingT(tag).attrMap().entrySet()) {
				U2<T, A> nb = en.getKey();
				U dstw = en.getValue()._0;
				W arcw = en.getValue()._1;

				DijkstraNode out = dmap.get(nb);
				if (out == null) { continue; } // already visited

				D dist = mod_dmtr.combine(cur.dist, mod_dmtr.getDistance(srcw, dstw, arcw));
				if (mod_dmtr.compare(dist, out.dist) < 0) {
					// PriorityQueue treats its elements as immutable, so need to remove first
					queue.remove(out);
					out.dist = dist;
					queue.add(out);
				}
			}

			scheme_new.pushNode(node, graph.getIncomingT(tag).nodeAttrMap().K0Map().keySet());
		}

		return scheme_new;
	}

}
