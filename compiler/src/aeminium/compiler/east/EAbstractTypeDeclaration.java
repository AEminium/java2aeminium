package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import aeminium.compiler.east.EBodyDeclaration;

public abstract class EAbstractTypeDeclaration extends EBodyDeclaration
{
	EAbstractTypeDeclaration(EAST east)
	{
		super(east);
	}

	public abstract void analyse();
	public abstract int optimize();
	public abstract void preTranslate();
	
	public abstract AbstractTypeDeclaration translate(List<CompilationUnit> cus);
}