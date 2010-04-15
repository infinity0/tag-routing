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
import tags.util.CompositeIterable;
import tags.util.CompositeMap;

import java.util.Iterator;
import java.util.Set;
import java.util.Map;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.HashMap;

import java.io.Reader;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
** A {@link DirectedGraph} backed by a GraphML file, with methods to retrieve
** the attributes defined there.
**
** @author infinity0
** @param <K> Type of primary key
** @param <U> Type of default node attribute
** @param <W> Type of default arc attribute
*/
public class GraphMLFile<K, U, W> extends DirectedSparseGraph<GraphMLFile.Node, GraphMLFile.Arc> {

	final private static long serialVersionUID = 5150455211806503192L;
	final private static String ERR_IMMUTABLE = "GraphMLFile only supports readable graphs at present.";

	final protected AttrGraphMLReader<DirectedGraph<Node, Arc>, Node, Arc> reader;
	protected boolean open; // set true to allow reader to proceed

	protected Map<K, Node> nodes;
	protected Transformer<Node, K> node_id;
	protected String vertex_pkey;
	protected Transformer<Node, U> node_attr;
	protected Transformer<Arc, W> arc_attr;

	/**
	** Private constructor; sets up a reader object with the default settings.
	*/
	private GraphMLFile() throws ParserConfigurationException, SAXException {
		this.reader = new AttrGraphMLReader<DirectedGraph<Node, Arc>, Node, Arc>(
			FactoryUtils.instantiateFactory(Node.class), FactoryUtils.instantiateFactory(Arc.class)
		);
	}

	/**
	** Create a new graph from the given file.
	**
	** @param fn The path of the file.
	*/
	public GraphMLFile(String fn) throws IOException, ParserConfigurationException, SAXException {
		this();
		open = true; this.reader.load(fn, this); open = false;
	}

	public GraphMLFile(Reader rd) throws IOException, ParserConfigurationException, SAXException {
		this();
		open = true; this.reader.load(rd, this); open = false;
	}

	@Override public boolean addEdge(Arc edge, Pair<? extends Node> endpoints, EdgeType edgeType) {
		if (open) { return super.addEdge(edge, endpoints, edgeType); }
		throw new UnsupportedOperationException(ERR_IMMUTABLE);
	}

	@Override public boolean addVertex(Node vertex) {
		if (open) { return super.addVertex(vertex); }
		throw new UnsupportedOperationException(ERR_IMMUTABLE);
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
		this.vertex_pkey = attr;
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
			graph_attr = new CompositeMap<String, AttrGraphMLMetadata<DirectedGraph<Node, Arc>>, Object>(
			  reader.getGraphAttrMetadata()) {
				@Override public Object itemFor(AttrGraphMLMetadata<DirectedGraph<Node, Arc>> meta) {
					return meta.transformerUntyped().transform(GraphMLFile.this);
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
	** @throws NullPointerException if {@link #setVertexPrimaryKey(String)} has
	**         not yet been called.
	*/
	public Node getVertex(K key) {
		return nodes.get(key);
	}

	/**
	** @throws NullPointerException if {@link #setDefaultVertexAttribute(String)}
	**         has not yet been called.
	*/
	public U getVertexAttribute(K key) {
		return node_attr.transform(nodes.get(key));
	}

	@SuppressWarnings("unchecked")
	public <V> V getVertexAttribute(K key, String attr) {
		return (V)reader.getVertexAttrMetadata().get(attr).transformerUntyped().transform(nodes.get(key));
	}

	/**
	** Returns a map view of the given source key's successors, each one being
	** mapped to the attribute of the edge that connects it to the source key.
	**
	** @throws NullPointerException if {@link #setDefaultEdgeAttribute(String)}
	**         has not yet been called.
	*/
	public Map<K, W> getSuccessorMap(K key) {
		if (arc_attr == null) { throw new NullPointerException(); }
		final Map<Node, Arc> successors = vertices.get(nodes.get(key)).getSecond();
		return new AbstractMap<K, W>() {

			@Override public int size() {
				return successors.size();
			}

			@Override public boolean containsKey(Object o) {
				return successors.containsKey(nodes.get(o));
			}

			@Override public boolean containsValue(Object o) {
				for (Arc edge: successors.values()) {
					if (arc_attr.transform(edge).equals(o)) { return true; }
				}
				return false;
			}

			@Override public W get(Object o) {
				return arc_attr.transform(successors.get(nodes.get(o)));
			}

			@Override public W put(K key, W val) {
				throw new UnsupportedOperationException(ERR_IMMUTABLE);
			}

			@Override public W remove(Object o) {
				throw new UnsupportedOperationException(ERR_IMMUTABLE);
			}

			@Override public void putAll(Map<? extends K, ? extends W> map) {
				throw new UnsupportedOperationException(ERR_IMMUTABLE);
			}

			@Override public void clear() {
				throw new UnsupportedOperationException(ERR_IMMUTABLE);
			}

			private transient Set<Map.Entry<K, W>> entries;
			@Override public Set<Map.Entry<K, W>> entrySet() {
				if (entries == null) {
					entries = new AbstractSet<Map.Entry<K, W>>() {

						final private Iterable<Map.Entry<K, W>> ib = new
						CompositeIterable<Map.Entry<Node, Arc>, Map.Entry<K, W>>(successors.entrySet()) {
							@Override public Map.Entry<K, W> nextFor(final Map.Entry<Node, Arc> en) {
								return new Maps.AbstractEntry<K, W>(node_id.transform(en.getKey())) {
									@Override public W getValue() { return arc_attr.transform(en.getValue()); }
								};
							}
						};

						@Override public int size() {
							return successors.size();
						}

						@Override public void clear() {
							throw new UnsupportedOperationException(ERR_IMMUTABLE);
						}

						@Override public boolean contains(Object o) {
							if (!(o instanceof Map.Entry)) { return false; }
							@SuppressWarnings("unchecked") Map.Entry<K, W> en = (Map.Entry<K, W>)o;
							Node key = nodes.get(en.getKey());
							if (key == null) { return false; }
							W testval = en.getValue();

							W val = arc_attr.transform(successors.get(key));
							return val == null? testval == null && successors.containsKey(key): val.equals(testval);
						}

						@Override public boolean remove(Object o) {
							throw new UnsupportedOperationException(ERR_IMMUTABLE);
						}

						@Override public Iterator<Map.Entry<K, W>> iterator() {
							return ib.iterator();
						}

					};
				}
				return entries;
			}

		};
	}


	public static class Node {
		@Override public String toString() { return "o"; }
	}

	public static class Arc {
		@Override public String toString() { return "->"; }
	}

}
