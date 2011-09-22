package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public abstract class EASTDependentNode extends EASTNode
{
	protected List<EASTDependentNode> childs;
	protected List<EASTDependentNode> parents;

	protected boolean root;

	public Task task;

	EASTDependentNode(EAST east)
	{
		super(east);

		this.childs = new ArrayList<EASTDependentNode>();
		this.parents = new ArrayList<EASTDependentNode>();

		this.root = true;
		this.task = null;
	}

	protected void link(EASTDependentNode child)
	{
		this.childs.add(child);
		child.parents.add(this);
	}

	@Override
	public void optimize()
	{
		this.root = this.parents.size() == 0;

		// single child
		if (this.parents.size() == 1 && this.parents.get(0).childs.size() == 1)
			this.root = false;

		// FIXME: if the parents change in the middle of optimize operations this might change..
		// FIXME: take task size into account (e.g.: tasks that are small like assignments are set
		// serialized, and sequentialy, if other child that was previously not a only child but is now
		// that child can be serialized as well (if no extra dependencies are required)

		for (EASTDependentNode child : this.childs)
			child.optimize();
	}

	protected final boolean isRoot()
	{
		return this.root;
	}

	protected List<Task> getTasks(Task parent, List<CompilationUnit> cus, List<Statement> stmts)
	{
		List<Task> tasks = new ArrayList<Task>();

		if (this.isRoot())
		{
			assert(this.task != null);
			tasks.add(this.task);
		} else
			tasks.addAll(this.getChildTasks(parent, cus, stmts));

		return tasks;
	}

	protected List<Task> getChildTasks(Task parent, List<CompilationUnit> cus, List<Statement> stmts)
	{
		List<Task> tasks = new ArrayList<Task>();

		for (EASTDependentNode child : this.childs)
			tasks.addAll(child.getTasks(parent, cus, stmts));

		return tasks;
	}
}
