// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.cont;

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
	** Probability that a (ie&#x2e; any) data source judges an item to be
	** worthless, given that the item does not exist in the source (ie&#x2e;
	** not in the map's {@link Map#keySet()}).
	*/
	final public double alpha;

	/**
	** Construct a new composer with an {@link #alpha} of 2^-4.
	*/
	public ProtoPTableComposer() {
		this(0.0625);
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

	/**
	** The {@link MeanProbabilityComposer#alpha(Map, Object)} method of this
	** composer just returns {@link #alpha}.
	*/
	final public ValueComposer<Probability, Probability> val_cmp = new MeanProbabilityComposer() {
		@Override protected <K> double alpha(Map<K, Probability> src, K item) {
			return alpha;
		}
	};

	/**
	** {@inheritDoc}
	*/
	@Override public <A> Probability composePTableGNode(Map<PTable<A, Probability>, Probability> src_score, A item) {
		return val_cmp.composeValue(MultiParts.viewTGraphs(src_score), item);
	}

	/**
	** {@inheritDoc}
	*/
	@Override public <A> Probability composePTableHNode(Map<PTable<A, Probability>, Probability> src_score, A item) {
		return val_cmp.composeValue(MultiParts.viewIndexes(src_score), item);
	}

}
