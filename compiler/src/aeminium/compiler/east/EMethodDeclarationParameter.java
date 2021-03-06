package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EMethodDeclarationParameter extends EASTNode implements EASTDeclaringNode
{
	protected final EASTDeclaringNode scope;
	protected final DataGroup datagroup;
	
	protected final ESimpleNameDeclaration name;
	
	public EMethodDeclarationParameter(EAST east, SingleVariableDeclaration original, EASTDeclaringNode scope, EASTExecutableNode parent, EMethodDeclarationParameter base)
	{
		super(east, original);

		this.scope = scope;
		
		this.datagroup = scope.getDataGroup();
		
		this.name = ESimpleNameDeclaration.create(this.east, original.getName(), this, parent, base == null ? null : base.name);
	}

	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}

	@Override
	public ETypeDeclaration getTypeDeclaration()
	{
		return this.scope.getTypeDeclaration();
	}
	
	@Override
	public SingleVariableDeclaration getOriginal()
	{
		return (SingleVariableDeclaration) this.original;
	}

	public static EMethodDeclarationParameter create(EAST east, SingleVariableDeclaration param, EASTDeclaringNode scope, EASTExecutableNode parent, EMethodDeclarationParameter base)
	{
		return new EMethodDeclarationParameter(east, param, scope, parent, base);
	}

	@Override
	public Task getTask()
	{
		return this.scope.getTask();
	}

	@Override
	public EASTDataNode getScope()
	{
		return this.scope;
	}
}