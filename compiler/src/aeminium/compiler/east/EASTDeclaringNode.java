package aeminium.compiler.east;

import aeminium.compiler.NodeDependency;

public interface EASTDeclaringNode extends EASTDataNode
{
	public NodeDependency getDependency();
}