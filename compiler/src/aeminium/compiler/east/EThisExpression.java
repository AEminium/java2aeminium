package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EThisExpression extends EExpression
{
	ThisExpression origin;
	ITypeBinding binding;
	
	EThisExpression(EAST east, ThisExpression origin)
	{
		super(east);

		this.origin = origin;
		this.binding = this.origin.resolveTypeBinding();
	}

	@Override
	public void optimize()
	{
		super.optimize();
	}

	@Override
	public Expression translate(Task parent, boolean write)
	{
		// TODO/FIXME: add this as a dependency when write == true??? 

		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(parent);

		/* in self task */
		this.task = parent.newStrongDependency("this");

		this.task.addField(ast.newSimpleType(ast.newName(this.binding.getQualifiedName())), "ae_ret", true);

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
		// TODO: write

		AST ast = this.east.getAST();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(task.getPathToRoot());
		access.setName(ast.newSimpleName("ae_this"));

		return access;
	}
}
