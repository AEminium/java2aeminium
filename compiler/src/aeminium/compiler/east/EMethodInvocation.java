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

	EMethodInvocation(EAST east, MethodInvocation origin)
	{
		super(east);

		this.origin = origin;

		this.expr = this.east.extend(origin.getExpression());
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
		{
			EExpression earg = this.east.extend((Expression) arg);
			this.link(earg);
			this.args.add(earg);
		} 	
		
		// TODO: add internal dependencies (System.out, and other statics here)?
	}

	@Override
	public Expression translate(Task parent, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		// TODO: allow regular method caling
		// TODO check if node has @AEminium annotation, if not create(or check if already exists) a new body 
		// that only calls the normal method
 
		assert (this.isRoot());

		this.task = parent.newSubtask(cus);

		System.err.println("TODO: MethodInvocation");
		FieldAccess ret = ast.newFieldAccess();

		return ret;
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.root = true;
	}
}
