package aeminium.compiler.east;

import java.util.List;
import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Task;

public abstract class EStatement extends EASTDependentNode
{
	EStatement(EAST east)
	{
		super(east);
	}

	public abstract void preTranslate(Task parent);
	public abstract List<Statement> translate(List<CompilationUnit> cus);
}
