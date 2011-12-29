package aeminium.compiler.datagroup;

import aeminium.compiler.east.EMethodInvocation;

public class SignatureItemInvocation extends SignatureItem
{
	private final EMethodInvocation invoke;
	
	public SignatureItemInvocation(EMethodInvocation invoke)
	{
		super();

		this.invoke = invoke;
	}
	
	public String toString()
	{
		return String.format("I: %s", this.invoke);
	}
}