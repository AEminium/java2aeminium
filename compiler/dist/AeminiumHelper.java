package aeminium;

import java.util.Collection;

import aeminium.runtime.Runtime;
import aeminium.runtime.Task;
import aeminium.runtime.NonBlockingTask;
import aeminium.runtime.Body;

import aeminium.runtime.implementations.Factory;

public class AeminiumHelper
{
	public final static Collection<Task> NO_DEPS = Runtime.NO_DEPS; 
	public final static short NO_HINTS = Runtime.NO_HINTS;
	public final static Task NO_PARENT = Runtime.NO_PARENT;

	public static Runtime rt;

	public static NonBlockingTask createNonBlockingTask(Body b, short hints)
	{
		return AeminiumHelper.rt.createNonBlockingTask(b, hints);
	}

	public static void init()
	{
		AeminiumHelper.rt = Factory.getRuntime();
		AeminiumHelper.rt.init();
	}

	public static void shutdown()
	{
		AeminiumHelper.rt.shutdown();
	}

	public static void schedule(Task task, Task parent, Collection<Task> deps)
	{
		AeminiumHelper.rt.schedule(task, parent, deps);
	}
}
