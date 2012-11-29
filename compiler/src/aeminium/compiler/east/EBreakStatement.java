package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class EBreakStatement extends EStatement {

	public EBreakStatement(EAST east, BreakStatement original,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EBreakStatement base) {
		super(east, original, scope, method, parent, base);
		
		System.out.println(original.getLabel());

	}

	/* factory */
	public static EBreakStatement create(EAST east, BreakStatement stmt,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EBreakStatement base) {
		return new EBreakStatement(east, stmt, scope, method, parent, base);
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
			this.task = parent.newSubTask(this, "breakstmt",
					this.base == null ? null : this.base.task);

	}

	@Override
	public BreakStatement getOriginal() {

		return (BreakStatement) this.original;
	}

}
