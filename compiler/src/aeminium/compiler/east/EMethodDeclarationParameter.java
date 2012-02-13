package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import aeminium.compiler.signature.*;

public class EMethodDeclarationParameter extends EASTNode implements EASTDataNode
{
	protected final EASTDataNode scope;
	protected final DataGroup datagroup;
	
	protected final ESimpleNameDeclaration name;
	
	public EMethodDeclarationParameter(EAST east, SingleVariableDeclaration original, EASTDataNode scope)
	{
		super(east, original);

		this.scope = scope;
		
		this.datagroup = scope.getDataGroup();
		
		this.name = ESimpleNameDeclaration.create(this.east, original.getName(), this);
	}

	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public SingleVariableDeclaration getOriginal()
	{
		return (SingleVariableDeclaration) this.original;
	}

	public static EMethodDeclarationParameter create(EAST east, SingleVariableDeclaration param, EASTDataNode scope)
	{
		return new EMethodDeclarationParameter(east, param, scope);
	}
}