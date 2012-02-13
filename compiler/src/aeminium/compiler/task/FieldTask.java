package aeminium.compiler.task;

import aeminium.compiler.east.EFieldDeclaration;

public class FieldTask extends Task
{
	protected FieldTask(EFieldDeclaration node, String name)
	{
		super(node, name, null);
		
		System.out.println("TODO: FieldTask");
	}

	public static FieldTask create(EFieldDeclaration node, String name)
	{
		return new FieldTask(node, name);
	}
}
