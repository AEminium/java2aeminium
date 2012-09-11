package aeminium.compiler.signature;

import java.util.HashSet;
import java.util.Set;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.east.EASTControlerNode;
import aeminium.compiler.east.EASTExecutableNode;

public class SignatureItemControl extends SignatureItem implements SignatureItemModification
{
	protected final DataGroup scope;
	
	public SignatureItemControl(DataGroup scope)
	{
		this.scope = scope;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof SignatureItemControl))
			return false;
		
		return this.scope.equals(((SignatureItemControl) other).scope);
	}
	
	@Override
	public int hashCode()
	{
		return this.scope.hashCode();
	}

	@Override
	public String toString()
	{
		return "[CONTROL] " + this.scope;
	}
	
	@Override
	public SignatureItemRead replace(DataGroup what, DataGroup with)
	{
		return new SignatureItemRead(this.scope.replace(what, with));
	}

	@Override
	public Set<EASTExecutableNode> getDependencies(EASTExecutableNode node, DependencyStack dependencyStack)
	{
		dependencyStack.control((EASTControlerNode) node, scope);
		return new HashSet<EASTExecutableNode>();
	}

	@Override
	public boolean isLocalTo(DataGroup scope)
	{
		return this.scope.beginsWith(scope);
	}

}
