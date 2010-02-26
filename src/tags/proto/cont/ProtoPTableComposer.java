// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.cont;

import tags.proto.Viewers;
import tags.util.ValueComposer;
import tags.util.MeanProbabilityComposer;
import tags.util.MapViewer;

import tags.proto.PTable;
import tags.util.Maps.MapX2;
import tags.util.Probability;
import java.util.Map;

/**
** Basic implementation of {@link PTableComposer} which uses {@link
** MeanProbabilityComposer} with a constant {@code alpha} value.
*/
public class ProtoPTableComposer implements PTableComposer<Probability, Probability> {

	/**
	** Probability that a data source judges an item to be worthless, given
	** that the item does not exist in the source.
	*/
	final public double alpha;

	/**
	** The {@link MeanProbabilityComposer#alpha(Map, Object)} method of this
	** composer just returns {@link #alpha}.
	*/
	final public ValueComposer<Probability, Probability> val_cmp = new MeanProbabilityComposer() {
		@Override protected <A> double alpha(Map<A, Probability> src, A item) {
			return alpha;
		}
	};

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
		this.alpha = alpha;
	}

	/**
	** @param alpha See {@link #alpha}
	*/
	public ProtoPTableComposer(Probability alpha) {
		this(alpha.val);
	}

	/**
	** {@inheritDoc}
	*/
	@Override public <I, A> Probability composePTableGNode(MapX2<I, PTable<A, Probability>, Probability> src_score, A item) {
		return val_cmp.composeValue(src_score, item, Viewers.<A, Probability>PTableTGraphs());
	}

	/**
	** {@inheritDoc}
	*/
	@Override public <I, A> Probability composePTableHNode(MapX2<I, PTable<A, Probability>, Probability> src_score, A item) {
		return val_cmp.composeValue(src_score, item, Viewers.<A, Probability>PTableIndexes());
	}

}
