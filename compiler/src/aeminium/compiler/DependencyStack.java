package aeminium.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aeminium.compiler.east.EASTControlerNode;
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
	
	protected final HashMap<DataGroup, EASTControlerNode> scopes;
	
	public DependencyStack()
	{
		this.reads = new HashMap<DataGroup, Set<EASTExecutableNode>>();
		this.writes = new HashMap<DataGroup, EASTExecutableNode>();
		this.merges = new HashMap<DataGroup, Set<DataGroup>>();
		
		this.scopes = new HashMap<DataGroup, EASTControlerNode>();
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
		
		dependencies.remove(node);
	
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

		dependencies.remove(node);

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
		
		for (SignatureItem item : signature.getItems())
		{
			if (item instanceof SignatureItemMerge)
				merges.add((SignatureItemMerge) item);
			else
				dependencies.addAll(((SignatureItemModification) item).getDependencies(node, this));
		}
		
		for (SignatureItemMerge item : merges)
			dependencies.addAll(item.getDependencies(node, this));
		
		/* control dependencies */
		for (DataGroup scope : this.scopes.keySet())
		{
			if (node.getScope().getDataGroup().beginsWith(scope))
			{
				EASTControlerNode controler = this.scopes.get(scope);
				dependencies.add((EASTExecutableNode) controler);
				controler.addControledNode(node);
				
				node.addController(controler);
			}
		}

		/* TODO:
		 * only add deps that are not direct parents?
		 * if the controler is a child (not a weak/strong dependency) 
		 * of a task in the given scope, the parent task becomes the controler for
		 * all other tasks inside the scope at the same level that of the parent and above*/
		
		return dependencies;		
	}
	
	/**
	 * Join 2 distinct stacks (i.e. obtained from the then/else stmts) by replacing
	 * every difference between them with a reference to node (i.e. the parent if node)
	 */
	public void join(DependencyStack other, EASTExecutableNode node)
	{
		HashMap<DataGroup, Set<EASTExecutableNode>> resultReads = new HashMap<DataGroup, Set<EASTExecutableNode>>();
		HashMap<DataGroup, EASTExecutableNode> resultWrites = new HashMap<DataGroup, EASTExecutableNode>();
		HashMap<DataGroup, Set<DataGroup>> resultMerges = new HashMap<DataGroup, Set<DataGroup>>();
		
		HashSet<DataGroup> totalWrites = new HashSet<DataGroup>();
		totalWrites.addAll(this.writes.keySet());
		totalWrites.addAll(other.writes.keySet());
		
		for (DataGroup dg : totalWrites)
		{
			if (!this.writes.containsKey(dg) ||
				!other.writes.containsKey(dg) ||
				!other.writes.get(dg).equals(this.writes.get(dg)))
			{
				resultWrites.put(dg, node);
			}
		}
		
		HashSet<DataGroup> totalReads = new HashSet<DataGroup>();
		totalReads.addAll(this.reads.keySet());
		totalReads.addAll(other.reads.keySet());
		
		for (DataGroup dg : totalReads)
		{
			if (!this.reads.containsKey(dg) || !other.reads.containsKey(dg))
				resultReads.put(dg, new HashSet<EASTExecutableNode>(Arrays.asList(node)));
			else
			{
				Set<EASTExecutableNode> readsA = this.reads.get(dg);
				Set<EASTExecutableNode> readsB = other.reads.get(dg);
				
				Set<EASTExecutableNode> readsCommon = new HashSet<EASTExecutableNode>();
				readsCommon.addAll(readsA);
				readsCommon.retainAll(readsB);
				
				// add the nodes in common, and if there is at least one in 
				// not included add node
				Set<EASTExecutableNode> readsResult = new HashSet<EASTExecutableNode>();
				readsResult.addAll(readsCommon);
				
				if (!readsCommon.containsAll(readsA) || !readsCommon.containsAll(readsB))
					readsResult.add(node);
				
				resultReads.put(dg, readsResult);
			}
		}
		
		HashSet<DataGroup> totalMerges = new HashSet<DataGroup>();
		totalMerges.addAll(this.merges.keySet());
		totalMerges.addAll(other.merges.keySet());
		
		for (DataGroup dg : totalMerges)
		{
			HashSet<DataGroup> merges = new HashSet<DataGroup>();

			if (this.merges.containsKey(dg))
				merges.addAll(this.merges.get(dg));

			if (other.merges.containsKey(dg))
				merges.addAll(other.merges.get(dg));
			
			resultMerges.put(dg, merges);
		}
		
		this.reads.clear();
		this.reads.putAll(resultReads);
		
		this.writes.clear();
		this.writes.putAll(resultWrites);
		
		this.merges.clear();
		this.merges.putAll(resultMerges);
	}

	public DependencyStack fork()
	{
		DependencyStack copy = new DependencyStack();
		
		for (DataGroup node : this.reads.keySet())
		{
			HashSet<EASTExecutableNode> reads = new HashSet<EASTExecutableNode>();
			reads.addAll(this.reads.get(node));
			copy.reads.put(node, reads);
		}

		copy.writes.putAll(this.writes);
		
		for (DataGroup node : this.merges.keySet())
		{
			HashSet<DataGroup> merges = new HashSet<DataGroup>();
			merges.addAll(this.merges.get(node));
			copy.merges.put(node, merges);
		}
		
		return copy;
	}

	public void control(EASTControlerNode node, DataGroup scope)
	{
		/* TODO: make functions use this */
		this.scopes.put(scope, node);
	}
}