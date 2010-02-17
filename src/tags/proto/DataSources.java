// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.proto;

/**
** Graph of remote data sources that point to each other. DOCUMENT.
**
** @param <R> Type of remote address
** @param <L> Type of local view
*/
public interface DataSources<R, L> {

	/**
	** Returns the set of seed sources, each mapped to its local view.
	*/
	public Map<R, L> getSeeds();

	/**
	** Set an outgoing arc for the given pair of sources.
	*/
	public void setOutgoing(R src, R dst);

	/**
	** Returns all sources that point to the given source.
	*/
	public Set<R> getIncoming(R src);

	/**
	** Returns a map of sources to their incoming sources.
	*/
	public Map<R, Set<R>> getIncoming();

	/**
	** Mark a data source as being in use.
	**
	** TODO maybe make this call inferScore() or something.
	*/
	public void useSource(R src);

}
