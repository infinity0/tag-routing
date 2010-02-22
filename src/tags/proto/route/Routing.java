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
import tags.proto.Index;
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

	final protected IndexComposer<W, S> mod_idx_cmp;
	final protected LookupScorer<W, S> mod_lku_scr;

	final protected DataSources<A, LocalIndex<T, A, W>, S> source;
	final protected Map<A, Set<T>> lookup;

	protected LocalIndex<T, A, W> index;

	public Routing(
		Query<?, T> query,
		StoreControl<?, T, A, ?, W, S, ?> sctl,
		IndexComposer<W, S> mod_idx_cmp,
		LookupScorer<W, S> mod_lku_scr
	) {
		super(query, sctl);
		this.mod_idx_cmp = mod_idx_cmp;
		this.mod_lku_scr = mod_lku_scr;
		// TODO NOW
		this.source = null;
		this.lookup = null;
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
	public Map<A, Set<T>> getLookups(AddressScheme<T, A, W> scheme) {
		Map<A, Set<T>> lookups = new HashMap<A, Set<T>>();
		for (A idx: source.localMap().keySet()) {
			if (source.seedMap().containsKey(idx)) {
				// select all T in scheme
				lookups.put(idx, scheme.tagSet());
			} else {
				Set<T> tags = new HashSet<T>();
				for (A in_node: source.getIncoming(idx)) {
					LocalIndex<T, A, W> view = source.localMap().get(in_node);
					assert view.getIncomingHarcAttrMap(idx) != null;
					// find Set<T> that we reached A by
					for (T tag: view.getIncomingHarcAttrMap(idx).keySet()) {
						// for all T in Set<T>, select all "short" paths in layer_lo.getAddressScheme()
						tags.addAll(scheme.ancestorMap().K0Map().get(tag));
					}
				}
				lookups.put(idx, tags);
			}
		}
		return lookups;
	}


	/**
	** DOCUMENT
	*/
	public Index<T, A, W> composeIndex() {
		throw new UnsupportedOperationException("not implemented");
		// follow basically what Naming.composeTGraph() does
	}

	/**
	** Returns a map of results to their scores.
	**
	** TODO NORM arguably this could be in a separate module instead of just
	** "pick the most relevant tag".
	*/
	public Map<A, W> getResults(AddressScheme<T, A, W> scheme) {
		Map<A, W> results = new HashMap<A, W>();
		for (A dst: index.nodeSetD()) {
			// get most relevant tag which points to it
			Map<T, W> in_tag = index.getIncomingDarcAttrMap(dst);
			assert in_tag != null && !in_tag.isEmpty();
			T nearest = scheme.getMostRelevant(in_tag.keySet());

			W wgt = null; // TODO HIGH combine(nearest.weight, in_tag.get(nearest));
			results.put(dst, wgt);
		}
		// TODO HIGH and same for nodeSetH()
		return results;
	}

}
