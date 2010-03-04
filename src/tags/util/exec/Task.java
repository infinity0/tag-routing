// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

/**
** An object representing a task.
**
** @param <K> Type of task parameter(s)
*/
public interface Task<K> {

	/**
	** Returns the task parameter(s).
	*/
	public K getKey();

	/**
	** Whether this task has completed (either successfully or abruptly).
	*/
	public boolean isDone();

}
