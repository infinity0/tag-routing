// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

/**
** Utilities for {@link Task}s.
*/
final public class Tasks {

	private Tasks() { }

	public static <K> Task<K> newTask(K key) {
		return new BaseTaskResult<K, Object, Exception>(key);
	}

	public static class BaseTaskResult<K, V, X extends Exception> implements TaskResult<K, V, X> {

		final public K key;
		protected V val;
		protected X err;
		protected boolean done;

		public BaseTaskResult(K key) {
			this.key = key;
		}

		@Override public K getKey() {
			return key;
		}

		@Override public V getValue() throws X {
			if (err == null) {
				return val;
			} else {
				throw err;
			}
		}

		@Override public X getError() {
			return err;
		}

		@Override public boolean isDone() {
			return done;
		}

		public void setValue(V val) {
			if (done) { throw new IllegalStateException("already done"); }
			this.val = val;
			done = true;
		}

		public void setError(X err) {
			if (done) { throw new IllegalStateException("already done"); }
			this.err = err;
			done = true;
		}

	}

}
