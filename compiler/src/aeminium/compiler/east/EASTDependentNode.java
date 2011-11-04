package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public abstract class EASTDependentNode extends EASTNode
{
	protected List<EASTDependentNode> weakDependencies;

	protected boolean root;
	protected Task task;

	EASTDependentNode(EAST east)
	{
		super(east);

		this.weakDependencies = new ArrayList<EASTDependentNode>();

		this.root = true;
	}

	@Override
	public void optimize()
	{
		this.root = true;
	}

	protected final boolean isRoot()
	{
		return this.root;
	}

	protected List<EASTDependentNode> getWeakDependencies()
	{
		return this.weakDependencies;
	}

	protected void addWeakDependency(EASTDependentNode node)
	{
		this.weakDependencies.add(node);
	}

	public Expression translateWeakDependency()
	{
		System.err.println("WeakDependency of unhandled node type");
		assert(false);
		return null;
	}
}
