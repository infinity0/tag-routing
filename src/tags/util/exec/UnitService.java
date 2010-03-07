// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

import java.util.concurrent.Executor;

/**
** A service that runs a maximum of one concurrent job.
*/
public class UnitService<S> {

	protected Executor exec;

	protected S state;
	protected boolean active;
	protected int completed;

	public UnitService(Executor exec) {
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
	** @throws IllegalStateException if a job is already running
	*/
	protected synchronized int execute(final Runnable run, final S next) {
		if (active) { throw new IllegalStateException("already running a job"); }
		exec.execute(new Runnable() {
			@Override public void run() {
				try {
					run.run();
				} catch (RuntimeException e) {
					// FIXME HIGH
				}
				synchronized(UnitService.this) {
					state = next;
					++completed;
					active = false;
				}
			}
		});
		active = true;
		return completed;
	}

	protected int execute(Runnable run) {
		return execute(run, state);
	}

}
