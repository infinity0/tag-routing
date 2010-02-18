// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

import junit.framework.TestCase;
import tags.util.Generators;
import tags.util.Maps;

import tags.util.SPUProbabilityInferer;

import tags.util.Probability;
import tags.util.Arc;
import static tags.util.Tuple.X2;
import tags.util.Maps.U2Map;

import java.util.*;

public class TGraphTest extends TestCase {

	public void testConstruction() {
		for (int t=0; t<0x10; ++t) {
		for (int g=0; g<0x10; ++g) {
		for (int tt=t*t/8; tt<7*t*t/8; tt+=(t+t)>>1) {
		for (int tg=t*g/8; tg<7*t*g/8; tg+=(t+g)>>1) {
			TGraph<String, Integer, Probability, Probability> G = randomTGraph(t, g, tt, tg);

			assertTrue(G.nodeMap().size() == t+g);
			assertTrue(G.nodeMap().K0Map().size() == t);
			assertTrue(G.nodeMap().K1Map().size() == g);

			// FIXME NORM arcMap() not implemented yet
			//assertTrue(G.arcMap().size() == tt+tg);
			//assertTrue(G.arcMap().K0Map().size() == tt);
			//assertTrue(G.arcMap().K1Map().size() == tg);

			int gtt = 0, gtg = 0;
			for (String tag: G.nodeMap().K0Map().keySet()) {
				U2Map<String, Integer, Probability> out_arc = G.getOutgoingT(tag).arcAttrMap();
				gtt += out_arc.K0Map().size();
				gtg += out_arc.K1Map().size();
			}
			assertTrue(gtt == tt);
			assertTrue(gtg == tg);
		}
		}
		}
		}
	}

	public void testLocalTGraph() throws java.io.IOException {
		DataSources<Integer, LocalTGraph<String, Integer, Probability, Probability>, Probability> src = new
		DataSources<Integer, LocalTGraph<String, Integer, Probability, Probability>, Probability>(
			LocalTGraph.<String, Integer, Probability, Probability>getFactory(),
			new SPUProbabilityInferer()
		);
		TGraph<String, Integer, Probability, Probability> G = randomTGraph(0x40, 0x10, 0x400, 0x40);
		src.setOutgoing(-1, Collections.<Integer>emptySet());
		LocalTGraph<String, Integer, Probability, Probability> G_ = src.useSource(-1);

		// load all nodes
		for (Map.Entry<String, Probability> en: G.nodeMap().K0Map().entrySet()) { G_.setNodeAttrT(en.getKey(), en.getValue()); }
		for (Map.Entry<Integer, Probability> en: G.nodeMap().K1Map().entrySet()) { G_.setNodeAttrG(en.getKey(), en.getValue()); }

		// empty since no arcs have been loaded
		assertTrue(G_.getCompletedTags().isEmpty());

		// load each node's out-arcs. this should cause the node to become "complete"
		for (String tag: G.nodeMap().K0Map().keySet()) {
			assertFalse(G_.getCompletedTags().contains(tag));
			G_.setOutgoingT(tag, G.getOutgoingT(tag).arcAttrMap());
			assertTrue(G_.getCompletedTags().contains(tag));
			// TODO HIGH this test is really more appropriate for when we load arcs FIRST then load nodes
			//assertTrue(G_.getOutgoingT(tag).nodeAttrMap().size() == G_.getOutgoingT(tag).arcAttrMap().size());
		}

		// TODO HIGH some more tests...
	}

	/**
	** @param t Number of tag nodes
	** @param g Number of tgraph nodes
	** @param tt Number of tag-tag arcs
	** @param tg Number of tag-tgraph arcs
	*/
	public static TGraph<String, Integer, Probability, Probability> randomTGraph(int t, int g, int tt, int tg) {
		U2Map<String, Integer, Probability> node_map = Maps.uniteDisjoint(new HashMap<String, Probability>(), new HashMap<Integer, Probability>());
		U2Map<Arc<String, String>, Arc<String, Integer>, Probability> arc_map = Maps.uniteDisjoint(new HashMap<Arc<String, String>, Probability>(), new HashMap<Arc<String, Integer>, Probability>());

		for (int i=0; i<t; ++i) { node_map.K0Map().put(Generators.rndKey(), Generators.rndProb(0, 0.0625)); }
		for (int i=0; i<g; ++i) { node_map.K1Map().put(i, Generators.rndProb(0, 0.5)); }

		List<String> ls_t = Arrays.asList(node_map.K0Map().keySet().toArray(new String[t]));
		List<Integer> ls_g = Arrays.asList(node_map.K1Map().keySet().toArray(new Integer[g]));
		Iterator<X2<String, String>> it_tt = Generators.rndPairs(ls_t, ls_t);
		Iterator<X2<String, Integer>> it_tg = Generators.rndPairs(ls_t, ls_g);

		for (int i=0; i<tt; ++i) { arc_map.K0Map().put(Arc.fromTuple(it_tt.next()), Generators.rndProb()); }
		for (int i=0; i<tg; ++i) { arc_map.K1Map().put(Arc.fromTuple(it_tg.next()), Generators.rndProb()); }

		return new TGraph<String, Integer, Probability, Probability>(node_map, arc_map);
	}

}
