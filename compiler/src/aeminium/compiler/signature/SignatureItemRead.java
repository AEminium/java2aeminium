package aeminium.compiler.signature;

import java.util.Set;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.east.EASTExecutableNode;

public class SignatureItemRead extends SignatureItem implements SignatureItemModification
{
	protected final DataGroup datagroup;
	
	public SignatureItemRead(DataGroup datagroup)
	{
		this.datagroup = datagroup;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof SignatureItemRead))
			return false;
		
		return this.datagroup.equals(((SignatureItemRead) other).datagroup);
	}
	
	@Override
	public int hashCode()
	{
		return this.datagroup.hashCode();
	}

	@Override
	public String toString()
	{
		return "[READ] " + this.datagroup;
	}
	
	@Override
	public SignatureItemRead replace(DataGroup what, DataGroup with)
	{
		return new SignatureItemRead(this.datagroup.replace(what, with));
	}

	@Override
	public Set<EASTExecutableNode> getDependencies(EASTExecutableNode node, DependencyStack dependencyStack)
	{
		return dependencyStack.read(node, this.datagroup);
	}

	@Override
	public boolean isLocalTo(DataGroup scope)
	{
		return this.datagroup.beginsWith(scope);
	}
}
