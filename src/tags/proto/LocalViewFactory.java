// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

/**
** Factory for making local views of remote data structures.
*/
public interface LocalViewFactory<R, L> {

	/**
	** Creates a new empty local view of the given remote data structure.
	*/
	public L createLocalView(R addr, DataSources<R, L> src);

}
