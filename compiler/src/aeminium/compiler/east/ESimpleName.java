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
		this.write = null;
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

	public void addReadTask(Task task)
	{
		System.out.println("read " + this.origin);
		if (this.write != null)
			task.addWeakDependency(this.write);
		else
			System.out.println("fail");
		this.reads.add(task);
	}

	public void setWriteTask(Task task)
	{
		// TODO/FIXME (no loops or ifs or ternary operators)

		System.out.println("write " + this.origin);
		if (this.reads.size() > 0)
			for (Task dep : this.reads)
				task.addWeakDependency(dep);
		else if (this.write != null)
			task.addWeakDependency(this.write);

		this.write = task;
		this.reads = new ArrayList<Task>();
	}

	@Override
	public Expression translate(Task parent, boolean read)
	{
		AST ast = this.east.getAST();

		String variable = this.east.resolveName(this.binding);
		ESimpleName node = (ESimpleName) this.east.getNode(variable);

		FieldAccess field = ast.newFieldAccess();

		field.setExpression(parent.getPathToTask(node.getTask()));
		field.setName((SimpleName) ASTNode.copySubtree(ast, this.origin));

		if (read)
			node.addReadTask(parent);

		return field;
	}
}
