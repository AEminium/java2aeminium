package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;

import aeminium.compiler.east.EAST;
import aeminium.compiler.east.EASTNode;
import aeminium.compiler.east.EAbstractTypeDeclaration;

public class ECompilationUnit extends EASTNode
{
	CompilationUnit origin;
	List<EAbstractTypeDeclaration> types;

	ECompilationUnit(CompilationUnit origin)
	{
		this.origin = origin;
		this.types = new ArrayList<EAbstractTypeDeclaration>();

		for (Object type : origin.types())
			this.types.add(EAST.extend((AbstractTypeDeclaration) type));
	}

	public void translate(List<CompilationUnit> cus)
	{
		for (EAbstractTypeDeclaration type : this.types)
			type.translate(cus);
	}
}
