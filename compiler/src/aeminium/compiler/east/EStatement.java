package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public abstract class EStatement extends EASTDependentNode
{
	EStatement(EAST east)
	{
		super(east);
	}

	public abstract List<Statement> translate(Task parent);
}
