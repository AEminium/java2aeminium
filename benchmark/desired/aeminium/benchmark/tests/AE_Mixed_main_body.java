package aeminium.benchmark.tests;

import aeminium.benchmark.tests.AE_Mixed_main_body_2;

import aeminium.Helper;
import aeminium.Proxy;

import java.util.ArrayList;
import java.util.HashMap;

class AE_Mixed_main_body implements aeminium.runtime.Body
{
	/* arguments */
	String[] args;

	/* variables */
	Mixed object;

	/* runtime */
	HashMap<Object, aeminium.runtime.Task> dependencies;

	Proxy proxy_1;
	AE_Mixed_method_body body_1;
	aeminium.runtime.Task task_1;
	ArrayList<aeminium.runtime.Task> deps_1;

	AE_Mixed_main_body_2 body_2;
	aeminium.runtime.Task task_2;
	ArrayList<aeminium.runtime.Task> deps_2;

	AE_Mixed_main_body(String[] args)
	{
		this.args = args;
		this.dependencies = new HashMap<Object, aeminium.runtime.Task>();
	}

	public void execute(aeminium.runtime.Runtime rt, aeminium.runtime.Task task) throws Exception
	{
		object = new Mixed();

		/* object.method() */
		this.deps_1 = new ArrayList<aeminium.runtime.Task>();
		Helper.swapDependency(this.deps_1, object, this.task_1, this.dependencies);

		this.proxy_1 = new Proxy<Integer>();
		this.body_1 = new AE_Mixed_method_body(object, this.proxy_1);
		this.task_1 = Helper.createNonBlockingTask(this.body_1, Helper.NO_HINTS);
		Helper.schedule(this.task_1, task, this.deps_1);

		this.dependencies.put(this.proxy_1, this.task_1);

		/* System.out.println(+); */
		this.deps_2 = new ArrayList<aeminium.runtime.Task>();
		Helper.swapDependency(this.deps_2, System.out, this.task_2, this.dependencies);
		Helper.swapDependency(this.deps_2, this.proxy_1, this.task_2, this.dependencies);

		this.body_2 = new AE_Mixed_main_body_2(this);
		this.task_2 = Helper.createNonBlockingTask(this.body_2, Helper.NO_HINTS);
		Helper.schedule(this.task_2, task, this.deps_2);
	}
}
