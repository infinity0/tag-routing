// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.proto.PTable;
import tags.proto.TGraph;
import tags.proto.Index;
import tags.util.Union.U2;
import tags.util.Maps.U2Map;
import java.util.Map;

import java.io.IOException;

/**
** An interface to the underlying storage network.
**
** All methods specified here should block until the operation is complete.
**
** @param <I> Type of identity
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
** @param <Z> Type of identity-score
*/
public interface StoreControl<I, T, A, U, W, S, Z> {

	/**
	** Retrieves the immediate trusted neighbours of a given identity, each
	** mapped to their score rating.
	**
	** @throws IOException there was an error retrieving the data (which may
	**         or may not indicate that the data does not exist).
	*/
	public Map<I, Z> getFriends(I id) throws IOException;

	/**
	** Retrieves the {@link PTable} for a given identity.
	**
	** @throws IOException there was an error retrieving the data (which may
	**         or may not indicate that the data does not exist).
	*/
	public PTable<A, S> getPTable(I id) throws IOException;

	/**
	** Retrieves the out-neighbours (and the weights of the out-arcs) for a
	** given source tag, in the given {@link TGraph}.
	**
	** @param addr Address of the {@link TGraph}.
	** @return {@code null} if the given node is not present in the data
	**         structure
	** @throws IOException there was an error retrieving the data (which may
	**         or may not indicate that the data does not exist).
	*/
	public U2Map<T, A, W> getTGraphOutgoing(A addr, T src) throws IOException;

	/**
	** Retrieves the node-attribute for a given tag or tgraph, in the given
	** {@link TGraph}.
	**
	** @param addr Address of the {@link TGraph}.
	** @return {@code null} if the given node is not present in the data
	**         structure
	** @throws IOException there was an error retrieving the data (which may
	**         or may not indicate that the data does not exist).
	*/
	public U getTGraphNodeAttr(A addr, U2<T, A> node) throws IOException;

	/**
	** Retrieves the out-neighbours (and the weights of the out-arcs) for a
	** given source tag, in the given {@link Index}.
	**
	** @param addr Address of the {@link Index}.
	** @return {@code null} if the given node is not present in the data
	**         structure
	** @throws IOException there was an error retrieving the data (which may
	**         or may not indicate that the data does not exist).
	*/
	public U2Map<A, A, W> getIndexOutgoing(A addr, T src) throws IOException;

}
