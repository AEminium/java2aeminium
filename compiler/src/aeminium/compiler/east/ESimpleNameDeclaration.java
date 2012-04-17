package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.NodeDependency;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.SimpleDataGroup;

public class ESimpleNameDeclaration extends EASTNode implements EASTDataNode
{
	protected final EASTDeclaringNode scope;

	protected final DataGroup datagroup;
	protected final IBinding binding;
	
	public ESimpleNameDeclaration(EAST east, SimpleName original, EASTDeclaringNode scope)
	{
		super(east, original);

		this.scope = scope;
		this.binding = original.resolveBinding();
		
		this.datagroup = scope.getDataGroup().append(new SimpleDataGroup(original.toString()));

		this.east.addNode(this.binding, this);
	}
	
	/* Factory */
	public static ESimpleNameDeclaration create(EAST east, SimpleName original, EASTDeclaringNode scope)
	{
		return new ESimpleNameDeclaration(east, original, scope);
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
	
	public NodeDependency getDependency()
	{
		return this.scope.getDependency();
	}
}