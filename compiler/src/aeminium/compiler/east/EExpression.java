package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.east.*;
import aeminium.compiler.Task;

public abstract class EExpression extends EASTDependentNode
{
	EExpression(EAST east)
	{
		super(east);
	}

	public abstract Expression translate(Task parent, boolean reads);
	public abstract void setWriteTask(Task writer);
}
