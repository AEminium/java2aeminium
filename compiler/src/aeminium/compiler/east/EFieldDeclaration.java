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
		
		FieldDeclaration field = (FieldDeclaration) ASTNode.copySubtree(ast, this.origin);

		Modifier priv = EFieldDeclaration.getModifier(field, ModifierKeyword.PRIVATE_KEYWORD);
		Modifier pub = EFieldDeclaration.getModifier(field, ModifierKeyword.PUBLIC_KEYWORD);
		Modifier vol = EFieldDeclaration.getModifier(field, ModifierKeyword.VOLATILE_KEYWORD);
		Modifier fin = EFieldDeclaration.getModifier(field, ModifierKeyword.FINAL_KEYWORD);

		if (priv != null)
			field.modifiers().remove(priv);

		if (pub == null)
			field.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		if (vol == null && fin == null)
			field.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));

		return field;
	}

	public void optimize()
	{
		//super.optimize();
		/* TODO: SimpleName ? */
	}

	public static Modifier getModifier(FieldDeclaration field, ModifierKeyword kw)
	{
		for (Object mod : field.modifiers())
			if (((Modifier) mod).getKeyword() == kw)
				return ((Modifier) mod);

		return null;
	}
}
