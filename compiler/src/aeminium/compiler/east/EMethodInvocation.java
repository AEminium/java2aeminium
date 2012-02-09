package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemDeferred;
import aeminium.compiler.signature.SimpleDataGroup;

public class EMethodInvocation extends EExpression
{
	protected final IMethodBinding binding;
	
	protected final DataGroup datagroup;

	protected final EExpression expr;
	protected final ArrayList<EExpression> arguments;
	
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

		this.signature.addItem(new SignatureItemDeferred(method, this.getDataGroup(), dgExpr, dgsArgs));
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
			this.expr.checkDependencies(stack);
		
		for (EExpression arg : this.arguments)
			arg.checkDependencies(stack);
		
		Signature closure = this.signature.closure();
		
		System.out.println("Closure: ");
		System.out.println(closure);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, closure);
		
		for (EASTExecutableNode node : deps)
		{
			if (node.equals(this.expr) || this.arguments.contains(node))
				this.strongDependencies.add(node);
			else
				this.weakDependencies.add(node);
		}
	}
}
