package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.NumberLiteral;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SimpleDataGroup;

public class ENumberLiteral extends EExpression
{
	protected final DataGroup datagroup;
	
	public ENumberLiteral(EAST east, NumberLiteral original, EASTDataNode scope)
	{
		super(east, original, scope);
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup("literal"));
	}

	/* factory */
	public static ENumberLiteral create(EAST east, NumberLiteral original, EASTDataNode scope)
	{
		return new ENumberLiteral(east, original, scope);
	}
	
	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public NumberLiteral getOriginal()
	{
		return (NumberLiteral) this.original;
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
