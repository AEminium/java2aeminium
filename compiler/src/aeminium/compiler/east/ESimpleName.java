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
	public Expression translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot() == false);

		FieldAccess root = ast.newFieldAccess();
		root.setExpression(ast.newThisExpression());
		root.setName(ast.newSimpleName("_root"));

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(root);
		access.setName((SimpleName) ASTNode.copySubtree(ast, this.origin));
		
		return access;
	}
}
