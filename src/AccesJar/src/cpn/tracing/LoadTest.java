package cpn.tracing;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

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

class TesslaEvent2{
    String stream;
    int time;
    Object value;
    
    public String toString() {
    	return "{" + stream + ", " + Integer.valueOf(time).toString() + ", " + value.toString() + "}";
    }
    
    public boolean equals(TesslaEvent2 te2) {
		return stream.equals(te2) && te2.time == time && te2.value.equals(value);
    }
}

class TesslaStream2 {
    String color;
    Vector<TesslaEvent2> data;
    Boolean editable;
    String name;
    String style; /* "signal" | "dots" | "graph" | "slim graph" | "plot" | "slim plot" | "events" | "unit events" | "bubbles" */
    Object text; /* ((e: TesslaEvent) => string) | null; */
}

public class LoadTest {

	/**
	 * @param args
	 * @throws Exception
	 */

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
	        Hashtable<String, Vector<TesslaEvent2>> inputStreams = new Hashtable<String, Vector<TesslaEvent2>>();

			for (int i = 1; i <= 50; i++) {
				System.out.println("Simulation step: " + s.getStep().toString() + ", time: " + s.getTime());
				s.execute();
				List<Marking> list = s.getMarking().getAllMarkings();
				System.out.println("New markings: " + list.toString());
				populate(list, inputStreams);
			}
			System.out.println("inputStreams: " + inputStreams.toString());

			executeTessla(inputStreams, res);
			
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

	private static void executeTessla(Hashtable<String, Vector<TesslaEvent2>> inputStreams, Engine res) {

		List<TesslaEvent2> tempList = new ArrayList<TesslaEvent2>();

	    Set<String> k = inputStreams.keySet();
	    Iterator<String> it = k.iterator();
	    while (it.hasNext()) {
	    	String streamName = it.next();
	    	Vector<TesslaEvent2> oneStream = inputStreams.get(streamName);
	    	tempList.addAll(oneStream);
	    }

	    tempList.sort((te1, te2) -> te1.time - te2.time);
	    Iterator<TesslaEvent2> it2 = tempList.iterator();
    	while (it2.hasNext()) {
    		TesslaEvent2 te = it2.next();
    		res.provide(te.stream);
    		res.setTime(te.time);
			System.out.println("Got: " + te.stream + " = " + te.value.toString() + " at " + Integer.valueOf(te.time).toString());
    	}

	}

	private static void populate(List<Marking> list, Hashtable<String, Vector<TesslaEvent2>> inputStreams) {
		for (Marking marking : list) {
			//System.out.println("Marking: " + marking.toString());

			if (marking.getTokenCount() > 0 && marking.getMarking().split("@").length > 1) {

				String stream = marking.getPlaceInstance().getNode().getName().getText();
				//System.out.println("Stream name: " + stream);
				
				Vector<TesslaEvent2> t_stream = inputStreams.get(stream);
				TesslaEvent2 lastTe;
				if (t_stream == null) {
					t_stream = new Vector<TesslaEvent2>();
					lastTe = new TesslaEvent2();
					lastTe.time = 0;
					lastTe.value = null;
					lastTe.stream = null;
				} else {
					lastTe = t_stream.lastElement();
				}

				//System.out.println("last Tessla Event: " + lastTe.toString());

				TesslaEvent2 te = new TesslaEvent2();
				te.stream = stream;
				te.time =  Integer.valueOf(marking.getMarking().split("@")[1]);
				te.value = marking.getMarking().split("@")[0];
				if ((te.time > lastTe.time) && (te.value != lastTe.value)) {
					//System.out.println("Adding Tessla Event: " + te.toString());
					t_stream.add(te);
					inputStreams.put(stream, t_stream);
					//System.out.println("Updated stream: " + inputStreams.toString());
				}
			}
		}

	}

}