package aeminium.compiler.signature;

import java.util.Set;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.east.EASTExecutableNode;

public interface SignatureItemModification
{
	public Set<EASTExecutableNode> getDependencies(EASTExecutableNode node, DependencyStack dependencyStack);
}
