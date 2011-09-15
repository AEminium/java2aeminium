package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class ENumberLiteral extends EExpression
{
	EAST east;
	NumberLiteral origin;

	ENumberLiteral(EAST east, NumberLiteral origin)
	{
		this.east = east;
		this.origin = origin;
	}

	@Override
	public Expression translate(TypeDeclaration decl, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		return (NumberLiteral) ASTNode.copySubtree(ast, this.origin);
	}
}
