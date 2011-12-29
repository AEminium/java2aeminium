package aeminium.compiler.datagroup;

import aeminium.compiler.east.EASTNode;

public class LocalDataGroup extends DataGroup
{
	EASTNode scope;

	public LocalDataGroup(EASTNode node)
	{
		super(node);
		
		// TODO get the scope node?
	}
	
	public String toString()
	{
		return String.format("(L: %s)", this.node);
	}
}
