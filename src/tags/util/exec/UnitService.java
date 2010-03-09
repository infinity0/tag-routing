// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutionException;

/**
** A service that runs a maximum of one concurrent job.
*/
public class UnitService<S> {

	protected Executor exec;

	protected S state;
	protected boolean active;
	protected int completed;

	volatile protected Exception last_ex;

	public UnitService(S state, Executor exec) {
		this.state = state;
		setExecutor(exec);
	}

	/**
	** @throws NullPointerException if {@code exec} is {@code null}
	*/
	public synchronized void setExecutor(Executor exec) {
		if (exec == null) { throw new NullPointerException(); }
		this.exec = exec;
	}

	public synchronized boolean isActive() {
		return active;
	}

	/**
	** Schedule the given job, with a state to transition into.
	**
	** @param run The job to run
	** @param next The state to transition into after the job is complete
	** @return An identifier for the job (increments for every job completed)
	** @throws IllegalStateException if a job is already running, or if the
	**         last job completed abruptly
	*/
	protected synchronized int execute(final Runnable run, final S next, final Iterable<DeferredMessage<?>> dmsg) {
		if (active) {
			throw new IllegalStateException("already running a job");
		}
		if (last_ex != null) {
			throw new IllegalStateException("previous job completed abruptly", last_ex);
		}
		exec.execute(new Runnable() {
			@Override public void run() {
				try {
					run.run();
				} catch (RuntimeException e) {
					last_ex = e; // TODO NORM make some better way of handling this
					throw e;
				}
				synchronized(UnitService.this) {
					state = next;
					++completed;
					active = false;
				}
				if (dmsg == null) { return; }
				for (DeferredMessage<?> d: dmsg) {
					try {
						d.send();
					} catch (MessageRejectedException e) {
						last_ex = e;
					}
				}
			}
		});
		active = true;
		return completed;
	}

	protected int execute(Runnable run, DeferredMessage<?> ... dmsg) {
		return execute(run, this.state, java.util.Arrays.asList(dmsg));
	}

	protected int execute(Runnable run, Iterable<DeferredMessage<?>> dmsg) {
		return execute(run, this.state, dmsg);
	}

	protected int execute(Runnable run, S next) {
		return execute(run, next, null);
	}

	protected int execute(Runnable run) {
		return execute(run, state);
	}

	/**
	** Sets the next state and then sends the message, reverting the state
	** if the send fails.
	*/
	protected synchronized void sendAtomic(S next, DeferredMessage<?> dmsg) throws MessageRejectedException {
		S old = this.state;
		this.state = next;
		try {
			dmsg.send();
		} catch (MessageRejectedException e) {
			this.state = old;
			throw e;
		}
	}

}
