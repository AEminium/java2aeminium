package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;


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
	
	public AST getAST()
	{
		return this.original.getAST();
	}

	public CompilationUnit getCU()
	{
		return (CompilationUnit) this.original.getRoot();
	}
	
	public abstract ASTNode getOriginal();
}