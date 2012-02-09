package aeminium.compiler.signature;

import java.util.ArrayList;
import aeminium.compiler.east.EMethodDeclaration;

public class SignatureItemDeferred extends SignatureItem
{
	protected final EMethodDeclaration method;
	protected final DataGroup dgRet;
	protected final DataGroup dgThis;
	protected final ArrayList<DataGroup> dgsArgs;
	
	public SignatureItemDeferred(EMethodDeclaration method, DataGroup dgRet, DataGroup dgThis, ArrayList<DataGroup> dgsArgs)
	{
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

		return new SignatureItemDeferred(this.method, dgRet, dgThis, dgsArgs);
	}
	
	public Signature getUndeferredSignature()
	{
		return this.method.undefer(this.dgRet, this.dgThis, this.dgsArgs);
	}

	public Signature filter(Signature sig)
	{
		Signature filtered = new Signature();
		
		// don't propagate local variable changes
		for (SignatureItem item : sig.getItems())
		{
			boolean local = item.isLocalTo(this.method.getDataGroup());
			
			if (local && this.dgRet != null && item.isLocalTo(this.dgRet))
				local = false;

			for (DataGroup arg : this.dgsArgs)
				if (local && item.isLocalTo(arg))
					local = false;
			
			if (!local)
				filtered.addItem(item);
		}
		
		return filtered;
	}
	
	@Override
	public boolean isLocalTo(DataGroup scope)
	{
		// TODO/FIXME: how to check if their modifications are local? 
		return false;
	}
}
