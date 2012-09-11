package aeminium.compiler.east;

import aeminium.compiler.task.Task;

public interface EASTControlerNode
{
	public void addControledNode(EASTExecutableNode node);
	public Task getScopeTask();
}
