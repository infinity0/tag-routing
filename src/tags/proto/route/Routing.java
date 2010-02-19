// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.util.LayerInterfaceLo;
import tags.proto.name.Naming;

import tags.proto.AddressScheme;
import tags.proto.DataSources;
import tags.proto.LocalIndex;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

/**
** DOCUMENT.
**
** @param <T> Type of tag
** @param <A> Type of address
** @param <W> Type of arc-attribute
** @param <S> Type of score
*/
public class Routing<T, A, W, S> extends LayerService<Query<?, T>, StoreControl<?, T, A, ?, W, S, ?>> implements
LayerInterfaceLo<Integer, Naming<T, A, ?, W, S>> {

	protected Naming<T, A, ?, W, S> layer_lo;

	final protected DataSources<A, LocalIndex<T, A, W>, S> source;
	final protected Map<A, Set<T>> lookup;
	final protected Map<A, LocalIndex<T, A, W>> result;

	public Routing(
		Query<?, T> query,
		StoreControl<?, T, A, ?, W, S, ?> sctl
	) {
		super(query, sctl);
		// TODO NOW
		this.source = null;
		this.lookup = null;
		this.result = null;
	}

	@Override public void setLayerLo(Naming<T, A, ?, W, S> layer_lo) {
		this.layer_lo = layer_lo;
	}

	@Override public void receive(Integer tkt) {
		throw new UnsupportedOperationException("not implemented");
	}

	public Map<A, LocalIndex<T, A, W>> getResults() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	** This is to be called whenever:
	**
	** - address scheme is updated
	** - we add a source
	*/
	public Map<A, Set<T>> getLookups(AddressScheme<T, A> scheme) {
		Map<A, Set<T>> lookups = new HashMap<A, Set<T>>();
		for (A idx: source.localMap().keySet()) {
			if (source.seedMap().containsKey(idx)) {
				// select all T in scheme
				lookups.put(idx, scheme.getAllTags());
			} else {
				Set<T> tags = new HashSet<T>();
				for (A in_node: source.getIncoming(idx)) {
					LocalIndex<T, A, W> view = source.localMap().get(in_node);
					assert view.getIncomingHarcAttrMap(idx) != null;
					// find Set<T> that we reached A by
					for (T tag: view.getIncomingHarcAttrMap(idx).keySet()) {
						// for all T in Set<T>, select all "short" paths in layer_lo.getAddressScheme()
						tags.addAll(scheme.getPrecedingT(tag));
					}
				}
				lookups.put(idx, tags);
			}
		}
		return lookups;
	}

	/**
	** Returns a map of results to their scores.
	*/
	public Map<A, W> getRankedResults() {
		// TODO HIGH probably will need to make an IndexComposer out of this
		throw new UnsupportedOperationException("not implemented");
		// for each result:
		//     an index will point to that result from a given set of tags
		//
		// use the index-score, the tags scores and the arc scores, to work out
		// an aggregate score for the result
	}

}
