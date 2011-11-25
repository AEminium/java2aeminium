package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EClassInstanceCreation extends EExpression
{
	ClassInstanceCreation origin;
	List<EExpression> args;

	EClassInstanceCreation(EAST east, ClassInstanceCreation origin)
	{
		super(east);

		this.origin = origin;
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
			this.args.add(this.east.extend((Expression) arg));
	}

	@Override
	public void optimize()
	{
		super.optimize();

		for (EExpression arg : this.args)
			arg.optimize();

		// TODO check if the constructor being used has @AEminium
		// if not, this call must be serialized, or at least run a serial version in a task that is paralell.
 	}

	@Override
	public Expression translate(Task parent, boolean reads)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(parent);

		/* in self task */
		this.task = parent.newStrongDependency("class");
		this.task.addField((Type) ASTNode.copySubtree(ast, this.origin.getType()), "ae_ret", true);

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

		ClassInstanceCreation create = ast.newClassInstanceCreation();
		create.setType((Type) ASTNode.copySubtree(ast, this.origin.getType()));

		// TODO calculate constructor read/write operations on parameters and "globals" (statics)
		// assuming worst case of write always
		for (EExpression arg: this.args)
			create.arguments().add(arg.translate(task, true));

		for (EExpression arg: this.args)
			arg.setWriteTask(task);

		return create;
	}

	@Override
	public void setWriteTask(Task writer)
	{
		System.err.println("TODO/FIXME: writing to a class creation, possibly ignore because no other ref is made");
	}
}
