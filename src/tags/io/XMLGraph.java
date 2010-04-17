// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.io;

import edu.uci.ics.jung.io.GraphMLMetadata;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.graph.util.EdgeType;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.FactoryUtils;

import tags.util.Maps;

import tags.util.ProxyIterable;
import tags.util.ProxyMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.HashMap;

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
** A {@link DirectedGraph} backed by a GraphML file, with methods to retrieve
** the attributes defined there.
**
** This implementation, like most of the data structures in the standard
** collections library, is not synchronized.
**
** @author infinity0
** @param <K> Type of primary key
** @param <U> Type of default node attribute
** @param <W> Type of default arc attribute
*/
public class XMLGraph<K, U, W> extends DirectedSparseGraph<XMLGraph.Node, XMLGraph.Arc> {

	final protected AttrGraphMLReader<DirectedGraph<Node, Arc>, Node, Arc> reader;
	protected boolean open; // set true to allow reader to proceed
	protected boolean loaded;

	protected Map<K, Node> nodes;
	protected Transformer<Node, K> node_id;
	protected String vx_prim_key;
	protected Transformer<Node, U> node_attr;
	protected Transformer<Arc, W> arc_attr;

	/**
	** Create a new graph with a default reader.
	*/
	public XMLGraph() throws ParserConfigurationException, SAXException {
		this.reader = new AttrGraphMLReader<DirectedGraph<Node, Arc>, Node, Arc>(
			FactoryUtils.instantiateFactory(Node.class), FactoryUtils.instantiateFactory(Arc.class)
		);
	}

	/**
	** Load graph data from the given file.
	**
	** @return this object, with the data loaded
	*/
	public XMLGraph<K, U, W> load(String fn) throws IOException {
		return load(new FileReader(fn));
	}

	/**
	** Load graph data from the given file.
	**
	** @return this object, with the data loaded
	*/
	public XMLGraph<K, U, W> load(File fn) throws IOException {
		return load(new FileReader(fn));
	}

	/**
	** Load graph data from the given reader.
	**
	** @return this object, with the data loaded
	*/
	public XMLGraph<K, U, W> load(Reader rd) throws IOException {
		if (loaded) { throw new IllegalStateException(EMSG_ALREADY_LOADED); }
		open = true;
		reader.load(rd, this); postLoad();
		open = false;
		loaded = true;
		return this;
	}

	/**
	** This method is called after data is loaded into the graph, but before
	** the graph is made immutable again.
	**
	** The default implementation does nothing.
	*/
	protected void postLoad() {
		// do nothing
	}

	@Override public boolean addEdge(Arc edge, Pair<? extends Node> endpoints, EdgeType edgeType) {
		if (open) { return super.addEdge(edge, endpoints, edgeType); }
		throw new UnsupportedOperationException(EMSG_IMMUTABLE);
	}

	@Override public boolean addVertex(Node vertex) {
		if (open) { return super.addVertex(vertex); }
		throw new UnsupportedOperationException(EMSG_IMMUTABLE);
	}

	/**
	** Set the primary key of the graph to a vertex attribute; the attribute
	** values must be unique to each vertex.
	**
	** After this is called, vertices can then retrieved by the value for this
	** attribute, using {@link #getVertex(Object)}.
	**
	** @param attr Name of the attribute
	** @throws ClassCastException if this graph's key type doesn't match that
	**         of the attribute name.
	*/
	@SuppressWarnings("unchecked")
	public void setVertexPrimaryKey(String attr) {
		AttrGraphMLMetadata<Node> meta = reader.getVertexAttrMetadata().get(attr);
		if (meta == null) { throw new IllegalStateException("graph has no vertex attribute called: " + attr); }
		Transformer<Node, ?> tfm = meta.transformerUntyped();

		Map<K, Node> kmap = new HashMap<K, Node>();
		for (Node node: vertices.keySet()) {
			kmap.put((K)tfm.transform(node), node);
		}

		if (vertices.size() != kmap.size()) {
			throw new IllegalArgumentException("not a vertex primary key: " + attr + " (" + kmap.size() + "/" + vertices.size() + ")");
		}

		this.nodes = kmap;
		this.vx_prim_key = attr;
		this.node_id = (Transformer<Node, K>)tfm;
	}

	@SuppressWarnings("unchecked")
	public void setDefaultVertexAttribute(String attr) {
		AttrGraphMLMetadata<Node> meta = reader.getVertexAttrMetadata().get(attr);
		if (meta == null) { throw new IllegalStateException("graph has no vertex attribute called: " + attr); }
		this.node_attr = (Transformer<Node, U>)meta.transformerUntyped();
	}

	@SuppressWarnings("unchecked")
	public void setDefaultEdgeAttribute(String attr) {
		AttrGraphMLMetadata<Arc> meta = reader.getEdgeAttrMetadata().get(attr);
		if (meta == null) { throw new IllegalStateException("graph has no edge attribute called: " + attr); }
		this.arc_attr = (Transformer<Arc, W>)meta.transformerUntyped();
	}

	private transient Map<String, Object> graph_attr;
	/**
	** View the graph's attributes as a map.
	*/
	public Map<String, Object> getGraphAttributes() {
		if (graph_attr == null) {
			graph_attr = new ProxyMap<String, AttrGraphMLMetadata<DirectedGraph<Node, Arc>>, Object>(
			  reader.getGraphAttrMetadata()) {
				@Override public Object itemFor(AttrGraphMLMetadata<DirectedGraph<Node, Arc>> meta) {
					return meta.transformerUntyped().transform(XMLGraph.this);
				}
			};
		}
		return graph_attr;
	}

	public Set<String> getVertexAttributeNames() {
		return reader.getVertexAttrMetadata().keySet();
	}

	public Set<String> getEdgeAttributeNames() {
		return reader.getEdgeAttrMetadata().keySet();
	}

	/**
	** A {@link Set} view of all keys.
	**
	** @throws NullPointerException if {@link #setVertexPrimaryKey(String)} has
	**         not yet been called.
	*/
	public Set<K> keySet() {
		return nodes.keySet();
	}

	/**
	** @return as described; or {@code null} if the key is absent
	** @throws NullPointerException if {@link #setVertexPrimaryKey(String)} has
	**         not yet been called.
	*/
	public Node getVertex(K key) {
		return nodes.get(key);
	}

	/**
	** @return as described; or {@code null} if the key is absent
	** @throws NullPointerException if {@link #setDefaultVertexAttribute(String)}
	**         has not yet been called.
	*/
	public U getVertexAttribute(K key) {
		Node node = nodes.get(key);
		return node == null? null: node_attr.transform(nodes.get(key));
	}

	@SuppressWarnings("unchecked")
	public <V> V getVertexAttribute(K key, String attr) {
		Node node = nodes.get(key);
		return node == null? null: (V)reader.getVertexAttrMetadata().get(attr).transformerUntyped().transform(node);
	}

	/**
	** Return a map view of the given source key's successors, each mapped to
	** the attribute of the edge that connects it to the source key.
	**
	** @return as described; or {@code null} if the key is absent
	** @throws IllegalStateException if either the vertex primary key, or the
	**         default edge attribute, has not been set.
	*/
	public Map<K, W> getSuccessorMap(K key) {
		Node node = nodes.get(key);
		return node == null? null: new KeyAttrMap(vertices.get(node).getSecond());
	}

	/**
	** Provides a key-attribute view of a node-arc map. The key is that of the
	** node, and the attribute is that the arc.
	*/
	protected class KeyAttrMap extends AbstractMap<K, W> {

		final protected Map<Node, Arc> map;

		protected KeyAttrMap(Map<Node, Arc> map) {
			if (XMLGraph.this.arc_attr == null) { throw new IllegalStateException(EMSG_DEF_ED_ATTR); }
			if (XMLGraph.this.nodes == null) { throw new IllegalStateException(EMSG_VX_PRIM_KEY); }
			this.map = map;
		}

		@Override public int size() {
			return map.size();
		}

		@Override public boolean containsKey(Object o) {
			return map.containsKey(XMLGraph.this.nodes.get(o));
		}

		@Override public boolean containsValue(Object o) {
			for (Arc edge: map.values()) {
				if (XMLGraph.this.arc_attr.transform(edge).equals(o)) { return true; }
			}
			return false;
		}

		@Override public W get(Object o) {
			return XMLGraph.this.arc_attr.transform(map.get(XMLGraph.this.nodes.get(o)));
		}

		@Override public W put(K key, W val) {
			throw new UnsupportedOperationException(EMSG_IMMUTABLE);
		}

		@Override public W remove(Object o) {
			throw new UnsupportedOperationException(EMSG_IMMUTABLE);
		}

		@Override public void putAll(Map<? extends K, ? extends W> map) {
			throw new UnsupportedOperationException(EMSG_IMMUTABLE);
		}

		@Override public void clear() {
			throw new UnsupportedOperationException(EMSG_IMMUTABLE);
		}

		private transient Set<Map.Entry<K, W>> entries;
		@Override public Set<Map.Entry<K, W>> entrySet() {
			if (entries == null) {
				entries = new AbstractSet<Map.Entry<K, W>>() {

					final private Iterable<Map.Entry<K, W>> ib = new
					ProxyIterable<Map.Entry<Node, Arc>, Map.Entry<K, W>>(map.entrySet()) {
						@Override public Map.Entry<K, W> nextFor(final Map.Entry<Node, Arc> en) {
							return Maps.immutableEntry(
							  XMLGraph.this.node_id.transform(en.getKey()),
							  XMLGraph.this.arc_attr.transform(en.getValue())
							);
						}
					};

					@Override public int size() {
						return map.size();
					}

					@Override public void clear() {
						throw new UnsupportedOperationException(EMSG_IMMUTABLE);
					}

					@Override public boolean contains(Object o) {
						if (!(o instanceof Map.Entry)) { return false; }
						@SuppressWarnings("unchecked") Map.Entry<K, W> en = (Map.Entry<K, W>)o;
						Node key = XMLGraph.this.nodes.get(en.getKey());
						if (key == null) { return false; }
						W testval = en.getValue();

						W val = XMLGraph.this.arc_attr.transform(map.get(key));
						return val == null? testval == null && map.containsKey(key): val.equals(testval);
					}

					@Override public boolean remove(Object o) {
						throw new UnsupportedOperationException(EMSG_IMMUTABLE);
					}

					@Override public Iterator<Map.Entry<K, W>> iterator() {
						return ib.iterator();
					}

				};
			}
			return entries;
		}

	}

	final private static long serialVersionUID = 5150455211806503192L;
	final private static String EMSG_IMMUTABLE = "XMLGraph only supports readable graphs at present.";
	final private static String EMSG_VX_PRIM_KEY = "No vertex primary key set.";
	final private static String EMSG_DEF_ED_ATTR = "No default edge attribute set.";
	final private static String EMSG_ALREADY_LOADED = "Graph already loaded.";

	public static class Node {
		@Override public String toString() { return "-o-"; }
	}

	public static class Arc {
		@Override public String toString() { return "-->"; }
	}

}
