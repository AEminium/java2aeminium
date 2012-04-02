package aeminium.compiler.east;

import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;

import aeminium.compiler.Dependency;
import aeminium.compiler.NodeDependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public abstract class EASTExecutableNode extends EASTNode
{
	protected final Signature signature;
	public final NodeDependency dependency;

	/* preTranslate */
	protected Task task;

	protected boolean inline = false; 
	private EASTExecutableNode inlinedTo;
	
	protected boolean simple = false;
	
	public EASTExecutableNode(EAST east, ASTNode original)
	{
		super(east, original);

		this.signature = new Signature();
		this.dependency = new NodeDependency(this);
	}
	
	public Task getTask()
	{
		assert (this.task != null);
		return this.task;
	}
	
	public abstract void checkSignatures();
	public abstract Signature getFullSignature();

	public abstract void checkDependencies(DependencyStack stack);
	
	public int optimize()
	{
		this.dependency.simplify();
		
		int sum = 0;

		HashSet<EASTExecutableNode> nodes = new HashSet<EASTExecutableNode>();
		
		for (Dependency dep : this.dependency.getStrongDependencies())
			if (dep instanceof NodeDependency)
				nodes.add(((NodeDependency) dep).getNode());

		if (this.dependency.getStrongDependencies().size() + this.dependency.getWeakDependencies().size() < 2)
			for (EASTExecutableNode node : nodes)
				sum += node.inline(this);
		
		for (EASTExecutableNode node : nodes)
			if (node.isSimpleTask())
				sum += node.inline(this);
		
		return sum;
	}
	
	public boolean isSimpleTask()
	{
		return this.simple;
	}

	public int inline(EASTExecutableNode inlineTo)
	{
		if (this.inline)
			return 0;
		
		while (inlineTo.inline)
			inlineTo = inlineTo.inlinedTo;
				
		this.dependency.inlineTo(inlineTo.dependency);
		this.inline = true;
		this.inlinedTo = inlineTo;

		return 1;
	}

	public abstract void preTranslate(Task parent);
}