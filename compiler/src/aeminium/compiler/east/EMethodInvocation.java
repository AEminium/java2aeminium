package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItem;
import aeminium.compiler.signature.SignatureItemDeferred;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EMethodInvocation extends EDeferredExpression
{
	protected final DataGroup datagroup;

	protected final EExpression expr;
	protected final ArrayList<EExpression> arguments;
	
	public EMethodInvocation(EAST east, MethodInvocation original, EASTDataNode scope)
	{
		super(east, original, scope, original.resolveMethodBinding());
		
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
			this.deferred = new SignatureItemDeferred(this.deferredDependency, method, this.getDataGroup(), dgExpr, dgsArgs);
			this.signature.addItem(this.deferred);
			
			for (int i = 0; i < method.parameters.size(); i++)
				this.signature.addItem(new SignatureItemRead(this.dependency, dgsArgs.get(i)));
		} else
		{
			Signature def = this.getEAST().getCompiler().getSignatureReader().getSignature(this.deferredDependency, this.binding.getKey(), this.getDataGroup(), dgExpr, dgsArgs);
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
			this.dependency.addStrong(this.expr.dependency);
		}
		
		for (EExpression arg : this.arguments)
		{
			arg.checkDependencies(stack);
			this.dependency.addStrong(arg.dependency);
		}
		
		Signature sig;
		if (this.deferred != null)
		{
			sig = this.deferred.closure();

			for (SignatureItem item : this.signature.getItems())
				if (!item.equals(this.deferred))
					sig.addItem(item);
		} else
			sig = this.signature;

		Set<Dependency> deps = stack.getDependencies(sig);
		this.dependency.addWeak(deps);
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
		// TODO inline ClassInstanceCreation
		System.out.println("TODO: EMethodInvocation.inline()");
		return 0;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "invoke");
		
		if (!this.isStatic())
			this.expr.preTranslate(this.task);
		
		for (EExpression arg : this.arguments)
			arg.preTranslate(this.task);
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		if (this.isAeminium())
		{
			MethodInvocation schedule = ast.newMethodInvocation();

			FieldAccess function = ast.newFieldAccess();
			function.setExpression(ast.newThisExpression());
			function.setName(ast.newSimpleName("ae_deferred"));
			
			schedule.setExpression(function);
			
			schedule.setName(ast.newSimpleName("schedule"));
			
			MethodInvocation deps = ast.newMethodInvocation();
			deps.setExpression(ast.newName("java.util.Arrays"));
			deps.setName(ast.newSimpleName("asList"));
			
			FieldAccess task = ast.newFieldAccess();
			task.setExpression(ast.newThisExpression());
			task.setName(ast.newSimpleName("ae_task"));
			
			deps.arguments().add(task);
			
			schedule.arguments().add(deps);
	
			if (!this.isStatic())
				schedule.arguments().add(this.expr.translate(out));
	
			for (EExpression arg : this.arguments)
				schedule.arguments().add(arg.translate(out));

			return schedule;
		} else
		{
			MethodInvocation invoke = ast.newMethodInvocation();
			invoke.setName(ast.newSimpleName(this.binding.getName()));

			if (!this.isStatic())
				invoke.setExpression(this.expr.translate(out));
			else if (this.expr != null)
				invoke.setExpression((Expression) ASTNode.copySubtree(ast, this.expr.getOriginal()));

			for (EExpression arg : this.arguments)
				invoke.arguments().add(arg.translate(out));

			return invoke;
		}
	}
		
	public boolean isStatic()
	{
		for (ModifierKeyword keyword : this.getModifiers())
			if (keyword.toString().equals("static"))
				return true;

		return false;
	}
}