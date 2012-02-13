package aeminium.compiler.east;

import aeminium.compiler.task.Task;

public interface EASTDeclaringNode extends EASTDataNode
{
	public Task getTask();
}