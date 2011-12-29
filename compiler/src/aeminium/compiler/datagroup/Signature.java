package aeminium.compiler.datagroup;

import java.util.HashSet;

public class Signature
{
	private HashSet<SignatureItem> items;
	
	public Signature()
	{
		this.items = new HashSet<SignatureItem>();
	}
	
	public void add(SignatureItem item)
	{
		this.items.add(item);
	}
	
	public void addFrom(Signature other)
	{
		this.items.addAll(other.items);
	}
	
	@Override
	public String toString()
	{
		String val = "";
		
		for (SignatureItem item : this.items)
			val += item.toString() +"\n";
		
		return val;
	}
}