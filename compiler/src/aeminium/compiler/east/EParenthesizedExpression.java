package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EParenthesizedExpression extends EExpression
{
	ParenthesizedExpression origin;
	EExpression expr;

	Object constant;
	ITypeBinding binding;

	EParenthesizedExpression(EAST east, ParenthesizedExpression origin)
	{
		super(east);

		this.origin = origin;
	
		this.expr = this.east.extend(origin.getExpression());
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.expr.optimize();

		// TODO: allow constant resolving
		this.constant = this.origin.resolveConstantExpressionValue();
		this.binding = this.origin.resolveTypeBinding();
	}

	@Override
	public Expression translate(Task parent, boolean read)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(parent, read);

		/* in self task */
		this.task = parent.newStrongDependency("paren");
		this.task.addField(this.east.buildTypeFromBinding(this.binding), "ae_ret", true);

		Block execute = ast.newBlock();

		FieldAccess this_ret = ast.newFieldAccess();
		this_ret.setExpression(ast.newThisExpression());
		this_ret.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(this_ret);
		assign.setRightHandSide(this.build(task, read));
		execute.statements().add(ast.newExpressionStatement(assign));

		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

		/* in parent task */
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName(this.task.getName()));

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(access);
		ret.setName(ast.newSimpleName("ae_ret"));

		return ret;
	}

	public Expression build(Task task, boolean read)
	{
		return this.expr.translate(task, read);
	}

	public void setWriteTask(Task task)
	{
		this.expr.setWriteTask(task);
	}
}
