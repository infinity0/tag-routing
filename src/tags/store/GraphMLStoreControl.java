// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.proto.PTable;
import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.Map;

import tags.io.XMLGraph;
import org.apache.commons.collections15.map.ReferenceMap;

import java.io.File;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
** DOCUMENT.
**
** @param <I> Type of identity
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
** @param <Z> Type of identity-score
*/
public class GraphMLStoreControl<I, T, A, U, W, S, Z> implements StoreControl<I, T, A, U, W, S, Z> {

	final public static String NODE_ID = "id";
	final public static String NODE_ATTR = "height";
	final public static String ARC_ATTR = "weight";

	final public static String FILE_PTB = "ptb.graphml";
	final public static String DIR_IDX = "idx";
	final public static String DIR_TGR = "tgr";

	final public File basedir;

	final protected XMLGraph<I, Double, Double> ptables;
	final protected ReferenceMap<String, XMLGraph<T, Double, Double>> tgraphs;
	final protected ReferenceMap<String, XMLGraph<A, Double, Double>> indexes;

	/**
	** @see #GraphMLStoreControl(File)
	*/
	public GraphMLStoreControl(String basedir) {
		this(new File(basedir));
	}

	/**
	** @throws IllegalArgumentException if {@code basedir} is not a directory
	*/
	public GraphMLStoreControl(File basedir) {
		if (!basedir.isDirectory()) {
			throw new IllegalArgumentException("not a directory: " + basedir);
		}
		this.basedir = basedir;

		this.ptables = null;
		this.tgraphs = null;
		this.indexes = null;
	}

	public Map<I, Z> getFriends(I id) throws IOException {
		File fp = getFile(id + ".fr");
		throw new UnsupportedOperationException("not implemented");
	}

	public PTable<A, S> getPTable(I id) throws IOException {
		File fp = getFile(id + ".ptab");
		throw new UnsupportedOperationException("not implemented");
	}

	public U2Map<T, A, W> getTGraphOutgoing(A addr, T src) throws IOException {
		File fp = getFile(addr + ".tgr");
		throw new UnsupportedOperationException("not implemented");
	}

	public U getTGraphNodeAttr(A addr, U2<T, A> node) throws IOException {
		File fp = getFile(addr + ".tgr");
		throw new UnsupportedOperationException("not implemented");
	}

	public U2Map<A, A, W> getIndexOutgoing(A addr, T src) throws IOException {
		File fp = getFile(addr + ".idx");
		throw new UnsupportedOperationException("not implemented");
	}

	protected File getFile(String s) {
		return new File(basedir, s);
	}

}
