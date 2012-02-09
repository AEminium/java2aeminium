package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.ASTNode;

public abstract class EASTNode
{
	protected final ASTNode original;
	protected final EAST east;
	
	public EASTNode(EAST east, ASTNode original)
	{
		this.east = east;
		this.original = original;
	}
	
	public EAST getEAST()
	{
		return this.east;
	}
	
	public abstract ASTNode getOriginal();	
	
	public abstract void checkSignatures();
}