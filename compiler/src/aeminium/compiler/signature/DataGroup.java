package aeminium.compiler.signature;

public abstract class DataGroup
{
	public abstract DataGroup append(SimpleDataGroup dg);
	public abstract DataGroup replace(DataGroup what, DataGroup with);
	public abstract boolean beginsWith(DataGroup scope);
}
