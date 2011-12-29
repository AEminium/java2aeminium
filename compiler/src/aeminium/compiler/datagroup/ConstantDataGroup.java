package aeminium.compiler.datagroup;

import aeminium.compiler.east.EASTNode;

public class ConstantDataGroup extends DataGroup
{
	public ConstantDataGroup(EASTNode node)
	{
		super(node);
	}
	
	public String toString()
	{
		return "(C)";
	}
}
