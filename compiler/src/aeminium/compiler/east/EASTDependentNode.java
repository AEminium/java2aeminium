package aeminium.compiler.east;

import java.util.HashMap;

import aeminium.compiler.east.EASTNode;

public abstract class EASTDependentNode extends EASTNode
{
	protected HashMap<EASTDependentNode, Boolean> childs;
	protected HashMap<EASTDependentNode, Boolean> parents;

	EASTDependentNode()
	{
		this.childs = new HashMap<EASTDependentNode, Boolean>();
		this.parents = new HashMap<EASTDependentNode, Boolean>();
	}

	protected void link(EASTDependentNode child)
	{
		/* by default everything is parallel */
		this.childs.put(child, false);
		child.parents.put(this, false);
	}

	protected boolean isSequential(EASTDependentNode child)
	{
		return this.childs.get(child);
	}
}
