// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

import tags.proto.Query;
import tags.proto.QueryProcessors;
import tags.proto.QueryProcessors.*;
import tags.store.GraphMLStoreControl;

/**
** Entry point for the tag-routing suite.
**
** @author infinity0
*/
public class Tags {

	public static void main(String[] args) throws Throwable {

		Options opt = new Options();
		opt.addOption("h", "help", false, "print this help message");
		opt.addOption(OptionBuilder.withDescription("base data directory").
		  withLongOpt("basedir").withArgName("DIR").hasArg().create('d'));
		opt.addOption(OptionBuilder.withDescription("seed identity").
		  withLongOpt("seed-id").withArgName("ID").hasArg().create('s'));

		CommandLineParser parser = new GnuParser();
		CommandLine line = parser.parse(opt, args);

		if (line.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.setLeftPadding(4);
			formatter.printHelp("tags.Tags [tags]", opt);
			System.exit(0);
		}

		String basedir = line.getOptionValue('d');
		if (basedir == null) { exitErrorMessage("no basedir supplied", 2); }
		String seedid = line.getOptionValue('s');
		if (seedid == null) { exitErrorMessage("no seedid supplied", 2); }
		String[] tags = line.getArgs();
		if (tags.length == 0) { System.exit(0); }

		System.out.println("basedir=" + basedir + "; seedid=" + seedid + ";");
		System.out.println("tags=" + java.util.Arrays.asList(tags));

		GraphMLStoreControl<String, Double, Double, Double> sctl = new
		GraphMLStoreControl<String, Double, Double, Double>(basedir);

		// FIXME NOW this needs to be Probability, not Double
		//QueryProcessors.processQuery(sctl, seedid, java.util.Arrays.asList(tags));

	}

	public static void exitErrorMessage(String message, int code) {
		System.err.println(message);
		System.exit(code);
	}

}
