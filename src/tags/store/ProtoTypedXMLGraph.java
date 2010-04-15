// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.io.TypedXMLGraph;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
** A basic implementation of {@link TypedXMLGraph} suitable for the data format
** assumed by {@link GraphMLStoreControl}.
*/
public class ProtoTypedXMLGraph<T extends Enum<T>, K, U, W> extends TypedXMLGraph<T, K, U, W> {

	public ProtoTypedXMLGraph(Class<T> typecl) throws ParserConfigurationException, SAXException {
		super(typecl);
	}

	@Override public int getIDForString(String vid) {
		if (vid.charAt(0) != 'n') { throw new IllegalArgumentException("illegal vertex id"); }
		return Integer.parseInt(vid.substring(1));
	}

	@Override public String getAttributeNameForType(T type) {
		return "base_" + type.name();
	}

	final private static long serialVersionUID = -1328383656184693368L;

}
