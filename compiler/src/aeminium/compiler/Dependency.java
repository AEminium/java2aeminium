package aeminium.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public abstract class Dependency
{
	/* here order matters so we can't use a set, maybe some sort of OrderedSet? */
	protected final ArrayList<NodeDependency> strongDependencies;

	protected final Set<Dependency> weakDependencies;
	protected final Set<Dependency> children;
	
	protected final Set<Dependency> reverseDependencies;
	
	public Dependency()
	{
		this.strongDependencies = new ArrayList<NodeDependency>();
		this.weakDependencies = new HashSet<Dependency>();
		this.children = new HashSet<Dependency>();

		this.reverseDependencies = new HashSet<Dependency>();
	}
	
	public void addStrong(NodeDependency dep)
	{
		this.strongDependencies.add(dep);
	}
	
	public void addChild(Dependency dep)
	{
		this.children.add(dep);
	}
	
	public void addWeak(Dependency dep)
	{
		if (!this.isStrongParentOf(dep))
		{
			this.weakDependencies.add(dep);
			dep.reverseDependencies.add(this);
		}
	}
	
	public void addWeak(Collection<Dependency> deps)
	{
		for (Dependency dep : deps)
			this.addWeak(dep);
	}
	
	public ArrayList<NodeDependency> getStrongDependencies()
	{
		return new ArrayList<NodeDependency>(this.strongDependencies);
	}

	public Set<Dependency> getWeakDependencies()
	{
		return new HashSet<Dependency>(this.weakDependencies);
	}
	
	public Set<Dependency> getChildren()
	{
		return new HashSet<Dependency>(this.children);
	}
	
	public Set<Dependency> getReverseDependencies()
	{
		return new HashSet<Dependency>(this.reverseDependencies);
	}
	
	public void simplify()
	{
		Set<Dependency> deps = new HashSet<Dependency>();
		deps.addAll(this.weakDependencies);
		deps.addAll(this.strongDependencies);

		Set<Dependency> temp_weak = new HashSet<Dependency>(this.weakDependencies);
		temp_weak.removeAll(this.strongDependencies);

		for (Dependency dep : deps)
		{
			if (dep instanceof RuntimeDependency)
				continue;
			
			temp_weak.remove(dep.weakDependencies);
			temp_weak.remove(dep.strongDependencies);
		}

		this.weakDependencies.clear();
		this.weakDependencies.addAll(temp_weak);
	}
	
	public boolean isStrongParentOf(Dependency node)
	{		
		Stack<Dependency> tovisit = new Stack<Dependency>();
		Set<Dependency> visited = new HashSet<Dependency>();
	
		tovisit.add(this);
		
		while (!tovisit.empty())
		{
			Dependency dep = tovisit.pop();

			if (dep.equals(node))
				return true;
			
			visited.add(dep);
			
			for (Dependency child : dep.strongDependencies)
				if (!visited.contains(child))
					tovisit.push(child);
		}

		return false;
	}
	
	public boolean isParentOf(Dependency node)
	{		
		Stack<Dependency> tovisit = new Stack<Dependency>();
		Set<Dependency> visited = new HashSet<Dependency>();
	
		tovisit.add(this);
		
		while (!tovisit.empty())
		{
			Dependency dep = tovisit.pop();

			if (dep.equals(node))
				return true;
			
			visited.add(dep);
			
			for (Dependency child : dep.children)
				if (!visited.contains(child))
					tovisit.push(child);

			for (Dependency child : dep.strongDependencies)
				if (!visited.contains(child))
					tovisit.push(child);
		}

		return false;
	}

	public boolean dependentFree()
	{
		Stack<Dependency> sub = new Stack<Dependency>();

		sub.add(this);
		
		while (!sub.empty())
		{
			Dependency dep = sub.pop();
			
			for (Dependency rev : dep.reverseDependencies)
				if (!this.isParentOf(rev))
					return false;
					
			sub.addAll(dep.strongDependencies);
			sub.addAll(dep.children);
		}
		
		return true;
	}
	
	public abstract ArrayList<String> getPath();

}
