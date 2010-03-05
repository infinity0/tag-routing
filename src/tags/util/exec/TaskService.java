// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

/**
** DOCUMENT.
**
** @param <K> Type of task parameter(s)
** @param <V> Type of task result(s)
** @param <X> Type of exception(s) thrown
*/
public interface TaskService<K, V, X extends Exception> {

	/**
	** Whether there are submitted tasks still to be run.
	*/
	public boolean hasPending();

	/**
	** Whether there are completed tasks that have not been reclaimed.
	*/
	public boolean hasComplete();

	/**
	** Submit a task for execution, blocking if necessary.
	*/
	public void submit(Task<K> task);

	/**
	** Returns a completed task and its result, blocking if no such tasks exist.
	*/
	public TaskResult<K, V, X> reclaim();

	/**
	** Stop accepting submissions. Pending tasks are still run, and completed
	** tags are still held until they have been reclaimed.
	*/
	public void close();

}
