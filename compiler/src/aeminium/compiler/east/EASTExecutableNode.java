package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public abstract class EASTExecutableNode extends EASTNode
{
	public final static boolean HARD_AGGREGATION = true;
	public static final boolean CYCLE_AGGREGATION = true;
	
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
	
	protected final EASTExecutableNode parent;
	protected final EASTExecutableNode base;

	private HashSet<EASTExecutableNode> inlined;
	private boolean sequential = false;
	
	public EASTExecutableNode(EAST east, ASTNode original, EASTExecutableNode parent, EASTExecutableNode base)
	{
		super(east, original);

		this.signature = new Signature();
		
		this.strongDependencies = new ArrayList<EASTExecutableNode>();
		this.weakDependencies = new HashSet<EASTExecutableNode>();
		this.children = new HashSet<EASTExecutableNode>();
		this.controlers = new HashSet<EASTControlerNode>();
		this.inlined = new HashSet<EASTExecutableNode>();
		
		this.inlineTask = false;
		this.parent = parent;
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
		
		if (this.inlineTask && this.base != null)
			sum += this.base.inline(this.inlinedTo.base);
		
		HashSet<EASTExecutableNode> nodes = new HashSet<EASTExecutableNode>();
		nodes.addAll(this.strongDependencies);

		if (this.strongDependencies.size() + this.weakDependencies.size() < 2)
			for (EASTExecutableNode node : nodes)
				sum += node.inline(this);
		
		for (EASTExecutableNode node : nodes)
		{
			if (node.isSimpleTask() && node.parent.isSafeInline(node))
				sum += node.inline(this);
		}
		
		for (EASTExecutableNode node : nodes)
		{
			if (node.base != null && node.base.inlineTask)
				sum += node.inline(this);
		}
		
		return sum;
	}
	
	public boolean isSafeInline(EASTExecutableNode node)
	{
		/* default behaviour, not safe everywere: overrided by EBlock because of control statements */
		return true;
	}

	public boolean isSimpleTask()
	{
		return false;
	}

	public boolean isSingleTask()
	{
		for (EASTExecutableNode node : this.strongDependencies)
			if (!node.inlineTask || !node.isSingleTask())
				return false;
		
		return true;
	}
	
	protected boolean isSequential()
	{
		return this.sequential;
	}
	
	public int sequentialize()
	{
		int sum = 0;
		
		this.sequential  = true;
		
		ArrayList<EASTExecutableNode> nodes = new ArrayList<EASTExecutableNode>();
		nodes.addAll(this.strongDependencies);
		nodes.addAll(this.children);
		
		for (EASTExecutableNode node : nodes)
			sum += node.sequentialize();

		if (!this.inlineTask)
			sum += this.inline(this.parent);
		
		return sum;
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
			inlineTo.addChildren(dep);

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

		for (EASTControlerNode node : this.controlers)
			deps.add((EASTExecutableNode) node);
		
		Set<EASTExecutableNode> remove = new HashSet<EASTExecutableNode>();
		
		for (EASTExecutableNode dep : deps)
		{
			Set<EASTExecutableNode> majored = dep.getMajoredNodes();
			for (EASTExecutableNode weak: this.weakDependencies)
				if (majored.contains(weak))
					remove.add(weak);
		}

		for (EASTExecutableNode dep : this.inlined)
		{
			Set<EASTExecutableNode> subtree = dep.getSubTree();
			for (EASTExecutableNode weak: this.weakDependencies)
				if (subtree.contains(weak))
					remove.add(weak);
		}
		
		this.weakDependencies.removeAll(remove);

		return remove.size();
	}
	
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
		
		for (EASTExecutableNode dep : this.weakDependencies)
			nodes.addAll(dep.getMajoredNodes());

		for (EASTExecutableNode dep : this.inlined)
			nodes.addAll(dep.getSubTree());

		for (EASTExecutableNode dep : this.children)
			nodes.addAll(dep.getSubTree());
		
		return nodes;
	}
	
	private HashSet<EASTExecutableNode> getSubTree()
	{
		HashSet<EASTExecutableNode> nodes = new HashSet<EASTExecutableNode>();

		nodes.addAll(this.children);
		nodes.addAll(this.strongDependencies);

		for (EASTExecutableNode dep : this.strongDependencies)
			nodes.addAll(dep.getSubTree());
		
		for (EASTExecutableNode dep : this.children)
			nodes.addAll(dep.getSubTree());

		for (EASTExecutableNode dep : this.inlined)
			nodes.addAll(dep.getSubTree());

		nodes.removeAll(this.inlined);

		return nodes;
}

	public abstract void preTranslate(Task parent);
	
	public abstract EASTDataNode getScope();

	public void addController(EASTControlerNode controler)
	{
		assert(!this.equals(controler));
		
		this.controlers.add(controler);
		this.addWeakDependency((EASTExecutableNode) controler);
	}

	public void addStrongDependency(EASTExecutableNode dep)
	{
		while (dep.inlineTask)
			dep = dep.inlinedTo;
		
		if (!dep.equals(this))
		{
			this.strongDependencies.add(dep);
			this.weakDependencies.remove(dep);
		}
	}
	
	public void addWeakDependency(EASTExecutableNode dep)
	{
		while (dep.inlineTask)
			dep = dep.inlinedTo;

		if (!this.strongDependencies.contains(dep) && !dep.equals(this))
			this.weakDependencies.add(dep);
	}
	
	protected void addChildren(EASTExecutableNode dep)
	{
		assert(!dep.equals(this));
		this.children.add(dep);
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