// Released under GPLv2 or later. See http://www.gnu.org/ for details.
package tags;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;

import tags.QueryTypes;
import tags.QueryTypes.BasicProcess;
import tags.QueryTypes.BasicEnvironment;
import tags.QueryTypes.BasicAgent;
import tags.proto.Query;
import tags.store.GraphMLStoreControl;
import tags.store.ProbabilityProxyStoreControl;

import tags.ui.ResultsReporter;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
** Entry point for the tag-routing suite.
**
** @author infinity0
*/
public class Tags {

	final private static Level[] levels = new Level[]{Level.OFF, Level.INFO, Level.FINE, Level.ALL};

	public static void main(String[] args) throws Throwable {
		System.err.println("current working directory: " + new File(".").getCanonicalPath());

		Options opt = new Options();
		opt.addOption("h", "help", false, "print this help message");
		opt.addOption(OptionBuilder.withDescription("show additional information (0-2)").
		  withLongOpt("verbose").withArgName("LEVEL").hasOptionalArg().create('v'));
		opt.addOption(OptionBuilder.withDescription("base data directory").
		  withLongOpt("basedir").withArgName("DIR").hasArg().create('d'));
		opt.addOption(OptionBuilder.withDescription("seed identity").
		  withLongOpt("seed-id").withArgName("ID").hasArg().create('s'));
		opt.addOption(OptionBuilder.withDescription("number of further steps to run after query bootstrap").
		  withLongOpt("numsteps").withArgName("NUM").hasArg().create('n'));
		opt.addOption(OptionBuilder.withDescription("milliseconds between sucessive steps").
		  withLongOpt("interval").withArgName("MS").hasArg().create('i'));

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

		int steps = Integer.parseInt(line.getOptionValue('n', "16"));
		int verbose = line.hasOption('v')? Integer.parseInt(line.getOptionValue('v', "1")): 0;

		BasicEnvironment<String> env = QueryTypes.makeProtoEnvironment(
		  new ProbabilityProxyStoreControl<String, String, String>(
		    new GraphMLStoreControl<String, Double, Double, Double>(basedir)
		  )
		);
		BasicAgent<String> agt = QueryTypes.makeProtoAgent(Tags.levels[verbose]);

		String interval = line.getOptionValue('i');
		if (interval != null) {
			agt.setInterval(Integer.parseInt(interval));
		}

		String[] tags = line.getArgs();
		if (tags.length == 0) { System.exit(0); }

		System.out.println("basedir=" + basedir + "; steps=" + steps + "; interval=" + interval + "; verbose=" + verbose);
		System.out.println("id=" + seedid + "; tags=" + java.util.Arrays.asList(tags));
		runQueries(basedir, env, agt, seedid, tags, steps);

		System.exit(0);
	}

	public static void exitErrorMessage(String message, int code) {
		System.err.println(message);
		System.exit(code);
	}

	public static <K> void runQueries(
	  String basedir, BasicEnvironment<K> env, BasicAgent<K> agt,
	  K id, String[] tags, int steps
	) throws IOException {
		agt.log.info("----");

		for (String tag: tags) {
			BasicProcess<K> proc = QueryTypes.makeProtoProcess(id, tag, env);
			proc.attachLogger(agt.log);
			agt.log.info("Starting query " + proc);
			agt.runUntilAfter(proc, steps, new FileResultsReporter(basedir, proc), selectSteps(steps));
			agt.log.info("----");
			agt.log.info("----");
		}
	}

	public static int[] selectSteps(int steps) {
		int[] ii = new int[0x20];
		int i;
		for (i=0; i<32; ++i) {
			ii[i] = steps-1;
			steps >>= 1;
			if (steps == 0) { break; }
		}
		return Arrays.copyOfRange(ii, 0, i+1);
	}

	public static class FileResultsReporter implements ResultsReporter {

		final protected FileWriter out;

		public FileResultsReporter(File basedir, Query<?, ?> query) throws IOException {
			out = new FileWriter(new File(new File(basedir, "res"), query.id + "." + query.tag + ".res"));
			out.write("Query results for " + query + "\n\f");
			out.flush();
		}

		public FileResultsReporter(String basedir, Query<?, ?> query) throws IOException {
			this(new File(basedir), query);
		}

		@Override public void addReport(String report) {
			try {
				out.write(report);
				out.write('\f');
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override public void finalize() throws IOException {
			out.flush();
			out.close();
		}

	}

}
