package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;

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

	public void translate(AST ast, List<CompilationUnit> cus)
	{
		CompilationUnit unit = ast.newCompilationUnit();
		
		unit.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, this.origin.getPackage()));

		unit.imports().addAll(ASTNode.copySubtrees(ast, this.origin.imports()));

		ImportDeclaration helper = ast.newImportDeclaration();
		helper.setName(ast.newName("aeminium.runtime.AeminiumHelper"));
		unit.imports().add(helper);

		for (EAbstractTypeDeclaration type : this.types)
			unit.types().add(type.translate(ast, cus));

		cus.add(unit);
	}

	public void optimize()
	{
		for (EAbstractTypeDeclaration type : this.types)
			type.optimize();
	}
}
