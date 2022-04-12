import de.uni_luebeck.isp.tessla.interpreter.JavaApi;
import de.uni_luebeck.isp.tessla.interpreter.JavaApi.*;
import de.uni_luebeck.isp.tessla.interpreter.StreamEngine;

class Filosofos {

    public static void main(String[] args) {

        String spec_str = "in e1: Events[Unit]\r\n" + 
        		"in e2: Events[Unit]\r\n" + 
        		"in t1: Events[Unit]\r\n" + 
        		"in t2: Events[Unit]\r\n" +
        		"\r\n" +
        		"def eating_time_f1 = on(e1, default(time(e1) - time(t1), 0))\r\n" + 
        		"def eating_time_f2 = on(e2, default(time(e2) - time(t2), 0))\r\n" +
        		"\r\n" +
        		"out eating_time_f1\r\n" + 
        		"out eating_time_f2";


		System.out.println("Compiling...");

		Engine res = JavaApi.compile(spec_str ,"spec.tessla").engine();

		System.out.println("Ready!");

		res.addListener(new EngineListener(){
			public void event(String stream, scala.math.BigInt time, Object value) {
				System.out.println("Got: " + stream + " = " + value + " at " + time);
			}

			public void printEvent(scala.math.BigInt time, Object value) {

			}
		});

		res.setTime(0);
		res.provide("t1");
		res.provide("t2");
		res.setTime(1);
		res.provide("e1");
		res.setTime(3);
		res.provide("t1");
		res.setTime(5);
		res.provide("e2");
		res.setTime(7);
		res.provide("t2");
		res.setTime(9);
		res.provide("e1");
		res.step();

		System.out.println("Finished!");

    }

}
