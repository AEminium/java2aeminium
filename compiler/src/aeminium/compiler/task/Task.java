package aeminium.compiler.task;

import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.east.EASTExecutableNode;

public abstract class Task
{
	protected final EASTExecutableNode node;
	protected final String name;
	protected final Task parent;
	protected final Task base;

	protected int subtasks;
	
	protected final CompilationUnit cu;
	protected final TypeDeclaration decl;
	protected final ArrayList<MethodDeclaration> constructors;
	protected final MethodDeclaration execute;
	
	protected boolean hasEmptyConstructor = false;
	
	@SuppressWarnings("unchecked")
	protected Task(EASTExecutableNode node, String name, Task parent, Task base)
	{
		this.node = node;
		this.name = name;
		this.parent = parent; 
		this.base = base;
		
		this.subtasks = 0;

		/* build CU and TypeDecl */
		AST ast = node.getAST();

		CompilationUnit originalCU = node.getCU();
		
		this.cu = ast.newCompilationUnit();
		this.cu.setPackage((PackageDeclaration) ASTNode.copySubtree(ast, originalCU.getPackage()));
		this.cu.imports().addAll(ASTNode.copySubtrees(ast, originalCU.imports()));

		this.decl = ast.newTypeDeclaration();
		this.decl.setName(ast.newSimpleName(this.name));
		this.decl.superInterfaceTypes().add(ast.newSimpleType(ast.newName("aeminium.runtime.Body")));
		
		/* constructor */
		this.constructors = new ArrayList<MethodDeclaration>();
		
		MethodDeclaration defaultConstructor = ast.newMethodDeclaration();
		this.constructors.add(defaultConstructor);
		
		this.decl.bodyDeclarations().add(defaultConstructor);
		
		if (this.base != null)
		{
			this.decl.setSuperclassType(ast.newSimpleType(ast.newName(this.base.getTypeName())));
			this.base.addEmptyConstructor();
		}

		/* public void execute(Runtime rt, Task task) throws Exception */
		this.execute = ast.newMethodDeclaration();
		this.decl.bodyDeclarations().add(this.execute);
		
		this.cu.types().add(this.decl);
	}

	@SuppressWarnings("unchecked")
	private void addEmptyConstructor()
	{
		if (this.hasEmptyConstructor)
			return;
		
		AST ast = this.getNode().getAST();

		MethodDeclaration constructor = ast.newMethodDeclaration();
		constructor.setName(ast.newSimpleName(this.name));
		constructor.setConstructor(true);
		constructor.setBody(ast.newBlock());

		this.constructors.add(constructor);	
		this.decl.bodyDeclarations().add(constructor);

		this.hasEmptyConstructor = true;
	}

	public Task newSubTask(EASTExecutableNode node, String suffix, Task base)
	{
		this.subtasks++;
		
		return SubTask.create(node, this.name + "_" + this.subtasks + "_" + suffix, this, base);
	}
	
	@Override
	public String toString()
	{
		return this.name;
	}
	
	public String getTypeName()
	{
		return this.name;
	}
	
	public String getFieldName()
	{
		return this.base == null ? this.name : this.base.getFieldName();
	}
	
	@SuppressWarnings("unchecked")
	public void addField(Type type, String name, boolean isVolatile)
	{
		if (this.base != null)
			return;
		
		AST ast = this.node.getAST();
		
		VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
		frag.setName(ast.newSimpleName(name));

		FieldDeclaration field = ast.newFieldDeclaration(frag);
		field.setType((Type) ASTNode.copySubtree(ast, type));
		field.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		if (isVolatile)
			field.modifiers().add(ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));

		this.decl.bodyDeclarations().add(field);
	}
	
	@SuppressWarnings("unchecked")
	protected void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive)
	{
		AST ast = this.node.getAST();
		
		// this.ae_task = AeminiumHelper.createNonBlockingTask(this, AeminiumHelper.NO_HINTS);
		Assignment task_asgn = ast.newAssignment();

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName("ae_task"));

		task_asgn.setLeftHandSide(task_access);
		
		MethodInvocation task_create = ast.newMethodInvocation();
		task_create.setExpression(ast.newSimpleName("AeminiumHelper"));
		task_create.setName(ast.newSimpleName("createNonBlockingTask"));

		task_create.arguments().add(ast.newThisExpression());
		task_create.arguments().add(ast.newName("AeminiumHelper.NO_HINTS"));

		task_asgn.setRightHandSide(task_create);

		body.statements().add(ast.newExpressionStatement(task_asgn));

		// add bodies
		for (EASTExecutableNode node : this.node.getStrongDependencies())
		{
			Task dep = node.getTask();
			
			SimpleType type = ast.newSimpleType(ast.newSimpleName(dep.getTypeName()));
			String name = "ae_" + dep.getFieldName();

			this.addField(type, name, false);

			// this.ae_s1 = new ....()
			Assignment child_asgn = ast.newAssignment();

			FieldAccess child_access = ast.newFieldAccess();
			child_access.setExpression(ast.newThisExpression());
			child_access.setName(ast.newSimpleName(name));

			child_asgn.setLeftHandSide(child_access);
			child_asgn.setRightHandSide(dep.create());

			body.statements().add(ast.newExpressionStatement(child_asgn));
		}

		for (EASTExecutableNode node :  this.node.getChildren())
		{
			Task child = node.getTask();
			
			SimpleType type = ast.newSimpleType(ast.newSimpleName(child.getTypeName()));
			String name = "ae_" + child.getFieldName();

			this.addField(type, name, false);
		}

		// AeminiumHelper.schedule(this.ae_task, ..., Arrays.asList(this.ae_1.ae_task, this.ae_2.ae_task) );
		MethodInvocation schedule = ast.newMethodInvocation();
		schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
		schedule.setName(ast.newSimpleName("schedule"));

		schedule.arguments().add((Expression) ASTNode.copySubtree(ast, task_access));
		
		// this.ae_parent == null ? AeminiumHelper.NO_PARENT : ae_parent.ae_task
		ConditionalExpression expr = ast.newConditionalExpression();
		
		InfixExpression inf = ast.newInfixExpression();
		inf.setLeftOperand(ast.newSimpleName("ae_parent"));
		inf.setRightOperand(ast.newNullLiteral());
		inf.setOperator(InfixExpression.Operator.EQUALS);

		FieldAccess parent_task = ast.newFieldAccess();
		parent_task.setExpression(ast.newSimpleName("ae_parent"));
		parent_task.setName(ast.newSimpleName("ae_task"));
	
		expr.setExpression(inf);
		expr.setThenExpression(ast.newName("AeminiumHelper.NO_PARENT"));
		expr.setElseExpression(parent_task);

		schedule.arguments().add(expr);

		/* can't use a HashSet here because order is important. any "OrderedSet" implementation maybe? */
		ArrayList<Task> deps = new ArrayList<Task>();
		
		for (EASTExecutableNode node : this.node.getStrongDependencies())
		{
			Task dep = node.getTask();
			if (!deps.contains(dep))
				deps.add(dep);
		}
		
		for (EASTExecutableNode node : this.node.getWeakDependencies())
		{
			Task dep = node.getTask();
			if (!deps.contains(dep) && this != dep && !this.isDescendentOf(dep))
				deps.add(dep);
		}
		
		if (deps.size() > 0)
		{
			MethodInvocation asList = ast.newMethodInvocation();
			asList.setExpression(ast.newName("java.util.Arrays"));
			asList.setName(ast.newSimpleName("asList"));

			for (Task dep : deps)
			{
				FieldAccess dep_task = ast.newFieldAccess();
				dep_task.setExpression(this.getPathToNearestTask(dep));
				dep_task.setName(ast.newSimpleName("ae_task"));

				asList.arguments().add(dep_task);
			}
			
			schedule.arguments().add(asList);
		} else
			schedule.arguments().add(ast.newName("AeminiumHelper.NO_DEPS"));
		
		body.statements().add(ast.newExpressionStatement(schedule));

		constructor.setName(ast.newSimpleName(this.name));
		constructor.setConstructor(true);
		constructor.setBody(body);
	}
	
	public Expression getPathToNearestTask(Task target)
	{
		AST ast = this.node.getAST();

		Task self = this;
		Stack<Task> tasks_target = new Stack<Task>();
		Stack<Task> tasks_self = new Stack<Task>();

		while (target != null)
		{
			tasks_target.push(target);
			target = target.parent;
		}

		while (self != null)
		{
			tasks_self.push(self);
			self = self.parent;
		}

		assert(!tasks_target.empty() && !tasks_self.empty());
 
		while (!tasks_target.empty() && !tasks_self.empty() && tasks_target.peek() == tasks_self.peek())
		{
			tasks_target.pop();
			tasks_self.pop();
		}

		Expression path = ast.newThisExpression();

		self = this;
		if (!tasks_self.empty())
		{
			while (true)
			{
				path = self.pathToParent(path);

				if (self == tasks_self.peek())
					break;
				
				self = self.parent;
			}
		}
		
		while (!tasks_target.empty())
		{
			Task descendent = tasks_target.pop();
			
			if (descendent.isChildOf(descendent.parent) && !this.isDescendentOf(descendent.parent))
				break;

			FieldAccess field = ast.newFieldAccess();
			field.setExpression(path);
			field.setName(ast.newSimpleName("ae_" + descendent.getFieldName()));
			path = field;
		}

		return path;
	}

	public Expression pathToParent(Expression currentPath)
	{
		AST ast = this.node.getAST();
		
		FieldAccess field = ast.newFieldAccess();
		field.setExpression(currentPath);
		field.setName(ast.newSimpleName("ae_parent"));

		return field;
	}

	public Expression getPathToRoot()
	{
		AST ast = this.node.getAST();

		if (this.parent == null)
			return ast.newThisExpression();

		return this.pathToParent(this.parent.getPathToRoot());
	}
	
	public boolean isDescendentOf(Task other)
	{
		for (Task parent = this.parent; parent != null; parent = parent.parent)
			if (parent == other)
				return true;
		
		return false;
	}
	
	public boolean isChildOf(Task other)
	{
		if (this.parent == null)
			return false;
		
		Set<EASTExecutableNode> children = this.parent.node.getChildren();
		for (EASTExecutableNode child : children)
			if (child.getTask() == this)
				return true;

		return false;
	}
	
	@SuppressWarnings("unchecked")
	protected void fillExecute()
	{
		AST ast = this.node.getAST();
		
		this.execute.setName(ast.newSimpleName("execute"));
		this.execute.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		SingleVariableDeclaration runtime = ast.newSingleVariableDeclaration();
		runtime.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Runtime")));
		runtime.setName(ast.newSimpleName("rt"));
		this.execute.parameters().add(runtime);

		SingleVariableDeclaration task = ast.newSingleVariableDeclaration();
		task.setType(ast.newSimpleType(ast.newName("aeminium.runtime.Task")));
		task.setName(ast.newSimpleName("task"));
		this.execute.parameters().add(task);

		this.execute.thrownExceptions().add(ast.newSimpleName("Exception"));
		
		this.execute.setBody(ast.newBlock());
	}

	@SuppressWarnings("unchecked")
	public ClassInstanceCreation create()
	{
		AST ast = this.node.getAST();

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.getTypeName())));

		if (this.parent != null)
			creation.arguments().add(ast.newThisExpression());

		return creation;
	}
	
	public CompilationUnit translate()
	{
		this.fillConstructor(this.constructors.get(0), this.node.getAST().newBlock(), false);
		this.fillExecute();
		
		return this.cu;
	}

	public MethodDeclaration getExecute()
	{
		return this.execute;
	}
	
	public ArrayList<MethodDeclaration> getConstructors()
	{
		return this.constructors;
	}

	public EASTExecutableNode getNode()
	{
		return this.node;
	}
}
