package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EInfixExpression extends EExpression
{
	InfixExpression origin;
	EExpression left;
	EExpression right;
	List<EExpression> extended;

	Object constant;
	ITypeBinding type;

	EInfixExpression(EAST east, InfixExpression origin)
	{
		super(east);

		this.origin = origin;

		this.left = this.east.extend(origin.getLeftOperand());
		this.link(this.left);

		this.right = this.east.extend(origin.getRightOperand());
		this.link(this.right);

		if (origin.hasExtendedOperands())
		{
			this.extended = new ArrayList<EExpression>();
			for (Object ext : origin.extendedOperands())
			{
				EExpression expr = this.east.extend((Expression) ext);
				this.link(expr);
				this.extended.add(expr);	
			}
		}

		// TODO: allow constant resolving
		this.constant = this.origin.resolveConstantExpressionValue();
		this.type = this.origin.resolveTypeBinding();
	}

	@Override
	public void optimize()
	{
		super.optimize();
		this.root = false;
	}

	@Override
	public Expression translate(Task parent, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		if (this.isRoot())
		{
			AST ast = this.east.getAST();
			this.task = parent.newSubtask(cus);

			// task body
			Block body = ast.newBlock();

			// _ret = X(...);
			Assignment assign = ast.newAssignment();

			FieldAccess access = ast.newFieldAccess();
			access.setExpression(ast.newThisExpression());
			access.setName(ast.newSimpleName("_ret"));

			assign.setLeftHandSide(access);
			assign.setRightHandSide(this.build(this.task, cus, prestmts));
		
			body.statements().add(ast.newExpressionStatement(assign));

			this.task.addField(ast.newSimpleType(ast.newName(this.type.getQualifiedName())), "_ret");

			List<Expression> arguments = new ArrayList<Expression>();
			List<Expression> dependencies = new ArrayList<Expression>();
			arguments.add(ast.newThisExpression());

			List<Task> children = this.getChildTasks();
			for (Task child : children)
			{
				arguments.add(child.getBodyAccess());
				dependencies.add(child.getTaskAccess());
			}

			MethodDeclaration constructor = this.task.createDefaultConstructor(children);
			this.task.addConstructor(constructor);
			this.task.setExecute(body);
		
			prestmts.addAll(this.task.schedule(parent, arguments, dependencies));

			FieldAccess ret_body = this.task.getBodyAccess();

			FieldAccess ret_access = ast.newFieldAccess();
			ret_access.setExpression(ret_body);
			ret_access.setName(ast.newSimpleName("_ret"));

			return ret_access;
		} else
			return this.build(parent, cus, prestmts);
	}

	public Expression build(Task task, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		AST ast = this.east.getAST();

		InfixExpression expr = ast.newInfixExpression();
		expr.setOperator(this.origin.getOperator());
		expr.setLeftOperand(this.left.translate(task, cus, prestmts));
		expr.setRightOperand(this.right.translate(task, cus, prestmts));

		if (this.extended != null)
			for (EExpression oper : this.extended)
				expr.extendedOperands().add(oper.translate(task, cus, prestmts));

		return expr;
	}
}
