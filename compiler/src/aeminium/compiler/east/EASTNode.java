package aeminium.compiler.east;

public abstract class EASTNode
{
	protected EAST east;
	
	EASTNode(EAST east)
	{
		this.east = east;
	}

	public abstract void optimize();
}
