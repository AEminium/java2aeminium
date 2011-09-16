package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EInfixExpression extends EExpression
{
	InfixExpression origin;
	EExpression left;
	EExpression right;

	EInfixExpression(EAST east, InfixExpression origin)
	{
		super(east);

		this.origin = origin;

		this.left = this.east.extend(origin.getLeftOperand());
		this.link(this.left);

		this.right = this.east.extend(origin.getRightOperand());
		this.link(this.right);
	}

	@Override
	public Expression translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot() == false);

		System.err.println("InfixExpression");
		// TODO

		return null;
	}
}
