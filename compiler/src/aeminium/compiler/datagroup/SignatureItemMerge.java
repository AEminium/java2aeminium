package aeminium.compiler.datagroup;

public class SignatureItemMerge extends SignatureItem
{
	private final DataGroup from;
	private final DataGroup to;
	
	public SignatureItemMerge(DataGroup to, DataGroup from)
	{
		super();

		this.from = from;
		this.to = to;
	}
	
	public String toString()
	{
		return String.format("M: %s <- %s", this.to, this.from);
	}
}