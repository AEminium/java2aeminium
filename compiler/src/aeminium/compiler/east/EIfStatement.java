package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.task.Task;

public class EIfStatement extends EStatement
{
	protected final EExpression expr;
	protected final EStatement thenStmt;
	protected final EStatement elseStmt;
	
	public EIfStatement(EAST east, IfStatement original, EASTDataNode scope, EMethodDeclaration method, EASTExecutableNode parent, EIfStatement base)
	{
		super(east, original, scope, method, parent, base);

		this.expr = EExpression.create(this.east, original.getExpression(), scope, this, base == null ? null : base.expr);
		this.thenStmt = EStatement.create(this.east, original.getThenStatement(), scope, method, this, base == null ? null : base.thenStmt);
		
		if (original.getElseStatement() == null)
			this.elseStmt = null;
		else
			this.elseStmt = EStatement.create(this.east, original.getElseStatement(), scope, method, this, base == null ? null : base.elseStmt);
	}

	/* factory */
	public static EIfStatement create(EAST east, IfStatement original, EASTDataNode scope, EMethodDeclaration method, EASTExecutableNode parent, EIfStatement base)
	{
		return new EIfStatement(east, original, scope, method, parent, base);
	}

	@Override
	public IfStatement getOriginal()
	{
		return (IfStatement) this.original;
	}

	@Override
	public void checkSignatures()
	{
		this.expr.checkSignatures();
		
		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
		
		this.thenStmt.checkSignatures();
		
		if (this.elseStmt != null)
			this.elseStmt.checkSignatures();
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();

		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());
		sig.addAll(this.thenStmt.getFullSignature());
		
		if (this.elseStmt != null)
			sig.addAll(this.elseStmt.getFullSignature());

		return sig;
	}
	
	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.expr.checkDependencies(stack);
		
		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		this.addStrongDependency(this.expr);
		
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
		
		if (this.elseStmt == null)
		{
			this.thenStmt.checkDependencies(stack);

			this.addChildren(this.thenStmt);
		} else
		{
			DependencyStack copy = stack.fork();

			this.thenStmt.checkDependencies(stack);
			this.elseStmt.checkDependencies(copy);

			stack.join(copy, this);
			
			this.addChildren(this.thenStmt);
			this.addChildren(this.elseStmt);
		}
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;
		
		sum += this.expr.optimize();
		sum += this.thenStmt.optimize();
		
		if (this.thenStmt.isSingleTask())
			sum += this.thenStmt.inline(this);
		
		if (this.elseStmt != null)
		{
			sum += this.elseStmt.optimize();
			
			if (this.elseStmt.isSingleTask())
				sum += this.elseStmt.inline(this);
		}
		
		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "if", this.base == null ? null : this.base.task);
		
		this.expr.preTranslate(this.task);
		this.thenStmt.preTranslate(this.task);
		
		if (this.elseStmt != null)
			this.elseStmt.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<Statement> build(List<CompilationUnit> out)
	{		
		AST ast = this.getAST();

		IfStatement ifstmt = ast.newIfStatement();
		
		ifstmt.setExpression(this.expr.translate(out));

		Block then_block = ast.newBlock();
		then_block.statements().addAll(this.thenStmt.translate(out));
		ifstmt.setThenStatement(then_block);

		if (this.elseStmt != null)
		{
			Block else_block = ast.newBlock();
			else_block.statements().addAll(this.elseStmt.translate(out));
			ifstmt.setElseStatement(else_block);
		}

		return Arrays.asList((Statement)ifstmt);
	}
	
	@Override
	public boolean isSimpleTask()
	{
		if (!this.expr.inlineTask || ! this.thenStmt.inlineTask)
			return false;
		
		if (this.elseStmt != null && !this.elseStmt.inlineTask)
			return false;
		
		return EASTExecutableNode.HARD_AGGREGATION;
	}
	
	@Override
	public boolean isSafeInline(EASTExecutableNode node)
	{
		return false;
	}
}
