package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EInfixExpression extends EExpression
{
	EAST east;
	InfixExpression origin;
	EExpression left;
	EExpression right;


	EInfixExpression(EAST east, InfixExpression origin)
	{
		this.east = east;
		this.origin = origin;

		this.left = this.east.extend(origin.getLeftOperand());
		this.link(this.left);

		this.right = this.east.extend(origin.getRightOperand());
		this.link(this.right);
	}

	@Override
	public Expression translate(TypeDeclaration decl, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		// TODO

		return null;
	}
}
