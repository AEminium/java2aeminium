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

		if (node.isAeminium())
			path.add(subpath);
		
		return path;
	}
}
