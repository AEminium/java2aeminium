package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EIfStatement extends EStatement
{
	IfStatement origin;
	EExpression expr;
	EStatement then_stmt;
	EStatement else_stmt;

	EIfStatement(EAST east, IfStatement origin)
	{
		super(east);
		this.origin = origin;

		this.expr = this.east.extend((Expression) origin.getExpression());
		this.then_stmt = this.east.extend(origin.getThenStatement());

		if (this.origin.getElseStatement() != null)
			this.else_stmt = this.east.extend(origin.getElseStatement());
	}
	
	@Override
	public void optimize()
	{
		super.optimize();
		this.expr.optimize();
		this.then_stmt.optimize();

		if (this.else_stmt != null)
			this.else_stmt.optimize();
	}

	@Override
	public List<Statement> translate(Task parent)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(parent);

		this.task = parent.newChild("if");

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

		IfStatement ifstmt = ast.newIfStatement();
		ifstmt.setExpression(this.expr.translate(task, false));

		List<Statement> then_stmts = this.then_stmt.translate(task);
		if (then_stmts.size() > 1)
		{
			Block then_block = ast.newBlock();
			then_block.statements().addAll(then_stmts);
			ifstmt.setThenStatement(then_block);
		} else
			ifstmt.setThenStatement(then_stmts.get(0));

		if (this.else_stmt != null)
		{
			List<Statement> else_stmts = this.else_stmt.translate(task);
			if (else_stmts.size() > 1)
			{
				Block else_block = ast.newBlock();
				else_block.statements().addAll(then_stmts);
				ifstmt.setElseStatement(else_block);
			} else
				ifstmt.setElseStatement(else_stmts.get(0));
		}

		return Arrays.asList((Statement)ifstmt);
	}
}
