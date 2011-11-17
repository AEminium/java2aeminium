package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class ESimpleName extends EExpression
{
	SimpleName origin;
	IBinding binding;

	Task write;
	List<Task> reads;

	ESimpleName(EAST east, SimpleName origin)
	{
		super(east);

		this.origin = origin;
		this.reads = new ArrayList<Task>();
	}

	@Override
	public void optimize()
	{
		super.optimize();

		this.root = false;
		this.binding = this.origin.resolveBinding();
	}

	public void setTask(Task task)
	{
		this.task = task;
	}

	public Task getTask()
	{
		return this.task;
	}

	private void addDependencies(Task parent, boolean write)
	{
		if (write)
		{
			if (this.reads.size() > 0)
				for (Task dep : this.reads)
					parent.addWeakDependency(dep);
			else if (this.write != null)
				parent.addWeakDependency(this.write);

			this.write = parent;
			this.reads = new ArrayList<Task>();
		} else
		{
			if (this.write != null)
				parent.addWeakDependency(this.write);
			this.reads.add(parent);
		}
	}

	@Override
	public Expression translate(Task parent, boolean write)
	{
		AST ast = this.east.getAST();

		String variable = this.east.resolveName(this.binding);
		ESimpleName node = (ESimpleName) this.east.getNode(variable);

		FieldAccess field = ast.newFieldAccess();
		field.setExpression(parent.getPathToTask(node.getTask()));
		field.setName((SimpleName) ASTNode.copySubtree(ast, this.origin));

		// FIXME: this is wrong, not only it depends on the creation, but also on the last task that
		// accessed it. That can be determined in compile time, if no conditional code is found
		// (no loops or ifs or ternary operators)

		this.addDependencies(parent, write);

		return field;
	}
}
