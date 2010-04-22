// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags.ui;

/**
** A basic interface for receiving formatted reports.
**
** TODO NORM refactor this. QueryAgent should send data instead of strings.
** (I have no time to do this right now.)
*/
public interface ResultsReporter {

	/**
	** Receive a report.
	*/
	public void addReport(String report);

}
