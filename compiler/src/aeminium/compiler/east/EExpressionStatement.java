package aeminium.compiler.east;

import java.util.List;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.osgi.internal.resolver.ComputeNodeOrder;

import aeminium.compiler.Task;

public class EExpressionStatement extends EStatement
{
	ExpressionStatement origin;
	EExpression expr;

	EExpressionStatement(EAST east, ExpressionStatement origin)
	{
		super(east);
		this.origin = origin;

		this.expr = this.east.extend((Expression) origin.getExpression());
	}

	@Override
	public void analyse()
	{
		super.analyse();
		this.expr.analyse();
		
		this.signature.addFrom(this.expr.getSignature());
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		sum += this.expr.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newChild("expstmt");
		else
			this.task = parent;
		
		this.expr.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Statement> translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(cus);

		cus.add(this.task.getCompilationUnit());
		
		Block execute = ast.newBlock();
		execute.statements().addAll(this.build(cus));
		this.task.setExecute(execute);

		MethodDeclaration constructor = this.task.createConstructor();
		this.task.addConstructor(constructor);

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName(this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}

	public List<Statement> build(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		ExpressionStatement expr_stmt = ast.newExpressionStatement(this.expr.translate(cus));

		return Arrays.asList((Statement)expr_stmt);
	}
}
