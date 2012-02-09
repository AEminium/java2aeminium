package aeminium.compiler.signature;

import java.util.Set;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.east.EASTExecutableNode;

public class SignatureItemWrite extends SignatureItem implements SignatureItemModification
{
	protected final DataGroup datagroup;
	
	public SignatureItemWrite(DataGroup datagroup)
	{
		this.datagroup = datagroup;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof SignatureItemWrite))
			return false;
		
		return this.datagroup.equals(((SignatureItemWrite) other).datagroup);
	}
	
	@Override
	public int hashCode()
	{
		return this.datagroup.hashCode();
	}
	
	@Override
	public String toString()
	{
		return "[WRITE] " + this.datagroup;
	}
	
	@Override
	public SignatureItemWrite replace(DataGroup what, DataGroup with)
	{
		return new SignatureItemWrite(this.datagroup.replace(what, with));
	}
	
	@Override
	public Set<EASTExecutableNode> getDependencies(EASTExecutableNode node, DependencyStack dependencyStack)
	{
		return dependencyStack.write(node, this.datagroup);
	}
	
	@Override
	public boolean isLocalTo(DataGroup scope)
	{
		return this.datagroup.beginsWith(scope);
	}
}
