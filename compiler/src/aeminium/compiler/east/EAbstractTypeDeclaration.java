package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import aeminium.compiler.east.EASTNode;
import aeminium.compiler.east.EBodyDeclaration;

public abstract class EAbstractTypeDeclaration extends EBodyDeclaration
{
	EAbstractTypeDeclaration(EAST east)
	{
		super(east);
	}

	public abstract AbstractTypeDeclaration translate(List<CompilationUnit> cus);
	public abstract void optimize();
}
