package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.SimpleName;

public class ESimpleNameExpression extends ENameExpression
{

	public ESimpleNameExpression(EAST east, SimpleName original, EASTDataNode scope)
	{
		super(east, original, scope);
		this.simple = true;
	}
	
	/* factory */
	public static ESimpleNameExpression create(EAST east, SimpleName original, EASTDataNode scope)
	{
		return new ESimpleNameExpression(east, original, scope);
	}
	
	@Override
	public SimpleName getOriginal()
	{
		return (SimpleName) this.original;
	}
}