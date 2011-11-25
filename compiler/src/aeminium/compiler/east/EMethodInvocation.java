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
	IMethodBinding binding;
	EMethodDeclaration declaration;
	
	EMethodInvocation(EAST east, MethodInvocation origin)
	{
		super(east);

		this.origin = origin;

		this.expr = this.east.extend(origin.getExpression());
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
			this.args.add(this.east.extend((Expression) arg));
		
		// TODO: add internal dependencies (System.out, and other statics here)?
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.expr.optimize();
		
		for (EExpression arg : this.args)
			arg.optimize(); 
		
		this.declaration = (EMethodDeclaration) this.east.getNode(this.east.resolveName(this.origin.resolveMethodBinding()));
		this.binding = this.origin.resolveMethodBinding();
		this.type = this.origin.resolveTypeBinding();
	}

	@Override
	public Expression translate(Task parent, boolean reads)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.buildSequential(parent);

		/* in self task */
		this.task = parent.newStrongDependency("invoke");
		this.task.setInvocation();

		Type ret_type = this.east.buildTypeFromBinding(this.type);
		if (ret_type instanceof PrimitiveType)
			ret_type = this.east.boxPrimitiveType((PrimitiveType) ret_type);

		ParameterizedType caller_type = ast.newParameterizedType(ast.newSimpleType(ast.newName("aeminium.runtime.CallerBody")));
		caller_type.typeArguments().add((Type) ASTNode.copySubtree(ast, ret_type));

		this.task.setSuperClass(caller_type);

		Block execute = ast.newBlock();
		execute.statements().add(ast.newExpressionStatement(this.build(task)));
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

	public Expression buildSequential(Task task)
	{
		System.err.println("TODO sequential invocation");
		return null;
	}

	public Expression build(Task task)
	{
		AST ast = this.east.getAST();

		String method_name = this.east.resolveName(this.binding);
		EMethodDeclaration method = (EMethodDeclaration) this.east.getNode(method_name);

		if (method == null || !method.isAEminium())
		{
			System.err.println("TODO: sequential invocation");
			return null;
		}
		
		ClassInstanceCreation create = ast.newClassInstanceCreation();
		create.setType(ast.newSimpleType(ast.newSimpleName(method.task.getType())));	
		
		create.arguments().add(ast.newThisExpression());

		if (!method.isStatic())
			create.arguments().add(this.expr.translate(task, true));

		for (EExpression arg : this.args)
			create.arguments().add(arg.translate(task, true));

		// TODO check if method read/write operations on the owner object (this) and on the arguments
		// assuming worst case of always write
		if (!method.isStatic())
			this.expr.setWriteTask(task);

		for (EExpression arg : this.args)
			arg.setWriteTask(task);

		return create;
	}

	@Override
	public void setWriteTask(Task writer)
	{
		// nothing to do here
	}
}
