package aeminium.compiler;

import aeminium.compiler.east.EASTExecutableNode;
public class ArticicialDependency
{
	public final EASTExecutableNode node;
	public final String name;
	
	public ArticicialDependency(EASTExecutableNode node, String name)
	{
		this.node = node;
		this.name = name;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null || !(other instanceof ArticicialDependency))
			return false;
		
		ArticicialDependency _other = (ArticicialDependency) other;
		return this.node.equals(_other.node) && this.name.equals(_other.name);
	}
	
	@Override
	public int hashCode()
	{
		return this.node.hashCode() ^ this.name.hashCode();
	}
	
	@Override
	public String toString()
	{
		return this.node.toString() + "|" + this.name;
	}
}
