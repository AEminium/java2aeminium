package aeminium.compiler.task;

import aeminium.compiler.east.EFieldDeclaration;

public class FieldTask extends Task
{
	protected FieldTask(EFieldDeclaration node, String name, Task base)
	{
		super(node, name, null, base);
		
		System.err.println("TODO: FieldTask");
	}

	public static FieldTask create(EFieldDeclaration node, String name, Task base)
	{
		return new FieldTask(node, name, base);
	}
}
