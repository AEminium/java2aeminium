package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.task.Task;
import aeminium.compiler.task.WhileSubTask;

public class EWhileStatement extends EStatement
{
	protected final EExpression expr;
	protected final EStatement body;

	protected final EExpression expr_loop;
	protected final EStatement body_loop;
	
	protected WhileSubTask task_loop;
	
	public EWhileStatement(EAST east, WhileStatement original, EASTDataNode scope, EMethodDeclaration method, EWhileStatement base)
	{
		super(east, original, scope, method, base);
		
		this.expr = EExpression.create(east, original.getExpression(), scope, base == null ? null : base.expr);
		this.body = EStatement.create(east, original.getBody(), scope, method, base == null ?  null : base.body);
		
		this.expr_loop = EExpression.create(east, original.getExpression(), scope, this.expr);
		this.body_loop = EStatement.create(east, original.getBody(), scope, method, this.body);
	}

	/* factory */
	public static EWhileStatement create(EAST east, WhileStatement original, EASTDataNode scope, EMethodDeclaration method, EWhileStatement base)
	{
		return new EWhileStatement(east, original, scope, method, base);
	}
	
	@Override
	public WhileStatement getOriginal()
	{
		return (WhileStatement) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
		this.body.checkSignatures();
		
		this.expr_loop.checkSignatures();
		this.body_loop.checkSignatures();

		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
		/* FIXME: add expr_loop read? */
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());
		sig.addAll(this.body.getFullSignature());
		
		/* FIXME: add expr_loop/body_loop signatures? probably not because they don't add anything new*/

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		this.strongDependencies.add(this.expr);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			if (!node.equals(this.expr))
				this.weakDependencies.add(node);
				
		DependencyStack copy = stack.fork();
		this.body.checkDependencies(copy);
		this.expr_loop.checkDependencies(copy);
		
		DependencyStack copy2 = copy.fork();
		this.body_loop.checkDependencies(copy2);
		copy.join(copy2, this);
		
		stack.join(copy, this);

		this.children.add(this.body);

		// TODO: this is only valid for the sequential translation used bellow
		this.children.add(this);
	}

	@Override
	public int optimize()
	{
		int sum = 0;
		
		sum += this.expr.optimize();
		sum += this.body.optimize();
		
		sum += this.expr_loop.optimize();
		sum += this.body_loop.optimize();

		sum += super.optimize();
		
		return sum;
	}

	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "while", this.base == null ? null : this.base.task);
		
		this.expr.preTranslate(this.task);
		this.body.preTranslate(this.task);

		assert(!this.inlineTask);
		
		if (!this.inlineTask)
		{
			this.task_loop = WhileSubTask.create(this, this.task.getTypeName() + "loop", parent, this.task);
			this.expr_loop.preTranslate(this.task_loop);
			this.body_loop.preTranslate(this.task_loop);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Statement> translate(List<CompilationUnit> out)
	{
		if (this.inlineTask)
			return this.build(out);

		out.add(this.task.translate());
		out.add(this.task_loop.translate());

		this.task.getExecute().getBody().statements().addAll(this.build(out));
		this.task_loop.getExecute().getBody().statements().addAll(this.buildLoop(out));
		
		AST ast = this.getAST();
		
		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName("ae_" + this.task.getFieldName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		this.postTranslate(this.task);

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Statement> build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		IfStatement stmt = ast.newIfStatement();
		stmt.setExpression(this.expr.translate(out));

		Block block = ast.newBlock();
		block.statements().addAll(this.body.translate(out));

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName("ae_" + this.task.getFieldName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task_loop.create());

		block.statements().add(ast.newExpressionStatement(assign));
		
		stmt.setThenStatement(block);
		
		return Arrays.asList((Statement) stmt);
	}

	@SuppressWarnings("unchecked")
	public List<Statement> buildLoop(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		IfStatement stmt = ast.newIfStatement();
		stmt.setExpression(this.expr_loop.translate(out));

		Block block = ast.newBlock();
		block.statements().addAll(this.body_loop.translate(out));

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName("ae_" + this.task_loop.getFieldName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task_loop.create());

		block.statements().add(ast.newExpressionStatement(assign));
		
		stmt.setThenStatement(block);
		
		return Arrays.asList((Statement) stmt);
	}

	public EASTExecutableNode getBody()
	{
		return this.body;
	}
}
