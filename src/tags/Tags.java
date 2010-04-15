// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags;

import tags.store.GraphMLStoreControl;

/**
** Entry point for the tag-routing suite.
**
** @author infinity0
*/
public class Tags {

	public static void main(String[] args) throws Throwable {
		//for (String a: args) { System.out.println(a); }
		String basedir = args.length == 0? "../test.graphml": args[0];
		System.out.println("Hello, world!");

		GraphMLStoreControl<String, Double, Double, Double> sctl = new
		GraphMLStoreControl<String, Double, Double, Double>(basedir);

	}

}
