package aeminium.compiler.east;

import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.Task;

public class ECastExpression extends EExpression {

	protected final DataGroup datagroup;

	protected final EExpression expr;

	public ECastExpression(EAST east, CastExpression original,
			EASTDataNode scope, EASTExecutableNode parent, ECastExpression base) {
		super(east, original, scope, parent, base);

		this.datagroup = scope.getDataGroup().append(
				new SimpleDataGroup("cast"));

		this.expr = EExpression.create(east, original.getExpression(), scope,
				this, base == null ? null : base.expr);

	}

	/* factory */
	public static ECastExpression create(EAST east, CastExpression original,
			EASTDataNode scope, EASTExecutableNode parent, ECastExpression base) {
		return new ECastExpression(east, original, scope, parent, base);
	}

	@Override
	public DataGroup getDataGroup() {

		return this.datagroup;
	}

	@Override
	public Expression build(List<CompilationUnit> out) {

		AST ast = this.getAST();

		CastExpression cast = ast.newCastExpression();
		cast.setExpression(this.expr.translate(out));
		cast.setType(this.getType());

		return cast;

	}

	@Override
	public void checkSignatures() {

		this.expr.checkSignatures();

		this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));

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
	public void preTranslate(Task parent) {

		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "cast",
					this.base == null ? null : this.base.task);

		this.expr.preTranslate(this.task);

	}

	@Override
	public CastExpression getOriginal() {

		return (CastExpression) this.original;
	}

}
