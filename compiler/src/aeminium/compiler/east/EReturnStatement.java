package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemControl;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SignatureItemWrite;
import aeminium.compiler.task.Task;

public class EReturnStatement extends EStatement implements EASTControlerNode
{
	protected final EExpression expr;
	protected final Set<EASTExecutableNode> controled;
	
	public EReturnStatement(EAST east, ReturnStatement original, EASTDataNode scope, EMethodDeclaration method, EASTExecutableNode parent, EReturnStatement base)
	{
		super(east, original, scope, method, parent, base);

		if (original.getExpression() == null)
			this.expr = null;
		else
			this.expr = EExpression.create(this.east, original.getExpression(), scope, this, base == null ? null : base.expr);

		this.controled = new HashSet<EASTExecutableNode>();
	}

	/* factory */
	public static EReturnStatement create(EAST east, ReturnStatement stmt, EASTDataNode scope, EMethodDeclaration method, EASTExecutableNode parent, EReturnStatement base)
	{
		return new EReturnStatement(east, stmt, scope, method, parent, base);
	}
	
	@Override
	public ReturnStatement getOriginal()
	{
		return (ReturnStatement) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.signature.addItem(new SignatureItemControl(this.method.getDataGroup()));
		
		if (this.expr != null)
		{
			this.expr.checkSignatures();
			
			this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
			this.signature.addItem(new SignatureItemWrite(this.method.returnDataGroup));
			this.signature.addItem(new SignatureItemMerge(this.method.returnDataGroup, this.expr.getDataGroup()));
		}
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		
		if (this.expr != null)
			sig.addAll(this.expr.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		if (this.expr != null)
		{
			this.expr.checkDependencies(stack);
			this.addStrongDependency(this.expr);
		}

		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;

		if (this.expr != null)
			sum += this.expr.optimize();

		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "ret", this.base == null ? null : this.base.task);
		
		if (this.expr != null)
			this.expr.preTranslate(this.task);
	}

	@Override
	public List<Statement> build(List<CompilationUnit> out)
	{
		List<Statement> stmts = new ArrayList<Statement>();
		
		AST ast = this.getAST();
		
		if (this.expr != null)
		{
			FieldAccess ret = ast.newFieldAccess();
			ret.setExpression(this.task.getPathToRoot());
			ret.setName(ast.newSimpleName("ae_ret"));
	
			Assignment assign = ast.newAssignment();
			assign.setLeftHandSide(ret);
			assign.setRightHandSide(this.expr.translate(out)); 
	
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
			
			stmts.add(ast.newExpressionStatement(assign));
			stmts.add(ifstmt);			
		}
		
		if (this.controled.size() != 0)
		{
			FieldAccess finished = ast.newFieldAccess();
			finished.setExpression(task.getPathToRoot());
			finished.setName(ast.newSimpleName("ae_finished"));
			
			Assignment finish_assign = ast.newAssignment();
			finish_assign.setLeftHandSide(finished);
			finish_assign.setRightHandSide(ast.newBooleanLiteral(true));
			
			stmts.add(ast.newExpressionStatement(finish_assign));
		}

		return stmts;
	}
	
	

	@Override
	public void addControledNode(EASTExecutableNode node)
	{
		this.controled.add(node);
		
		this.method.control();
	}

	@Override
	public Task getScopeTask()
	{
		return this.method.getTask();
	}
	
	@Override
	public boolean isSimpleTask()
	{
		return EASTExecutableNode.HARD_AGGREGATION;
	}
}
