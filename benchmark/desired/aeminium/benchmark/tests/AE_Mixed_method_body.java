package aeminium.benchmark.tests;

import aeminium.Helper;
import aeminium.Proxy;

import java.util.ArrayList;
import java.util.HashMap;

class AE_Mixed_method_body implements aeminium.runtime.Body
{
	/* arguments */
	Mixed _this;
	Integer x;

	/* return */
	Proxy<Integer> _ret;

	/* variables */

	/* runtime */
	HashMap<Object, aeminium.runtime.Task> dependencies;

	AE_Mixed_method_body(Mixed _this, Proxy<Integer> _ret, Integer x)
	{
		this._this = _this;
		this._ret = _ret;
		this.x = x;

		this.dependencies = new HashMap<Object, aeminium.runtime.Task>();
	}

	public void execute(aeminium.runtime.Runtime rt, aeminium.runtime.Task task) throws Exception
	{
		this._ret.value = 1;
	}
}
