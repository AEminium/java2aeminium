package aeminium.compiler.datagroup;

import aeminium.compiler.east.EASTNode;

public abstract class DataGroup
{
	public final EASTNode node;
	
	public DataGroup(EASTNode node)
	{
		this.node = node;
	}
}