// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.route;

import tags.proto.Viewers;
import tags.util.ValueComposer;
import tags.util.MeanProbabilityComposer;
import tags.util.MapViewer;

import tags.proto.LocalIndex;
import tags.util.Maps.MapX2;
import tags.util.Union.U2;
import tags.util.Arc;
import tags.util.Probability;
import java.util.Map;

/**
** DOCUMENT.
*/
public class ProbabilityIndexComposer implements IndexComposer<Probability, Probability> {

	/**
	** Probability that a data source judges an arc to be worthless, given that
	** it does not exist in the source, but not both of its endpoint nodes are.
	*/
	final public double alpha_1;

	/**
	** Probability that a data source judges an arc to be worthless, given that
	** it does not exist in the source, but both of its endpoint nodes are.
	*/
	final public double alpha_2;

	/**
	** The {@link MeanProbabilityComposer#alpha(Map, Object)} method of this
	** composer will return either {@link #alpha_1} or {@link #alpha_2},
	** whichever is appropriate.
	*/
	final public ValueComposer<Probability, Probability> val_cmp = new MeanProbabilityComposer() {
		@Override protected <A> double alpha(Map<A, Probability> src, A item) {
			// TODO HIGH
			// either return alpha_1 or alpha_2 depending on referents
			throw new UnsupportedOperationException("not implemented");
		}
	};

	/**
	** Construct a new composer with an {@link #alpha_1} of 2^-4 and an {@link
	** #alpha_2} of 2^-1.
	*/
	public ProbabilityIndexComposer() {
		this(0.0625, 0.5);
	}

	/**
	** @param alpha_1 See {@link #alpha_1}
	** @param alpha_2 See {@link #alpha_2}
	*/
	public ProbabilityIndexComposer(double alpha_1, double alpha_2) {
		this.alpha_1 = alpha_1;
		this.alpha_2 = alpha_2;
	}

	/**
	** @param alpha_1 See {@link #alpha_1}
	** @param alpha_2 See {@link #alpha_2}
	*/
	public ProbabilityIndexComposer(Probability alpha_1, Probability alpha_2) {
		this(alpha_1.val, alpha_2.val);
	}

	/**
	** {@inheritDoc}
	*/
	public <T, A> Probability composeArc(MapX2<A, LocalIndex<T, A, Probability>, Probability> src_score, U2<Arc<T, A>, Arc<T, A>> arc) {
		return val_cmp.composeValue(src_score, arc, Viewers.<T, A, Probability>IndexArcMap());
	}

}
