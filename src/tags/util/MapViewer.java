// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util;

import java.util.Map;

/**
** A class for viewing objects as maps.
**
** @param <T> Type of object to view as a map
** @param <M> Type of map
*/
public interface MapViewer<T, M extends Map> {

	/**
	** Returns a map view of the given object (or part of it).
	*/
	public M mapFor(T obj);

}
