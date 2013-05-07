package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.task.Task;

public class EForStatement extends EStatement {

	protected final EStatement body;
	protected final EExpression expr;

	private ArrayList<EExpression> initializers = new ArrayList<EExpression>();
	private ArrayList<EExpression> updaters = new ArrayList<EExpression>();
	protected boolean doall;

	public EForStatement(EAST east, ForStatement original, EASTDataNode scope,
			EMethodDeclaration method, EASTExecutableNode parent,
			EForStatement base) {
		super(east, original, scope, method, parent, base);

		this.expr = EExpression.create(east, original.getExpression(), scope,
				this, base == null ? null : base.expr);

		this.initializers = new ArrayList<EExpression>();

		for (int i = 0; i < original.initializers().size(); i++) {

			System.out.println(original.initializers().get(i).getClass()
					.getName());
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

		stack.join(copy, this);

		this.addChildren(this.body);

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

		sum += this.expr.sequentialize();
		sum += this.body.sequentialize();

		sum += super.optimize();

		return sum;
	}

	@Override
	public void preTranslate(Task parent) {

		if (this.inlineTask)
			this.task = parent;
		else {
			this.task = parent.newSubTask(this, "for", this.base == null ? null
					: this.base.task);

		}

		this.expr.preTranslate(this.task);
		this.body.preTranslate(this.task);

		for (int i = 0; i < this.updaters.size(); i++)
			this.updaters.get(i).preTranslate(this.task);

		for (int i = 0; i < this.initializers.size(); i++)
			this.initializers.get(i).preTranslate(this.task);

		// checkDoAll();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<Statement> build(List<CompilationUnit> out) {

		AST ast = this.getAST();

		if (checkDoAll()) {

			this.task.doallDecl();
			InfixExpression einFix = (InfixExpression) expr.translate(out);

			System.out.println("rrrrr = " + initializers.get(0).getOriginal());
			EAssignment assign = (EAssignment) initializers.get(0);
			ENumberLiteral start = (ENumberLiteral) assign.right;

			System.out.println(updaters.get(0).getClass().getName());

			EPostfixExpression ePostfix = (EPostfixExpression) updaters.get(0);

			if (ePostfix.operator.toString().equals("++"))
				this.task.createDoallTaskAssign(start.original.toString(),
						einFix, "1");
			else
				this.task.createDoallTaskAssign(start.original.toString(),
						einFix, "-1");

			this.task.addDoallTaskAssign(this.task.getConstructors().get(0),
					this.task.getNode().getAST().newBlock());

			return this.body.translate(out);

		} else {

			ForStatement stmt = ast.newForStatement();

			Block body = ast.newBlock();
			body.statements().addAll(this.body.translate(out));

			for (int i = 0; i < initializers.size(); i++)
				stmt.initializers().add(initializers.get(i).translate(out));

			for (int i = 0; i < updaters.size(); i++)
				stmt.updaters().add(updaters.get(i).translate(out));

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

	// verifica se e ciclo doall
	private boolean checkDoAll() {

		EBlock bodyBlock = (EBlock) body;

		body.getStrongDependencies();

		if (checkForBodyDeps(bodyBlock) != true)
			if (checkDoallFor(bodyBlock.getOriginal())) {
				this.doall = true;
				this.task.doallFor();
				return true;
			}
		return false;

	}

	
	// verifica se existem dependencias entre os statements do ciclo, dependencias directas
	private boolean checkForBodyDeps(EBlock body) {

		boolean check = false;
		for (EStatement stmt : body.stmts) {

			ArrayList<Task> deps = stmt.task.getDeps();
			deps.remove(stmt.getTask());

			for (Task task : deps) {
				if (task.getFieldName().contains(this.getTask().getFieldName())) {
					// existe uma dependencia entre os statements do ciclo
					check = true;
					break;
				}
			}

			if (check)
				break;
		}
		return check;
	}

	//verifica se nao existem loop-carried dependencies entre os statements do ciclo
	//detecta grande parte dos casos onde se pode aplicar o doall, nao sendo garantido que detecta todas as possibilidades de aplicacao
	//TODO acrescentar verificacoes que possam permiter englobar mais casos ainda nao verificados
	private boolean checkDoallFor(Block body) {

		//percorre todos os statemens do ciclo
		for (Object tmp : body.statements()) {

			Statement obj = (Statement) tmp;

			if (obj instanceof ExpressionStatement) {

				ExpressionStatement exprStmt = (ExpressionStatement) obj;

				if (checkExpressionStmt(exprStmt) != true)
					return false;

			} else if (obj instanceof EmptyStatement) {
				// nao necessita de nenhuma verificação
			} else if (obj instanceof IfStatement) {

				System.err.println("Statement Not Implemented yet: "
						+ obj.getClass().getName());
				return false;
			}

			else {
				System.err.println("Statement Not Implemented yet: "
						+ obj.getClass().getName());
				return false;
			}
		}

		return true;

	}

	private boolean checkExpressionStmt(ExpressionStatement exprStmt) {

		if (exprStmt.getExpression() instanceof MethodInvocation)
			return false;

		if (exprStmt.getExpression() instanceof Assignment) {

			Assignment asgnExpr = (Assignment) exprStmt.getExpression();

			if (checkAssignExpression(asgnExpr) != true)
				return false;
		}

		return true;
	}

	private boolean checkAssignExpression(Assignment asgnExpr) {

		if (asgnExpr.getLeftHandSide() instanceof Name
				|| asgnExpr.getRightHandSide() instanceof MethodInvocation)
			return false;

		if (asgnExpr.getLeftHandSide() instanceof ArrayAccess) {

			ArrayAccess arrayAccess = (ArrayAccess) asgnExpr.getLeftHandSide();

			if (checkArrayAccessExpr(arrayAccess, asgnExpr) != true)
				return false;
		}

		if (asgnExpr.getRightHandSide() instanceof InfixExpression) {

			InfixExpression binary = (InfixExpression) asgnExpr
					.getRightHandSide();

			if (binary.getRightOperand() instanceof MethodInvocation
					|| binary.getLeftOperand() instanceof MethodInvocation)
				return false;
		}

		return true;
	}

	private boolean checkArrayAccessExpr(ArrayAccess arrayAccess,
			Assignment asgnExpr) {

		if (arrayAccess.getIndex() instanceof NumberLiteral)
			return false;

		if (asgnExpr.getRightHandSide() instanceof InfixExpression) {
			InfixExpression binary = (InfixExpression) asgnExpr
					.getRightHandSide();

			if (binary.getRightOperand() instanceof NumberLiteral
					&& binary.getLeftOperand() instanceof NumberLiteral) {
				return true;
			}

			if (binary.getRightOperand() instanceof Name
					&& binary.getLeftOperand() instanceof Name) {
				return true;
			}

			if (binary.getRightOperand() instanceof NumberLiteral
					&& binary.getLeftOperand() instanceof Name) {
				return true;
			}

			if (binary.getRightOperand() instanceof Name
					&& binary.getLeftOperand() instanceof NumberLiteral) {
				return true;
			}
		}

		if (arrayAccess.getIndex() instanceof InfixExpression) {

			if (asgnExpr.getRightHandSide() instanceof NumberLiteral)
				return true;

			if (asgnExpr.getRightHandSide() instanceof InfixExpression) {

				InfixExpression binary = (InfixExpression) asgnExpr
						.getRightHandSide();

				if (binary.getRightOperand() instanceof NumberLiteral
						&& binary.getLeftOperand() instanceof NumberLiteral) {
					return true;
				}
			}
			return false;
		}

		return true;
	}
}
