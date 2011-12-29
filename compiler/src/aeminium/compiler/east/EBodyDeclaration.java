package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.EASTNode;

public abstract class EBodyDeclaration extends EASTNode
{
	EBodyDeclaration(EAST east)
	{
		super(east);
	}
	
	public abstract BodyDeclaration translate(List<CompilationUnit> cus);
}
