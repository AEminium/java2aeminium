package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.Task;

public abstract class EASTDependentNode extends EASTNode
{
	protected boolean root;
	protected Task task;

	EASTDependentNode(EAST east)
	{
		super(east);

		this.root = true;
	}

	@Override
	public void analyse()
	{
	}

	@Override
	public int optimize()
	{
		/* TODO: add generic optimizations here */
		return 0;
	}
	
	protected final boolean isRoot()
	{
		return this.root;
	}
}
