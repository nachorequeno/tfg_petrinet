package cpn.tracing;

import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.link;
import static j2html.TagCreator.script;
import static j2html.TagCreator.title;
import j2html.tags.ContainerTag;
import j2html.tags.specialized.HtmlTag;
import cpn.tracing.Event;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JFileChooser;

import org.cpntools.accesscpn.engine.highlevel.HighLevelSimulator;
import org.cpntools.accesscpn.engine.highlevel.checker.Checker;
import org.cpntools.accesscpn.engine.highlevel.checker.ErrorInitializingSMLInterface;
import org.cpntools.accesscpn.engine.highlevel.instance.Marking;
import org.cpntools.accesscpn.model.PetriNet;
import org.cpntools.accesscpn.model.importer.DOMParser;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;

import de.uni_luebeck.isp.tessla.core.TesslaAST.Type;
import de.uni_luebeck.isp.tessla.core.util.Lazy;
import de.uni_luebeck.isp.tessla.interpreter.JavaApi;
import de.uni_luebeck.isp.tessla.interpreter.JavaApi.Engine;
import de.uni_luebeck.isp.tessla.interpreter.JavaApi.EngineListener;
import de.uni_luebeck.isp.tessla.interpreter.StreamEngine.Stream;

import scala.Tuple3;
import scala.collection.immutable.Map;
import scala.Option;

class TesslaEvent {
    String color;
    Number time;
    Number value;
}

class TesslaStream {
    String color;
    Vector<TesslaEvent> data;
    Boolean editable;
    String name;
    String style; /* "signal" | "dots" | "graph" | "slim graph" | "plot" | "slim plot" | "events" | "unit events" | "bubbles" */
    Object text; /* ((e: TesslaEvent) => string) | null; */
}

public class LoadTestHtml {

    static HashMap<String, Vector<TesslaEvent>> streams = new HashMap<String, Vector<TesslaEvent>>();
    static EngineListener eng_listener = null;
    static Engine res;

	public static void main(final String[] args) throws Exception {
		final JFileChooser chooser = new JFileChooser();
		if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			Hashtable<String, Vector<Event>> res_sim = simulate(chooser.getSelectedFile(), 500);
			Engine eng = generateTesslaEngine();
			executeTessla(res_sim, eng);		
			//LoadTestHtml ld = new LoadTestHtml();
			//ld.printFactories();
			//streams.put("hola", new Vector<TesslaEvent>(10));
			//String string_streams = ld.invokeJavascriptTesslaGraal();
			//String string_streams = ld.Streams2Text(streams);
			String string_streams = Streams2Text(streams);
			HtmlTag out = toHTML("Hola!", string_streams);
			System.out.println(out.renderFormatted());
		}

	}

	public static Hashtable<String, Vector<Event>> simulate(File file, int amount) throws Exception {	
		System.out.println("[pnpl] Simulation launching");
        Hashtable<String, Vector<Event>> streams = new Hashtable<String, Vector<Event>>();

		try {
			final HighLevelSimulator simulator = load(file);
			List<Marking> allMarkings = simulator.getMarking().getAllMarkings();
			System.out.println("[pnpl] Initial state: " + simulator.getStep().toString() + ", time: " + simulator.getTime()); 
			populate(allMarkings, streams);
			for (int i = 0; i < amount; i++) {
				simulator.execute();
				allMarkings = simulator.getMarking().getAllMarkings();
				System.out.println("[pnpl] Simulation step: " + simulator.getStep().toString() + ", time: " + simulator.getTime()); 
				populate(allMarkings, streams);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		System.out.println("[pnpl] Simulation ended");
		return streams;
	}

	private static HighLevelSimulator load(final File file) throws Exception {

		System.out.println("[pnpl] Getting petri net from file...");
		final PetriNet petriNet = DOMParser.parse(file.toURI().toURL());

		System.out.println("[pnpl] Creating simulator instance...");

		//Simulator instance
		final HighLevelSimulator simulator = HighLevelSimulator.getHighLevelSimulator();

		simulator.setSimulationReportOptions(false, false, "");

		final Checker checker = new Checker(petriNet, file, simulator);

		try {
			checker.checkEntireModel(file.getParent(), file.getParent());
		} catch (final ErrorInitializingSMLInterface e) {
			// Ignore
		}
		System.out.println("[pnpl] Simulator created");
		return simulator;
	}

	public static HtmlTag toHTML(String title, String string_streams) throws NoSuchMethodException, ScriptException, IOException, URISyntaxException {
		ContainerTag ct = new ContainerTag("svg");
		ct.withId("container").withStyle("width:800px;");
		HtmlTag out = html(
				    head(
				        title(title),
				        link().withRel("stylesheet").withHref("/css/main.css")
				    ),
				    body(
				         h1(title),
				         //img().withId("container").withStyle("width:800px;"),
				         ct,
				         script().withSrc("d3.js"),
				         script().withSrc("tessla-visualizer.js"),
				         script(string_streams)
				    )
			).withLang("en");
		return out;
	}
	
    private File getFileFromResource(String fileName) throws URISyntaxException{
    	// String fileName = "./lib/tessla-visualizer.js";
    	ClassLoader cl = getClass().getClassLoader();
    	//File f = new File(cl.getResource(fileName).getFile());
        URL resource = cl.getResource(fileName);
    	
        if (resource == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {

            // failed if files have whitespaces or special characters
            //return new File(resource.getFile());

            return new File(resource.toURI());
        }

    }

    private void printFactories() {
		ScriptEngineManager sem = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = sem.getEngineFactories();
		for (ScriptEngineFactory factory : factories)
		    System.out.println(factory.getEngineName() + " " + factory.getEngineVersion() + " " + factory.getNames());
		if (factories.isEmpty())
		    System.out.println("No Script Engines found");
    }
    
    private String invokeJavascriptTesslaGraal() throws URISyntaxException, IOException {    
		/* ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("JavaScript"); */
		// read script file
		//engine.eval(Files.newBufferedReader(Paths.get("./lib/tessla-visualizer.js"), StandardCharsets.UTF_8));
		File f = getFileFromResource("./tessla-visualizer.js");
	    BufferedReader lines = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8);
	    /* engine.eval(lines); */
		
		Context context = Context.newBuilder("js")
			    .allowHostAccess(HostAccess.ALL)
			    //allows access to all Java classes
			    .allowHostClassLookup(className -> true)
			    .build();
		CharSequence jsSourceCode = lines.readLine();
		context.eval("js", jsSourceCode);
		return null;
    }
    

    private static String Streams2Text(HashMap<String, Vector<TesslaEvent>> streams2) {    
    	String res = "tesslaVisualizer.visualizer(container, {\r\n" + 
    			"        streams: [";
        // Iterating HashMap through for loop  	
         /*{
            style: "dots",
            name: "hans",
            data: [{time: 5, value: 6}, {time: 10, value: 10}, {time: 17, value: 20}],
          }, */
        Set<String> streamNames = streams2.keySet();
        for (String name: streamNames) {
        	Vector<TesslaEvent> value = streams2.get(name);
        	res = res + "{\nstyle: \"dots\",\nname: \"" + name + "\",\ndata:" + TimeEvents2Text(value) + "},\n";
        }
        
		return res + "] });";
    }
    
    private static String TimeEvents2Text(Vector<TesslaEvent> value) {
    	// [{time: 5, value: 6}, {time: 10, value: 10}, {time: 17, value: 20}]
    	String res = "[";
    	for (int i = 0; i < value.size(); i++) {
    		String timestamp = value.get(i).time.toString();
    		String val = value.get(i).value.toString();
    		res = res + "{time: " + timestamp + ", value: " + val +"},";
    	}
    	res = res + "]";
    	return res;
    }
    
	private String invokeJavascriptTessla() throws NoSuchMethodException, ScriptException, IOException, URISyntaxException {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("JavaScript");
		// read script file
		//engine.eval(Files.newBufferedReader(Paths.get("./lib/tessla-visualizer.js"), StandardCharsets.UTF_8));
		File f = getFileFromResource("./tessla-visualizer.js");
        BufferedReader lines = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8);
        engine.eval(lines);
      
		Invocable inv = (Invocable) engine;
	    
	    Object main = engine.get("Main");
	    Object container = inv.invokeMethod(main, "container");
	    Object tesslaVisualizer = engine.get("tesslaVisualizer");
        
	    
		// get Runnable interface object from engine. This interface methods
	    // are implemented by script functions with the matching name.
	    
	    Vector<TesslaStream> json_streams = new Vector<TesslaStream>(streams.size());
	    Set<String> k = streams.keySet();

	    Iterator<String> it = k.iterator();
	    while (it.hasNext()) {
	    	TesslaStream ts = new TesslaStream();
	    	ts.name = it.next();
	    	ts.data = streams.get(ts.name);
	    	json_streams.add(ts);
	    	//json_streams[i] = ts;
	    }
	    
		// call function from script file
        Object res = inv.invokeMethod(tesslaVisualizer, "visualizer", container, json_streams);
	    
		return res.toString();
	}
	
	private static void specIterator() {
		System.out.println("Element list!");
		Iterator<String> peNames = scala.collection.JavaConverters.asJava(res.productElementNames());
		while (peNames.hasNext()){
			String elemName = peNames.next();
			System.out.println("Got: " + elemName);
		}
		
		Map<String, Lazy<Object>> defs = res.spec().definitions();
		System.out.println("defs: " + defs.toString());
		
		List<Tuple3<Option<String>, Stream, Type>> outStreams = scala.collection.JavaConverters.asJava(res.spec().outStreams());
		
		System.out.println("outStreams: " + res.spec().outStreams().toString());
		Iterator<Tuple3<Option<String>, Stream, Type>> it = outStreams.iterator();
		while (it.hasNext()) {
			Tuple3 t = (Tuple3) it.next();
			//System.out.println("outStream: " + t._1().getClass().getName().toString());
			Option<String> name = (Option<String>) t._1();
			System.out.println("os name: " + name.get());
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
		
        streams = new HashMap<String, Vector<TesslaEvent>>();
		eng_listener = new EngineListener() {
			public void event(String stream, scala.math.BigInt time, Object value) {
				System.out.println("Got: " + stream + " = " + value + " at " + time);
				TesslaEvent te = new TesslaEvent();
				te.time = time;
				te.value = (Number) value;
				Vector<TesslaEvent> t_stream = streams.get(stream);
				if (t_stream == null) {
					t_stream = new Vector<TesslaEvent>();
				}
				t_stream.add(te);
				streams.put(stream, t_stream);
			}

			public void printEvent(scala.math.BigInt time, Object value) {

			}
		};

		res.addListener(eng_listener);
		
		return res;
	}

private static void executeTessla(Hashtable<String, Vector<Event>> inputStreams, Engine res) {

		List<Event> tempList = new ArrayList<Event>();

	    Set<String> k = inputStreams.keySet();
	    Iterator<String> it = k.iterator();
	    while (it.hasNext()) {
	    	String streamName = it.next();
	    	Vector<Event> oneStream = inputStreams.get(streamName);
	    	tempList.addAll(oneStream);
	    }

	    tempList.sort((te1, te2) -> te1.getTime() - te2.getTime());
	    Iterator<Event> it2 = tempList.iterator();
    	while (it2.hasNext()) {
    		Event te = it2.next();
			System.out.println("Inserting: " + te.getStream() + " = " + te.getValue().toString() + " at " + Integer.valueOf(te.getTime()).toString());
			res.setTime(te.getTime());
			res.provide(te.getStream());
    	}
	}

	private static void populate(List<Marking> allMarkings, Hashtable<String, Vector<Event>> inputStreams) {
		System.out.println("[pnpl] allMarkings " + allMarkings.toString());
		for (Marking marking : allMarkings) {
			if (marking.getTokenCount() > 0 && marking.getMarking().split("@").length > 1) {
				
				String stream = marking.getPlaceInstance().getNode().getName().getText();
				
				Vector<Event> t_stream = inputStreams.get(stream);
				Event lastTe;
				if (t_stream == null) {
					t_stream = new Vector<Event>();
					lastTe = new Event();
				} else {
					lastTe = t_stream.lastElement();
				}

				Event te = new Event();
				te.setStream(stream);
				te.setTime(Integer.valueOf(marking.getMarking().split("@")[1]));
				te.setValue(marking.getMarking().split("@")[0]);
				if ((te.getTime() > lastTe.getTime()) && (te.getValue() != lastTe.getValue())) {
					t_stream.add(te);
					inputStreams.put(stream, t_stream);
					System.out.println("[pnpl] event: " + te.toString());
				}
			}
		}

	}

}