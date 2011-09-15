package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;

import aeminium.compiler.east.EAST;

import aeminium.compiler.east.EAbstractTypeDeclaration;
import aeminium.compiler.east.EFieldDeclaration;
import aeminium.compiler.east.EMethodDeclaration;

public class ETypeDeclaration extends EAbstractTypeDeclaration
{
	TypeDeclaration origin;
	List<EFieldDeclaration> fields;
	List<EMethodDeclaration> methods;

	ETypeDeclaration(TypeDeclaration origin)
	{
		this.origin = origin;

		/* not suported yet */
		assert(origin.isInterface());
		assert(origin.getSuperclassType() == null);

		this.fields = new ArrayList<EFieldDeclaration>();
		for (Object field : origin.getFields())
			this.fields.add(EAST.extend((FieldDeclaration) field));

		this.methods = new ArrayList<EMethodDeclaration>();
		for (Object method : origin.getMethods())
			this.methods.add(EAST.extend((MethodDeclaration) method));
	}

	public TypeDeclaration translate(AST ast, List<CompilationUnit> cus)
	{
		TypeDeclaration type = ast.newTypeDeclaration();
		type.setName((SimpleName) ASTNode.copySubtree(ast, this.origin.getName()));

		for (EFieldDeclaration field : this.fields)
			type.bodyDeclarations().add(field.translate(ast, cus));

		for (EMethodDeclaration method : this.methods)
			type.bodyDeclarations().add(method.translate(ast, cus));

		return type;
	}

	public void optimize()
	{
		for (EFieldDeclaration field : this.fields)
			field.optimize();

		for (EMethodDeclaration method : this.methods)
			method.optimize();
	}
}
