package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemDeferred;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EMethodInvocation extends EExpression
{
	protected final IMethodBinding binding;
	
	protected final DataGroup datagroup;

	protected final EExpression expr;
	protected final ArrayList<EExpression> arguments;
	
	/* checkSignature */
	protected SignatureItemDeferred deferred;
	
	public EMethodInvocation(EAST east, MethodInvocation original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.binding = original.resolveMethodBinding();
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("invoke " + original.getName().toString()));
		
		if (original.getExpression() == null)
			this.expr = null;
		else
			this.expr = EExpression.create(this.east, original.getExpression(), scope);
		
		this.arguments = new ArrayList<EExpression>();
		for (Object arg : original.arguments())
			this.arguments.add(EExpression.create(this.east, (Expression) arg, scope));
	}

	/* factory */
	public static EMethodInvocation create(EAST east, MethodInvocation invoke, EASTDataNode scope)
	{
		return new EMethodInvocation(east, invoke, scope);
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
		if (this.expr != null)
			this.expr.checkSignatures();
		
		for (EExpression arg : this.arguments)
			arg.checkSignatures();
		
		EMethodDeclaration method = ((EMethodDeclaration) this.east.getNode(this.binding));
		
		ArrayList<DataGroup> dgsArgs = new ArrayList<DataGroup>();
		for (EExpression arg : this.arguments)
			dgsArgs.add(arg.getDataGroup());
		
		DataGroup dgExpr = this.expr == null ? null : this.expr.getDataGroup();

		this.deferred = new SignatureItemDeferred(method, this.getDataGroup(), dgExpr, dgsArgs);
		this.signature.addItem(this.deferred);
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();

		sig.addAll(this.signature);
		
		if (this.expr != null)
			sig.addAll(this.expr.getFullSignature());
		
		for (EExpression arg : this.arguments)
			sig.addAll(arg.getFullSignature());

		return sig;
	}
	
	@Override
	public void checkDependencies(DependencyStack stack)
	{
		if (this.expr != null)
		{
			this.expr.checkDependencies(stack);
			this.strongDependencies.add(this.expr);
		}
		
		for (EExpression arg : this.arguments)
		{
			arg.checkDependencies(stack);
			this.strongDependencies.add(arg);
		}
		
		Signature closure = this.deferred.closure();

		Set<EASTExecutableNode> deps = stack.getDependencies(this, closure);
		
		for (EASTExecutableNode node : deps)
			if (!node.equals(this.expr) && !this.arguments.contains(node))
				this.weakDependencies.add(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = super.optimize();

		if (this.expr != null)
			sum += this.expr.optimize();
		
		for (EExpression arg : this.arguments)
			sum += arg.optimize();
		
		return sum;
	}
	
	@Override
	public int inline(EASTExecutableNode inlineTo)
	{
		// TODO inline ClassInstanceCreation
		System.out.println("TODO: EMethodInvocation.inline()");
		return 0;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "invoke");
		
		if (this.expr != null)
			this.expr.preTranslate(this.task);
		
		for (EExpression arg : this.arguments)
			arg.preTranslate(this.task);
	}

	@SuppressWarnings("unchecked")
	public Expression translate(List<CompilationUnit> out)
	{
		if (this.inlineTask)
			return this.build(out);
		
		out.add(this.task.translate());
		
		AST ast = this.getAST();
		
		/* in task */
		this.task.getExecute().getBody().statements().add(ast.newExpressionStatement(this.build(out)));

		/* parent task */
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_" + this.task.getName()));

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(access);
		ret.setName(ast.newSimpleName("ae_ret"));

		return ret;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();
		
		EMethodDeclaration method = this.getMethod();

		ClassInstanceCreation create = ast.newClassInstanceCreation();
		create.setType(ast.newSimpleType(ast.newSimpleName(method.getTask().getName())));	
		
		create.arguments().add(ast.newThisExpression());

		if (!method.isStatic())
			create.arguments().add(this.expr.translate(out));

		for (EExpression arg : this.arguments)
			create.arguments().add(arg.translate(out));

		return create;
	}
	
	public EMethodDeclaration getMethod()
	{
		return (EMethodDeclaration) this.east.getNode(this.binding);
	}
}