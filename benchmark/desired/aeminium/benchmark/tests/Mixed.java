package aeminium.benchmark.tests;

import aeminium.Helper;
import aeminium.Proxy;

public class Mixed
{
	Mixed() { }

	public static void main(String[] args)
	{
		Helper.init();

		Helper.schedule
		(
			Helper.createNonBlockingTask(new AE_Mixed_main_body(args), Helper.NO_HINTS),
			Helper.NO_PARENT,
			Helper.NO_DEPS
		);

		Helper.shutdown();
	}
}
