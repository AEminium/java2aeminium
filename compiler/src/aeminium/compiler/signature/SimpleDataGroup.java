package aeminium.compiler.signature;

public class SimpleDataGroup extends DataGroup
{
	private static int total = 0;
	
	private final int id;
	private final String description;
	
	public SimpleDataGroup(String description)
	{
		this.id = total++;
		this.description = description;
	}
	
	public String toString()
	{
		return String.format("(%d: %s)", this.id, this.description);
	}

	@Override
	public int hashCode()
	{
		return this.id;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || !(other instanceof SimpleDataGroup))
			return false;
		
		SimpleDataGroup _other = (SimpleDataGroup) other;

		return this.id == _other.id;
	}
	
	@Override
	public DataGroup append(SimpleDataGroup dg)
	{
		return new CompoundDataGroup(this, dg);
	}

	@Override
	public DataGroup replace(DataGroup what, DataGroup with)
	{
		if (this.equals(what))
			return with;
		
		return this;
	}

	@Override
	public boolean beginsWith(DataGroup scope)
	{
		if (this.equals(scope))
			return true;
		return false;
	}
}
