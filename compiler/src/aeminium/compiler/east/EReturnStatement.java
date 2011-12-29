package aeminium.compiler.east;

import java.util.List;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import aeminium.compiler.Task;

public class EReturnStatement extends EStatement
{
	private final ReturnStatement origin;
	private final EExpression expr;

	EReturnStatement(EAST east, ReturnStatement origin)
	{
		super(east);
		this.origin = origin;

		if (origin.getExpression() != null)
			this.expr = this.east.extend((Expression) origin.getExpression());
		else
			this.expr = null;
	}
	
	@Override
	public void analyse()
	{
		super.analyse();
		
		this.expr.analyse();
		
		this.signature.addFrom(this.expr.getSignature());
		// TODO/FIXME: merge with the return datagroup
		
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
			this.task = parent.newChild("ret");
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
		assert(this.expr != null);

		if (!this.isRoot())
			return this.build(cus);

		cus.add(this.task.getCompilationUnit());
		
		Block execute = ast.newBlock();
		execute.statements().addAll(this.build(cus));
		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

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

		FieldAccess ret = ast.newFieldAccess();
		ret.setExpression(task.getPathToRoot());
		ret.setName(ast.newSimpleName("ae_ret"));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(ret);
		assign.setRightHandSide(this.expr.translate(cus)); 

		// if the value is required, push it to the caller task
		IfStatement ifstmt = ast.newIfStatement();
		
		FieldAccess caller_task = ast.newFieldAccess();
		caller_task.setExpression(task.getPathToRoot());
		caller_task.setName(ast.newSimpleName("ae_parent"));

		InfixExpression cond = ast.newInfixExpression();
		cond.setLeftOperand(caller_task);
		cond.setOperator(Operator.NOT_EQUALS);
		cond.setRightOperand(ast.newNullLiteral());

		ifstmt.setExpression(cond);

		Assignment push_assign = ast.newAssignment();

		FieldAccess caller_ret = ast.newFieldAccess();
		caller_ret.setExpression((Expression) ASTNode.copySubtree(ast, caller_task));
		caller_ret.setName(ast.newSimpleName("ae_ret"));

		push_assign.setLeftHandSide(caller_ret);
		push_assign.setRightHandSide((Expression) ASTNode.copySubtree(ast, ret));

		ifstmt.setThenStatement(ast.newExpressionStatement(push_assign));

		return Arrays.asList(ast.newExpressionStatement(assign), ifstmt);
	}
}
