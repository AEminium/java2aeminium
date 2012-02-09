package aeminium.compiler.signature;

import java.util.ArrayList;

public class CompoundDataGroup extends DataGroup
{
	protected final ArrayList<SimpleDataGroup> datagroups;
	
	public CompoundDataGroup(SimpleDataGroup ... datagroups)
	{
		this.datagroups = new ArrayList<SimpleDataGroup>();
		
		for (SimpleDataGroup dg : datagroups)
			this.datagroups.add(dg);
	}

	public CompoundDataGroup(ArrayList<SimpleDataGroup> datagroups, SimpleDataGroup datagroup)
	{
		this.datagroups = new ArrayList<SimpleDataGroup>();
		
		for (SimpleDataGroup dg : datagroups)
			this.datagroups.add(dg);
		
		this.datagroups.add(datagroup);
	}

	public CompoundDataGroup(ArrayList<SimpleDataGroup> datagroups)
	{
		this.datagroups = new ArrayList<SimpleDataGroup>();
		
		for (SimpleDataGroup dg : datagroups)
			this.datagroups.add(dg);
	}

	@Override
	public DataGroup append(SimpleDataGroup dg)
	{
		return new CompoundDataGroup(this.datagroups, dg);
	}

	@Override
	public DataGroup replace(DataGroup what, DataGroup with)
	{
		ArrayList<SimpleDataGroup> result = new ArrayList<SimpleDataGroup>();
		ArrayList<SimpleDataGroup> whatDgs = new ArrayList<SimpleDataGroup>();
		ArrayList<SimpleDataGroup> withDgs = new ArrayList<SimpleDataGroup>();
		
		if (what instanceof CompoundDataGroup)
			whatDgs.addAll(((CompoundDataGroup) what).datagroups);
		else
			whatDgs.add((SimpleDataGroup) what);
		
		if (with instanceof CompoundDataGroup)
			withDgs.addAll(((CompoundDataGroup) with).datagroups);
		else
			withDgs.add((SimpleDataGroup) with);
		
		// FIXME: naive replace
		for (int i = 0; i < this.datagroups.size(); i++)
		{
			int j;
			for (j = 0; j < whatDgs.size() && i+j < this.datagroups.size(); j++)
				if (!this.datagroups.get(i + j).equals(whatDgs.get(j)))
					break;
			
			if (j == whatDgs.size())
			{
				i += j - 1;
				result.addAll(withDgs);
			} else
				result.add(this.datagroups.get(i));
		}

		return new CompoundDataGroup(result);
	}
	
	@Override
	public String toString()
	{
		String str = "[";
		
		for (SimpleDataGroup dg : this.datagroups)
			str += dg.toString() + ", ";
		
		return str + "]";
	}
	
	@Override
	public int hashCode()
	{
		int x = 0;
		
		for (SimpleDataGroup dg : this.datagroups)
			x ^= dg.hashCode();
		
		return x;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || !(other instanceof CompoundDataGroup))
			return false;
		
		CompoundDataGroup _other = (CompoundDataGroup) other;
		
		if (this.datagroups.size() != _other.datagroups.size())
			return false;
		
		for (int i = 0; i < this.datagroups.size(); i++)
			if (!this.datagroups.get(i).equals(_other.datagroups.get(i)))
				return false;
		
		return true;
	}

	@Override
	public boolean beginsWith(DataGroup scope)
	{
		if (scope instanceof SimpleDataGroup && scope.equals(this.datagroups.get(0)))
			return true;
		
		CompoundDataGroup _scope = (CompoundDataGroup) scope;
		
		if (this.datagroups.size() < _scope.datagroups.size())
			return false;
		
		for (int i = 0; i < _scope.datagroups.size(); i++)
			if (!this.datagroups.get(i).equals(_scope.datagroups.get(i)))
				return false;
		
		return true;
	}
}
