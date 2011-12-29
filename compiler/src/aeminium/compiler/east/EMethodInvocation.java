package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Task;
import aeminium.compiler.datagroup.DataGroup;
import aeminium.compiler.datagroup.SignatureItemInvocation;

public class EMethodInvocation extends EExpression
{
	private final MethodInvocation origin;

	private final EExpression expr;
	private final List<EExpression> args;

	EMethodDeclaration declaration;
	ITypeBinding type;
	IMethodBinding binding;
	
	EMethodInvocation(EAST east, MethodInvocation origin)
	{
		super(east);

		this.origin = origin;

		this.expr = this.east.extend(origin.getExpression());
		this.args = new ArrayList<EExpression>();

		for (Object arg : origin.arguments())
			this.args.add(this.east.extend((Expression) arg));
	}

	@Override
	public void analyse()
	{
		super.analyse();

		this.expr.analyse();
		
		for (EExpression arg : this.args)
			arg.analyse(); 
		
		this.binding = this.origin.resolveMethodBinding();
		this.type = this.origin.resolveTypeBinding();
		this.declaration = (EMethodDeclaration) this.east.getNode(this.east.resolveName(this.binding));


		this.signature.addFrom(this.expr.getSignature());
		for (EExpression arg : this.args)
			this.signature.addFrom(arg.getSignature());
		
		this.signature.add(new SignatureItemInvocation(this));
		/*
		 * DO NOT add arguments (or even "this") as a read signature
		 * after replacing arguments they will have their respective read/writes
		 * if they occur at all
		*/
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		sum += this.expr.optimize();
		for (EExpression arg : this.args)
			sum += arg.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newStrongDependency("invoke");

		this.expr.preTranslate(this.task);
		
		for (EExpression arg : this.args)
			arg.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(cus);

		cus.add(this.task.getCompilationUnit());

		/* in self task */
		Type ret_type = this.east.buildTypeFromBinding(this.type);

		if (!this.isSequential())
		{
			this.task.setInvocation();

			if (ret_type instanceof PrimitiveType)
				ret_type = this.east.boxPrimitiveType((PrimitiveType) ret_type);

			ParameterizedType caller_type = ast.newParameterizedType(ast.newSimpleType(ast.newName("aeminium.runtime.CallerBody")));
			caller_type.typeArguments().add((Type) ASTNode.copySubtree(ast, ret_type));

			this.task.setSuperClass(caller_type);
		}

		Block execute = ast.newBlock();
		execute.statements().add(ast.newExpressionStatement(this.build(cus)));
		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

		/* in parent task */
		if ((ret_type instanceof PrimitiveType) &&
			((PrimitiveType) ret_type).getPrimitiveTypeCode() == PrimitiveType.VOID)
			return null;

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName(this.task.getName()));

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(access);
		ret.setName(ast.newSimpleName("ae_ret"));

		return ret;
	}

	public boolean isSequential()
	{
		String method_name = this.east.resolveName(this.binding);
		EMethodDeclaration method = (EMethodDeclaration) this.east.getNode(method_name);

		return method == null || !method.isAEminium();
	}

	@SuppressWarnings("unchecked")
	public Expression buildSequential(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		MethodInvocation invoke = ast.newMethodInvocation();
		invoke.setExpression(this.expr.translate(cus));

		for (EExpression arg : this.args)
			invoke.arguments().add(arg.translate(cus));

		return invoke;
	}

	@SuppressWarnings("unchecked")
	public Expression build(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		if (this.isSequential())
			return this.buildSequential(cus);

		String method_name = this.east.resolveName(this.binding);
		EMethodDeclaration method = (EMethodDeclaration) this.east.getNode(method_name);

		ClassInstanceCreation create = ast.newClassInstanceCreation();
		create.setType(ast.newSimpleType(ast.newSimpleName(method.getTask().getType())));	
		
		create.arguments().add(ast.newThisExpression());

		if (!method.isStatic())
			create.arguments().add(this.expr.translate(cus));

		for (EExpression arg : this.args)
			create.arguments().add(arg.translate(cus));

		return create;
	}

	@Override
	public DataGroup getDataGroup()
	{
		return this.declaration.getReturnDataGroup();
	}
}