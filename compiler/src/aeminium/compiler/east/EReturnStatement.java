package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;

import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.east.EAST;
import aeminium.compiler.east.EStatement;

public class EReturnStatement extends EStatement
{
	ReturnStatement origin;
	EExpression expr;

	EReturnStatement(ReturnStatement origin)
	{
		this.origin = origin;

		if (origin.getExpression() != null)
		{
			this.expr = EAST.extend((Expression) origin.getExpression());
			this.link(this.expr);
		}
	}

	@Override
	public void translate(boolean sequential, AST ast, List<Statement> stmts)
	{
		// return can't have any child
		assert(sequential == true);

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

			assign.setRightHandSide(this.expr.translate(this.isSequential(this.expr), ast, stmts));
	
			ExpressionStatement stmt = ast.newExpressionStatement(assign);
			stmts.add(stmt);
		}

		// TODO: mark task as finished?
	}
}
