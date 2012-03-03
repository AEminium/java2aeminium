package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;

public abstract class EBodyDeclaration extends EASTExecutableNode implements EASTDataNode
{
	protected final ETypeDeclaration type;
	protected final DataGroup datagroup;
	
	public EBodyDeclaration(EAST east, BodyDeclaration original, ETypeDeclaration type)
	{
		super(east, original);
		
		this.type = type;
		this.datagroup = this.isStatic() ? this.type.staticDataGroup : this.type.thisDataGroup;
	}

	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}
	
	@Override
	public ETypeDeclaration getTypeDeclaration()
	{
		return this.type;
	}
	
	public void checkDependencies()
	{
		DependencyStack stack = new DependencyStack();
		this.checkDependencies(stack);
	}
	
	/**
	 * Gets a modifier from a list by its common name
	 * @param name The common name of the modifier (e.g.: "public", "static", "@AEminium")
	 */
	public IExtendedModifier getModifier(String name)
	{
		for (Object modifier : this.getOriginal().modifiers())
			if (modifier.toString().equals(name))
				return (IExtendedModifier) modifier;

		return null;
	}
	
	@Override
	public abstract BodyDeclaration getOriginal();
	
	public boolean isStatic()
	{
		return this.getModifier("static") != null;
	}
	
	public ETypeDeclaration getType()
	{
		return this.type;
	}
}