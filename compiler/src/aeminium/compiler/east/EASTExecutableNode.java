package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public abstract class EASTExecutableNode extends EASTNode
{
	protected final Signature signature;
	
	/* here order matters so we can't use a set */
	protected final ArrayList<EASTExecutableNode> strongDependencies;

	protected final Set<EASTExecutableNode> weakDependencies;
	protected final Set<EASTExecutableNode> children;
	
	protected final Set<EASTControlerNode> controlers;

	/* optimize */
	protected boolean inlineTask;
	
	/* preTranslate */
	protected Task task;

	private EASTExecutableNode inlinedTo;
	
	public EASTExecutableNode(EAST east, ASTNode original)
	{
		super(east, original);

		this.signature = new Signature();
		
		this.strongDependencies = new ArrayList<EASTExecutableNode>();
		this.weakDependencies = new HashSet<EASTExecutableNode>();
		this.children = new HashSet<EASTExecutableNode>();
		this.controlers = new HashSet<EASTControlerNode>();
		
		this.inlineTask = false;
	}
	
	public ArrayList<EASTExecutableNode> getStrongDependencies()
	{
		return this.strongDependencies;
	}
	
	public Set<EASTExecutableNode> getWeakDependencies()
	{
		return this.weakDependencies;
	}

	public Set<EASTExecutableNode> getChildren()
	{
		return this.children;
	}
	
	public Task getTask()
	{
		assert (this.task != null);
		return this.task;
	}
	
	public abstract void checkSignatures();
	public abstract Signature getFullSignature();

	public abstract void checkDependencies(DependencyStack stack);
	
	public int optimize()
	{
		this.simplifyDependencies();
		
		int sum = 0;
		
		HashSet<EASTExecutableNode> nodes = new HashSet<EASTExecutableNode>();
		nodes.addAll(this.strongDependencies);

		if (this.strongDependencies.size() + this.weakDependencies.size() < 2)
			for (EASTExecutableNode node : nodes)
				sum += node.inline(this);
		
		for (EASTExecutableNode node : nodes)
			if (node.isSimpleTask())
				sum += node.inline(this);
		
		return sum;
	}
	
	public boolean isSimpleTask()
	{
		return false;
	}

	public int inline(EASTExecutableNode inlineTo)
	{
		if (this.inlineTask)
			return 0;
		
		while (inlineTo.inlineTask)
			inlineTo = inlineTo.inlinedTo;
		
		for (EASTExecutableNode dep : this.strongDependencies)
			if (!inlineTo.strongDependencies.contains(dep))
				inlineTo.strongDependencies.add(dep);

		for (EASTExecutableNode dep : this.weakDependencies)
			if (!inlineTo.weakDependencies.contains(dep))
				inlineTo.weakDependencies.add(dep);

		for (EASTExecutableNode dep : this.children)
			if (!inlineTo.children.contains(dep))
				inlineTo.children.add(dep);

		inlineTo.strongDependencies.remove(this);
		inlineTo.weakDependencies.remove(this);
		inlineTo.children.remove(this);
		
		this.inlineTask = true;
		this.inlinedTo = inlineTo;

		return 1;
	}

	public void simplifyDependencies()
	{
		// TODO simplifyDependencies()
		// watch out for inlined tasks
		// System.err.println("TODO: EASTExecutableNode.simplifyDependencies()");
		Set<EASTExecutableNode> deps = new HashSet<EASTExecutableNode>();
		deps.addAll(this.weakDependencies);
		deps.addAll(this.strongDependencies);
		
		Set<EASTExecutableNode> temp_weak = new HashSet<EASTExecutableNode>(this.weakDependencies);
		
		for (EASTExecutableNode dep : deps)
		{
			temp_weak.remove(dep.weakDependencies);
			temp_weak.remove(dep.strongDependencies);
		}
		
		this.weakDependencies.clear();
		this.weakDependencies.addAll(temp_weak);
	}
	
	public abstract void preTranslate(Task parent);
	
	public abstract EASTDataNode getScope();

	public void addController(EASTControlerNode controler)
	{
		this.controlers.add(controler);
	}

	@SuppressWarnings("unchecked")
	public void postTranslate(Task task)
	{
		AST ast = this.getAST();

		if (this.controlers.size() == 0)
			return;

		Expression cond = null;
		
		for (EASTControlerNode controler : this.controlers)
		{
			FieldAccess access = ast.newFieldAccess();
			access.setExpression(task.getPathToNearestTask(controler.getScopeTask()));
			access.setName(ast.newSimpleName("ae_finished"));
			
			PrefixExpression prefix = ast.newPrefixExpression();
			prefix.setOperator(org.eclipse.jdt.core.dom.PrefixExpression.Operator.NOT);
			prefix.setOperand(access);

			if (cond == null)
				cond = prefix;
			else
			{
				InfixExpression expr = ast.newInfixExpression();
				expr.setLeftOperand(cond);
				expr.setRightOperand(prefix);
				expr.setOperator(Operator.CONDITIONAL_AND);
				
				cond = expr;
			}
		}
		
		IfStatement ifstmt = ast.newIfStatement();
		
		ifstmt.setExpression(cond);
		ifstmt.setThenStatement((Statement) ASTNode.copySubtree(ast, task.getExecute().getBody()));
		
		Block block = ast.newBlock();
		block.statements().add(ifstmt);
		
		task.getExecute().setBody(block);
	}
}