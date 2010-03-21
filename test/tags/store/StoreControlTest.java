// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import junit.framework.TestCase;

import edu.uci.ics.jung.graph.*;
import edu.uci.ics.jung.io.*;
import org.apache.commons.collections15.*;

import java.util.*;
import java.io.*;

public class StoreControlTest extends TestCase {

	public void testBasic() {
		// pass
	}

	public static class Node {
		@Override public String toString() { return "o"; }
	}

	public static class Arc {
		@Override public String toString() { return "--"; }
	}

	public void testJUNG() throws IOException, javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException {

		Factory<Node> fac_v = FactoryUtils.instantiateFactory(Node.class);
		Factory<Arc> fac_e = FactoryUtils.instantiateFactory(Arc.class);
		GraphMLReader<DirectedGraph<Node, Arc>, Node, Arc> reader = new
		GraphMLReader<DirectedGraph<Node, Arc>, Node, Arc>(fac_v, fac_e);

		DirectedGraph<Node, Arc> g = new DirectedSparseGraph<Node, Arc>();
		reader.load("../test.graphml", g);

	}

}
