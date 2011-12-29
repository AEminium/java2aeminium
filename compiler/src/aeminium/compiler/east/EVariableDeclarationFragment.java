package aeminium.compiler.east;

import java.util.List;
import java.util.ArrayList;

import org.eclipse.jdt.core.dom.*;

import aeminium.compiler.datagroup.LocalDataGroup;
import aeminium.compiler.datagroup.SignatureItemMerge;
import aeminium.compiler.datagroup.SignatureItemRead;
import aeminium.compiler.datagroup.SignatureItemWrite;

public class EVariableDeclarationFragment extends EASTDependentNode
{
	private final VariableDeclarationFragment origin;
	private final EExpression expr;
	private final ESimpleName name;

	IBinding binding;

	Type type;

	EVariableDeclarationFragment(EAST east, VariableDeclarationFragment origin)
	{
		super(east);
		this.origin = origin;
	
		this.name = this.east.extend(origin.getName());
		this.name.setDataGroup(new LocalDataGroup(this));
		
		if (origin.getInitializer() != null)
			this.expr = this.east.extend(origin.getInitializer());
		else
			this.expr = null;
	}

	@Override
	public void analyse()
	{
		super.analyse();
	
		this.binding = this.origin.resolveBinding();
		this.east.putNode(this.east.resolveName(this.binding), this.name);

		this.name.analyse();

		if (this.expr != null)
		{
			this.expr.analyse();

			this.signature.addFrom(this.expr.getSignature());
	
			this.signature.add(new SignatureItemRead(this.expr.getDataGroup()));
			this.signature.add(new SignatureItemWrite(this.name.getDataGroup()));
			this.signature.add(new SignatureItemMerge(this.name.getDataGroup(), this.expr.getDataGroup()));
		}
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
	
		sum += this.name.optimize();

		if (this.expr != null)
			sum += this.expr.optimize();

		return sum;
	}
	
	public void preTranslate(EVariableDeclarationStatement parent)
	{
		if (this.isRoot())
			this.task = parent.task.newChild("declfrag");
		else
			this.task = parent.task;
		
		this.type = parent.getType();
		
		this.name.preTranslate(this.task);
		
		if (this.expr != null)
			this.expr.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	public List<Statement> translate(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		assert(this.isRoot());

		if (!this.isRoot())
			return this.build(cus);

		cus.add(this.task.getCompilationUnit());

		Block execute = ast.newBlock();
		execute.statements().addAll(this.build(cus));
		task.setExecute(execute);

		MethodDeclaration constructor = task.createConstructor();
		task.addConstructor(constructor);

/*
		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName(this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());
*/
		return new ArrayList<Statement>();
	}

	public List<Statement> build(List<CompilationUnit> cus)
	{
		AST ast = this.east.getAST();

		task.addField(this.type, this.origin.getName().toString(), true);

		List<Statement> stmts = new ArrayList<Statement>();

		if (this.expr != null)
		{
			Assignment assign = ast.newAssignment();

			assign.setRightHandSide(this.expr.translate(cus));
			assign.setLeftHandSide(this.name.translate(cus));

			stmts.add(ast.newExpressionStatement(assign));
		}

		return stmts;
	}
}
