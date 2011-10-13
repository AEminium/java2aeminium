package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EExpressionStatement extends EStatement
{
	ExpressionStatement origin;
	EExpression expr;

	EExpressionStatement(EAST east, ExpressionStatement origin)
	{
		super(east);
		this.origin = origin;

		this.expr = this.east.extend((Expression) origin.getExpression());
	}

	@Override
	public void optimize()
	{
		super.optimize();
		this.expr.optimize();
	}

	@Override
	public List<Statement> translate(Task parent)
	{
		System.err.println("translate: ExpressionStatement");
		return null;
	}
}
