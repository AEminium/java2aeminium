package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class ESimpleName extends EExpression
{
	EAST east;
	SimpleName origin;

	ESimpleName(EAST east, SimpleName origin)
	{
		this.east = east;
		this.origin = origin;
	}

	@Override
	public Expression translate(TypeDeclaration decl, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot() == false);

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName((SimpleName) ASTNode.copySubtree(ast, this.origin));

		return access;
	}
}
