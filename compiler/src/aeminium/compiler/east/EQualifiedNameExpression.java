package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.QualifiedName;

public class EQualifiedNameExpression extends ENameExpression
{
	public EQualifiedNameExpression(EAST east, QualifiedName original, EASTDataNode scope, EQualifiedNameExpression base)
	{
		super(east, original, scope, base);
	}
	
	/* factory */
	public static EQualifiedNameExpression create(EAST east, QualifiedName original, EASTDataNode scope, EQualifiedNameExpression base)
	{
		return new EQualifiedNameExpression(east, original, scope, base);
	}
	
	@Override
	public QualifiedName getOriginal()
	{
		return (QualifiedName) this.original;
	}
}