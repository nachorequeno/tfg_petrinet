package cpn.tracing;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;

import org.cpntools.accesscpn.engine.highlevel.HighLevelSimulator;
import org.cpntools.accesscpn.engine.highlevel.InstancePrinter;
import org.cpntools.accesscpn.engine.highlevel.checker.Checker;
import org.cpntools.accesscpn.engine.highlevel.checker.ErrorInitializingSMLInterface;
import org.cpntools.accesscpn.engine.highlevel.instance.Marking;
import org.cpntools.accesscpn.model.ModelPrinter;
import org.cpntools.accesscpn.model.PetriNet;
import org.cpntools.accesscpn.model.importer.DOMParser;
	
import de.uni_luebeck.isp.tessla.interpreter.JavaApi;
import de.uni_luebeck.isp.tessla.interpreter.JavaApi.Engine;
import de.uni_luebeck.isp.tessla.interpreter.JavaApi.EngineListener;

public class LoadTest {

	/**
	 * @param args
	 * @throws Exception
	 */

	private final static List<String> events = Arrays.asList("e1", "e2");
	private final static List<String> status = Arrays.asList("t1", "t2");
	
	private static int maxTimeStamp = 0;

	public static void main(final String[] args) throws Exception {
		final JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			load(chooser.getSelectedFile());
		}

	}

	private static void load(final File selectedFile) throws Exception {
		final PetriNet petriNet = DOMParser.parse(selectedFile.toURI().toURL());
		System.out.println(ModelPrinter.printModel(petriNet));
		System.out.println("=======================================================");
		System.out.println(InstancePrinter.printModel(petriNet));
		System.out.println("=======================================================");
		System.out.println(InstancePrinter.printMonitors(petriNet));
		final HighLevelSimulator s = HighLevelSimulator.getHighLevelSimulator();
		try {
			s.setSimulationReportOptions(true, false, "");
			final Checker checker = new Checker(petriNet, selectedFile, s);
			try {
				checker.checkEntireModel(selectedFile.getParent(), selectedFile.getParent());
			} catch (final ErrorInitializingSMLInterface e) {
				// Ignore
			}
			System.out.println("Done");

			Engine res = generateTesslaEngine();

			for (int i = 1; i <= 50; i++) {

				s.execute(i);
				List<Marking> list = s.getMarking().getAllMarkings();
				executeTessla(list, res);
			}

			res.step();

		} finally {
			s.destroy();
		}
	}

	private static Engine generateTesslaEngine() {

		String spec_str = "in e1: Events[Unit]\r\n" + "in e2: Events[Unit]\r\n" + "in t1: Events[Unit]\r\n"
				+ "in t2: Events[Unit]\r\n" + "\r\n"
				+ "def eating_time_f1 = on(e1, default(time(e1) - time(t1), 0))\r\n"
				+ "def eating_time_f2 = on(e2, default(time(e2) - time(t2), 0))\r\n" + "\r\n" + "out eating_time_f1\r\n"
				+ "out eating_time_f2";

		System.out.println("Compiling...");

		Engine res = JavaApi.compile(spec_str, "spec.tessla").engine();

		System.out.println("Ready!");

		res.addListener(new EngineListener() {
			public void event(String stream, scala.math.BigInt time, Object value) {
				System.out.println("Got: " + stream + " = " + value + " at " + time);
			}

			public void printEvent(scala.math.BigInt time, Object value) {

			}
		});
		return res;
	}

	private static void executeTessla(List<Marking> list, Engine res) {

		for (Marking marking : list) {
			if (marking.getTokenCount() > 0 && marking.getMarking().split("@").length > 1) {
				int timestamp = Integer.valueOf(marking.getMarking().split("@")[1]) ;
				if (timestamp >= maxTimeStamp) {
					res.provide(marking.getPlaceInstance().getNode().getName().getText());
					res.setTime(Integer.valueOf(timestamp));
					maxTimeStamp = timestamp;
				}
			}
		}

	}

}