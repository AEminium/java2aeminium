package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class ENumberLiteral extends EExpression
{
	NumberLiteral origin;

	ENumberLiteral(EAST east, NumberLiteral origin)
	{
		super(east);
		this.origin = origin;
	}

	@Override
	public void optimize()
	{
		super.optimize();
		this.root = false;
	}

	@Override
	public Expression translate(Task parent, boolean write)
	{
		AST ast = this.east.getAST();

		return (NumberLiteral) ASTNode.copySubtree(ast, this.origin);
	}
}
