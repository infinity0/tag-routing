// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import junit.framework.TestCase;

import tags.io.AttrGraphMLReader;
import tags.io.AttrGraphMLMetadata;

import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.io.*;
import org.apache.commons.collections15.*;

import java.util.*;
import java.io.*;

public class StoreControlTest extends TestCase {

	public void testBasic() {
		// pass
	}

	public static class Vx {
		@Override public String toString() { return "o"; }
	}

	public static class Ed {
		@Override public String toString() { return "->"; }
	}

	public void testJUNG() throws IOException, javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException {

		Factory<Vx> fac_v = FactoryUtils.instantiateFactory(Vx.class);
		Factory<Ed> fac_e = FactoryUtils.instantiateFactory(Ed.class);
		AttrGraphMLReader<DirectedGraph<Vx, Ed>, Vx, Ed> reader = new
		AttrGraphMLReader<DirectedGraph<Vx, Ed>, Vx, Ed>(fac_v, fac_e);

		DirectedGraph<Vx, Ed> g = new DirectedSparseGraph<Vx, Ed>();
		reader.load("../test.graphml", g);

		for (Vx vx: g.getVertices()) {
			StringBuilder s = new StringBuilder();
			s.append(vx).append(" ( ");
			for (Map.Entry<String, GraphMLMetadata<Vx>> en: reader.getVertexMetadata().entrySet()) {
				s.append(en.getKey()).append(':');
				s.append(en.getValue().transformer.transform(vx)).append(' ');
			}
			s.append(')');
			//System.out.println(s);
		}

		for (Ed ed: g.getEdges()) {
			StringBuilder s = new StringBuilder();
			s.append(ed).append(" ( ");
			for (Map.Entry<String, AttrGraphMLMetadata<Ed>> en: reader.getEdgeAttrMetadata().entrySet()) {
				s.append(en.getKey()).append(':');
				s.append(en.getValue().transformer().transform(ed)).append(' ');
			}
			s.append(')');
			//System.out.println(s);
		}

	}

}
