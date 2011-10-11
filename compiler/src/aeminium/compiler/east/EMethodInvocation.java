package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class EMethodInvocation extends EExpression
{
	MethodInvocation origin;

	EExpression expr;
	List<EExpression> args;

	ITypeBinding type;
	EMethodDeclaration declaration;

	EMethodInvocation(EAST east, MethodInvocation origin)
	{
		super(east);

		this.origin = origin;

		this.expr = this.east.extend(origin.getExpression());
		this.link(this.expr);

		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
		{
			EExpression earg = this.east.extend((Expression) arg);
			this.link(earg);
			this.args.add(earg);
		} 	
		
		// TODO: add internal dependencies (System.out, and other statics here)?
	}

	protected List<Task> getTasks()
	{
		System.out.println("eher");
		return super.getTasks();
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
			return this.buildNative();
	}

	public Expression build(Task task, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		System.out.println("Task parent: "+ this.task.parent);

		AST ast = this.east.getAST();

		if (this.declaration != null && this.declaration.isAEminium())
		{
			List<Expression> arguments = new ArrayList<Expression>();
			List<Expression> dependencies = new ArrayList<Expression>();
		
			if (this.declaration.getModifier("static") == null)
				arguments.add(expr.translate(task, cus, prestmts));

			for (EExpression arg : this.args)
				arguments.add(arg.translate(task, cus, prestmts));

			prestmts.addAll(task.scheduleSubtask(this.declaration.task, arguments, dependencies));

			FieldAccess ret_body = task.getBodyAccess();

			FieldAccess ret_access = ast.newFieldAccess();
			ret_access.setExpression(ret_body);
			ret_access.setName(ast.newSimpleName("_ret"));

			System.out.println("Body:"+ ret_access);
			return ret_access;
		} else
			return this.buildNative(task, cus, prestmts);
	}

	public Expression build(Task task, List<CompilationUnit> cus, List<Statement> prestmts)
	{
		AST ast = this.east.getAST();

		MethodInvocation method = ast.newMethodInvocation();
		method.setExpression(this.expr.translate(task, cus, prestmts);

		for (EExpression arg : this.args)
			arguments.add(arg.translate(task, cus, prestmts));
	}


	@Override
	public void optimize()
	{
		super.optimize();

		this.declaration = (EMethodDeclaration) this.east.getNode(this.east.resolveName(this.origin.resolveMethodBinding()));
		this.type = this.origin.resolveTypeBinding();
	}
}
