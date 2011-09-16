package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EMethodInvocation extends EExpression
{
	EAST east;
	MethodInvocation origin;

	EExpression expr;
	List<EExpression> args;

	EMethodInvocation(EAST east, MethodInvocation origin)
	{
		this.east = east;
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
	public Expression translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		assert (this.isRoot());

		// TODO
		System.err.println("TODO: MethodInvocation");
		FieldAccess ret = ast.newFieldAccess();

		return ret;
	}

	@Override
	public boolean isRoot()
	{
		return true;
	}
}
