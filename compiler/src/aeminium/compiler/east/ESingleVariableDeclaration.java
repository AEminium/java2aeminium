package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public class ESingleVariableDeclaration extends EASTDependentNode
{
	SingleVariableDeclaration origin;
	EExpression expr;
	ESimpleName name;

	IBinding binding;

	ESingleVariableDeclaration(EAST east, SingleVariableDeclaration origin)
	{
		super(east);
		this.origin = origin;
	
		this.name = this.east.extend(origin.getName());

		if (origin.getInitializer() != null)
			this.expr = this.east.extend(origin.getInitializer());
	}

	@Override
	public void optimize()
	{
		super.optimize();
	
		this.name.optimize();

		if (this.expr != null)
			this.expr.optimize();

		this.binding = this.origin.resolveBinding();
	}

	// TODO:
	/*public void translate(EASTDependentNode owner)
	{
		this.name.setTask(owner.task);
		this.name.addWeakDependency(owner);
		this.east.putNode(this.east.resolveName(this.binding), this.name);
	} */

	public void translate(EMethodDeclaration owner)
	{
		this.name.setTask(owner.task);
		this.east.putNode(this.east.resolveName(this.binding), this.name);
	}
}
