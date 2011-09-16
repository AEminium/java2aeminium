package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;

public class EReturnStatement extends EStatement
{
	EAST east;
	ReturnStatement origin;
	EExpression expr;

	EReturnStatement(EAST east, ReturnStatement origin)
	{
		this.east = east;
		this.origin = origin;

		if (origin.getExpression() != null)
		{
			this.expr = this.east.extend((Expression) origin.getExpression());
			this.link(this.expr);
		}
	}

	@Override
	public void translate(EMethodDeclaration method, List<CompilationUnit> cus, List<Statement> stmts)
	{
		AST ast = this.east.getAST();

		// return can't have any child
		assert(this.isRoot() == false);

		if (this.expr != null)
		{
			// this._root._ret = ...;
			Assignment assign = ast.newAssignment();
	
			FieldAccess root = ast.newFieldAccess();
			root.setExpression(ast.newThisExpression());
			root.setName(ast.newSimpleName("_root"));

			FieldAccess ret = ast.newFieldAccess();
			ret.setExpression(root);
			ret.setName(ast.newSimpleName("_ret"));

			assign.setLeftHandSide(ret);
			assign.setRightHandSide(this.expr.translate(method, cus, stmts));
	
			ExpressionStatement stmt = ast.newExpressionStatement(assign);
			stmts.add(stmt);
		}

		// TODO: mark task as finished?
	}
}
