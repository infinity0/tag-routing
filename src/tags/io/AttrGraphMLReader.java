// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.io;

import edu.uci.ics.jung.io.GraphMLReader;
import edu.uci.ics.jung.io.GraphMLMetadata;
import edu.uci.ics.jung.graph.Hypergraph;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.BidiMap;
import org.apache.commons.collections15.bidimap.DualHashBidiMap;
import org.apache.commons.collections15.bidimap.UnmodifiableBidiMap;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.SAXException;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;

/**
** Extension of {@link GraphMLReader} which has extra methods for exploring
** {@code <key>} elements (ie. attributes) of GraphML.
**
** @see http://graphml.graphdrawing.org/primer/graphml-primer.html#AttributesDefinition
*/
public class AttrGraphMLReader<G extends Hypergraph<V, E>, V, E> extends GraphMLReader<G, V, E> {

	public static enum AttrType { BOOLEAN, INT, LONG, FLOAT, DOUBLE, STRING }

	final protected BidiMap<String, String> keyname = new DualHashBidiMap<String, String>();
	final protected Map<String, AttrType> keytype = new HashMap<String, AttrType>();

	final protected BidiMap<String, String> _keyname = UnmodifiableBidiMap.decorate(keyname);
	final protected Map<String, AttrType> _keytype = Collections.unmodifiableMap(keytype);

	final static public Map<String, AttrType> attr_type;
	final static public Map<AttrType, Class<?>> attr_class;

	static {
		Map<String, AttrType> tmp1 = new HashMap<String, AttrType>();
		tmp1.put("boolean", AttrType.BOOLEAN);
		tmp1.put("int", AttrType.INT);
		tmp1.put("long", AttrType.LONG);
		tmp1.put("float", AttrType.FLOAT);
		tmp1.put("double", AttrType.DOUBLE);
		tmp1.put("string", AttrType.STRING);
		attr_type = Collections.unmodifiableMap(tmp1);

		Map<AttrType, Class<?>> tmp2 = new EnumMap<AttrType, Class<?>>(AttrType.class);
		tmp2.put(AttrType.BOOLEAN, boolean.class);
		tmp2.put(AttrType.INT, int.class);
		tmp2.put(AttrType.LONG, long.class);
		tmp2.put(AttrType.FLOAT, float.class);
		tmp2.put(AttrType.DOUBLE, double.class);
		tmp2.put(AttrType.STRING, String.class);
		attr_class = Collections.unmodifiableMap(tmp2);
	}

	public AttrGraphMLReader(Factory<V> vertex_factory, Factory<E> edge_factory) throws ParserConfigurationException, SAXException {
		super(vertex_factory, edge_factory);
	}

	public AttrGraphMLReader() throws ParserConfigurationException, SAXException {
		super();
	}

	@Override protected void createKey(Attributes atts) throws SAXNotSupportedException {
		Map<String, String> key_atts = getAttributeMap(atts);
		String id = key_atts.get("id");
		String name = key_atts.remove("attr.name");
		String type = key_atts.remove("attr.type");

		if (keyname.containsKey(id) || keyname.containsValue(name)) {
			throw new SAXNotSupportedException("duplicate key " + id + ":" + name);
		}

		keyname.put(id, name);
		AttrType t = attr_type.get(type);

		assert !keytype.containsKey(id);
		keytype.put(id, t == null? AttrType.STRING: t);

		super.createKey(atts);
	}

	/**
	** Returns a bidirectional map relating key IDs and key attribute-names.
	*/
	public BidiMap<String, String> getKeyNames() {
		return _keyname;
	}

	/**
	** Returns a map from key ID to attribute-type.
	*/
	public Map<String, AttrType> getKeyTypes() {
		return _keytype;
	}

	// public AttrGraphMLMetadata<G, ?> getGraphMetadata(String key);
	// public AttrGraphMLMetadata<V, ?> getVertexMetadata(String key);
	// public AttrGraphMLMetadata<E, ?> getEdgeMetadata(String key);

}
