package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EReturnStatement extends EStatement
{
	ReturnStatement origin;
	EExpression expr;

	EReturnStatement(EAST east, ReturnStatement origin)
	{
		super(east);
		this.origin = origin;

		if (origin.getExpression() != null)
			this.expr = this.east.extend((Expression) origin.getExpression());
	}
	
	@Override
	public void optimize()
	{
		super.optimize();
		this.expr.optimize();
	}

	@Override
	public Statement translate(Task parent)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());
		assert(this.expr != null);

		if (!this.isRoot())
			return this.build(parent);

		this.task = parent.newStrongDependency("ret");

		Block execute = ast.newBlock();
		execute.statements().add(this.build(task));
		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

		return ast.newEmptyStatement();
	}

	public Statement build(Task task)
	{
		AST ast = this.east.getAST();

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(task.getRootBody());
		ret.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(ret);
		assign.setRightHandSide(this.expr.translate(task)); 

		return ast.newExpressionStatement(assign);
	}
}
