package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.task.Task;

public class EWhileStatement extends EStatement
{
	protected final EExpression expr;
	protected final EStatement body;
	
	public EWhileStatement(EAST east, WhileStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		super(east, original, scope, method);
		
		this.expr = EExpression.create(east, original.getExpression(), scope);
		this.body = EStatement.create(east, original.getBody(), scope, method);
	}

	/* factory */
	public static EWhileStatement create(EAST east, WhileStatement original, EASTDataNode scope, EMethodDeclaration method)
	{
		return new EWhileStatement(east, original, scope, method);
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
		
		this.signature.addItem(new SignatureItemRead(this.dependency, this.expr.getDataGroup()));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());
		sig.addAll(this.body.getFullSignature());
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		this.dependency.addStrong(this.expr.dependency);
		
		Set<Dependency> deps = stack.getDependencies(this.signature);
		this.dependency.addWeak(deps);

		DependencyStack copy = stack.fork();
		this.body.checkDependencies(copy);

		stack.join(copy, this.dependency);

		this.dependency.addChild(this.body.dependency);

		// TODO: this is only valid for the sequential translation used bellow
		this.dependency.addChild(this.dependency);
	}

	@Override
	public int optimize()
	{
		int sum = 0;
		
		/* force expr task to be inlined */
		sum += this.expr.inlineTo(this);

		sum += this.expr.optimize();
		sum += this.body.optimize();
		
		sum += super.optimize();
		
		return sum;
	}

	@Override
	public void preTranslate(Task parent)
	{
		if (this.inline)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "while");
		
		this.expr.preTranslate(this.task);
		this.body.preTranslate(this.task);
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
		task_access.setName(ast.newSimpleName("ae_" + this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		block.statements().add(ast.newExpressionStatement(assign));
		
		stmt.setThenStatement(block);
		
		return Arrays.asList((Statement) stmt);
	}

	public EASTExecutableNode getBody()
	{
		return this.body;
	}
}
