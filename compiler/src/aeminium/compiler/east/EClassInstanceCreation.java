package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EClassInstanceCreation extends EExpression
{
	EAST east;
	ClassInstanceCreation origin;
	List<EExpression> args;

	EClassInstanceCreation(EAST east, ClassInstanceCreation origin)
	{
		this.east = east;
		this.origin = origin;
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

		System.err.println("TODO: ClassInstanceCreation");
		return null;
	}
}
