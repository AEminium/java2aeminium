package aeminium.compiler.east;

import java.util.List;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;
import aeminium.compiler.Task;
import aeminium.compiler.datagroup.SignatureItemRead;

public class EIfStatement extends EStatement
{
	IfStatement origin;
	EExpression expr;
	EStatement then_stmt;
	EStatement else_stmt;

	EIfStatement(EAST east, IfStatement origin)
	{
		super(east);
		this.origin = origin;

		this.expr = this.east.extend((Expression) origin.getExpression());
		this.then_stmt = this.east.extend(origin.getThenStatement());

		if (this.origin.getElseStatement() != null)
			this.else_stmt = this.east.extend(origin.getElseStatement());
	}
	
	@Override
	public void analyse()
	{
		super.analyse();
		this.expr.analyse();
		this.then_stmt.analyse();

		if (this.else_stmt != null)
			this.else_stmt.analyse();
		
		this.signature.addFrom(this.expr.getSignature());
		this.signature.add(new SignatureItemRead(this.expr.getDataGroup()));
		
		this.signature.addFrom(this.then_stmt.getSignature());
		
		if (this.else_stmt != null)
			this.signature.addFrom(this.else_stmt.getSignature());
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		
		sum += this.expr.optimize();
		sum += this.then_stmt.optimize();
		
		if (this.else_stmt != null)
			sum += this.else_stmt.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.isRoot())
			this.task = parent.newChild("if");
		else
			this.task = parent;
		
		this.expr.preTranslate(this.task);
		this.then_stmt.preTranslate(this.task);
		
		if (this.else_stmt != null)
			this.else_stmt.preTranslate(this.task);
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

		MethodDeclaration constructor = task.createConstructor();
		this.task.addConstructor(constructor);

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName(this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}

	@SuppressWarnings("unchecked")
	public List<Statement> build(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		IfStatement ifstmt = ast.newIfStatement();
		ifstmt.setExpression(this.expr.translate(cus));

		List<Statement> then_stmts = this.then_stmt.translate(cus);
		if (then_stmts.size() > 1)
		{
			Block then_block = ast.newBlock();
			then_block.statements().addAll(then_stmts);
			ifstmt.setThenStatement(then_block);
		} else
			ifstmt.setThenStatement(then_stmts.get(0));

		if (this.else_stmt != null)
		{
			List<Statement> else_stmts = this.else_stmt.translate(cus);
			if (else_stmts.size() > 1)
			{
				Block else_block = ast.newBlock();
				else_block.statements().addAll(then_stmts);
				ifstmt.setElseStatement(else_block);
			} else
				ifstmt.setElseStatement(else_stmts.get(0));
		}

		return Arrays.asList((Statement)ifstmt);
	}
}
