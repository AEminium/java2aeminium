package aeminium.benchmark.tests;

import aeminium.Helper;
import aeminium.Proxy;

import java.util.ArrayList;
import java.util.HashMap;

class AE_Mixed_main_body_2 implements aeminium.runtime.Body
{
	/* runtime */
	AE_Mixed_main_body parent;
	HashMap<Object, aeminium.runtime.Task> dependencies;

	AE_Mixed_main_body_2(AE_Mixed_main_body parent)
	{
		this.parent = parent;
	}

	public void execute(aeminium.runtime.Runtime rt, aeminium.runtime.Task task) throws Exception
	{
		System.out.println(this.parent.proxy_1.value);
	}
}
