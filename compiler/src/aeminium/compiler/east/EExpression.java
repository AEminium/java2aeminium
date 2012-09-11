package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

public abstract class EExpression extends EASTExecutableNode implements EASTDataNode
{
	protected final EASTDataNode scope;
	protected final ITypeBinding binding;
	
	public EExpression(EAST east, Expression original, EASTDataNode scope)
	{
		super(east, original);
		
		this.scope = scope;
		this.binding = original.resolveTypeBinding();
	}
	
	public static EExpression create(EAST east, Expression expr, EASTDataNode scope)
	{
		if (expr instanceof ArrayCreation)
			return EArrayCreation.create(east, (ArrayCreation) expr, scope);
		
		if (expr instanceof ArrayInitializer)
			return EArrayInitializer.create(east, (ArrayInitializer) expr, scope);
		
		if (expr instanceof ArrayAccess)
			return EArrayAccess.create(east, (ArrayAccess) expr, scope);

		if (expr instanceof Assignment)
			return EAssignment.create(east, (Assignment) expr, scope);
		
		if (expr instanceof MethodInvocation)
			return EMethodInvocation.create(east, (MethodInvocation) expr, scope);
		
		if (expr instanceof Name)
			return ENameExpression.create(east, (Name) expr, scope);
		
		if (expr instanceof ClassInstanceCreation)
			return EClassInstanceCreation.create(east, (ClassInstanceCreation) expr, scope);
		
		if (expr instanceof NumberLiteral)
			return ENumberLiteral.create(east, (NumberLiteral) expr, scope);
		
		if (expr instanceof InfixExpression)
			return EInfixExpression.create(east, (InfixExpression) expr, scope);
	
		if (expr instanceof PrefixExpression)
			return EPrefixExpression.create(east, (PrefixExpression) expr, scope);
		
		if (expr instanceof ParenthesizedExpression)
			return EParenthesizedExpression.create(east, (ParenthesizedExpression) expr, scope);
		
		if (expr instanceof StringLiteral)
			return EStringLiteral.create(east, (StringLiteral) expr, scope);
				
		if (expr instanceof BooleanLiteral)
			return EBooleanLiteral.create(east, (BooleanLiteral) expr, scope);

		if (expr instanceof ThisExpression)
			return EThisExpression.create(east, (ThisExpression) expr, scope);
				
		if (expr instanceof FieldAccess)
			return EFieldAccess.create(east, (FieldAccess) expr, scope);
				
		System.err.println("Not implemented error: " + expr.getClass().getName());
		return null;
	}

	@Override
	public ETypeDeclaration getTypeDeclaration()
	{
		return this.scope.getTypeDeclaration();
	}
	
	@SuppressWarnings("unchecked")
	public Expression translate(List<CompilationUnit> out)
	{
		System.out.println(this + ": " + this.inlineTask);
		
		if (this.inlineTask)
			return this.build(out);
		
		out.add(this.task.translate());
		
		AST ast = this.getAST();
		
		/* in task */
		FieldAccess this_ret = ast.newFieldAccess();
		this_ret.setExpression(ast.newThisExpression());
		this_ret.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(this_ret);
		assign.setRightHandSide(this.build(out));

		this.task.getExecute().getBody().statements().add(ast.newExpressionStatement(assign));
		
		/* parent task */
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_" + this.task.getName()));

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(access);
		ret.setName(ast.newSimpleName("ae_ret"));

		this.postTranslate(this.task);

		return ret;
	}
	
	public Type getType()
	{
		return EType.build(this.getAST(), this.binding);
	}
	
	public boolean isVoid()
	{
		return this.getType().toString().equals("void");
	}
	
	public EASTDataNode getScope()
	{
		return this.scope;
	}
	
	public abstract Expression build(List<CompilationUnit> out);
}
