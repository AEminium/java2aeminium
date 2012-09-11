package aeminium.compiler.east;

import aeminium.compiler.signature.DataGroup;

public interface EASTDataNode
{
	public ETypeDeclaration getTypeDeclaration();

	public DataGroup getDataGroup();

	public EASTDataNode getScope();
}
