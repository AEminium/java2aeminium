package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class ESimpleName extends EExpression
{
	SimpleName origin;

	ESimpleName(EAST east, SimpleName origin)
	{
		super(east);

		this.origin = origin;
	}

	@Override
	public Expression translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		// cannot be root node
		assert(!this.isRoot());

		FieldAccess root = ast.newFieldAccess();
		root.setExpression(ast.newThisExpression());
		root.setName(ast.newSimpleName("_root"));

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(root);
		access.setName((SimpleName) ASTNode.copySubtree(ast, this.origin));
		
		return access;
	}

	@Override
	protected List<Expression> getDependencies(EMethodDeclaration decl, List<CompilationUnit> cus, List<Statement> stmts)
	{
		System.err.println("TODO: SimpleName getDependencies()");

		return super.getDependencies(decl, cus, stmts);
	}	

}
