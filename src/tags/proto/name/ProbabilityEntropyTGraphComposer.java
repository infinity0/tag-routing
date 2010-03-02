// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto.name;

import tags.proto.Viewers;
import tags.util.ValueComposer;
import tags.util.MeanProbabilityComposer;
import tags.util.MeanEntropyComposer;
import tags.util.MapViewer;

import tags.proto.LocalTGraph;
import tags.util.Maps.MapX2;
import tags.util.Union.U2;
import tags.util.Arc;
import tags.util.Probability;
import tags.util.Entropy;
import java.util.Map;

/**
** DOCUMENT.
*/
public class ProbabilityEntropyTGraphComposer<T, A> implements TGraphComposer<T, A, Probability, Probability, Probability> {

	/**
	** Probability that a data source judges an item to be worthless, given
	** that the item does not exist in the source.
	*/
	final public double node_alpha;

	/**
	** Probability that a data source judges an arc to be worthless, given that
	** it does not exist in the source, but not both of its endpoint nodes are.
	*/
	final public double arc_alpha_1;

	/**
	** Probability that a data source judges an arc to be worthless, given that
	** it does not exist in the source, but both of its endpoint nodes are.
	*/
	final public double arc_alpha_2;

	/**
	** The {@link MeanProbabilityComposer#alpha(Object, Object)} method of this
	** composer just returns {@link #node_alpha}.
	*/
	final public ValueComposer<A, LocalTGraph<T, A, Probability, Probability>, Probability, U2<T, A>, Entropy> val_cmp_node =
	new MeanEntropyComposer<A, LocalTGraph<T, A, Probability, Probability>, U2<T, A>>(Viewers.<T, A>ProbabilityTGraphEntropyNodeMap()) {
		@Override protected double alpha(LocalTGraph<T, A, Probability, Probability> src, U2<T, A> item) {
			return node_alpha;
		}
	};

	/**
	** The {@link MeanProbabilityComposer#alpha(Object, Object)} method of this
	** composer returns either {@link #arc_alpha_1} or {@link #arc_alpha_2},
	** whichever is appropriate.
	*/
	final public ValueComposer<A, LocalTGraph<T, A, Probability, Probability>, Probability, U2<Arc<T, T>, Arc<T, A>>, Probability> val_cmp_arc =
	new MeanProbabilityComposer<A, LocalTGraph<T, A, Probability, Probability>, U2<Arc<T, T>, Arc<T, A>>>(Viewers.<T, A, Probability, Probability>TGraphArcMap()) {
		@Override protected double alpha(LocalTGraph<T, A, Probability, Probability> src, U2<Arc<T, T>, Arc<T, A>> item) {
			return src.hasEndpoints(item)? arc_alpha_2: arc_alpha_1;
		}
	};

	/**
	** Construct a new composer with a {@link #node_alpha} of 2^-4, {@link
	** #arc_alpha_1} of 2^-4 and an {@link #arc_alpha_2} of 2^-1.
	*/
	public ProbabilityEntropyTGraphComposer() {
		this(0.0625, 0.0625, 0.5);
	}

	/**
	** @param node_alpha See {@link #node_alpha}
	** @param arc_alpha_1 See {@link #arc_alpha_1}
	** @param arc_alpha_2 See {@link #arc_alpha_2}
	*/
	public ProbabilityEntropyTGraphComposer(double node_alpha, double arc_alpha_1, double arc_alpha_2) {
		this.node_alpha = node_alpha;
		this.arc_alpha_1 = arc_alpha_1;
		this.arc_alpha_2 = arc_alpha_2;
	}

	/**
	** @param node_alpha See {@link #node_alpha}
	** @param arc_alpha_1 See {@link #arc_alpha_1}
	** @param arc_alpha_2 See {@link #arc_alpha_2}
	*/
	public ProbabilityEntropyTGraphComposer(Probability node_alpha, Probability arc_alpha_1, Probability arc_alpha_2) {
		this(node_alpha.val, arc_alpha_1.val, arc_alpha_2.val);
	}

	/**
	** {@inheritDoc}
	*/
	public Probability composeNode(MapX2<A, LocalTGraph<T, A, Probability, Probability>, Probability> src_score, U2<T, A> node) {
		return val_cmp_node.composeValue(src_score, node).probability();
	}

	/**
	** {@inheritDoc}
	*/
	public Probability composeArc(MapX2<A, LocalTGraph<T, A, Probability, Probability>, Probability> src_score, U2<Arc<T, T>, Arc<T, A>> arc) {
		return val_cmp_arc.composeValue(src_score, arc);
	}

}
