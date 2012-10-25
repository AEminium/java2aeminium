package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemDeferred;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EMethodInvocation extends EDeferredExpression
{
	private static final boolean OPTIMIZE_PARALLELIZE = true;
	
	protected final DataGroup datagroup;

	protected final EExpression expr;
	protected final ArrayList<EExpression> arguments;
	
	public EMethodInvocation(EAST east, MethodInvocation original, EASTDataNode scope, EASTExecutableNode parent, EMethodInvocation base)
	{
		super(east, original, scope, original.resolveMethodBinding(), parent, base);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("invoke " + original.getName().toString()));
		
		if (original.getExpression() == null)
			this.expr = null;
		else
			this.expr = EExpression.create(this.east, original.getExpression(), scope, this, base == null ? null : base.expr);
		
		this.arguments = new ArrayList<EExpression>();
		
		for (int i = 0; i < original.arguments().size(); i++)
		{
			this.arguments.add
			(
				EExpression.create
				(
					this.east,
					(Expression) original.arguments().get(i),
					scope,
					this,
					base == null ? null : base.arguments.get(i)
				)
			);
		}
	}

	/* factory */
	public static EMethodInvocation create(EAST east, MethodInvocation invoke, EASTDataNode scope, EASTExecutableNode parent, EMethodInvocation base)
	{
		return new EMethodInvocation(east, invoke, scope, parent, base);
	}
		
	@Override
	public MethodInvocation getOriginal()
	{
		return (MethodInvocation) this.original;
	}

	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}
	
	@Override
	public void checkSignatures()
	{
		if (!this.isStatic())
			this.expr.checkSignatures();
		
		for (EExpression arg : this.arguments)
			arg.checkSignatures();
			
		ArrayList<DataGroup> dgsArgs = new ArrayList<DataGroup>();
		for (EExpression arg : this.arguments)
			dgsArgs.add(arg.getDataGroup());
		
		DataGroup dgExpr = this.isStatic() ? null : this.expr.getDataGroup();

		EMethodDeclaration method = this.getMethod();
		if (method != null)
		{
			this.deferred = new SignatureItemDeferred(method, this.getDataGroup(), dgExpr, dgsArgs);
			this.signature.addItem(this.deferred);
		} else
		{
			Signature def = this.getEAST().getCompiler().getSignatureReader().getSignature(this.binding.getKey(), this.getDataGroup(), dgExpr, dgsArgs);
			this.signature.addAll(def);
		}
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();

		sig.addAll(this.signature);
		
		if (!this.isStatic())
			sig.addAll(this.expr.getFullSignature());
		
		for (EExpression arg : this.arguments)
			sig.addAll(arg.getFullSignature());

		
		return sig;
	}
	
	@Override
	public void checkDependencies(DependencyStack stack)
	{
		if (!this.isStatic())
		{
			this.expr.checkDependencies(stack);
			this.addStrongDependency(this.expr);
		}
		
		for (EExpression arg : this.arguments)
		{
			arg.checkDependencies(stack);
			this.addStrongDependency(arg);
		}
		
		Signature sig;
		if (this.deferred != null)
			sig = this.deferred.closure();
		else
			sig = this.signature;

		Set<EASTExecutableNode> deps = stack.getDependencies(this, sig);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;

		if (!this.isStatic())
			sum += this.expr.optimize();
		
		for (EExpression arg : this.arguments)
			sum += arg.optimize();
		
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public int inline(EASTExecutableNode inlineTo)
	{
		if (!this.isAeminium())
			return super.inline(inlineTo);
		
		// TODO inline ClassInstanceCreation
		System.err.println("TODO: EMethodInvocation.inline()");

		return 0;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "invoke", this.base == null ? null : this.base.task);
		
		if (!this.isStatic())
			this.expr.preTranslate(this.task);
		
		for (int i = 0; i < this.arguments.size(); i++)
			this.arguments.get(i).preTranslate(this.task);
	}
	
	/* Returns the inlined version */
	@SuppressWarnings("unchecked")
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		MethodInvocation invoke = ast.newMethodInvocation();
		invoke.setName(ast.newSimpleName(this.binding.getName()));

		if (this.isStatic())
			invoke.setExpression(ast.newName(this.binding.getDeclaringClass().getName()));
		else
			invoke.setExpression(this.expr.translate(out));

		for (EExpression arg : this.arguments)
			invoke.arguments().add(arg.translate(out));

		return invoke;
	}
		
	@SuppressWarnings("unchecked")
	public Statement buildStmt(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		if (!this.isAeminium())
			return ast.newExpressionStatement(this.build(out));

		ClassInstanceCreation create = ast.newClassInstanceCreation();
		create.setType(ast.newSimpleType(ast.newSimpleName(this.getMethod().getTask().getTypeName())));	
		
		create.arguments().add(ast.newThisExpression());

		if (!this.isStatic())
			create.arguments().add(this.expr.translate(out));

		for (EExpression arg : this.arguments)
			create.arguments().add(arg.translate(out));

		if (!EMethodInvocation.OPTIMIZE_PARALLELIZE)
			return ast.newExpressionStatement(create);
		
		IfStatement ifstmt = ast.newIfStatement();
		
		MethodInvocation parallelize = ast.newMethodInvocation();
		parallelize.setExpression(ast.newSimpleName("rt"));
		parallelize.setName(ast.newSimpleName("parallelize"));
				
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_ret"));
		
		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(access);

		/* can't use build() here because that would translate() arguments twice */
		MethodInvocation invoke = ast.newMethodInvocation();
		invoke.setName(ast.newSimpleName(this.binding.getName()));
		invoke.arguments().addAll(ASTNode.copySubtrees(ast, create.arguments()));
		
		invoke.arguments().remove(0); /* task pointer */
		if (this.isStatic())
			invoke.setExpression(ast.newName(this.binding.getDeclaringClass().getName()));
		else
		{
			invoke.setExpression(this.expr.translate(out));
			invoke.arguments().remove(0); /* ae_this */
		}
		
		assign.setRightHandSide(invoke);
		
		ifstmt.setExpression(parallelize);
		ifstmt.setThenStatement(ast.newExpressionStatement(create));
		ifstmt.setElseStatement(ast.newExpressionStatement(assign));
		
		return ifstmt;
	}
	
	public boolean isStatic()
	{
		for (ModifierKeyword keyword : this.getModifiers())
			if (keyword.toString().equals("static"))
				return true;

		return false;
	}
}