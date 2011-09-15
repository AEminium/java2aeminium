package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;

import aeminium.compiler.east.EASTNode;

public abstract class EASTDependentNode extends EASTNode
{
	protected boolean sequential;
	protected List<EASTDependentNode> childs;
	protected List<EASTDependentNode> parents;

	EASTDependentNode()
	{
		/* by default everything is parallel */
		this.sequential = false;

		this.childs = new ArrayList<EASTDependentNode>();
		this.parents = new ArrayList<EASTDependentNode>();
	}

	protected void link(EASTDependentNode child)
	{
		this.childs.add(child);
		child.parents.add(this);
	}

	public boolean isSequential()
	{
		return this.sequential;
	}

	@Override
	public void optimize()
	{
		this.sequential = this.childs.size() < 2;
	}
}
