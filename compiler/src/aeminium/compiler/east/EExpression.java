package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.*;

public abstract class EExpression extends EASTExecutableNode implements EASTDataNode
{
	protected final EASTDataNode scope;
	
	public EExpression(EAST east, Expression original, EASTDataNode scope)
	{
		super(east, original);
		
		this.scope = scope;
	}
	
	public static EExpression create(EAST east, Expression expr, EASTDataNode scope)
	{
		if (expr instanceof MethodInvocation)
			return EMethodInvocation.create(east, (MethodInvocation) expr, scope);
		
		if (expr instanceof SimpleName)
			return ESimpleNameExpression.create(east, (SimpleName) expr, scope);
		
		if (expr instanceof ClassInstanceCreation)
			return EClassInstanceCreation.create(east, (ClassInstanceCreation) expr, scope);
		
		if (expr instanceof NumberLiteral)
			return ENumberLiteral.create(east, (NumberLiteral) expr, scope);
		
		if (expr instanceof InfixExpression)
			return EInfixExpression.create(east, (InfixExpression) expr, scope);
		
		System.err.println("Not implemented error: " + expr.getClass().getName());
		return null;
	}
}
