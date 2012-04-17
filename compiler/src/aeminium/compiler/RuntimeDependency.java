package aeminium.compiler;

import java.util.ArrayList;

import aeminium.compiler.east.EDeferredExpression;

public class RuntimeDependency extends Dependency
{
	protected final EDeferredExpression node;
	protected final String subpath;

	public RuntimeDependency(EDeferredExpression node, String subpath)
	{
		super();
		
		this.node = node;
		this.subpath = subpath;
	}

	@Override
	public ArrayList<String> getPath()
	{
		ArrayList<String> path = this.node.dependency.getPath();
		path.add(this.subpath);
		
		return path;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof RuntimeDependency))
			return false;
		
		RuntimeDependency _other = (RuntimeDependency) other;
		return this.node.equals(_other.node) && this.subpath.equals(_other.subpath);
	}

	@Override
	public int hashCode()
	{
		return this.node.hashCode() ^ this.subpath.hashCode();
	}
}
