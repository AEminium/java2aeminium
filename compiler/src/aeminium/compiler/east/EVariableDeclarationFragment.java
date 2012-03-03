package aeminium.compiler.east;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItemMerge;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SignatureItemWrite;
import aeminium.compiler.task.Task;

public class EVariableDeclarationFragment extends EASTExecutableNode implements EASTDeclaringNode
{
	protected final EASTDataNode scope;
	protected final Type dataType;
	protected final DataGroup datagroup;
	
	protected final ESimpleNameDeclaration name;
	protected final EExpression expr;
	
	public EVariableDeclarationFragment(EAST east, VariableDeclarationFragment original, EASTDataNode scope, Type dataType)
	{
		super(east, original);

		this.scope = scope;
		this.datagroup = scope.getDataGroup();

		AST ast = this.getAST();
		if (original.getExtraDimensions() == 0)
			this.dataType = dataType;
		else
			this.dataType = ast.newArrayType((Type) ASTNode.copySubtree(ast, dataType), original.getExtraDimensions());
			
		this.name = ESimpleNameDeclaration.create(this.east, original.getName(), this);
		
		if (original.getInitializer() != null)
			this.expr = EExpression.create(this.east, original.getInitializer(), this.scope);
		else
			this.expr = null;
	}

	@Override
	public VariableDeclarationFragment getOriginal()
	{
		return (VariableDeclarationFragment) this.original;
	}

	public static EVariableDeclarationFragment create(EAST east, VariableDeclarationFragment frag, EASTDataNode scope, Type dataType)
	{
		return new EVariableDeclarationFragment(east, frag, scope, dataType);
	}

	@Override
	public DataGroup getDataGroup()
	{
		return this.datagroup;
	}
	
	@Override
	public ETypeDeclaration getTypeDeclaration()
	{
		return this.scope.getTypeDeclaration();
	}
	
	
	@Override
	public void checkSignatures()
	{
		if (this.expr != null)
		{
			this.expr.checkSignatures();
			
			this.signature.addItem(new SignatureItemRead(this.expr.getDataGroup()));
			this.signature.addItem(new SignatureItemWrite(this.name.getDataGroup()));
			this.signature.addItem(new SignatureItemMerge(this.name.getDataGroup(), this.expr.getDataGroup()));
		}
	}
	
	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.expr.getFullSignature());
		
		return sig;
	}
	
	@Override
	public void checkDependencies(DependencyStack stack)
	{
		if (this.expr != null)
		{
			this.expr.checkDependencies(stack);
			this.strongDependencies.add(this.expr);
		}

		Set<EASTExecutableNode> deps = stack.getDependencies(this, this.signature);
		
		for (EASTExecutableNode node : deps)
			if (!node.equals(this.expr))
				this.weakDependencies.add(node);
	}
	
	@Override
	public int optimize()
	{
		int sum = 0;

		if (this.expr != null)
			sum += this.expr.optimize();

		sum += super.optimize();
		
		return sum;
	}
	
	@Override
	public void preTranslate(Task parent)
	{
		if (this.inlineTask)
			this.task = parent;
		else
			this.task = parent.newSubTask(this, "varfrag");
		
		if (this.expr != null)
			this.expr.preTranslate(this.task);
	}

	@SuppressWarnings("unchecked")
	public List<Statement> translate(List<CompilationUnit> out)
	{
		if (this.inlineTask)
			return this.build(out);
		
		this.task.translate();
		this.task.getExecute().getBody().statements().addAll(this.build(out));
		
		AST ast = this.getAST();
		
		FieldAccess task_access = ast.newFieldAccess();
		task_access.setExpression(ast.newThisExpression());
		task_access.setName(ast.newSimpleName("ae_" + this.task.getName()));

		Assignment assign = ast.newAssignment();
		assign.setLeftHandSide(task_access);
		assign.setRightHandSide(this.task.create());

		return Arrays.asList((Statement) ast.newExpressionStatement(assign));
	}

	private List<Statement> build(List<CompilationUnit> out)
	{
		AST ast = this.getAST();

		this.task.addField(this.dataType, this.getOriginal().getName().toString(), true);

		List<Statement> stmts = new ArrayList<Statement>();

		if (this.expr != null)
		{
			Assignment assign = ast.newAssignment();

			assign.setLeftHandSide((SimpleName) ASTNode.copySubtree(ast, this.name.getOriginal()));
			assign.setRightHandSide(this.expr.translate(out));

			stmts.add(ast.newExpressionStatement(assign));
		}

		return stmts;
	}
}
