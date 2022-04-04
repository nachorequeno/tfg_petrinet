package tessla_example;

import de.uni_luebeck.isp.tessla.interpreter.Interpreter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import de.uni_luebeck.isp.tessla.core.Tessla.Specification;
import de.uni_luebeck.isp.tessla.core.Tessla.Statement;
//import de.uni_luebeck.isp.tessla.core.TesslaAST;
import de.uni_luebeck.isp.tessla.interpreter.Trace;
import de.uni_luebeck.isp.tessla.interpreter.Trace.Event;
import scala.collection.Iterator;
import scala.collection.immutable.Seq;
import scala.collection.JavaConverters;

public class Example {

	public static Seq<String> convertListToSeq(List<String> inputList) {
	    return JavaConverters.asScalaIteratorConverter(inputList.iterator()).asScala().toSeq();
	}

	
	public static void main(String[] args) throws URISyntaxException {
		// TODO Auto-generated method stub
	
		String csv_trace = "ts, x, y\n"
				+ "1, 2, 3\n"
				+ "2, 5, 6\n"
				+ "5, 8, 9\n";

		/* String spec_str = "in temperature: Events[Int]\r\n" + 
		"\r\n" + 
		"def low = temperature < 3\r\n" + 
		"def high = temperature > 8\r\n" + 
		"def unsafe = low || high\r\n" + 
		"\r\n" + 
		"out unsafe";

		// TesslaAST<Object>.Specification spec = new TesslaAST.Specification(spec_str, null, null, null, 0);
		
		List<String> inputList; 
		Seq<String> s = convertListToSeq(inputList);
		Specification spec = new Specification(s); 

		
		String in_trace_str = "1: temperature = 6\r\n" + 
				"2: temperature = 2\r\n" + 
				"3: temperature = 1\r\n" + 
				"4: temperature = 5\r\n" + 
				"5: temperature = 9";
		*/
		//Iterator<Event> in_trace = Trace.fromCsvString(in_trace_str, "<str>", null);
		
		//URI uri = new URI("file:///H:\\Mi unidad\\TFG\\tfg_autoadaptativo\\bib\\TeSSLa\\example\\temperature\\temperature.input");
		//URI uri = new URI("file:///G:/temperature/temperature.input");
		//File ctfFile = new File(uri);
		//File ctfFile = new File("G:\\temperature\\temperature.input");
		File ctfFile = new File("G:/temperature/temperature.input");
		if (ctfFile.exists()) {
			System.out.println("ok");
		}
		//Iterator<Event> in_trace = Trace.fromFile(ctfFile, null);
		Iterator<Event> in_trace = Trace.fromCsvString(csv_trace, "<str>", null);

		//System.out.println(in_trace.toString());
		/*while(in_trace.hasNext()) {
			Event e = in_trace.next();
			System.out.println(e.toString());
		}*/
		
		//Interpreter.run(spec, in_trace, null, null);
	}

}