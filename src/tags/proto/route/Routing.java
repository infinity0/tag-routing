// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.LayerService;
import tags.proto.Query;
import tags.store.StoreControl;
import tags.util.LayerInterfaceHi;
import tags.util.LayerInterfaceLo;
import tags.proto.name.Naming;

import tags.proto.DataSources;
import tags.proto.LocalIndex;
import java.util.Set;
import java.util.Map;

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
	public Map<A, Set<T>> getLookups() {
		throw new UnsupportedOperationException("not implemented");
		// if A in seeds, select all T in scheme
		// otherwise
		// - find Set<T> that we reached A by, using data from this.source
		// - for all T in Set<T>, select all "short" paths in layer_lo.getAddressScheme()
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
