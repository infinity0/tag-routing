// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.proto.PTable;
import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;

import java.io.File;
import java.io.IOException;

/**
** A {@link StoreControl} that stores all its data in as standard Java data
** structure objects in main memory.
**
** @param <I> Type of identity
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
** @param <Z> Type of identity-score
*/
public class RAMStoreControl<I, T, A, U, W, S, Z> implements StoreControl<I, T, A, U, W, S, Z> {

	final public Map<A, Set<T>> map_doc = new HashMap<A, Set<T>>();
	final public Map<T, Set<A>> map_tag = new HashMap<T, Set<A>>();

	final public Map<I, Map<I, Z>> map_frn = new HashMap<I, Map<I, Z>>();
	final public Map<I, PTable<A, S>> map_ptb = new HashMap<I, PTable<A, S>>();
	final public Map<A, Map<T, U2Map<T, A, W>>> map_tgr = new HashMap<A, Map<T, U2Map<T, A, W>>>();
	final public Map<A, U2Map<T, A, U>> map_tgr_node = new HashMap<A, U2Map<T, A, U>>();
	final public Map<A, Map<T, U2Map<A, A, W>>> map_idx = new HashMap<A, Map<T, U2Map<A, A, W>>>();

	public RAMStoreControl() { }

	public String getSummary() {
		return "" + map_doc.size() + " documents, " + map_tag.size() + " tags, " + map_frn.size() + " ids, " + map_tgr.size() + " tgraphs, " + map_idx.size() + " indexes.";
	}

	@Override public Map<I, Z> getFriends(I id) throws IOException {
		Map<I, Z> frn;

		frn = map_frn.get(id);
		if (frn == null) { throw new IOException("friend-list not available for: " + id); }
		return frn;
	}

	@Override public PTable<A, S> getPTable(I id) throws IOException {
		PTable<A, S> ptb;

		ptb = map_ptb.get(id);
		if (ptb == null) { throw new IOException("ptable not available for: " + id); }
		return ptb;
	}

	@Override public U2Map<T, A, W> getTGraphOutgoing(A addr, T src) throws IOException {
		Map<T, U2Map<T, A, W>> tgr;

		tgr = map_tgr.get(addr);
		if (tgr == null) { throw new IOException("tgraph not available for: " + addr); }
		return tgr.get(src);
	}

	@Override public U getTGraphNodeAttr(A addr, U2<T, A> node) throws IOException {
		U2Map<T, A, U> tgr_node;

		tgr_node = map_tgr_node.get(addr);
		if (tgr_node == null) { throw new IOException("tgraph not available for: " + addr); }
		return tgr_node.get(node);
	}

	@Override public U2Map<A, A, W> getIndexOutgoing(A addr, T src) throws IOException {
		Map<T, U2Map<A, A, W>> idx;

		idx = map_idx.get(addr);
		if (idx == null) { throw new IOException("index not available for: " + addr); }
		return idx.get(src);
	}

}
