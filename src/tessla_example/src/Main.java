import de.uni_luebeck.isp.tessla.interpreter.JavaApi;
import de.uni_luebeck.isp.tessla.interpreter.JavaApi.*;
import de.uni_luebeck.isp.tessla.interpreter.StreamEngine;

class Main {

    public static void main(String[] args) {

        String spec_str = "in temperature: Events[Int]\r\n" +
		"\r\n" +
		"def low = temperature < 3\r\n" +
		"def high = temperature > 8\r\n" +
		"def unsafe = low || high\r\n" +
		"\r\n" +
		"out unsafe";


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

		res.setTime(1);
		res.provide("temperature",2);
		res.setTime(2);
		res.provide("temperature",15);
		res.setTime(5);
		res.provide("temperature",8);
		res.step();

		System.out.println("Finished!");

    }

}
