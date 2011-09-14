package aeminium;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;

import aeminium.runtime.Runtime;
import aeminium.runtime.Task;
import aeminium.runtime.NonBlockingTask;
import aeminium.runtime.Body;

import aeminium.runtime.implementations.Factory;

public class Helper
{
	public final static Collection<Task> NO_DEPS = Runtime.NO_DEPS; 
	public final static short NO_HINTS = Runtime.NO_HINTS;
	public final static Task NO_PARENT = Runtime.NO_PARENT;

	public static Runtime rt;

	public static NonBlockingTask createNonBlockingTask(Body b, short hints)
	{
		return Helper.rt.createNonBlockingTask(b, hints);
	}

	public static void init()
	{
		Helper.rt = Factory.getRuntime();
		Helper.rt.init();
	}

	public static void shutdown()
	{
		Helper.rt.shutdown();
	}

	public static void schedule(Task task, Task parent, Collection<Task> deps)
	{
		Helper.rt.schedule(task, parent, deps);
	}

	public static void swapDependency(ArrayList<Task> target, Object obj, Task task, HashMap<Object, Task> deps)
	{
		Task d = deps.get(obj);

		if (d != null)
			target.add(d);

		deps.put(obj, task);
	}
}
