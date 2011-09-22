package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class ETypeDeclaration extends EAbstractTypeDeclaration
{
	TypeDeclaration origin;
	List<EFieldDeclaration> fields;
	List<EMethodDeclaration> methods;

	ETypeDeclaration(EAST east, TypeDeclaration origin)
	{
		super(east);
		this.origin = origin;

		/* FIXME: not suported yet */
		assert(!origin.isInterface());
		assert(origin.getSuperclassType() == null);

		this.fields = new ArrayList<EFieldDeclaration>();
		for (Object field : origin.getFields())
			this.fields.add(this.east.extend((FieldDeclaration) field));

		this.methods = new ArrayList<EMethodDeclaration>();
		for (Object method : origin.getMethods())
			this.methods.add(this.east.extend((MethodDeclaration) method));
	}

	public TypeDeclaration translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		TypeDeclaration type = ast.newTypeDeclaration();
		type.setName((SimpleName) ASTNode.copySubtree(ast, this.origin.getName()));

		for (EFieldDeclaration field : this.fields)
			type.bodyDeclarations().add(field.translate(cus));

		for (EMethodDeclaration method : this.methods)
			type.bodyDeclarations().add(method.translate(cus));

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
