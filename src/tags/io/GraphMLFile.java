// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.io;

import tags.io.AttrGraphMLReader;
import tags.io.AttrGraphMLMetadata;
import edu.uci.ics.jung.io.GraphMLMetadata;

import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.FactoryUtils;

import java.util.Map;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
** A {@link DirectedGraph} backed by a GraphML file, with methods to retrieve
** the attributes defined there.
**
** @author infinity0
*/
public class GraphMLFile extends DirectedSparseGraph<GraphMLFile.Vx, GraphMLFile.Ed> {

	final private static long serialVersionUID = 5150455211806503192L;

	final public static String NID = "id";
	final public static String NAT = "height";
	final public static String AAT = "weight";

	final protected AttrGraphMLReader<DirectedGraph<Vx, Ed>, Vx, Ed> reader;

	public GraphMLFile(String filename) throws IOException, ParserConfigurationException, SAXException {
		this.reader = new AttrGraphMLReader<DirectedGraph<Vx, Ed>, Vx, Ed>(
			FactoryUtils.instantiateFactory(Vx.class), FactoryUtils.instantiateFactory(Ed.class)
		);
		this.reader.load(filename, this);
	}

	public void debug() {

		for (Vx vx: getVertices()) {
			StringBuilder s = new StringBuilder();
			s.append(vx).append(" ( ");
			for (Map.Entry<String, GraphMLMetadata<Vx>> en: reader.getVertexMetadata().entrySet()) {
				s.append(en.getKey()).append(':');
				s.append(en.getValue().transformer.transform(vx)).append(' ');
			}
			s.append(')');
			System.out.println(s);
		}

		for (Ed ed: getEdges()) {
			StringBuilder s = new StringBuilder();
			s.append(ed).append(" ( ");
			for (Map.Entry<String, AttrGraphMLMetadata<Ed>> en: reader.getEdgeAttrMetadata().entrySet()) {
				s.append(en.getKey()).append(':');
				s.append(en.getValue().transformer().transform(ed)).append(' ');
			}
			s.append(')');
			System.out.println(s);
		}

	}

	public static class Vx {
		@Override public String toString() { return "o"; }
	}

	public static class Ed {
		@Override public String toString() { return "->"; }
	}

}
