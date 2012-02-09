package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.SimpleName;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;

public class ESimpleNameExpression extends EExpression
{
	protected final IBinding binding;
	
	public ESimpleNameExpression(EAST east, SimpleName original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.binding = original.resolveBinding();
	}

	@Override
	public DataGroup getDataGroup()
	{
		return ((ESimpleNameDeclaration) this.east.getNode(this.binding)).getDataGroup();
	}

	@Override
	public SimpleName getOriginal()
	{
		return (SimpleName) this.original;
	}

	/* factory */
	public static ESimpleNameExpression create(EAST east, SimpleName original, EASTDataNode scope)
	{
		return new ESimpleNameExpression(east, original, scope);
	}

	@Override
	public void checkSignatures()
	{
		// Nothing
	}

	@Override
	public Signature getFullSignature()
	{
		// Nothing
		return new Signature();
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		// Nothing
	}
}
