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
		//for (String a: args) { System.out.println(a); }
		String fn = args.length == 0? "../test.graphml": args[0];
		System.out.println("Hello, world!");

		GraphMLFile<String, Double, Double> gf = new GraphMLFile<String, Double, Double>(fn);
		gf.setVertexPrimaryKey("id");
		gf.setDefaultEdgeAttribute("weight");
		//gf.setDefaultVertexAttribute("height");

		System.out.println(gf.getGraphAttributes());
		System.out.println(gf.getVertexAttributeNames());
		System.out.println(gf.getEdgeAttributeNames());

		for (String id: gf.keySet()) {
			System.out.println(id + " : " + gf.getSuccessorMap(id));
		}
	}

}
