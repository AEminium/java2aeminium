package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.SimpleName;

public class ESimpleNameExpression extends ENameExpression
{

	public ESimpleNameExpression(EAST east, SimpleName original, EASTDataNode scope, EASTExecutableNode parent, ESimpleNameExpression base)
	{
		super(east, original, scope, parent, base);
	}
	
	/* factory */
	public static ESimpleNameExpression create(EAST east, SimpleName original, EASTDataNode scope, EASTExecutableNode parent, ESimpleNameExpression base)
	{
		return new ESimpleNameExpression(east, original, scope, parent, base);
	}
	
	@Override
	public SimpleName getOriginal()
	{
		return (SimpleName) this.original;
	}
}