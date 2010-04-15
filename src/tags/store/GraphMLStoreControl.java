// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.util.Maps;

import tags.proto.PTable;
import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.Map;

import tags.io.TypedXMLGraph;
import org.apache.commons.collections15.map.ReferenceMap;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
** DOCUMENT.
**
** @param <N> Type of node (identity, object address, tag)
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score (both score and identity-score)
*/
public class GraphMLStoreControl<N, U, W, S> implements StoreControl<N, N, N, U, W, S, S> {

	final protected File basedir;
	final protected File dir_tgr;
	final protected File dir_idx;

	final protected TypedXMLGraph<T_PTB, N, U, S> ptables;
	final protected ReferenceMap<N, TypedXMLGraph<T_TGR, N, U, W>> tgraphs;
	final protected ReferenceMap<N, TypedXMLGraph<T_IDX, N, U, W>> indexes;

	/**
	** @see #GraphMLStoreControl(File)
	*/
	public GraphMLStoreControl(String basedir) throws IOException {
		this(new File(basedir));
	}

	/**
	** @throws IllegalArgumentException if {@code basedir} is not a directory
	*/
	public GraphMLStoreControl(File basedir) throws IOException {
		if (!basedir.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + basedir);
		}
		this.basedir = basedir;
		this.dir_tgr = new File(basedir, DIR_TGR);
		this.dir_idx = new File(basedir, DIR_IDX);

		this.ptables = makeTypedXMLGraph(T_PTB.class);
		this.ptables.load(new File(basedir, FILE_PTB));
		this.ptables.setVertexPrimaryKey(NODE_ID);
		this.ptables.setDefaultEdgeAttribute(ARC_ATTR);

		this.tgraphs = new ReferenceMap<N, TypedXMLGraph<T_TGR, N, U, W>>();
		this.indexes = new ReferenceMap<N, TypedXMLGraph<T_IDX, N, U, W>>();
	}

	public Map<N, S> getFriends(N id) throws IOException {
		return ptables.getSuccessorTypedMap(id).get(T_PTB.z);
	}

	public PTable<N, S> getPTable(N id) throws IOException {
		Map<T_PTB, Map<N, S>> out = ptables.getSuccessorTypedMap(id);
		return new PTable<N, S>(out.get(T_PTB.g), out.get(T_PTB.h));
	}

	public U2Map<N, N, W> getTGraphOutgoing(N addr, N src) throws IOException {
		TypedXMLGraph<T_TGR, N, U, W> tgr = getTGraph(addr);
		Map<T_TGR, Map<N, W>> out = tgr.getSuccessorTypedMap(src);
		return Maps.uniteDisjoint(out.get(T_TGR.t), out.get(T_TGR.g));
	}

	public U getTGraphNodeAttr(N addr, U2<N, N> node) throws IOException {
		TypedXMLGraph<T_TGR, N, U, W> tgr = getTGraph(addr);
		return tgr.getVertexAttribute(node.isT0()? node.getT0(): node.getT1());
	}

	public U2Map<N, N, W> getIndexOutgoing(N addr, N src) throws IOException {
		TypedXMLGraph<T_IDX, N, U, W> idx = getIndex(addr);
		Map<T_IDX, Map<N, W>> out = idx.getSuccessorTypedMap(src);
		return Maps.uniteDisjoint(out.get(T_IDX.d), out.get(T_IDX.h));
	}

	protected TypedXMLGraph<T_TGR, N, U, W> getTGraph(N addr) throws IOException {
		TypedXMLGraph<T_TGR, N, U, W> graph = tgraphs.get(addr);
		if (graph == null) {
			graph = makeTypedXMLGraph(T_TGR.class);
			graph.load(new File(dir_idx, addr.toString() + ".graphml"));
			graph.setVertexPrimaryKey(NODE_ID);
			graph.setDefaultVertexAttribute(NODE_ATTR);
			graph.setDefaultEdgeAttribute(ARC_ATTR);
			tgraphs.put(addr, graph);
		}
		return graph;
	}

	protected TypedXMLGraph<T_IDX, N, U, W> getIndex(N addr) throws IOException {
		TypedXMLGraph<T_IDX, N, U, W> graph = indexes.get(addr);
		if (graph == null) {
			graph = makeTypedXMLGraph(T_IDX.class);
			graph.load(new File(dir_idx, addr.toString() + ".graphml"));
			graph.setVertexPrimaryKey(NODE_ID);
			graph.setDefaultEdgeAttribute(ARC_ATTR);
			indexes.put(addr, graph);
		}
		return graph;
	}

	final public static String NODE_ID = "id";
	final public static String NODE_ATTR = "height";
	final public static String ARC_ATTR = "weight";

	final public static String FILE_PTB = "ptb.graphml";
	final public static String DIR_IDX = "idx";
	final public static String DIR_TGR = "tgr";

	public enum T_PTB { z, g, h }
	public enum T_TGR { t, g }
	public enum T_IDX { d, t, h }

	final public static <T extends Enum<T>, N, U, W> TypedXMLGraph<T, N, U, W>
	makeTypedXMLGraph(Class<T> typecl) throws IOException {
		try {
			return new ProtoTypedXMLGraph<T, N, U, W>(typecl);
		} catch (ParserConfigurationException e) {
			throw new IOException("Error creating new TypedXMLGraph", e);
		} catch (SAXException e) {
			throw new IOException("Error creating new TypedXMLGraph", e);
		}
	}

}
