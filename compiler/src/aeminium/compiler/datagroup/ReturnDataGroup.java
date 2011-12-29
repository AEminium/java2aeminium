package aeminium.compiler.datagroup;

import aeminium.compiler.east.EASTNode;

public class ReturnDataGroup extends DataGroup
{
	public ReturnDataGroup(EASTNode node)
	{
		super(node);
	}
	
	public String toString()
	{
		return String.format("(R: %s)", this.node);
	}
}
