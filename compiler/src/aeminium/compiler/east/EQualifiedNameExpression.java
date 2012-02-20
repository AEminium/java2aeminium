package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.QualifiedName;

public class EQualifiedNameExpression extends ENameExpression
{
	public EQualifiedNameExpression(EAST east, QualifiedName original, EASTDataNode scope)
	{
		super(east, original, scope);
	}
	
	/* factory */
	public static EQualifiedNameExpression create(EAST east, QualifiedName original, EASTDataNode scope)
	{
		return new EQualifiedNameExpression(east, original, scope);
	}
	
	@Override
	public QualifiedName getOriginal()
	{
		return (QualifiedName) this.original;
	}
}