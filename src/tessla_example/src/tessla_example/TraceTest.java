package tessla_example;

import java.net.URISyntaxException;

import de.uni_luebeck.isp.tessla.interpreter.Trace;
import de.uni_luebeck.isp.tessla.interpreter.Trace.Event;
import scala.collection.Iterator;

public class TraceTest {

	
	public static void main(String[] args) throws URISyntaxException {
		// TODO Auto-generated method stub
	
		String csv_trace = "ts, x, y\n"
				+ "1, 2, 3\n"
				+ "2, 5, 6\n"
				+ "5, 8, 9\n";

		Iterator<Event> in_trace = Trace.fromCsvString(csv_trace, "<str>", scala.Option.empty());

		while(in_trace.hasNext()) {
			Event e = in_trace.next();
			System.out.println(e.toString());
		}
	}

}