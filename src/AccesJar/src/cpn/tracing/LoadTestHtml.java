package cpn.tracing;

import static j2html.TagCreator.attrs;
import static j2html.TagCreator.body;
import static j2html.TagCreator.h1;
import static j2html.TagCreator.head;
import static j2html.TagCreator.html;
import static j2html.TagCreator.link;
import static j2html.TagCreator.main;
import static j2html.TagCreator.script;
import static j2html.TagCreator.title;
import j2html.tags.specialized.HtmlTag;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JFileChooser;
import org.graalvm.polyglot.*;

import org.cpntools.accesscpn.engine.highlevel.HighLevelSimulator;
import org.cpntools.accesscpn.engine.highlevel.InstancePrinter;
import org.cpntools.accesscpn.engine.highlevel.checker.Checker;
import org.cpntools.accesscpn.engine.highlevel.checker.ErrorInitializingSMLInterface;
import org.cpntools.accesscpn.engine.highlevel.instance.Marking;
import org.cpntools.accesscpn.model.ModelPrinter;
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

public class LoadTest {

	private static int maxTimeStamp = 0;

    static HashMap<String, Vector<TesslaEvent>> streams = null;
    
	public static void main(final String[] args) throws Exception {
		//final JFileChooser chooser = new JFileChooser();
		//if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			/* List<Marking> list = simulate(chooser.getSelectedFile());
			callTessla(list); */
			LoadTest ld = new LoadTest();
			ld.printFactories();
			String string_streams = ld.invokeJavascriptTesslaGraal();
			HtmlTag out = toHTML("Hola!", string_streams);
			System.out.println(out.toString());
		//}

	}

/*	private static void load(final File selectedFile) throws Exception {
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
	}*/

	private static List<Marking> simulate(final File selectedFile) throws Exception {
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

			s.execute(10);
			List<Marking> list = s.getMarking().getAllMarkings();
			return list;

		} finally {
			s.destroy();
		}
	}
	
	private static void callTessla(List<Marking> list) throws Exception {
		Engine res = generateTesslaEngine();
		executeTessla(list, res);
		res.step();
	}

	public static HtmlTag toHTML(String title, String string_streams) throws NoSuchMethodException, ScriptException, IOException, URISyntaxException {
		HtmlTag out = html(
				    head(
				        title(title),
				        link().withRel("stylesheet").withHref("/css/main.css")
				    ),
				    body(
				    	j2html.TagCreator.main(attrs("#main.content"),
				            h1("Heading!"),
				            script("d3.js"),
				            script("tessla-visualizer.js"),
				            script(string_streams)
				        )
				    )
			);
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
	    /* final TesslaStream  ts = inv.getInterface(TesslaStream.class);
	    final TesslaEvent  te = inv.getInterface(TesslaEvent.class);
	    
	    TesslaStream[] json_streams;
	    Enumeration<String> k = streams.keys();

	    int i = 0;
	    while (k.hasMoreElements()) {
	    	ts.name = k.nextElement();
	    	ts.data = streams.get(ts.name);
	    	//json_streams.add(ts);
	    	json_streams[i] = ts;
	    	i++;
	    }*/
	    
	    final TesslaEvent  te = inv.getInterface(TesslaEvent.class);
	    
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
		// String res = (String) inv.invokeFunction("tesslaVisualizer.visualizer", container, json_streams);
        Object res = inv.invokeMethod(tesslaVisualizer, "visualizer", container, json_streams);
	    
		/* tesslaVisualizer.visualizer(container, {
        streams: [
          {
            style: "dots",
            name: "hans",
            data: [{time: 5, value: 6}, {time: 10, value: 10}, {time: 17, value: 20}],
          },
          {
            style: "graph",
            name: "gunter",
            data: [{time: 8, value: 3}, {time: 10, value: -2}, {time: 21, value: 7}, {time: 29.7, value: 5}],
          },
          {
            name: "fritz",
            style: "signal",
            data: [{time: 0, value: 0}, {time: 3, value: 10}, {time: 17, value: "fritz", color: "red"}, {time: 21, value: 7}, {time: 25, value: 17}]
          },
        ]
      	}); */
		return res.toString();
	}
	
	private static Engine generateTesslaEngine() {

		String spec_str = "in e1: Events[Unit]\r\n" + "in e2: Events[Unit]\r\n" + "in t1: Events[Unit]\r\n"
				+ "in t2: Events[Unit]\r\n" + "\r\n"
				+ "def eating_time_f1 = on(e1, default(time(e1) - time(t1), 0))\r\n"
				+ "def eating_time_f2 = on(e2, default(time(e2) - time(t2), 0))\r\n" + "\r\n" + "out eating_time_f1\r\n"
				+ "out eating_time_f2";

		System.out.println("Compiling...");
				
		Engine res = JavaApi.compile(spec_str, "spec.tessla").engine();

		/*System.out.println("Element list!");
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
		}*/
		
		
		System.out.println("Ready!");

		/* res.addListener(new EngineListener() {
			public void event(String stream, scala.math.BigInt time, Object value) {
				System.out.println("Got: " + stream + " = " + value + " at " + time);
			}

			public void printEvent(scala.math.BigInt time, Object value) {

			}
		}); */
		
        streams = new HashMap<String, Vector<TesslaEvent>>();
		EngineListener eng_listener = new EngineListener() {
			public void event(String stream, scala.math.BigInt time, Object value) {
				//System.out.println("Got: " + stream + " = " + value + " at " + time);
				TesslaEvent te = new TesslaEvent();
				te.time = time;
				te.value = (Number) value;
				Vector<TesslaEvent> t_stream = streams.get(stream);
				t_stream.add(te);
				streams.put(stream, t_stream);
			}

			public void printEvent(scala.math.BigInt time, Object value) {

			}
		};

		res.addListener(eng_listener);
		
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