package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Type;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemDeferred;
import aeminium.compiler.signature.SignatureItemWrite;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class EClassInstanceCreation extends EExpression
{
	protected final DataGroup datagroup;
	
	protected final Type type;
	protected final IMethodBinding constructor;
	
	protected final ArrayList<EExpression> arguments;
	
	/* checkSignature */
	protected SignatureItemDeferred deferred;
	
	public EClassInstanceCreation(EAST east, ClassInstanceCreation original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.type = original.getType();
		this.constructor = original.resolveConstructorBinding();
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("new " + this.type.toString()));
		
		this.arguments = new ArrayList<EExpression>();
		for (Object arg : original.arguments())
			this.arguments.add(EExpression.create(east, (Expression) arg, this));
	}

	/* factory */
	public static EClassInstanceCreation create(EAST east, ClassInstanceCreation original, EASTDataNode scope)
	{
		return new EClassInstanceCreation(east, original, scope);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public ClassInstanceCreation getOriginal()
	{
		return (ClassInstanceCreation) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		for (EExpression arg : this.arguments)
			arg.checkSignatures();

		EMethodDeclaration method = ((EMethodDeclaration) this.east.getNode(this.constructor));
		
		ArrayList<DataGroup> datagroupsArgs = new ArrayList<DataGroup>();
		for (EExpression arg : this.arguments)
			datagroupsArgs.add(arg.getDataGroup());
		
		this.deferred = new SignatureItemDeferred(method, this.getDataGroup(), null, datagroupsArgs);
		this.signature.addItem(this.deferred);
		this.signature.addItem(new SignatureItemWrite(this.getDataGroup()));
	}
	
	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		
		for (EExpression arg : this.arguments)
			sig.addAll(arg.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		for (EExpression arg : this.arguments)
		{
			arg.checkDependencies(stack);
			this.strongDependencies.add(arg);
		}
		
		Signature closure = this.deferred.closure();
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, closure);
		
		for (EASTExecutableNode node : deps)
			if (!this.arguments.contains(node))
				this.weakDependencies.add(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		for (EExpression arg : this.arguments)
			sum += arg.optimize();
		
		return sum;
	}
	
	@Override
	public int inline(EASTExecutableNode inlineTo)
	{
		// TODO inline ClassInstanceCreation
		System.out.println("TODO: EClassInstanceCreation.inline()");
		return 0;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "new");
		
		for (EExpression arg : this.arguments)
			arg.preTranslate(this.task);
	}
	
	@Override
	public Expression build(List<CompilationUnit> out)
	{
		// TODO: EClassInstanceCreation.build();
		System.err.println("TODO: EClassInstanceCreation.build()");
		return null;
	}
}
