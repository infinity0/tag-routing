// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

import java.util.Queue;
import java.util.LinkedList;

/**
** Quick 'n' dirty implementation of {@link TaskService} that executes tasks
** in the same thread as the caller. Mainly for use in test code.
**
** @param <K> Type of task parameter(s)
** @param <V> Type of task result(s)
** @param <X> Type of exception(s) thrown
*/
abstract public class UnthreadedTaskService<K, V, X extends Exception> implements TaskService<K, V, X> {

	final protected Queue<TaskResult<K, V, X>> queue = new LinkedList<TaskResult<K, V, X>>();

	@Override public boolean hasPending() {
		return !queue.isEmpty();
	}

	@Override public boolean hasComplete() {
		return !queue.isEmpty();
	}

	@Override public void submit(Task<K> task) {
		queue.add(execute(task));
	}

	@Override public TaskResult<K, V, X> reclaim() {
		return queue.remove();
	}

	@Override public void close() {
		// do nothing
		// TODO NORM have a "close state"
	}

	@SuppressWarnings("unchecked")
	protected TaskResult<K, V, X> execute(Task<K> task) {
		try {
			return Services.newTaskResult(task, getResultFor(task.getKey()), null);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			return Services.newTaskResult(task, null, (X)e);
		}
	}

	abstract protected V getResultFor(K key) throws X;

}
