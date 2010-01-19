// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.init;

import tags.proto.MultiParts;
import tags.util.ValueComposer;
import tags.util.MeanProbabilityComposer;
import tags.util.Probability;

import tags.proto.PTable;
import java.util.Map;

/**
** Basic implementation of {@link PTableComposer} which uses {@link
** MeanProbabilityComposer} with a constant {@code alpha} value.
*/
public class ProtoPTableComposer implements PTableComposer<Probability, Probability> {

	/**
	** Probability that a (ie. any) data source judges an item to be worthless,
	** given that the item does not exist in the source (ie. not in the map's
	** {@link Map#keySet()}).
	*/
	final public double alpha;

	/**
	** Construct a new composer with an {@link #alpha} of 2^-4.
	*/
	public ProtoPTableComposer() {
		this(1/16);
	}

	/**
	** @param alpha See {@link #alpha}
	*/
	public ProtoPTableComposer(double alpha) {
		this(new Probability(alpha));
	}

	/**
	** @param alpha See {@link #alpha}
	*/
	public ProtoPTableComposer(Probability alpha) {
		this.alpha = alpha.val;
	}

	final public ValueComposer<Probability, Probability> val_cmp = new MeanProbabilityComposer() {

		/**
		** {@inheritDoc}
		**
		** This implementation returns {@link ProtoPTableComposer#alpha}.
		*/
		@Override protected <K> double alpha(Map<K, Probability> src, K item) {
			return alpha;
		}

	};

	/**
	** {@inheritDoc}
	*/
	public <A> Probability composePTableGNode(Map<PTable<A, Probability>, Probability> src_score, A item) {
		return val_cmp.composeValue(MultiParts.viewTGraphs(src_score), item);
	}

	/**
	** {@inheritDoc}
	*/
	public <A> Probability composePTableHNode(Map<PTable<A, Probability>, Probability> src_score, A item) {
		return val_cmp.composeValue(MultiParts.viewIndexes(src_score), item);
	}

}
