// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.util.LayerInterfaceLo;
import tags.proto.route.Routing;
import tags.proto.init.Init;

import tags.proto.LocalTGraph;
import tags.proto.TGraph;
import tags.proto.AddressScheme;
import tags.util.Union.U2;
import tags.util.Tuple.X2;
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
LayerInterfaceLo<Integer, Init<?, A, S, ?>> {

	protected Routing<T, A, W, S> layer_hi;
	protected Init<?, A, S, ?> layer_lo;

	final protected DistanceMetric<?, U, W> mod_dmtr;
	final protected TGraphComposer<U, W, S> mod_tgr_cmp;
	final protected TGraphScorer<U, W, S> mod_tgr_scr;

	final protected Map<A, LocalTGraph<T, A, U, W>> source;
	final protected Map<A, S> score;
	final protected LocalTGraph<T, A, U, W> graph;
	final protected AddressScheme<T, A> scheme;

	public Naming(
		Query<?, T> query,
		StoreControl<?, T, A, U, W, S, ?> sctl,
		DistanceMetric<?, U, W> mod_dmtr,
		TGraphComposer<U, W, S> mod_tgr_cmp,
		TGraphScorer<U, W, S> mod_tgr_scr
	) {
		super(query, sctl);
		this.mod_dmtr = mod_dmtr;
		this.mod_tgr_cmp = mod_tgr_cmp;
		this.mod_tgr_scr = mod_tgr_scr;
		// TODO NOW
		this.source = null;
		this.score = null;
		this.graph = null;
		this.scheme = null;
	}

	@Override public void setLayerHi(Routing<T, A, W, S> layer_hi) {
		this.layer_hi = layer_hi;
	}

	@Override public Integer request() {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override public void setLayerLo(Init<?, A, S, ?> layer_lo) {
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
	** are using as a data source (ie. source.values()).
	*/
	protected Set<T> getCompletedTags() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** Make a new {@link #graph} from {@link #source}, {@link #score}. To be
	** called whenever the {@linkplain #getCompletedTags() completed set}
	** changes.
	*/
	protected TGraph<T, A, U, W> composeTGraph() {
		throw new UnsupportedOperationException("not implemented");
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
