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
	ITypeBinding binding;

	EInfixExpression(EAST east, InfixExpression origin)
	{
		super(east);

		this.origin = origin;

		this.left = this.east.extend(origin.getLeftOperand());
		this.right = this.east.extend(origin.getRightOperand());

		if (origin.hasExtendedOperands())
		{
			this.extended = new ArrayList<EExpression>();
			for (Object ext : origin.extendedOperands())
				this.extended.add(this.east.extend((Expression) ext));	
		}
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.left.optimize();
		this.right.optimize();

		if (this.extended != null)
			for (EExpression ext : this.extended)
				ext.optimize();

		// TODO: allow constant resolving
		this.constant = this.origin.resolveConstantExpressionValue();
		this.binding = this.origin.resolveTypeBinding();
	}

	@Override
	public Expression translate(Task parent, boolean write)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(parent);

		/* in self task */
		this.task = parent.newStrongDependency("infix");
		this.task.addField(this.east.buildTypeFromBinding(this.binding), "ae_ret", true);

		Block execute = ast.newBlock();

		FieldAccess this_ret = ast.newFieldAccess();
		this_ret.setExpression(ast.newThisExpression());
		this_ret.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(this_ret);
		assign.setRightHandSide(this.build(task));
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

	public Expression build(Task task)
	{
		AST ast = this.east.getAST();

		InfixExpression infix = ast.newInfixExpression();
		infix.setLeftOperand(this.left.translate(task, false));
		infix.setRightOperand(this.right.translate(task, false));
		infix.setOperator(this.origin.getOperator());

		if (this.extended != null)
		{
			for (EExpression ext: this.extended)
				infix.extendedOperands().add(ext.translate(task, false));
		}

		return infix;
	}
}
