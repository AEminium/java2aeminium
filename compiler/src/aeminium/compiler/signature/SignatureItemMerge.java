package aeminium.compiler.signature;

import java.util.HashSet;
import java.util.Set;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.east.EASTExecutableNode;

public class SignatureItemMerge extends SignatureItem implements SignatureItemModification
{
	protected final DataGroup to;
	protected final DataGroup from;
	
	public SignatureItemMerge(DataGroup to, DataGroup from)
	{
		this.to = to;
		this.from = from;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof SignatureItemMerge))
			return false;
		
		SignatureItemMerge _other = (SignatureItemMerge) other;
		
		return this.from.equals(_other.from) && this.to.equals(_other.to);
	}
	
	@Override
	public int hashCode()
	{
		return this.from.hashCode() ^ this.to.hashCode();
	}
	
	@Override
	public String toString()
	{
		return "[MERGE] " + this.to + " <- " + this.from;
	}
	
	@Override
	public SignatureItemMerge replace(DataGroup what, DataGroup with)
	{
		return new SignatureItemMerge
		(
			this.to.replace(what, with),
			this.from.replace(what, with)
		);
	}
	
	@Override
	public Set<EASTExecutableNode> getDependencies(EASTExecutableNode node, DependencyStack dependencyStack)
	{
		dependencyStack.merge(this.to, this.from);
		return new HashSet<EASTExecutableNode>();
	}

	@Override
	public boolean isLocalTo(DataGroup scope)
	{
		/* 
		 * merging to a local datagroups has no implications to outer methods
		 * merging local datagroups to external datagroups also has no implications
		 * only external <-> external
		 * (writing to member variables is still an external write to the method)
		 */
		return this.to.beginsWith(scope) || this.from.beginsWith(scope);
	}
}
