package aeminium.compiler.task;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Stack;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import aeminium.compiler.east.EASTExecutableNode;

public abstract class Task {
	private final boolean OUTPUT_GRAPH = true;

	protected final EASTExecutableNode node;
	protected final String name;
	protected final Task parent;
	protected final Task base;

	protected int subtasks;

	protected Assignment task_asgn;

	protected final CompilationUnit cu;
	protected TypeDeclaration decl;
	protected ArrayList<MethodDeclaration> constructors;
	protected final MethodDeclaration execute;
	protected final MethodDeclaration iterate;

	protected boolean hasEmptyConstructor = false;
	protected boolean translated = false;
	protected boolean doallFor = false;

	@SuppressWarnings("unchecked")
	protected Task(EASTExecutableNode node, String name, Task parent, Task base) {
		this.node = node;
		this.name = name;
		this.parent = parent;
		this.base = base;

		this.subtasks = 0;

		/* build CU and TypeDecl */
		AST ast = node.getAST();

		CompilationUnit originalCU = node.getCU();

		this.cu = ast.newCompilationUnit();
		this.cu.setPackage((PackageDeclaration) ASTNode.copySubtree(ast,
				originalCU.getPackage()));
		this.cu.imports().addAll(
				ASTNode.copySubtrees(ast, originalCU.imports()));

		/* constructor */
		this.constructors = new ArrayList<MethodDeclaration>();

		MethodDeclaration defaultConstructor = ast.newMethodDeclaration();
		this.constructors.add(defaultConstructor);

		this.decl = ast.newTypeDeclaration();
		this.decl.setName(ast.newSimpleName(this.name));

		this.decl.superInterfaceTypes().add(
				ast.newSimpleType(ast.newName("aeminium.runtime.Body")));

		this.decl.bodyDeclarations().add(defaultConstructor);

		if (this.base != null) {
			this.decl.setSuperclassType(ast.newSimpleType(ast.newName(this.base
					.getTypeName())));
			this.base.addEmptyConstructor();
		}

		/* public void execute(Runtime rt, Task task) throws Exception */
		this.execute = ast.newMethodDeclaration();
		this.iterate = ast.newMethodDeclaration();
		this.decl.bodyDeclarations().add(this.execute);
		this.decl.bodyDeclarations().add(this.iterate);

		this.cu.types().add(this.decl);
	}

	@SuppressWarnings("unchecked")
	private void addEmptyConstructor() {
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

	public Task newSubTask(EASTExecutableNode node, String suffix, Task base) {
		this.subtasks++;

		return SubTask.create(node, this.name + "_" + this.subtasks + "_"
				+ suffix, this, base);
	}

	@Override
	public String toString() {
		return this.name;
	}

	public String getTypeName() {
		return this.name;
	}

	public String getFieldName() {
		return this.base == null ? this.name : this.base.getFieldName();
	}

	@SuppressWarnings("unchecked")
	public void addField(Type type, String name, boolean isVolatile) {
		if (this.base != null)
			return;

		AST ast = this.node.getAST();

		VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
		frag.setName(ast.newSimpleName(name));

		FieldDeclaration field = ast.newFieldDeclaration(frag);
		field.setType((Type) ASTNode.copySubtree(ast, type));
		field.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		if (isVolatile)
			field.modifiers().add(
					ast.newModifier(ModifierKeyword.VOLATILE_KEYWORD));

		this.decl.bodyDeclarations().add(field);
	}

	@SuppressWarnings("unchecked")
	protected void fillConstructor(MethodDeclaration constructor, Block body,
			boolean recursive) {
		AST ast = this.node.getAST();

		// this.ae_task = AeminiumHelper.createNonBlockingTask(this,
		// AeminiumHelper.NO_HINTS);
		createTaskAssign();
		addTaskAssign(constructor, body);

		// add bodies
		for (EASTExecutableNode node : this.node.getStrongDependencies()) {
			Task dep = node.getTask();

			SimpleType type = ast.newSimpleType(ast.newSimpleName(dep
					.getTypeName()));
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

		for (EASTExecutableNode node : this.node.getChildren()) {
			Task child = node.getTask();

			SimpleType type = ast.newSimpleType(ast.newSimpleName(child
					.getTypeName()));
			String name = "ae_" + child.getFieldName();

			this.addField(type, name, false);
		}

		// AeminiumHelper.schedule(this.ae_task, ...,
		// Arrays.asList(this.ae_1.ae_task, this.ae_2.ae_task) );
		MethodInvocation schedule = ast.newMethodInvocation();
		schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
		schedule.setName(ast.newSimpleName("schedule"));

		schedule.arguments().add(
				ASTNode.copySubtree(ast, task_asgn.getLeftHandSide()));

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

		/*
		 * can't use a HashSet here because order is important. any "OrderedSet"
		 * implementation maybe?
		 */
		ArrayList<Task> deps = new ArrayList<Task>();

		for (EASTExecutableNode node : this.node.getStrongDependencies()) {
			Task dep = node.getTask();

			if (!deps.contains(dep))
				deps.add(dep);
		}

		for (EASTExecutableNode node : this.node.getWeakDependencies()) {
			Task dep = node.getTask();

			if (!deps.contains(dep) && this != dep && !this.isDescendentOf(dep))
				deps.add(dep);
		}

		if (deps.size() > 0) {
			MethodInvocation asList = ast.newMethodInvocation();
			asList.setExpression(ast.newName("java.util.Arrays"));
			asList.setName(ast.newSimpleName("asList"));

			for (Task dep : deps) {
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

		if (OUTPUT_GRAPH) {
			try {
				FileWriter f = new FileWriter("graph", true);

				f.append(this.name);

				for (EASTExecutableNode node : this.node.getChildren())
					f.append(" child-" + node.getTask().name);

				for (EASTExecutableNode node : this.node
						.getStrongDependencies())
					f.append(" strong-" + node.getTask().name);

				for (EASTExecutableNode node : this.node.getWeakDependencies())
					if (node.getTask() != null)
						f.append(" weak-" + node.getTask().name);

				f.append('\n');
				f.flush();
				f.close();
			} catch (IOException e) {
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void fillField(FieldDeclaration field, Block body, boolean recursive) {

		AST ast = this.node.getAST();

		// this.ae_task = AeminiumHelper.createNonBlockingTask(this,
		// AeminiumHelper.NO_HINTS);
		Assignment task_asgn = ast.newAssignment();

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName("ae_task"));

		task_asgn.setLeftHandSide(task_access);

		for (EASTExecutableNode node : this.node.getChildren()) {
			Task child = node.getTask();

			SimpleType type = ast.newSimpleType(ast.newSimpleName(child
					.getTypeName()));
			String name = "ae_" + child.getFieldName();

			this.addField(type, name, false);
		}

		// AeminiumHelper.schedule(this.ae_task, ...,
		// Arrays.asList(this.ae_1.ae_task, this.ae_2.ae_task) );
		MethodInvocation schedule = ast.newMethodInvocation();
		schedule.setExpression(ast.newSimpleName("AeminiumHelper"));
		schedule.setName(ast.newSimpleName("schedule"));

		schedule.arguments().add(ASTNode.copySubtree(ast, task_access));

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

		/*
		 * can't use a HashSet here because order is important. any "OrderedSet"
		 * implementation maybe?
		 */
		ArrayList<Task> deps = new ArrayList<Task>();

		for (EASTExecutableNode node : this.node.getStrongDependencies()) {
			Task dep = node.getTask();

			if (!deps.contains(dep))
				deps.add(dep);
		}

		for (EASTExecutableNode node : this.node.getWeakDependencies()) {
			Task dep = node.getTask();

			if (!deps.contains(dep) && this != dep && !this.isDescendentOf(dep))
				deps.add(dep);
		}

		if (deps.size() > 0) {
			MethodInvocation asList = ast.newMethodInvocation();
			asList.setExpression(ast.newName("java.util.Arrays"));
			asList.setName(ast.newSimpleName("asList"));

			for (Task dep : deps) {
				FieldAccess dep_task = ast.newFieldAccess();
				dep_task.setExpression(this.getPathToNearestTask(dep));
				dep_task.setName(ast.newSimpleName("ae_task"));

				asList.arguments().add(dep_task);
			}

			schedule.arguments().add(asList);
		} else
			schedule.arguments().add(ast.newName("AeminiumHelper.NO_DEPS"));

		body.statements().add(ast.newExpressionStatement(schedule));

		if (OUTPUT_GRAPH) {
			try {
				FileWriter f = new FileWriter("graph", true);

				f.append(this.name);

				for (EASTExecutableNode node : this.node.getChildren())
					f.append(" child-" + node.getTask().name);

				for (EASTExecutableNode node : this.node
						.getStrongDependencies())
					f.append(" strong-" + node.getTask().name);

				for (EASTExecutableNode node : this.node.getWeakDependencies())
					f.append(" weak-" + node.getTask().name);

				f.append('\n');
				f.flush();
				f.close();
			} catch (IOException e) {
			}
		}

	}

	public Expression getPathToNearestTask(Task target) {
		AST ast = this.node.getAST();

		Task self = this;
		Task ttarget = target;

		Stack<Task> tasks_target = new Stack<Task>();
		Stack<Task> tasks_self = new Stack<Task>();

		while (target != null) {
			tasks_target.push(target);
			target = target.parent;
		}

		target = ttarget;

		while (self != null) {
			tasks_self.push(self);
			self = self.parent;
		}

		assert (!tasks_target.empty() && !tasks_self.empty());

		while (!tasks_target.empty() && !tasks_self.empty()
				&& tasks_target.peek().equals(tasks_self.peek())) {
			tasks_target.pop();
			tasks_self.pop();
		}

		Expression path = ast.newThisExpression();

		self = this;
		if (!tasks_self.empty()) {
			while (true) {
				path = self.pathToParent(path);

				if (self == tasks_self.peek())
					break;

				self = self.parent;
			}
		}

		while (!tasks_target.empty()) {
			Task descendent = tasks_target.pop();

			if (descendent.isChildOf(descendent.parent)
					&& !this.isDescendentOf(descendent.parent))
				break;

			FieldAccess field = ast.newFieldAccess();
			field.setExpression(path);
			field.setName(ast.newSimpleName("ae_" + descendent.getFieldName()));
			path = field;
		}

		return path;
	}

	public Expression pathToParent(Expression currentPath) {
		AST ast = this.node.getAST();

		FieldAccess field = ast.newFieldAccess();
		field.setExpression(currentPath);
		field.setName(ast.newSimpleName("ae_parent"));

		return field;
	}

	public Expression getPathToRoot() {
		AST ast = this.node.getAST();

		if (this.parent == null)
			return ast.newThisExpression();

		return this.pathToParent(this.parent.getPathToRoot());
	}

	public boolean isDescendentOf(Task other) {
		for (Task parent = this.parent; parent != null; parent = parent.parent)
			if (parent == other)
				return true;

		return false;
	}

	public boolean isChildOf(Task other) {
		if (this.parent == null)
			return false;

		Set<EASTExecutableNode> children = this.parent.node.getChildren();
		for (EASTExecutableNode child : children)
			if (child.getTask() == this)
				return true;

		return false;
	}

	public ArrayList<Task> getDeps() {

		ArrayList<Task> deps = new ArrayList<Task>();

		for (EASTExecutableNode node : this.node.getStrongDependencies()) {
			Task dep = node.getTask();

			if (!deps.contains(dep))
				deps.add(dep);
		}

		for (EASTExecutableNode node : this.node.getWeakDependencies()) {
			Task dep = node.getTask();

			if (!deps.contains(dep) && this != dep && !this.isDescendentOf(dep))
				deps.add(dep);
		}

		if (deps.size() > 0)
			return deps;
		else
			return null;
	}

	@SuppressWarnings("unchecked")
	protected void fillExecute() {
		AST ast = this.node.getAST();

		this.execute.setName(ast.newSimpleName("execute"));
		this.execute.modifiers().add(
				ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		SingleVariableDeclaration runtime = ast.newSingleVariableDeclaration();
		runtime.setType(ast.newSimpleType(ast
				.newName("aeminium.runtime.Runtime")));
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
	protected void fillIterate() {
		AST ast = this.node.getAST();

		this.iterate.setName(ast.newSimpleName("iterate"));
		this.iterate.modifiers().add(
				ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));

		SingleVariableDeclaration integer = ast.newSingleVariableDeclaration();
		integer.setType(ast.newSimpleType(ast.newName("Long")));
		integer.setName(ast.newSimpleName("o"));
		this.iterate.parameters().add(integer);

		this.iterate.setBody(ast.newBlock());
	}

	@SuppressWarnings("unchecked")
	public ClassInstanceCreation create() {
		AST ast = this.node.getAST();

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.getTypeName())));

		if (this.parent != null)
			creation.arguments().add(ast.newThisExpression());

		return creation;
	}

	public CompilationUnit translate() {
		if (this.translated)
			return this.cu;

		this.fillConstructor(this.constructors.get(0), this.node.getAST()
				.newBlock(), false);
		this.fillExecute();
		this.fillIterate();

		this.translated = true;

		return this.cu;
	}

	public MethodDeclaration getExecute() {
		return this.execute;
	}

	public MethodDeclaration getIterate() {
		return this.iterate;
	}

	public ArrayList<MethodDeclaration> getConstructors() {
		return this.constructors;
	}

	public EASTExecutableNode getNode() {
		return this.node;
	}

	public boolean getDoallFor() {
		return this.doallFor;
	}

	public void doallFor() {
		this.doallFor = true;
	}

	//Cria o declaracao especifica para uma tarefa generica
	@SuppressWarnings("unchecked")
	public void normalDecl() {

		AST ast = node.getAST();

		this.decl.superInterfaceTypes().add(
				ast.newSimpleType(ast.newName("aeminium.runtime.Body")));
	}

	//Cria o declaracao especifica para uma tarefa do tipo Doall
	@SuppressWarnings("unchecked")
	public void doallDecl() {

		AST ast = node.getAST();
		//
		// this.decl.superInterfaceTypes().add(
		// ast.newSimpleType(ast.newName("aeminium.runtime.helpers.loops.ForBody")));

		ParameterizedType paramType = ast.newParameterizedType(ast
				.newSimpleType(ast
						.newName("aeminium.runtime.helpers.loops.ForBody")));
		SimpleType simpleType = ast.newSimpleType(ast.newSimpleName("Long"));
		paramType.typeArguments().add(simpleType);

		this.decl.superInterfaceTypes().clear();
		this.decl.superInterfaceTypes().add(paramType);
	}

	//Cria o cabecalho especifico para uma tarefa generica
	@SuppressWarnings("unchecked")
	private void createTaskAssign() {

		AST ast = this.node.getAST();

		task_asgn = ast.newAssignment();

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

	}

	//Cria o cabecalho especifico para uma tarefa do tipo Doall
	@SuppressWarnings("unchecked")
	public void createDoallTaskAssign(String start, InfixExpression inFix,
			String increment) {

		AST ast = this.node.getAST();

		task_asgn = ast.newAssignment();

		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName("ae_task"));

		task_asgn.setLeftHandSide(task_access);

		MethodInvocation task_create = ast.newMethodInvocation();
		task_create.setExpression(ast.newSimpleName("ForTask"));
		task_create.setName(ast.newSimpleName("createFor"));

		task_create.arguments().add(ast.newName("AeminiumHelper.rt"));

		ClassInstanceCreation range = ast.newClassInstanceCreation();
		range.setType(ast.newSimpleType(ast.newSimpleName("Range")));
		range.arguments().add(ast.newNumberLiteral(start));

		if (inFix.getRightOperand().toString().contains("this")) {

			range.arguments().add(
					ASTNode.copySubtree(ast, inFix.getRightOperand()));

		} else
			range.arguments().add(
					ast.newNumberLiteral(inFix.getRightOperand().toString()));

		range.arguments().add(ast.newNumberLiteral(increment));

		task_create.arguments().add(range);
		task_create.arguments().add(ast.newThisExpression());

		task_asgn.setRightHandSide(task_create);

	}

	@SuppressWarnings("unchecked")
	public void addTaskAssign(MethodDeclaration constructor, Block body) {

		AST ast = this.node.getAST();

		body.statements().add(ast.newExpressionStatement(task_asgn));
	}

	//altero o body do construtor da tarefa para as espeficicacoes de uma tarefa doall
	@SuppressWarnings("unchecked")
	public void addDoallTaskAssign(MethodDeclaration constructor, Block body) {

		AST ast = this.node.getAST();

		if (constructor.getBody().statements().size() == 3) {

			ExpressionStatement expr = (ExpressionStatement) constructor
					.getBody().statements()
					.get(constructor.getBody().statements().size() - 2);

			if (expr.toString().contains("this.ae_task=AeminiumHelper")) {
				constructor.getBody().statements()
						.remove(constructor.getBody().statements().size() - 2);

				constructor.getBody().statements()
						.add(ast.newExpressionStatement(task_asgn));

			} else {

				constructor.getBody().statements().remove(expr);
				constructor.getBody().statements().add(expr);

			}
		}
	}
}
