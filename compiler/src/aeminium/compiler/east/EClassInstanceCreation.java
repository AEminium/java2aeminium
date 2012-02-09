package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.Type;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemDeferred;
import aeminium.compiler.signature.SignatureItemWrite;
import aeminium.compiler.signature.SimpleDataGroup;

public class EClassInstanceCreation extends EExpression
{
	protected final DataGroup datagroup;
	
	protected final Type type;
	protected final IMethodBinding constructor;
	
	protected final ArrayList<EExpression> arguments;
	
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
		
		this.signature.addItem(new SignatureItemWrite(this.getDataGroup()));
		this.signature.addItem(new SignatureItemDeferred(method, this.getDataGroup(), null, datagroupsArgs));
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
			arg.checkDependencies(stack);
		
		this.signature.closure();
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
		{
			if (this.arguments.contains(node))
				this.strongDependencies.add(node);
			else
				this.weakDependencies.add(node);
		}
	}
}
