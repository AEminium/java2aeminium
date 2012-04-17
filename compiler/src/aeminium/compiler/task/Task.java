package aeminium.compiler.task;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

import aeminium.compiler.Dependency;
import aeminium.compiler.NodeDependency;
import aeminium.compiler.east.EASTExecutableNode;

public abstract class Task
{
	protected final EASTExecutableNode node;
	protected final String name;
	protected final Task parent;

	protected int subtasks;
	
	protected final CompilationUnit cu;
	protected final TypeDeclaration decl;
	protected final ArrayList<MethodDeclaration> constructors;
	protected final MethodDeclaration execute;
	
	@SuppressWarnings("unchecked")
	protected Task(EASTExecutableNode node, String name, Task parent)
	{
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
		this.constructors = new ArrayList<MethodDeclaration>();
		
		MethodDeclaration defaultConstructor = ast.newMethodDeclaration();
		this.constructors.add(defaultConstructor);
		
		this.decl.bodyDeclarations().add(defaultConstructor);

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
	protected void fillConstructor(MethodDeclaration constructor, Block body, boolean recursive, ArrayList<Task> overrideTasks)
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
		for (NodeDependency dep : this.node.dependency.getStrongDependencies())
		{
			Task dep_task = dep.getNode().getTask();
			
			SimpleType type = ast.newSimpleType(ast.newSimpleName(dep_task.getName()));
			String name = "ae_" + dep_task.getName();

			if (!recursive)
				this.addField(type, name, false);

			// this.ae_s1 = new ....()
			Assignment child_asgn = ast.newAssignment();

			FieldAccess child_access = ast.newFieldAccess();
			child_access.setExpression(ast.newThisExpression());
			child_access.setName(ast.newSimpleName(name));

			child_asgn.setLeftHandSide(child_access);
			child_asgn.setRightHandSide(dep_task.create());

			body.statements().add(ast.newExpressionStatement(child_asgn));
		}

		for (Dependency dep :  this.node.dependency.getChildren())
		{
			if (!(dep instanceof NodeDependency))
				continue;
			
			NodeDependency _dep = (NodeDependency) dep;

			if (_dep.dependentFree())
				continue;
			
			Task child = _dep.getNode().getTask();
			
			SimpleType type = ast.newSimpleType(ast.newSimpleName(child.getName()));
			String name = "ae_" + child.getName();

			if (!recursive)
				this.addField(type, name, true);
		}

		if (!(this instanceof MethodTask))
		{
			// AeminiumHelper.schedule(this.ae_task, ..., Arrays.asList(this.ae_1.ae_task, this.ae_2.ae_task) );
			MethodInvocation schedule = ast.newMethodInvocation();
			schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
			schedule.setName(ast.newSimpleName("schedule"));
	
			schedule.arguments().add((Expression) ASTNode.copySubtree(ast, task_access));
			
			FieldAccess parent_task = ast.newFieldAccess();
			parent_task.setExpression(ast.newSimpleName("ae_parent"));
			parent_task.setName(ast.newSimpleName("ae_task"));
			
			schedule.arguments().add(parent_task);
	
			if (overrideTasks == null)
			{
				/* can't use a HashSet here because order is important. any "OrderedSet" implementation maybe? */
				ArrayList<Expression> deps = new ArrayList<Expression>();
				
				for (Dependency dep : this.node.dependency.getStrongDependencies())
				{
					Expression path = this.node.dependency.getPathTo(dep);
					
					FieldAccess task_path = ast.newFieldAccess();
					task_path.setExpression(path);
					task_path.setName(ast.newSimpleName("ae_task"));
					
					deps.add(task_path);
				}
				
				for (Dependency dep : this.node.dependency.getWeakDependencies())
				{
					if (dep.isParentOf(this.node.dependency))
						continue;

					Expression path = this.node.dependency.getPathTo(dep);
					FieldAccess task_path = ast.newFieldAccess();
					task_path.setExpression(path);
					task_path.setName(ast.newSimpleName("ae_task"));

					deps.add(task_path);
				}
				
				if (deps.size() > 0)
				{
					MethodInvocation asList = ast.newMethodInvocation();
					asList.setExpression(ast.newName("java.util.Arrays"));
					asList.setName(ast.newSimpleName("asList"));
		
					asList.arguments().addAll(deps);
					schedule.arguments().add(asList);
				} else
					schedule.arguments().add(ast.newName("AeminiumHelper.NO_DEPS"));
			} else
			{
				MethodInvocation asList = ast.newMethodInvocation();
				asList.setExpression(ast.newName("java.util.Arrays"));
				asList.setName(ast.newSimpleName("asList"));
				
				for (Task dep : overrideTasks)
				{
					FieldAccess dep_access = ast.newFieldAccess();
					dep_access.setExpression(ast.newSimpleName("ae_parent"));
					dep_access.setName(ast.newSimpleName("ae_" + dep.getName()));
	
					FieldAccess dep_task = ast.newFieldAccess();
					dep_task.setExpression(dep_access);
					dep_task.setName(ast.newSimpleName("ae_task"));
	
					asList.arguments().add(dep_task);
				}
	
				schedule.arguments().add(asList);
			}
			
			body.statements().add(ast.newExpressionStatement(schedule));
		}
		
		constructor.setName(ast.newSimpleName(this.name));
		constructor.setConstructor(true);
		constructor.setBody(body);
	}
	

	public Expression undefer(Expression path)
	{
		return path;
	}
	
	public boolean isChildOf(Task other)
	{
		for (Task parent = this.parent; parent != null; parent = parent.parent)
			if (parent == other)
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
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.getName())));

		if (this.parent != null)
			creation.arguments().add(ast.newThisExpression());

		return creation;
	}
	
	public CompilationUnit translate()
	{
		this.fillConstructor(this.constructors.get(0), this.node.getAST().newBlock(), false, null);
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
	
	public ArrayList<String> getPath()
	{
		Task task = this;

		ArrayList<String> names = new ArrayList<String>();

		while (task != null)
		{
			names.add(0, task.getName());
			task = task.parent;
		};
		
		return names;
	}
}
