package aeminium.compiler.east;

import org.eclipse.jdt.core.dom.ASTNode;

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

		/* if the other task is only used by this one, it can be inlined */
		for (NodeDependency dep : this.dependency.getStrongDependencies())
			if (dep.getReverseDependencies().size() < 1)
				sum += dep.getNode().inlineTo(this);

		/* inline all simple tasks */
		for (NodeDependency dep : this.dependency.getStrongDependencies())
		{
			EASTExecutableNode node = dep.getNode();
			if (node.isSimpleTask())
				sum += node.inlineTo(this);
		}

		/* if this task is simple and only depends of task y, then inline it. */ 
		if (this.isSimpleTask() && 
			this.dependency.getStrongDependencies().size() + this.dependency.getWeakDependencies().size() < 2)
			for (NodeDependency dep : this.dependency.getStrongDependencies())
				sum += dep.getNode().inlineTo(this);

		return sum;
	}
	
	public boolean isSimpleTask()
	{
		return this.simple;
	}

	public int inlineTo(EASTExecutableNode inlineTo)
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

	public NodeDependency getDependency()
	{
		return this.dependency;
	}
	
	public abstract void preTranslate(Task parent);
}