// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.util.Maps;

import tags.proto.PTable;
import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;

import tags.io.TypedXMLGraph;
import org.apache.commons.collections15.map.ReferenceMap;
import java.lang.ref.SoftReference;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.CRC32;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
** A {@link StoreControl} that reads from a repository of GraphML files.
**
** Format: DOCUMENT
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

	final protected TypedXMLGraph<T_PTB, N, U, S> socnet;
	final protected ReferenceMap<N, Map<T_PTB, Map<N, S>>> idsuccs;
	final protected ReferenceMap<N, TypedXMLGraph<T_TGR, N, U, W>> tgraphs;
	final protected ReferenceMap<N, TypedXMLGraph<T_IDX, N, U, W>> indexes;
	final protected Map<N, DirectoryTGraph<N, U, W>> tgraphs_unwrap;
	final protected Map<N, DirectoryIndex<N, W>> indexes_unwrap;

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

		this.socnet = makeTypedXMLGraph(T_PTB.class);
		this.socnet.load(new File(basedir, FILE_PTB));
		this.socnet.setVertexPrimaryKey(NODE_ID);
		this.socnet.setDefaultEdgeAttribute(ARC_ATTR);

		this.idsuccs = new ReferenceMap<N, Map<T_PTB, Map<N, S>>>();
		this.tgraphs = new ReferenceMap<N, TypedXMLGraph<T_TGR, N, U, W>>();
		this.indexes = new ReferenceMap<N, TypedXMLGraph<T_IDX, N, U, W>>();
		this.tgraphs_unwrap = new HashMap<N, DirectoryTGraph<N, U, W>>();
		this.indexes_unwrap = new HashMap<N, DirectoryIndex<N, W>>();
	}

	@Override public Map<N, S> getFriends(N id) throws IOException {
		return getIDSuccs(id).get(T_PTB.z);
	}

	@Override public PTable<N, S> getPTable(N id) throws IOException {
		Map<T_PTB, Map<N, S>> out = getIDSuccs(id);
		return new PTable<N, S>(out.get(T_PTB.g), out.get(T_PTB.h));
	}

	@Override public U2Map<N, N, W> getTGraphOutgoing(N addr, N src) throws IOException {
		DirectoryTGraph<N, U, W> tgru = getTGraphUnwrap(addr);
		if (tgru != null) { return tgru.getOutgoing(src); }

		TypedXMLGraph<T_TGR, N, U, W> tgr = getTGraph(addr);
		Map<T_TGR, Map<N, W>> out = tgr.getSuccessorTypedMap(src);
		if (out == null) { return null; } // can't use ?: due to shitty broken type inference
		return Maps.uniteDisjoint(out.get(T_TGR.t), out.get(T_TGR.g));
	}

	@Override public U getTGraphNodeAttr(N addr, U2<N, N> node) throws IOException {
		DirectoryTGraph<N, U, W> tgru = getTGraphUnwrap(addr);
		if (tgru != null) { return tgru.getNodeAttr(node.isT0()? node.getT0(): node.getT1()); }

		TypedXMLGraph<T_TGR, N, U, W> tgr = getTGraph(addr);
		return tgr.getVertexAttribute(node.isT0()? node.getT0(): node.getT1());
	}

	@Override public U2Map<N, N, W> getIndexOutgoing(N addr, N src) throws IOException {
		DirectoryIndex<N, W> idxu = getIndexUnwrap(addr);
		if (idxu != null) { return idxu.getOutgoing(src); }

		TypedXMLGraph<T_IDX, N, U, W> idx = getIndex(addr);
		Map<T_IDX, Map<N, W>> out = idx.getSuccessorTypedMap(src);
		if (out == null) { return null; } // can't use ?: due to shitty broken type inference
		return Maps.uniteDisjoint(out.get(T_IDX.d), out.get(T_IDX.h));
	}

	protected Map<T_PTB, Map<N, S>> getIDSuccs(N id) throws IOException {
		Map<T_PTB, Map<N, S>> idsucc = idsuccs.get(id);
		if (idsucc == null) {
			idsucc = new EnumMap<T_PTB, Map<N, S>>(socnet.getSuccessorTypedMap(id));
			if (idsucc == null) { throw new IOException("identity " + id + " not found"); }
			// the below is a little hack due to the fact that ids point to themselves,
			// but this mapping should belong to the ptable rather than the friend table
			Map<N, S> map_z = new HashMap<N, S>(idsucc.get(T_PTB.z));
			Map<N, S> map_g = new HashMap<N, S>(idsucc.get(T_PTB.g));
			Map<N, S> map_h = new HashMap<N, S>(idsucc.get(T_PTB.h));
			map_h.put(id, map_z.remove(id));
			idsucc.put(T_PTB.z, map_z);
			idsucc.put(T_PTB.g, map_g);
			idsucc.put(T_PTB.h, map_h);
			idsuccs.put(id, idsucc);
		}
		//System.err.println(idsucc);
		return idsucc;
	}

	/**
	** @return {@code null} if the TGraph doesn't exist as a DirectoryTGraph
	*/
	protected DirectoryTGraph<N, U, W> getTGraphUnwrap(N addr) throws IOException {
		DirectoryTGraph<N, U, W> tgr = tgraphs_unwrap.get(addr);
		if (tgr == null) {
			File base = new File(dir_tgr, addr.toString());
			if (!base.isDirectory()) { return null; }
			tgr = new DirectoryTGraph<N, U, W>(base);
			tgraphs_unwrap.put(addr, tgr);
		}
		return tgr;
	}

	/**
	** @return {@code null} if the TGraph doesn't exist as a DirectoryTGraph
	*/
	protected DirectoryIndex<N, W> getIndexUnwrap(N addr) throws IOException {
		DirectoryIndex<N, W> idx = indexes_unwrap.get(addr);
		if (idx == null) {
			File base = new File(dir_idx, addr.toString());
			if (!base.isDirectory()) { return null; }
			idx = new DirectoryIndex<N, W>(base);
			System.out.println("loaded index at :" + addr);
			indexes_unwrap.put(addr, idx);
		}
		return idx;
	}

	/**
	** @throws IOException if the TGraph doesn't exist as a TypedXMLGraph
	*/
	protected TypedXMLGraph<T_TGR, N, U, W> getTGraph(N addr) throws IOException {
		TypedXMLGraph<T_TGR, N, U, W> graph = tgraphs.get(addr);
		if (graph == null) {
			try {
				graph = makeTypedXMLGraph(T_TGR.class);
				graph.load(GZIPReader(new File(dir_tgr, addr.toString() + ".graphmlz")));
				graph.setVertexPrimaryKey(NODE_ID);
				graph.setDefaultVertexAttribute(NODE_ATTR);
				graph.setDefaultEdgeAttribute(ARC_ATTR);
			} catch (RuntimeException e) {
				throw new IOException("couldn't initialise tgraph: " + addr, e);
			}
			tgraphs.put(addr, graph);
		}
		return graph;
	}

	/**
	** @throws IOException if the TGraph doesn't exist as a TypedXMLGraph
	*/
	protected TypedXMLGraph<T_IDX, N, U, W> getIndex(N addr) throws IOException {
		TypedXMLGraph<T_IDX, N, U, W> graph = indexes.get(addr);
		if (graph == null) {
			try {
				graph = makeTypedXMLGraph(T_IDX.class);
				graph.load(GZIPReader(new File(dir_idx, addr.toString() + ".graphmlz")));
				graph.setVertexPrimaryKey(NODE_ID);
				graph.setDefaultEdgeAttribute(ARC_ATTR);
			} catch (RuntimeException e) {
				throw new IOException("couldn't initialise index: " + addr, e);
			}
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
	public enum T_TGR_UNW { w, t, g }
	public enum T_IDX_UNW { d, h }

	public static Reader GZIPReader(File fn) throws IOException {
		return new InputStreamReader(new GZIPInputStream(new FileInputStream(fn)));
	}

	public static <T extends Enum<T>, N, U, W> TypedXMLGraph<T, N, U, W> makeTypedXMLGraph(
	  Class<T> typecl
	) throws IOException {
		try {
			return new ProtoTypedXMLGraph<T, N, U, W>(typecl);
		} catch (ParserConfigurationException e) {
			throw new IOException("Error creating new TypedXMLGraph", e);
		} catch (SAXException e) {
			throw new IOException("Error creating new TypedXMLGraph", e);
		}
	}

	public static class DirectoryIndex<N, W> extends DirectoryContainer<N, W> {

		protected SoftReference<Set<N>> node_set;

		public DirectoryIndex(File base) throws IOException {
			super(base);
			this.node_set = new SoftReference<Set<N>>(null);
		}

		protected Set<N> getNodeSet() throws IOException {
			Set<N> nset = node_set.get();
			if (nset == null) {
				nset = new HashSet<N>(this.<List<N>>cast(parseJSON("nodes", true)));
				if (nset == null) { throw new IOException("node set doesn't exist"); }
				node_set = new SoftReference<Set<N>>(nset);
			}
			return nset;
		}

		public U2Map<N, N, W> getOutgoing(N src) throws IOException {
			if (!getNodeSet().contains(src)) { return null; }
			return super.getOutgoing(src, T_IDX_UNW.d.ordinal(), T_IDX_UNW.h.ordinal());
		}

	}

	public static class DirectoryTGraph<N, U, W> extends DirectoryContainer<N, W> {

		protected SoftReference<Map<N, U>> node_map;

		public DirectoryTGraph(File base) throws IOException {
			super(base);
			this.node_map = new SoftReference<Map<N, U>>(null);
		}

		public U getNodeAttr(N src) throws IOException {
			U attr = getNodeMap().get(src);
			//System.out.println("loaded " + src + " in " + base + ": " + attr);
			return attr;
			//List tuple = getTuple(src);
			//System.out.println("loaded " + src + " in " + base + ": " + (tuple==null?"null":"non-null"));
			//return tuple == null? null: (U)tuple.get(T_TGR_UNW.w.ordinal());
		}

		protected Map<N, U> getNodeMap() throws IOException {
			Map<N, U> nmap = node_map.get();
			if (nmap == null) {
				nmap = cast(parseJSON("nodes", true));
				if (nmap == null) { throw new IOException("node map doesn't exist"); }
				node_map = new SoftReference<Map<N, U>>(nmap);
			}
			return nmap;
		}

		public U2Map<N, N, W> getOutgoing(N src) throws IOException {
			if (!getNodeMap().containsKey(src)) { return null; }
			return super.getOutgoing(src, T_TGR_UNW.t.ordinal(), T_TGR_UNW.g.ordinal());
		}

	}

	public static class DirectoryContainer<N, W> {

		final public File base;
		final protected ReferenceMap<String, Map<String, List>> buckets;

		final protected long mask;
		final protected String fmtstr;
		final protected Map attributes;

		public DirectoryContainer(File base) throws IOException {
			if (!base.isDirectory()) {
				throw new IllegalArgumentException("not a directory: " + base);
			}
			this.base = base;
			this.buckets = new ReferenceMap<String, Map<String, List>>();
			this.attributes = cast(parseJSON("attributes", false));
			this.mask = this.<Long>cast(attributes.remove("mask"));
			this.fmtstr = "%0" + Long.toHexString(mask).length() + "x";
		}

		protected U2Map<N, N, W> getOutgoing(N src, int i_map0, int i_map1) throws IOException {
			List tuple = getTuple(src);
			if (tuple == null) { return null; }
			Map<N, W> out_t = cast(tuple.get(i_map0));
			//System.out.println(out_t.keySet());
			Map<N, W> out_g = cast(tuple.get(i_map1));
			return Maps.uniteDisjoint(out_t, out_g);
		}

		protected Object parseJSON(String addr, boolean ignore) throws IOException {
			try {
				File fn = new File(base, addr+EXT);
				if (!fn.exists() && ignore) { return new HashMap(); }
				return JSON.get().parse(GZIPReader(fn));
			} catch (ParseException e) {
				throw badData(e);
			} catch (ClassCastException e) {
				throw badData(e);
			}
		}

		protected List getTuple(N src) throws IOException {
			String tag = src.toString();
			CRC32 crc = new CRC32();
			crc.update(tag.getBytes());
			String bid = String.format(fmtstr, crc.getValue()&mask);

			Map<String, List> bucket = buckets.get(bid);
			if (bucket == null) {
				bucket = cast(parseJSON(bid, true));
				buckets.put(bid, bucket);
			}
			return bucket.get(tag);
		}

		final protected String EXT = ".json.gz";

		final protected static ThreadLocal<JSONParser> JSON = new ThreadLocal<JSONParser>() {
			@Override protected synchronized JSONParser initialValue() {
				return new JSONParser();
			}
		};

		/**
		** @throws IOException if the cast failed
		*/
		@SuppressWarnings("unchecked")
		final protected static <T> T cast(Object o) throws IOException {
			try {
				return (T)o;
			} catch (ClassCastException e) {
				throw badData(e);
			}
		}

		final protected static IOException badData(Exception e) {
			return new IOException("bad data format", e);
		}

	}

}
