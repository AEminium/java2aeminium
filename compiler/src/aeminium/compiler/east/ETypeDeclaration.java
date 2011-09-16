package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;
import aeminium.compiler.east.*;

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
		assert(origin.isInterface());
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

	public static void addExecuteMethod(AST ast, TypeDeclaration type, Block body)
	{
		MethodDeclaration execute = ast.newMethodDeclaration();
		execute.setName(ast.newSimpleName("execute"));
		execute.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		SingleVariableDeclaration runtime = ast.newSingleVariableDeclaration();
		runtime.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Runtime")));
		runtime.setName(ast.newSimpleName("rt"));

		execute.parameters().add(runtime);

		SingleVariableDeclaration task = ast.newSingleVariableDeclaration();
		task.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));
		task.setName(ast.newSimpleName("task"));

		execute.parameters().add(task);

		execute.thrownExceptions().add(ast.newSimpleName("Exception"));
		execute.setBody(body);

		type.bodyDeclarations().add(execute);
	}
}
