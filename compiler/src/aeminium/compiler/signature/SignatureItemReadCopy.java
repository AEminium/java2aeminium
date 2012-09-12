package aeminium.compiler.signature;

import java.util.Set;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.east.EASTExecutableNode;

public class SignatureItemReadCopy extends SignatureItemRead
{
	public SignatureItemReadCopy(DataGroup datagroup)
	{
		super(datagroup);
	}
	
	@Override
	public Set<EASTExecutableNode> getDependencies(EASTExecutableNode node, DependencyStack dependencyStack)
	{
		return dependencyStack.readCopy(node, this.datagroup);
	}
}
