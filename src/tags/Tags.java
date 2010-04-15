// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags;

import tags.io.GraphMLFile;

/**
** Entry point for the tag-routing suite.
**
** @author infinity0
*/
public class Tags {

	public static void main(String[] args) throws Throwable {
		System.out.println("Hello, world!");
		String fn = args.length == 0? "../test.graphml": args[0];
		GraphMLFile gf = new GraphMLFile(fn);
		gf.debug();
		//for (String a: args) { System.out.println(a); }
	}

}
