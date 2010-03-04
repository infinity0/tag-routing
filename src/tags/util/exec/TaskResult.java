// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

/**
** An object representing a task and its pending result. This is meant to be a
** lighter version of {@link java.util.concurrent.Future}.
**
** @param <K> Type of task parameter(s)
** @param <V> Type of task result(s)
** @param <X> Type of exception(s) thrown
*/
public interface TaskResult<K, V, X extends Exception> extends Task<K> {

	/**
	** Returns the task result(s).
	*/
	public V getValue() throws X;

	/**
	** Returns the error that caused the task to complete abruptly, or {@code
	** null} if the task completed successfully.
	*/
	public X getError();

}
