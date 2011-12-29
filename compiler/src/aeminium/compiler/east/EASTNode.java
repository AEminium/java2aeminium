package aeminium.compiler.east;

import aeminium.compiler.datagroup.Signature;

public abstract class EASTNode
{
	protected final EAST east;
	protected final Signature signature;

	EASTNode(EAST east)
	{
		this.east = east;

		this.signature = new Signature();
	}

	public Signature getSignature()
	{
		return this.signature;
	}
	
	public abstract void analyse();
	public abstract int optimize();
	// public abstract void preTranslate();
	// public abstract void translate();
}