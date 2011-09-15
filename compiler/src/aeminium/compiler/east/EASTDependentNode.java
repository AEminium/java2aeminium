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
		this.childs = new ArrayList<EASTDependentNode>();
		this.parents = new ArrayList<EASTDependentNode>();
	}

	protected void link(EASTDependentNode child)
	{
		this.childs.add(child);
		child.parents.add(this);
	}

	@Override
	public void optimize()
	{
		// TODO optimize this
	}

	public boolean isRoot()
	{
		return this.parents.size() == 0;
	}
}
