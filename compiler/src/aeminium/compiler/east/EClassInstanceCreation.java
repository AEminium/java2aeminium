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
	IMethodBinding binding;

	EClassInstanceCreation(EAST east, ClassInstanceCreation origin)
	{
		super(east);

		this.origin = origin;
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
		{
			EExpression earg = this.east.extend((Expression) arg);
			this.link(earg);
			this.args.add(earg);
		}

		// TODO: add internal dependencies (System.out, and other statics here)?
	}

	@Override
	public void optimize()
	{
		super.optimize();

		// TODO check if the constructor being used has @AEminium
		// if not, this call must be serialized, or at least run a serial version in a task that is paralell.
 	}

	@Override
	public Expression translate(Task parent, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		if (this.isRoot())
		{
			this.task = parent.newSubtask(cus);
		
			// TODO: IMPROVE alow @AEminium on constructors? 
			AST ast = this.east.getAST();

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

			this.task.addField(this.origin.getType(), "_ret");

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

			// TODO add task creation and etc..
			FieldAccess ret_body = this.task.getBodyAccess();

			FieldAccess ret_access = ast.newFieldAccess();
			ret_access.setExpression(ret_body);
			ret_access.setName(ast.newSimpleName("_ret"));

			return ret_access;
		} else
		{
			return this.build(task, cus, prestmts);
		}
	}

	public Expression build(Task task, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		AST ast = this.east.getAST();

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType((SimpleType) ASTNode.copySubtree(ast, this.origin.getType()));

		for (EExpression arg : this.args)
			creation.arguments().add(arg.translate(task, cus, prestmts));

		return creation;
	}
}
