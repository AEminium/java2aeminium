package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.*;

public class EFieldDeclaration extends EBodyDeclaration
{
	private final EAST east;
	private final FieldDeclaration origin;

	EFieldDeclaration(EAST east, FieldDeclaration origin)
	{
		super(east);

		this.east = east;
		this.origin = origin;
	}

	@Override
	public void analyse()
	{
		/* TODO: FieldDeclaration. analyse */
	}

	@Override
	public int optimize()
	{
		/* TODO: FieldDeclaration.optimize */
		return 0;
	}
	
	public void preTranslate()
	{
		/* TODO: FieldDeclaration.preTranslate */
		System.err.println("TODO: FieldDeclaration.preTranslate");
	}
	
	@SuppressWarnings("unchecked")
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
	
	public static Modifier getModifier(FieldDeclaration field, ModifierKeyword kw)
	{
		for (Object mod : field.modifiers())
			if (((Modifier) mod).getKeyword() == kw)
				return ((Modifier) mod);

		return null;
	}
}
