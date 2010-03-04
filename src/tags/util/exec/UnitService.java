// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.util.exec;

import java.util.concurrent.Executor;

/**
** A service that runs a maximum of one concurrent job.
*/
public class UnitService {

	protected Executor exec;

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
	** Schedule the given job.
	**
	** @return An identifier for the job (increments for every job completed)
	** @throws IllegalStateException if a job is already running
	*/
	protected synchronized int start(final Runnable run) {
		if (active) {
			throw new IllegalStateException("already running a job");
		}
		exec.execute(new Runnable() {
			@Override public void run() {
				try {
					run.run();
				} catch (RuntimeException e) {
					// FIXME LOW
				}
				synchronized(UnitService.this) {
					++completed;
					active = false;
				}
			}
		});
		active = true;
		return completed;
	}

}
