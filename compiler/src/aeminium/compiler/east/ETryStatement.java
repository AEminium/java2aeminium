package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class ETryStatement extends EStatement {

	protected final EStatement body;
	protected final EStatement finaly;

	public ETryStatement(EAST east, TryStatement original, EASTDataNode scope,
			EMethodDeclaration method, EASTExecutableNode parent,
			ETryStatement base) {
		super(east, original, scope, method, parent, base);

		this.body = EStatement.create(east, original.getBody(), scope, method,
				this, base == null ? null : base.body);

		this.finaly = EStatement.create(east, original.getFinally(), scope,
				method, this, base == null ? null : base.finaly);

	}

	/* factory */
	public static ETryStatement create(EAST east, TryStatement stmt,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, ETryStatement base) {
		return new ETryStatement(east, stmt, scope, method, parent, base);
	}

	@Override
	public List<Statement> build(List<CompilationUnit> out) {

		AST ast = this.getAST();

		TryStatement trystmt = ast.newTryStatement();

		Block body = ast.newBlock();
		Block finaly = ast.newBlock();

		body.statements().addAll(this.body.translate(out));
		trystmt.setBody(body);

		finaly.statements().addAll(this.finaly.translate(out));
		trystmt.setFinally(finaly);

		return Arrays.asList((Statement) trystmt);
	}

	@Override
	public void checkSignatures() {

		this.body.checkSignatures();

	}

	@Override
	public Signature getFullSignature() {

		Signature sig = new Signature();

		sig.addAll(this.signature);
		sig.addAll(this.body.getFullSignature());
		sig.addAll(this.finaly.getFullSignature());

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack) {
		Set<EASTExecutableNode> deps = stack.getDependencies(this,
				this.signature);

		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);

		DependencyStack copy = stack.fork();
		this.body.checkDependencies(copy);

		stack.join(copy, this);

		DependencyStack copy2 = stack.fork();
		this.finaly.checkDependencies(copy2);

		stack.join(copy2, this);

		this.addChildren(this.body);
		this.addChildren(this.finaly);

	}

	@Override
	public int optimize() {
		int sum = 0;

		sum += super.optimize();
		sum += this.body.optimize();
		sum += this.finaly.optimize();

		sum += this.body.sequentialize();
		sum += this.finaly.sequentialize();

		return sum;
	}

	@Override
	public void preTranslate(Task parent) {

		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "trystmt",
					this.base == null ? null : this.base.task);

		this.body.preTranslate(this.task);
		this.finaly.preTranslate(this.task);

	}

	@Override
	public TryStatement getOriginal() {

		return (TryStatement) this.original;
	}

}
