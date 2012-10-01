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

	protected final EWhileStatement loop;
	
	public EWhileStatement(EAST east, WhileStatement original, EASTDataNode scope, EMethodDeclaration method, EWhileStatement base)
	{
		super(east, original, scope, method, base);
		
		this.expr = EExpression.create(east, original.getExpression(), scope, base == null ? null : base.expr);
		this.body = EStatement.create(east, original.getBody(), scope, method, base == null ?  null : base.body);
		
		if (base == null)
			this.loop = EWhileStatement.create(east, original, scope, method, this);
		else
			this.loop = null;
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

		if (this.loop != null)
			this.loop.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
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
		this.addStrongDependency(this.expr);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
				
		DependencyStack copy = stack.fork();
		this.body.checkDependencies(copy);
		
		if (this.loop != null)
		{
			DependencyStack copy2 = copy.fork();
			this.loop.checkDependencies(copy2);
			copy.join(copy2, this);
		}

		stack.join(copy, this);

		this.addChildren(this.body);

		// TODO: this is only valid for the sequential translation used bellow
		if (this.loop != null)
			this.addChildren(this.loop);
/*		else
			this.addChildren(this); /* FIXME: this will probably break somewhere */
	}

	@Override
	public int optimize()
	{
		int sum = 0;
		
		sum += this.expr.optimize();
		sum += this.body.optimize();
		
		if (this.loop == null)
		{
			if (this.expr.base.inlineTask)
				this.expr.inline(this);
			
			if (this.body.base.inlineTask)
				this.body.inline(this);
		} else
			sum += this.loop.optimize();
		
		sum += super.optimize();
		
		return sum;
	}

	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
		{
			if (this.loop != null)
				this.task = parent.newSubTask(this, "while", this.base == null ? null : this.base.task);
			else
				this.task = WhileSubTask.create(this, this.base.task.getTypeName() + "loop", parent, this.base.task);
		}
		
		this.expr.preTranslate(this.task);
		this.body.preTranslate(this.task);
		
		if (this.loop != null)
			this.loop.preTranslate(this.task);
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
		
		if (this.loop != null)
			block.statements().addAll(this.loop.translate(out));
		else
		{
			/* the same thing as a normal translate here.
			 * because doing so would create an infinite loop */
			FieldAccess task_access = ast.newFieldAccess();
			task_access.setExpression(ast.newThisExpression());
			task_access.setName(ast.newSimpleName("ae_" + this.task.getFieldName()));

			Assignment assign = ast.newAssignment();
			assign.setLeftHandSide(task_access);
			assign.setRightHandSide(this.task.create());

			block.statements().add(ast.newExpressionStatement(assign));	
		}

		stmt.setThenStatement(block);
		
		return Arrays.asList((Statement) stmt);
	}

	/*
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
	}*/

	public EASTExecutableNode getBody()
	{
		return this.body;
	}
}
