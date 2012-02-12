package aeminium.compiler.signature;

import java.util.HashSet;
import java.util.Set;

public class Signature
{
	private final Set<SignatureItem> items;
	
	public Signature()
	{
		this.items = new HashSet<SignatureItem>();
	}
	
	public void addItem(SignatureItem item)
	{
		this.items.add(item);
	}
	
	public void addAll(Signature signature)
	{
		this.items.addAll(signature.items);
	}

	public Set<SignatureItem> getItems()
	{
		return this.items;
	}
	
	@Override
	public String toString()
	{
		String str = "";
		
		for (SignatureItem item : this.items)
			str += item.toString() + "\n";

		return str;
	}
}
