package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.*;

public abstract class EStatement extends EASTExecutableNode
{
	protected final EASTDataNode scope;
	protected final EMethodDeclaration method;
	
	public EStatement(EAST east, Statement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original);

		this.scope = scope;
		this.method = method;
	}

	public static EStatement create(EAST east, Statement stmt, EASTDataNode scope, EMethodDeclaration method)
	{
		if (stmt instanceof Block)
			return EBlock.create(east, (Block) stmt, scope, method);
		
		if (stmt instanceof VariableDeclarationStatement)
			return EVariableDeclarationStatement.create(east, (VariableDeclarationStatement) stmt, scope, method);
		
		if (stmt instanceof ReturnStatement)
			return EReturnStatement.create(east, (ReturnStatement) stmt, scope, method);
		
		if (stmt instanceof IfStatement)
			return EIfStatement.create(east, (IfStatement) stmt, scope, method);
		
		System.err.println("Not implemented error: " + stmt.getClass().getName());
		
		return null;
	}
}
