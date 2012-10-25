package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class ESimpleNameDeclaration extends EASTNode implements EASTDataNode
{
	protected final EASTDeclaringNode scope;

	protected final DataGroup datagroup;
	protected final IBinding binding;
	
	public final ESimpleNameDeclaration base;
	
	public ESimpleNameDeclaration(EAST east, SimpleName original, EASTDeclaringNode scope, EASTExecutableNode parent, ESimpleNameDeclaration base)
	{
		super(east, original);

		this.scope = scope;
		this.binding = original.resolveBinding();
		
		if (base != null)
			this.datagroup = base.datagroup;
		else
			this.datagroup = scope.getDataGroup().append(new SimpleDataGroup(original.toString()));

		this.base = base;
		
		this.east.addNode(this.binding, this);
	}
	
	/* Factory */
	public static ESimpleNameDeclaration create(EAST east, SimpleName original, EASTDeclaringNode scope, EASTExecutableNode parent, ESimpleNameDeclaration base)
	{
		return new ESimpleNameDeclaration(east, original, scope, parent, base);
	}
	
	@Override
	public SimpleName getOriginal()
	{
		return (SimpleName) this.original;
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
	
	public Task getDeclaringTask()
	{
		return this.scope.getTask();
	}

	@Override
	public EASTDataNode getScope()
	{
		return this.scope;
	}
}