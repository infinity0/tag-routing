// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.proto.PTable;
import tags.proto.TGraph;
import tags.proto.Index;

import java.util.Map;
import java.util.concurrent.Executor;

/**
** DOCUMENT.
**
** @param <I> Type of identity
** @param <T> Type of tag
** @param <A> Type of address
** @param <U> Type of node-attribute
** @param <W> Type of arc-attribute
** @param <S> Type of score
** @param <R> Type of identity-score
*/
public interface StoreControl<I, T, A, U, W, S, R> {

	/**
	** Retrieves the friends (and their score-ratings) of a given identity.
	*/
	public Map<I, R> getFriends(I id);

	/**
	** Retrieves the {@link PTable} for a given identity.
	*/
	public PTable<A, S> getPTable(I id);

	/**
	** Retrieves the out-neighbours (and the weights of the out-arcs) for a
	** given source tag, in the given {@link TGraph}.
	**
	** @param addr Address of the {@link TGraph}.
	*/
	public Map<T, W> getTGraphOutgoing(A addr, T src);

	/**
	** Retrieves the weight for a given tag, in the given {@link TGraph}.
	*/
	public U getTGraphTagAttr(A addr, T tag);

	/**
	** Retrieves the out-neighbours (and the weights of the out-arcs) for a
	** given source tag, in the given {@link Index}.
	**
	** @param addr Address of the {@link Index}.
	*/
	public Map<A, W> getIndexOutgoing(A addr, T src);

}
