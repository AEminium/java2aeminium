package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import aeminium.compiler.signature.*;
import aeminium.compiler.task.Task;

public class EMethodDeclarationParameter extends EASTNode implements EASTDeclaringNode
{
	protected final EASTDeclaringNode scope;
	protected final DataGroup datagroup;
	
	protected final ESimpleNameDeclaration name;
	
	public EMethodDeclarationParameter(EAST east, SingleVariableDeclaration original, EASTDeclaringNode scope)
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

	public static EMethodDeclarationParameter create(EAST east, SingleVariableDeclaration param, EASTDeclaringNode scope)
	{
		return new EMethodDeclarationParameter(east, param, scope);
	}

	@Override
	public Task getTask()
	{
		return this.scope.getTask();
	}
}