package aeminium.compiler.east;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.Statement;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.task.Task;

public class EConstructorInvocation extends EStatement {

	public EConstructorInvocation(EAST east, ConstructorInvocation original,
			EASTDataNode scope, EMethodDeclaration method,
			EASTExecutableNode parent, EConstructorInvocation base) {
		super(east, original, scope, method, parent, base);

	}

	/* factory */
	public static EConstructorInvocation create(EAST east,
			ConstructorInvocation stmt, EASTDataNode scope,
			EMethodDeclaration method, EASTExecutableNode parent,
			EConstructorInvocation base) {
		return new EConstructorInvocation(east, stmt, scope, method, parent,
				base);
	}

	@Override
	public List<Statement> build(List<CompilationUnit> out) {

		AST ast = this.getAST();

		ConstructorInvocation constinvstmt = ast.newConstructorInvocation();

		return Arrays.asList((Statement) constinvstmt);
	}

	@Override
	public void checkSignatures() {
		// TODO Auto-generated method stub

	}

	@Override
	public Signature getFullSignature() {

		Signature sig = new Signature();

		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack) {

		Set<EASTExecutableNode> deps = stack.getDependencies(this,
				this.signature);
		for (EASTExecutableNode node : deps)
			this.addWeakDependency(node);

	}

	@Override
	public int optimize() {
		int sum = 0;

		sum += super.optimize();

		return sum;
	}

	@Override
	public void preTranslate(Task parent) {

	}

	@Override
	public ConstructorInvocation getOriginal() {

		return (ConstructorInvocation) this.original;
	}
}
