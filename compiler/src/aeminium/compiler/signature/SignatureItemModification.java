package aeminium.compiler.signature;

import java.util.Set;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;

public interface SignatureItemModification
{
	public Set<Dependency> getDependencies(DependencyStack dependencyStack);
}
