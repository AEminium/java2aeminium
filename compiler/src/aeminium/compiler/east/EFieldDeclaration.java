package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;
import aeminium.compiler.east.*;

public class EFieldDeclaration extends EBodyDeclaration
{
	EAST east;
	FieldDeclaration origin;

	EFieldDeclaration(EAST east, FieldDeclaration origin)
	{
		super(east);
		this.east = east;
		this.origin = origin;
	}

	public FieldDeclaration translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		FieldDeclaration field = (FieldDeclaration) ASTNode.copySubtree(origin.getAST(), this.origin);
		field.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		field.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));

		return field;
	}

	public void optimize()
	{
		//super.optimize();
		/* TODO ? */
	}
}
