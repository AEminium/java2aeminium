package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.east.EAST;

import aeminium.compiler.east.ETypeDeclaration;
import aeminium.compiler.east.EMethodDeclaration;

public class EFieldDeclaration
{
	FieldDeclaration origin;

	EFieldDeclaration(FieldDeclaration origin)
	{
		this.origin = origin;
	}

	public FieldDeclaration translate(AST ast, List<CompilationUnit> cus)
	{
		FieldDeclaration field = (FieldDeclaration) ASTNode.copySubtree(origin.getAST(), this.origin);
		field.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		field.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));

		return field;
	}

	public void optimize()
	{
		/* TODO? */
	}
}
