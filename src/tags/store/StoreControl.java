// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.store;

import tags.proto.PTable;

import java.util.Map;

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
	public <I, R> Map<I, R> getFriends(I id);

	/**
	** Retrieves the {@link PTable} for a given identity.
	*/
	public <I> PTable<A, S> getPTable(I id);

	/**
	** Retrieves the out-neighbours (and the weights of the out-arcs) for a
	** given source tag, in the given {@link TGraph}.
	**
	** @param addr Address of the {@link TGraph}.
	*/
	public <T, A, W> Map<T, W> getTGraphOutgoing(A addr, T src);

	/**
	** Retrieves the weight for a given tag, in the given {@link TGraph}.
	*/
	public <T, A, U> U getTGraphTagAttr(A addr, T tag);

	/**
	** Retrieves the out-neighbours (and the weights of the out-arcs) for a
	** given source tag, in the given {@link Index}.
	**
	** @param addr Address of the {@link Index}.
	*/
	public <T, A, W> Map<A, W> getIndexOutgoing(A addr, T src);

}
