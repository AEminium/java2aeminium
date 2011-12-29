package aeminium.compiler.datagroup;

public class SignatureItemWrite extends SignatureItem
{
	private final DataGroup to;

	public SignatureItemWrite(DataGroup to)
	{
		this.to = to;
	}
	
	public String toString()
	{
		return String.format("W: %s", this.to);
	}
}
