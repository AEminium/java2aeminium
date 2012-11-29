package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class EThrowStatement extends EStatement {

	protected final EExpression expr;

	public EThrowStatement(EAST east, ThrowStatement original,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EThrowStatement base) {
		super(east, original, scope, method, parent, base);

		this.expr = EExpression.create(this.east, original.getExpression(),
				scope, this, base == null ? null : base.expr);

		System.out.println(original.getExpression().toString());
	}

	/* factory */
	public static EThrowStatement create(EAST east, ThrowStatement stmt,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EThrowStatement base) {
		return new EThrowStatement(east, stmt, scope, method, parent, base);
	}

	@Override
	public List<Statement> build(List<CompilationUnit> out) {

		AST ast = this.getAST();

		Expression expr = this.expr.translate(out);

		ThrowStatement exprstmt = ast.newThrowStatement();
		exprstmt.setExpression(expr);
		
		return Arrays.asList((Statement) exprstmt);

	}

	@Override
	public void checkSignatures() {

		this.expr.checkSignatures();

	}

	@Override
	public Signature getFullSignature() {

		Signature sig = new Signature();

		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack) {

		this.expr.checkDependencies(stack);
		this.addStrongDependency(this.expr);

		Set<EASTExecutableNode> deps = stack.getDependencies(this,
				this.signature);
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);

	}

	@Override
	public int optimize() {
		int sum = 0;

		sum += this.expr.optimize();
		sum += super.optimize();

		return sum;
	}

	@Override
	public void preTranslate(Task parent) {

		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "throwstmt",
					this.base == null ? null : this.base.task);

		this.expr.preTranslate(this.task);

	}

	@Override
	public ThrowStatement getOriginal() {

		return (ThrowStatement) this.original;
	}

}
