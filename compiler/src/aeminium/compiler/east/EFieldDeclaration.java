package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.FieldTask;
import aeminium.compiler.task.Task;

public class EFieldDeclaration extends EBodyDeclaration
{
	protected final ETypeDeclaration type;
	protected final Type dataType;
	protected final ArrayList<EVariableDeclarationFragment> fragments;
	
	public EFieldDeclaration(EAST east, FieldDeclaration original, ETypeDeclaration type)
	{
		super(east, original, type);

		this.type = type;
		this.dataType = original.getType();

		this.fragments = new ArrayList<EVariableDeclarationFragment>();
		
		for (Object frag : original.fragments())
			this.fragments.add(EVariableDeclarationFragment.create(this.east, (VariableDeclarationFragment) frag, this, this.dataType));
	}

	/* Factory */
	public static EFieldDeclaration create(EAST east, FieldDeclaration original, ETypeDeclaration type)
	{
		return new EFieldDeclaration(east, original, type);
	}
	
	@Override
	public FieldDeclaration getOriginal()
	{
		return (FieldDeclaration) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		for (EVariableDeclarationFragment frag : this.fragments)
			frag.checkSignatures();
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		
		for (EVariableDeclarationFragment frag : this.fragments)
			sig.addAll(frag.getFullSignature());

		return sig;
	}
	
	@Override
	public void checkDependencies(DependencyStack stack)
	{
		for (EVariableDeclarationFragment frag : this.fragments)
		{
			frag.checkDependencies(stack);
			this.dependency.addStrong(frag.dependency);
		}
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;
		
		for (EVariableDeclarationFragment frag : this.fragments)
			sum += frag.optimize();
		
		sum += super.optimize();
		
		return sum;
	}
	
	public void preTranslate()
	{
		String name = this.type.getOriginal().getName() + "_" + this.getOriginal().getStartPosition();

		this.preTranslate(FieldTask.create(this, name));		
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		this.task = parent;
		
		for (EVariableDeclarationFragment frag : this.fragments)
			frag.preTranslate(this.task);
	}

	@SuppressWarnings("unchecked")
	public FieldDeclaration translate(ArrayList<CompilationUnit> out)
	{
		// TODO: EFieldDeclaration.translate();
		System.err.println("TODO: Paralell EFieldDeclaration.translate()");
		
		AST ast = this.getAST();
		FieldDeclaration decl = (FieldDeclaration) ASTNode.copySubtree(ast, this.original);
		
		for (int i = 0; i < decl.modifiers().size(); )
		{
			Object modifier = decl.modifiers().get(i);
			String str = modifier.toString();

			if (str.equals("private") || str.equals("protected"))
			{
				decl.modifiers().remove(i);
				decl.modifiers().add(i, ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
			}
			
			if (str.equals("final"))
			{
				decl.modifiers().remove(i);
				continue;
			}
			
			i++;
		}
		
		return decl;
	}
}
