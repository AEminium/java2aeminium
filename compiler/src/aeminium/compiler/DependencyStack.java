package aeminium.compiler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aeminium.compiler.east.EASTExecutableNode;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItem;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemModification;

public class DependencyStack
{
	protected final HashMap<DataGroup, Set<EASTExecutableNode>> reads;
	protected final HashMap<DataGroup, EASTExecutableNode> writes;
	protected final HashMap<DataGroup, Set<DataGroup>> merges;
	
	public DependencyStack()
	{
		this.reads = new HashMap<DataGroup, Set<EASTExecutableNode>>();
		this.writes = new HashMap<DataGroup, EASTExecutableNode>();
		this.merges = new HashMap<DataGroup, Set<DataGroup>>();
	}
	
	public Set<EASTExecutableNode> read(EASTExecutableNode node, DataGroup from)
	{
		Set<DataGroup> groupsFrom = this.getGroups(from);
		Set<EASTExecutableNode> dependencies = new HashSet<EASTExecutableNode>();

		for (DataGroup _from : groupsFrom)
		{
			if (!this.reads.containsKey(_from))
				this.reads.put(_from, new HashSet<EASTExecutableNode>());
			
			this.reads.get(_from).add(node);
			
			if (this.writes.containsKey(_from))
				dependencies.add(this.writes.get(_from));
		}
		
		dependencies.remove(from);

		return dependencies;
	}
	
	public Set<EASTExecutableNode> write(EASTExecutableNode node, DataGroup to)
	{
		Set<DataGroup> groupsTo = this.getGroups(to);
		Set<EASTExecutableNode> dependencies = new HashSet<EASTExecutableNode>();
			
		for (DataGroup _to : groupsTo)
		{
			if (this.reads.containsKey(_to))
			{
				Set<EASTExecutableNode> reads = this.reads.get(_to);
				dependencies.addAll(reads);
				reads.clear();
			}
			
			this.writes.put(_to, node);
		}

		dependencies.remove(to);

		return dependencies;
	}
	
	public void merge(DataGroup to, DataGroup from)
	{
		// TODO/FIXME: should this be bidirectional?
		ArrayList<DataGroup> groupsTo = new ArrayList<DataGroup>(this.getGroups(to));		// copy to an ArrayList
		ArrayList<DataGroup> groupsFrom = new ArrayList<DataGroup>(this.getGroups(from));	// otherwise ConcurrentModificationException
		
		for (DataGroup _to : groupsTo)
		{
			for (DataGroup _from : groupsFrom)
			{
				this.merges.get(_to).add(_from);
				this.merges.get(_from).add(_to);
			}
		}
	}

	protected Set<DataGroup> getGroups(DataGroup group)
	{
		if (this.merges.containsKey(group))
			return this.merges.get(group);
		
		HashSet<DataGroup> groups = new HashSet<DataGroup>();
		groups.add(group);
		this.merges.put(group, groups);
		
		return groups;
	}

	public Set<EASTExecutableNode> getDependencies(EASTExecutableNode node, Signature signature)
	{
		Set<EASTExecutableNode> dependencies = new HashSet<EASTExecutableNode>();
		
		ArrayList<SignatureItemMerge> merges = new ArrayList<SignatureItemMerge>();
		
		//System.out.println("Dependencies for: " + node.toString());
		//System.out.println("Signature:\n"+ signature.toString());
		
		for (SignatureItem item : signature.getItems())
		{
			if (item instanceof SignatureItemMerge)
				merges.add((SignatureItemMerge) item);
			else
				dependencies.addAll(((SignatureItemModification) item).getDependencies(node, this));
		}
		
		for (SignatureItemMerge item : merges)
			dependencies.addAll(item.getDependencies(node, this));
		
		//for (EASTExecutableNode dep : dependencies)
		//	System.out.println(dep);

		//try {
		//	System.in.read();
		//} catch (IOException e) {
		//	// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}
		return dependencies;		
	}
}
