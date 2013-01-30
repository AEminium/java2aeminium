package aeminium.compiler.east;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.task.ForSubTask;
import aeminium.compiler.task.Task;

public class EForStatement extends EStatement {

	protected final EStatement body;
	protected final EStatement body2;
	protected final EExpression expr;

	private ArrayList<EExpression> initializers = new ArrayList<EExpression>();
	private ArrayList<EExpression> updaters = new ArrayList<EExpression>();

	protected final EForStatement loop;
	protected final int depth;

	public EForStatement(EAST east, ForStatement original, EASTDataNode scope,
			EMethodDeclaration method, EASTExecutableNode parent,
			EForStatement base) {
		super(east, original, scope, method, parent, base);

		this.expr = EExpression.create(east, original.getExpression(), scope,
				this, base == null ? null : base.expr);

		this.initializers = new ArrayList<EExpression>();

		for (int i = 0; i < original.initializers().size(); i++) {
			this.initializers.add(EExpression.create(this.east,
					(Expression) original.initializers().get(i), scope, this,
					base == null ? null : base.initializers.get(i)));
		}

		this.updaters = new ArrayList<EExpression>();

		for (int i = 0; i < original.updaters().size(); i++) {
			this.updaters.add(EExpression.create(this.east,
					(Expression) original.updaters().get(i), scope, this,
					base == null ? null : base.updaters.get(i)));
		}

		this.body = EStatement.create(east, original.getBody(), scope, method,
				this, base == null ? null : base.body);

		this.body2 = EStatement.create(east,
				parseBlock(updaters.get(0).getOriginal() + ";", null), scope,
				method, this, base == null ? null : base.body2);

		this.depth = this.getLoopDepth();

		if (base == null
				&& this.depth < EASTExecutableNode.NESTED_LOOP_DEPTH_AGGREGATION)
			this.loop = EForStatement.create(east, original, scope, method,
					parent, this);
		else
			this.loop = null;

	}

	/* factory */
	public static EForStatement create(EAST east, ForStatement stmt,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EForStatement base) {

		return new EForStatement(east, stmt, scope, method, parent, base);
	}

	@Override
	public void checkSignatures() {

		this.expr.checkSignatures();
		this.body.checkSignatures();

		for (EExpression stmt : this.initializers)
			stmt.checkSignatures();
		for (EExpression stmt : this.updaters)
			stmt.checkSignatures();

		if (this.loop != null)
			this.loop.checkSignatures();

		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));

	}

	@Override
	public Signature getFullSignature() {

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
	public void checkDependencies(DependencyStack stack) {

		this.expr.checkDependencies(stack);
		this.addStrongDependency(this.expr);

		for (EExpression stmt : this.initializers) {
			stmt.checkDependencies(stack);
			this.addStrongDependency(stmt);
		}

		for (EExpression stmt : this.updaters) {
			stmt.checkDependencies(stack);
			this.addStrongDependency(stmt);
		}

		Set<EASTExecutableNode> deps = stack.getDependencies(this,
				this.signature);

		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);

		DependencyStack copy = stack.fork();
		this.body.checkDependencies(copy);

		if (this.loop != null) {
			DependencyStack copy2 = copy.fork();
			this.loop.checkDependencies(copy2);
			copy.join(copy2, this);
		}

		stack.join(copy, this);

		this.addChildren(this.body);

		// TODO: this is only valid for the sequential translation used bellow
		if (this.loop != null)
			this.addChildren(this.loop);
		/*
		 * else this.addChildren(this); /* FIXME: this will probably break
		 * somewhere
		 */

	}

	@Override
	public int optimize() {
		int sum = 0;

		sum += this.expr.optimize();
		sum += this.body.optimize();

		for (EExpression frag : this.updaters)
			sum += frag.optimize();

		for (EExpression frag : this.initializers)
			sum += frag.optimize();

		if (this.depth >= EASTExecutableNode.NESTED_LOOP_DEPTH_AGGREGATION) {
			sum += this.expr.sequentialize();
			sum += this.body.sequentialize();
		} else if (this.loop == null) {
			if (this.expr.base.inlineTask)
				this.expr.inline(this);

			if (this.body.base.inlineTask)
				this.body.inline(this);
		} else
			sum += this.loop.optimize();

		sum += super.optimize();

		return sum;
	}

	@Override
	public void preTranslate(Task parent) {

		if (this.inlineTask)
			this.task = parent;
		else {
			if (this.loop != null
					|| (this.depth >= EASTExecutableNode.NESTED_LOOP_DEPTH_AGGREGATION)) {
				this.task = parent.newSubTask(this, "for",
						this.base == null ? null : this.base.task);

			} else
				this.task = ForSubTask.create(this,
						this.base.task.getTypeName() + "loop", parent,
						this.base.task);

		}

		this.expr.preTranslate(this.task);
		this.body.preTranslate(this.task);
		// this.body2.preTranslate(this.task);

		for (int i = 0; i < this.updaters.size(); i++)
			this.updaters.get(i).preTranslate(this.task);

		for (int i = 0; i < this.initializers.size(); i++)
			this.initializers.get(i).preTranslate(this.task);

		if (this.loop != null)
			this.loop.preTranslate(this.task);

	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Statement> build(List<CompilationUnit> out) {
		AST ast = this.getAST();

		if (this.depth < EASTExecutableNode.NESTED_LOOP_DEPTH_AGGREGATION) {

			IfStatement stmt = ast.newIfStatement();
			stmt.setExpression(this.expr.translate(out));

			Block block = ast.newBlock();

			block.statements().addAll(this.body.translate(out));

			if (this.loop != null)
				block.statements().addAll(this.loop.translate(out));
			else {
				/*
				 * the same thing as a normal translate here. because doing so
				 * would create an infinite loop
				 */
				FieldAccess task_access = ast.newFieldAccess();
				task_access.setExpression(ast.newThisExpression());
				task_access.setName(ast.newSimpleName("ae_"
						+ this.task.getFieldName()));

				Assignment assign = ast.newAssignment();
				assign.setLeftHandSide(task_access);
				assign.setRightHandSide(this.task.create());

				block.statements().add(ast.newExpressionStatement(assign));
			}

			stmt.setThenStatement(block);

			return Arrays.asList((Statement) stmt);
		} else {
			ForStatement stmt = ast.newForStatement();

			Block body = ast.newBlock();
			body.statements().addAll(this.body.translate(out));

			stmt.setExpression(this.expr.translate(out));
			stmt.setBody(body);

			return Arrays.asList((Statement) stmt);
		}
	}

	@Override
	public ForStatement getOriginal() {

		return (ForStatement) this.original;
	}

	@Override
	protected int getLoopDepth() {
		return super.getLoopDepth() + 1;
	}

	public EASTExecutableNode getBody() {
		return this.body;
	}

	// By Alcides Fonseca
	public Block parseBlock(String expressionString, AST ast) {
		final String wholeProgramString = "class X { public void m() { "
				+ expressionString + " } }";
		final ASTParser astParser = ASTParser.newParser(AST.JLS3);
		astParser.setSource(wholeProgramString.toCharArray());
		final CompilationUnit compiledCode = (CompilationUnit) astParser
				.createAST(null);
		final TypeDeclaration typeDeclaration = (TypeDeclaration) compiledCode
				.types().get(0);
		final MethodDeclaration methodDeclaration = (MethodDeclaration) typeDeclaration
				.bodyDeclarations().get(0);
		final Block expression = methodDeclaration.getBody();
		morphExpression(expression, ast);
		return expression;
	}

	public Expression parseExpression(String expressionString, AST ast) {
		final String wholeProgramString = "class X {int a = "
				+ expressionString + ";}";
		final ASTParser astParser = ASTParser.newParser(AST.JLS3);
		astParser.setSource(wholeProgramString.toCharArray());
		final CompilationUnit compiledCode = (CompilationUnit) astParser
				.createAST(null);
		final TypeDeclaration typeDeclaration = (TypeDeclaration) compiledCode
				.types().get(0);
		final FieldDeclaration fieldDeclaration = (FieldDeclaration) typeDeclaration
				.bodyDeclarations().get(0);
		final VariableDeclarationFragment fragment = (VariableDeclarationFragment) fieldDeclaration
				.fragments().get(0);
		final Expression expression = fragment.getInitializer();
		morphExpression(expression, ast);
		return expression;
	}

	private void morphExpression(ASTNode exp, AST ast) {
		setAst(exp, ast);
		clearParent(exp);
	}

	private void clearParent(ASTNode exp) {
		try {
			final Field field = ASTNode.class.getDeclaredField("parent");
			field.setAccessible(true);
			field.set(exp, null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void setAst(ASTNode exp, AST ast) {
		// AST should never be null
		if (ast == null) {
			return;
		}
		try {
			final Field field = ASTNode.class.getDeclaredField("ast");
			field.setAccessible(true);
			field.set(exp, ast);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

}
