package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class EContinueStatement extends EStatement {

	public EContinueStatement(EAST east, ContinueStatement original,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EContinueStatement base) {
		super(east, original, scope, method, parent, base);

		System.out.println(original.getLabel());
	}

	/* factory */
	public static EContinueStatement create(EAST east, ContinueStatement stmt,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EContinueStatement base) {
		return new EContinueStatement(east, stmt, scope, method, parent, base);
	}

	@Override
	public List<Statement> build(List<CompilationUnit> out) {

		return Arrays.asList((Statement) this.original);

	}

	@Override
	public void checkSignatures() {
		// TODO Auto-generated method stub

	}

	@Override
	public Signature getFullSignature() {
		return new Signature();
	}

	@Override
	public void checkDependencies(DependencyStack stack) {
		// Nothing

	}

	@Override
	public void preTranslate(Task parent) {

		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "continuestmt",
					this.base == null ? null : this.base.task);

	}

	@Override
	public ContinueStatement getOriginal() {
		// TODO Auto-generated method stub
		return (ContinueStatement) this.original;
	}

}
