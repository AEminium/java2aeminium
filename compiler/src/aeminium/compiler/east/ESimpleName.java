package aeminium.compiler.east;

import java.util.List;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Task;
import aeminium.compiler.datagroup.DataGroup;

public class ESimpleName extends EExpression
{
	private final SimpleName origin;
	private IBinding binding;
	private String variable;
	
	ESimpleName(EAST east, SimpleName origin)
	{
		super(east);

		this.origin = origin;
	}

	@Override
	public void analyse()
	{
		super.analyse();

		this.binding = this.origin.resolveBinding();
		this.variable = this.east.resolveName(this.binding);
	}

	@Override
	public int optimize()
	{
		this.root = false;
		
		return super.optimize();
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		this.task = parent;
	}

	public Task getTask()
	{
		assert(this.task != null);
		return this.task;
	}

	@Override
	public Expression translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		ESimpleName node = (ESimpleName) this.east.getNode(variable);

		FieldAccess field = ast.newFieldAccess();

		field.setExpression(this.task.getPathToTask(node.getTask()));
		field.setName((SimpleName) ASTNode.copySubtree(ast, this.origin));

		return field;
	}

	@Override
	public DataGroup getDataGroup()
	{
		EExpression other = ((EExpression) this.east.getNode(this.variable));
	
		if (other == this)
			return this.datagroup;

		return other.getDataGroup();
	}
	
	 public void setDataGroup(DataGroup datagroup)
	 {
		 this.datagroup = datagroup;
	 }
}
