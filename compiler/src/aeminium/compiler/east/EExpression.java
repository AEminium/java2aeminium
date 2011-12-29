package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Task;
import aeminium.compiler.datagroup.DataGroup;

public abstract class EExpression extends EASTDependentNode
{
	protected DataGroup datagroup;

	EExpression(EAST east)
	{
		super(east);
	}

	public abstract void preTranslate(Task parent);
	public abstract Expression translate(List<CompilationUnit> cus);

	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}
}