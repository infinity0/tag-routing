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
public class ProtoPTableComposer<I, A> implements PTableComposer<I, A, Probability, Probability> {

	/**
	** Probability that a data source judges an item to be worthless, given
	** that the item does not exist in the source.
	*/
	final public double alpha;

	/**
	** The {@link MeanProbabilityComposer#alpha(Object, Object)} method of this
	** composer just returns {@link #alpha}.
	*/
	final public ValueComposer<I, PTable<A, Probability>, Probability, A, Probability> val_cmp_g =
	new MeanProbabilityComposer<I, PTable<A, Probability>, A>(Viewers.<A, Probability>PTableTGraphs()) {
		@Override protected double alpha(PTable<A, Probability> view, A item) {
			return alpha;
		}
	};

	final public ValueComposer<I, PTable<A, Probability>, Probability, A, Probability> val_cmp_h =
	new MeanProbabilityComposer<I, PTable<A, Probability>, A>(Viewers.<A, Probability>PTableIndexes()) {
		@Override protected double alpha(PTable<A, Probability> view, A item) {
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
	@Override public Probability composePTableGNode(MapX2<I, PTable<A, Probability>, Probability> src_score, A item) {
		return val_cmp_g.composeValue(src_score, item);
	}

	/**
	** {@inheritDoc}
	*/
	@Override public Probability composePTableHNode(MapX2<I, PTable<A, Probability>, Probability> src_score, A item) {
		return val_cmp_h.composeValue(src_score, item);
	}

}
