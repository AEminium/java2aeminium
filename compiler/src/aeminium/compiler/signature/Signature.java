package aeminium.compiler.signature;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
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
	
	public Signature closure()
	{
		Queue<SignatureItemDeferred> queue = new ArrayDeque<SignatureItemDeferred>();
		HashSet<SignatureItem> items = new HashSet<SignatureItem>();
		
		for (SignatureItem item : this.items)
			if (item instanceof SignatureItemDeferred)
				queue.add((SignatureItemDeferred) item);

		SignatureItemDeferred next;
		while ((next = queue.poll()) != null)
		{
			Set<SignatureItem> subitems = next.getUndeferredSignature().getItems();
			
			// don't process this item again
			items.add(next);
			
			for (SignatureItem subitem : subitems)
				if (items.add(subitem) && subitem instanceof SignatureItemDeferred)
					queue.add((SignatureItemDeferred) subitem);
		}
		
		Signature closure = new Signature();
		for (SignatureItem item : items)
			if (!(item instanceof SignatureItemDeferred))
				closure.addItem(item);
		
		return closure;
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
