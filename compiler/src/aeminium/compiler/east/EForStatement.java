package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.task.Task;

public class EForStatement extends EStatement
{
	protected final EExpression expr;
	protected final EStatement body;
	private List<EExpression> initializers = new ArrayList<EExpression>();
	private List<EExpression> updaters = new ArrayList<EExpression>();

	public EForStatement(EAST east, ForStatement original, EASTDataNode scope,
			EMethodDeclaration method)
	{
		super(east, original, scope, method);

		for (Object o : original.initializers())
		{
			this.initializers.add(EExpression.create(east, (Expression) o,
					scope));
		}
		for (Object o : original.updaters())
		{
			this.updaters.add(EExpression.create(east, (Expression) o, scope));
		}
		this.expr = EExpression.create(east, original.getExpression(), scope);
		this.body = EStatement.create(east, original.getBody(), scope, method);
	}

	/* factory */
	public static EForStatement create(EAST east, ForStatement original,
			EASTDataNode scope, EMethodDeclaration method)
	{
		return new EForStatement(east, original, scope, method);
	}

	@Override
	public WhileStatement getOriginal()
	{
		return (WhileStatement) this.original;
	}

	@Override
	public void checkSignatures()
	{
		for (EExpression stmt : this.initializers)
			stmt.checkSignatures();
		for (EExpression stmt : this.updaters)
			stmt.checkSignatures();
		this.expr.checkSignatures();
		this.body.checkSignatures();

		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();

		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());
		sig.addAll(this.body.getFullSignature());
		for (EExpression stmt : this.initializers)
			sig.addAll(stmt.getFullSignature());
		for (EExpression stmt : this.updaters)
			sig.addAll(stmt.getFullSignature());
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		this.strongDependencies.add(this.expr);

		for (EExpression stmt : this.initializers)
		{
			stmt.checkDependencies(stack);
			this.strongDependencies.add(stmt);
		}

		Set<EASTExecutableNode> deps = stack.getDependencies(this,
				this.signature);

		for (EASTExecutableNode node : deps)
			if (!node.equals(this.expr))
				this.weakDependencies.add(node);

		DependencyStack copy = stack.fork();
		this.body.checkDependencies(copy);

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

		sum += super.optimize();

		return sum;
	}

	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "for");

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
