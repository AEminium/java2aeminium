package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(parent);

		this.task = parent.newChild("expstmt");

		Block execute = ast.newBlock();
		execute.statements().addAll(this.build(task));
		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName(this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}

	public List<Statement> build(Task task)
	{
		AST ast = this.east.getAST();

		ExpressionStatement expr_stmt = ast.newExpressionStatement(this.expr.translate(task, true));

		return Arrays.asList((Statement)expr_stmt);
	}
}
