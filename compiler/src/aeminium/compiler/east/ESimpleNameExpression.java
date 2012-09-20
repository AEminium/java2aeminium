package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.SimpleName;

public class ESimpleNameExpression extends ENameExpression
{

	public ESimpleNameExpression(EAST east, SimpleName original, EASTDataNode scope, ESimpleNameExpression base)
	{
		super(east, original, scope, base);
	}
	
	/* factory */
	public static ESimpleNameExpression create(EAST east, SimpleName original, EASTDataNode scope, ESimpleNameExpression base)
	{
		return new ESimpleNameExpression(east, original, scope, base);
	}
	
	@Override
	public SimpleName getOriginal()
	{
		return (SimpleName) this.original;
	}
}