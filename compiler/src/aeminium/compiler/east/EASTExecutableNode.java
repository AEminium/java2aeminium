package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.Collection;
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
	
	protected final EASTExecutableNode base;

	private HashSet<EASTExecutableNode> inlined;
	
	public EASTExecutableNode(EAST east, ASTNode original, EASTExecutableNode base)
	{
		super(east, original);

		this.signature = new Signature();
		
		this.strongDependencies = new ArrayList<EASTExecutableNode>();
		this.weakDependencies = new HashSet<EASTExecutableNode>();
		this.children = new HashSet<EASTExecutableNode>();
		this.controlers = new HashSet<EASTControlerNode>();
		this.inlined = new HashSet<EASTExecutableNode>();
		
		this.inlineTask = false;
		this.base = base;
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
		int sum = this.simplifyDependencies();
		
		HashSet<EASTExecutableNode> nodes = new HashSet<EASTExecutableNode>();
		nodes.addAll(this.strongDependencies);

		if (this.strongDependencies.size() + this.weakDependencies.size() < 2)
			for (EASTExecutableNode node : nodes)
				sum += node.inline(this);
		
		for (EASTExecutableNode node : nodes)
			if (node.isSimpleTask())
				sum += node.inline(this);
		
		for (EASTExecutableNode node : nodes)
		{
			if (node.base != null && node.base.inlineTask)
				sum += node.inline(this);
		}
		
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
			inlineTo.addStrongDependency(dep);

		for (EASTExecutableNode dep : this.weakDependencies)
			inlineTo.addWeakDependency(dep);

		for (EASTExecutableNode dep : this.children)
			inlineTo.children.add(dep);

		inlineTo.inlined.add(this);
		
		inlineTo.strongDependencies.remove(this);
		inlineTo.weakDependencies.remove(this);
		inlineTo.children.remove(this);
		
		this.inlineTask = true;
		this.inlinedTo = inlineTo;

		return 1;
	}

	public int simplifyDependencies()
	{
		Set<EASTExecutableNode> deps = new HashSet<EASTExecutableNode>();
		deps.addAll(this.weakDependencies);
		deps.addAll(this.strongDependencies);
		deps.addAll(this.inlined);

		for (EASTControlerNode node : this.controlers)
			deps.add((EASTExecutableNode) node);
		
		Set<EASTExecutableNode> remove = new HashSet<EASTExecutableNode>();
		
		for (EASTExecutableNode dep : deps)
		{
			Set<EASTExecutableNode> majored = dep.getMajoredNodes();
			for (EASTExecutableNode weak: this.weakDependencies)
			{
				if (majored.contains(weak))
					remove.add(weak);
			}
		}
		
		this.weakDependencies.removeAll(remove);
		return remove.size();
	}
	
	@SuppressWarnings("unchecked")
	private HashSet<EASTExecutableNode> getMajoredNodes()
	{
		HashSet<EASTExecutableNode> nodes = new HashSet<EASTExecutableNode>();

		for (EASTControlerNode node : this.controlers)
			nodes.add((EASTExecutableNode) node);
		
		nodes.addAll(this.children);
		nodes.addAll(this.strongDependencies);
		nodes.addAll(this.weakDependencies);
		nodes.addAll(this.inlined);
		
		for (EASTControlerNode cont : this.controlers)
			nodes.addAll(((EASTExecutableNode) cont).getMajoredNodes());
		
		for (EASTExecutableNode dep : this.strongDependencies)
			nodes.addAll(dep.getMajoredNodes());
		
		for (EASTExecutableNode dep : this.weakDependencies)
			nodes.addAll(dep.getMajoredNodes());

		for (EASTExecutableNode dep : this.children)
			nodes.addAll(dep.getMajoredNodes());

		for (EASTExecutableNode dep : this.inlined)
			nodes.addAll(dep.getMajoredNodes());

		return nodes;
	}

	public abstract void preTranslate(Task parent);
	
	public abstract EASTDataNode getScope();

	public void addController(EASTControlerNode controler)
	{
		if (!controler.equals(this))
		{
			this.controlers.add(controler);
			this.addWeakDependency((EASTExecutableNode) controler);
		}
	}

	public void addStrongDependency(EASTExecutableNode dep)
	{
		if (!dep.equals(this))
		{
			this.strongDependencies.add(dep);
			this.weakDependencies.remove(dep);
		}
	}
	
	public void addWeakDependency(EASTExecutableNode dep)
	{
		if (!this.strongDependencies.contains(dep) && !dep.equals(this))
			this.weakDependencies.add(dep);
			
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