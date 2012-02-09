package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;

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
			frag.checkDependencies(stack);
	}
}
