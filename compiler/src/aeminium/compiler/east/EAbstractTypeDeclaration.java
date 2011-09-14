package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;

import aeminium.compiler.east.EASTNode;

public abstract class EAbstractTypeDeclaration extends EASTNode
{
	public abstract void translate(List<CompilationUnit> cus);
}
