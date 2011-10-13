package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EMethodInvocation extends EExpression
{
	MethodInvocation origin;

	EExpression expr;
	List<EExpression> args;

	ITypeBinding type;
	EMethodDeclaration declaration;

	EMethodInvocation(EAST east, MethodInvocation origin)
	{
		super(east);

		this.origin = origin;

		this.expr = this.east.extend(origin.getExpression());
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
			this.args.add(this.east.extend((Expression) arg));
		
		// TODO: add internal dependencies (System.out, and other statics here)?
	}


	@Override
	public Expression translate(Task parent)
	{
		System.err.println("TODO: MethodInvocation");
		return null;
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.expr.optimize();
		
		for (EExpression arg : this.args)
			arg.optimize(); 
		
		this.declaration = (EMethodDeclaration) this.east.getNode(this.east.resolveName(this.origin.resolveMethodBinding()));
		this.type = this.origin.resolveTypeBinding();
	}
}
