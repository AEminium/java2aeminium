package aeminium.compiler.signature;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import aeminium.compiler.Dependency;
import aeminium.compiler.east.EMethodDeclaration;

public class SignatureItemDeferred extends SignatureItem
{
	protected final Dependency dep;
	
	protected final EMethodDeclaration method;
	protected final DataGroup dgRet;
	protected final DataGroup dgThis;
	protected final ArrayList<DataGroup> dgsArgs;
	
	public SignatureItemDeferred(Dependency dep, EMethodDeclaration method, DataGroup dgRet, DataGroup dgThis, ArrayList<DataGroup> dgsArgs)
	{
		this.dep = dep;
		
		this.method = method;
		this.dgRet = dgRet;
		this.dgThis = dgThis;
		this.dgsArgs = dgsArgs;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof SignatureItemDeferred))
			return false;
		
		SignatureItemDeferred _other = ((SignatureItemDeferred) other);
		
		if (!this.method.equals(_other.method))
			return false;
		
		if (this.dgRet != _other.dgRet && !this.dgRet.equals(_other.dgRet))
			return false;

		if (this.dgThis != _other.dgThis && !this.dgThis.equals(_other.dgThis))
			return false;
		
		if (this.dgsArgs.size() != _other.dgsArgs.size())
			return false;

		for (int i = 0; i < this.dgsArgs.size(); i++)
			if (!this.dgsArgs.get(i).equals(_other.dgsArgs.get(i)))
				return false;

		return true;
	}
	
	@Override
	public int hashCode()
	{
		// FIXME: should take into account other parameters, but not strictly required
		return this.method.hashCode();
	}
	
	@Override
	public String toString()
	{
		String str = "[DEFER] " + this.method.getOriginal().getName() + "(";

		for (DataGroup arg : this.dgsArgs)
			str += arg + ", ";
		
		return str + ")";
	}
	
	@Override
	public SignatureItemDeferred replace(DataGroup what, DataGroup with)
	{
		DataGroup dgRet = this.dgRet == null ? null : this.dgRet.replace(what, with);
		DataGroup dgThis = this.dgThis == null ? null : this.dgThis.replace(what, with);

		ArrayList<DataGroup> dgsArgs = new ArrayList<DataGroup>();
		for (DataGroup arg : this.dgsArgs)
			dgsArgs.add(arg.replace(what, with));

		return new SignatureItemDeferred(this.dep, this.method, dgRet, dgThis, dgsArgs);
	}
	
	public Signature getUndeferredSignature()
	{
		return this.method.undefer(this.dep, this.dgRet, this.dgThis, this.dgsArgs);
	}

	public Signature closure()
	{
		Queue<SignatureItemDeferred> queue = new ArrayDeque<SignatureItemDeferred>();
		HashSet<SignatureItem> items = new HashSet<SignatureItem>();
		
		queue.add((SignatureItemDeferred) this);

		SignatureItemDeferred next;
		while ((next = queue.poll()) != null)
		{
			Set<SignatureItem> subitems = next.getUndeferredSignature().getItems();
			
			// don't process this item again
			items.add(next);
			
			for (SignatureItem item : subitems)
			{
				if (item instanceof SignatureItemDeferred)
				{
					if (items.add(item))
						queue.add((SignatureItemDeferred) item);
				} else
				{
					boolean local = item.isLocalTo(this.method.getBody().getDataGroup());
					boolean ret = this.dgRet == null ? false : item.isLocalTo(this.dgRet);
					boolean param = false;

					for (DataGroup arg : this.dgsArgs)
						param |= item.isLocalTo(arg);
					
					if (!local || ret || param)
						items.add(item);
				}
			}
		}
		
		Signature closure = new Signature();
		for (SignatureItem item : items)
			if (!(item instanceof SignatureItemDeferred))
				closure.addItem(item);
				
		return closure;
	}
	
	@Override
	public boolean isLocalTo(DataGroup scope)
	{
		// TODO/FIXME: how to check if their modifications are local? 
		return false;
	}

	@Override
	public SignatureItem setDependency(Dependency dep)
	{
		ArrayList<DataGroup> dgsArgs = new ArrayList<DataGroup>(this.dgsArgs);
		return new SignatureItemDeferred(dep, this.method, this.dgRet, this.dgThis, dgsArgs);
	}
}
