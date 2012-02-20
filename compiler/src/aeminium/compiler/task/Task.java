package aeminium.compiler.task;

import java.util.Stack;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.east.EASTExecutableNode;

public abstract class Task
{
	protected final EASTExecutableNode node;
	protected final String name;
	protected final Task parent;

	protected int subtasks;
	
	protected final CompilationUnit cu;
	protected final TypeDeclaration decl;
	protected final MethodDeclaration constructor;
	protected final MethodDeclaration execute;
	
	@SuppressWarnings("unchecked")
	protected Task(EASTExecutableNode node, String name, Task parent)
	{
		System.out.println("Task: " + name + " child of " + parent);
		
		this.node = node;
		this.name = name;
		this.parent = parent;

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
		this.constructor = ast.newMethodDeclaration();
		this.decl.bodyDeclarations().add(this.constructor);
		
		/* public void execute(Runtime rt, Task task) throws Exception */
		this.execute = ast.newMethodDeclaration();
		this.decl.bodyDeclarations().add(this.execute);
		
		this.cu.types().add(this.decl);
	}

	public Task newSubTask(EASTExecutableNode node, String suffix)
	{
		this.subtasks++;
		
		return SubTask.create(node, this.name + "_" + this.subtasks + "_" + suffix, this);
	}
	
	@Override
	public String toString()
	{
		return this.name;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	@SuppressWarnings("unchecked")
	public void addField(Type type, String name, boolean isVolatile)
	{
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
	protected void fillConstructor(Block body)
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
			
			SimpleType type = ast.newSimpleType(ast.newSimpleName(dep.getName()));
			String name = "ae_" + dep.getName();

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
			
			SimpleType type = ast.newSimpleType(ast.newSimpleName(child.getName()));
			String name = "ae_" + child.getName();

			this.addField(type, name, false);
		}

		// AeminiumHelper.schedule(this.ae_task, ..., Arrays.asList(this.ae_1.ae_task, this.ae_2.ae_task) );
		MethodInvocation schedule = ast.newMethodInvocation();
		schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
		schedule.setName(ast.newSimpleName("schedule"));

		schedule.arguments().add((Expression) ASTNode.copySubtree(ast, task_access));
		
		// this.ae_parent == null ? AeminiumHelper.NO_PARENT : this.ae_parent.ae_task
		ConditionalExpression expr = ast.newConditionalExpression();
		
		FieldAccess access = ast.newFieldAccess();
		access.setExpression(ast.newThisExpression());
		access.setName(ast.newSimpleName("ae_parent"));

		InfixExpression inf = ast.newInfixExpression();
		inf.setLeftOperand(access);
		inf.setRightOperand(ast.newNullLiteral());
		inf.setOperator(InfixExpression.Operator.EQUALS);

		FieldAccess parent_task = ast.newFieldAccess();
		parent_task.setExpression((Expression) ASTNode.copySubtree(ast,access));
		parent_task.setName(ast.newSimpleName("ae_task"));
	
		expr.setExpression(inf);
		expr.setThenExpression(ast.newName("AeminiumHelper.NO_PARENT"));
		expr.setElseExpression(parent_task);

		schedule.arguments().add(expr);

		if (this.node.getStrongDependencies().size() > 0 || this.node.getWeakDependencies().size() > 0)
		{
			MethodInvocation asList = ast.newMethodInvocation();
			asList.setExpression(ast.newName("java.util.Arrays"));
			asList.setName(ast.newSimpleName("asList"));

			for (EASTExecutableNode node: this.node.getStrongDependencies())
			{
				Task dep = node.getTask();
				
				FieldAccess dep_access = ast.newFieldAccess();
				dep_access.setExpression(ast.newThisExpression());
				dep_access.setName(ast.newSimpleName("ae_" + dep.getName()));

				FieldAccess dep_task = ast.newFieldAccess();
				dep_task.setExpression(dep_access);
				dep_task.setName(ast.newSimpleName("ae_task"));

				asList.arguments().add(dep_task);
			}

			for (EASTExecutableNode node : this.node.getWeakDependencies())
			{
				Task dep = node.getTask();
				
				FieldAccess dep_task = ast.newFieldAccess();
				dep_task.setExpression(this.getPathToTask(dep));
				dep_task.setName(ast.newSimpleName("ae_task"));

				asList.arguments().add(dep_task);
			}
			
			schedule.arguments().add(asList);
		} else
			schedule.arguments().add(ast.newName("AeminiumHelper.NO_DEPS"));

		body.statements().add(ast.newExpressionStatement(schedule));

		this.constructor.setName(ast.newSimpleName(this.name));
		this.constructor.setConstructor(true);
		this.constructor.setBody(body);
	}
	
	public Expression getPathToTask(Task target)
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

		while (!tasks_self.empty())
		{
			tasks_self.pop();

			FieldAccess field = ast.newFieldAccess();
			field.setExpression(path);
			field.setName(ast.newSimpleName("ae_parent"));
			path = field;
		}

		while (!tasks_target.empty())
		{
			FieldAccess field = ast.newFieldAccess();
			field.setExpression(path);
			field.setName(ast.newSimpleName("ae_" + tasks_target.pop().getName()));
			path = field;
		}

		return path;
	}

	public Expression getPathToRoot()
	{
		AST ast = this.node.getAST();

		if (this.parent == null)
			return ast.newThisExpression();

		FieldAccess access = ast.newFieldAccess();
		access.setExpression(this.parent.getPathToRoot());
		access.setName(ast.newSimpleName("ae_parent"));

		return access;
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
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.getName())));

		if (this.parent != null)
			creation.arguments().add(ast.newThisExpression());

		return creation;
	}
	
	public CompilationUnit translate()
	{
		this.fillConstructor(this.node.getAST().newBlock());
		this.fillExecute();

		return this.cu;
	}

	public MethodDeclaration getExecute()
	{
		return this.execute;
	}
	
	public MethodDeclaration getConstructor()
	{
		return this.constructor;
	}

	public EASTExecutableNode getNode()
	{
		return this.node;
	}
}
