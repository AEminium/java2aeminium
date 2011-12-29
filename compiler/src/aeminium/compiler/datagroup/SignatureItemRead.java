package aeminium.compiler.datagroup;

public class SignatureItemRead extends SignatureItem
{
	private final DataGroup from;
	
	public SignatureItemRead(DataGroup from)
	{
		super();
		
		this.from = from;
	}
	
	public String toString()
	{
		return String.format("R: %s", this.from);
	}
}
