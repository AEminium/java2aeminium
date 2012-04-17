package aeminium.compiler;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;

import aeminium.compiler.east.EASTExecutableNode;

public class NodeDependency extends Dependency
{
	protected final EASTExecutableNode node;

	public boolean fast;

	public NodeDependency(EASTExecutableNode node)
	{
		super();

		this.node = node;
	}
	
	public EASTExecutableNode getNode()
	{
		return this.node;
	}
	
	public void inlineTo(NodeDependency other)
	{
		for (NodeDependency dep : this.strongDependencies)
			if (!other.strongDependencies.contains(dep))
				other.strongDependencies.add(dep);
	
		for (Dependency dep : this.weakDependencies)
			if (!other.weakDependencies.contains(dep))
				other.weakDependencies.add(dep);
	
		for (Dependency dep : this.children)
			if (!other.children.contains(dep))
				other.children.add(dep);
	
		other.strongDependencies.remove(this);
		other.weakDependencies.remove(this);
		other.children.remove(this);
		
		for (Dependency dep : this.reverseDependencies)
		{
			other.reverseDependencies.add(dep);

			dep.weakDependencies.remove(this);
			dep.weakDependencies.add(other);
		}

		other.reverseDependencies.remove(this);
		
		assert(!other.reverseDependencies.contains(other));
	}

	@Override
	public ArrayList<String> getPath()
	{
		return this.node.getTask().getPath();
	}
	
	/* TODO: make this abstract and implemented in both subclasses? */
	public Expression getPathTo(Dependency target)
	{
		AST ast = this.node.getAST();

		ArrayList<String> this_path = this.getPath();
		ArrayList<String> target_path = target.getPath();
		
		while (!this_path.isEmpty() && !target_path.isEmpty() && this_path.get(0).equals(target_path.get(0)))
		{
			this_path.remove(0);
			target_path.remove(0);
		}

		Expression path = ast.newThisExpression();

		while (!this_path.isEmpty())
		{
			this_path.remove(0);

			FieldAccess field = ast.newFieldAccess();
			field.setExpression(path);
			field.setName(ast.newSimpleName("ae_parent"));
			path = field;
		}

		while (!target_path.isEmpty())
		{
			FieldAccess field = ast.newFieldAccess();
			field.setExpression(path);
			field.setName(ast.newSimpleName("ae_" + target_path.remove(0)));

			path = field;
		}

		return path;
	}
	
	@Override
	public String toString()
	{
		return "[NODE] "+this.node.getOriginal();
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof NodeDependency))
			return false;
		
		NodeDependency _other = (NodeDependency) other;
		return this.node.equals(_other.node);
	}

	@Override
	public int hashCode()
	{
		return this.node.hashCode();
	}

}