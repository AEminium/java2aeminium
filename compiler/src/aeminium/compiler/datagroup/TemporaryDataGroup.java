package aeminium.compiler.datagroup;

import aeminium.compiler.east.EASTNode;


public class TemporaryDataGroup extends DataGroup
{
	public TemporaryDataGroup(EASTNode node)
	{
		super(node);
	}
	
	public String toString()
	{
		return String.format("(T: %s)", this.hashCode());
	}
}
