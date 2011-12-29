package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.Task;
import aeminium.compiler.datagroup.ConstantDataGroup;

public class ENumberLiteral extends EExpression
{
	NumberLiteral origin;

	ENumberLiteral(EAST east, NumberLiteral origin)
	{
		super(east);

		this.origin = origin;
		this.datagroup = new ConstantDataGroup(this);
	}

	@Override
	public void analyse()
	{
		super.analyse();
	}

	@Override
	public int optimize()
	{
		this.root = false;
		return 0;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		// Nothing to do here
	}

	@Override
	public Expression translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		return (NumberLiteral) ASTNode.copySubtree(ast, this.origin);
	}
}
