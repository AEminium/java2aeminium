package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.DataGroup;
import aeminium.compiler.signature.Signature;
import aeminium.compiler.signature.SignatureItem;
import aeminium.compiler.signature.SignatureItemRead;
import aeminium.compiler.signature.SimpleDataGroup;
import aeminium.compiler.task.MethodTask;
import aeminium.compiler.task.Task;

public class EMethodDeclaration extends EBodyDeclaration implements EASTDeclaringNode
{
	protected final IMethodBinding binding;

	protected final ArrayList<EMethodDeclarationParameter> parameters;
	protected final EBlock body;
	
	protected final DataGroup returnDataGroup;
	
	public EMethodDeclaration(EAST east, MethodDeclaration original, ETypeDeclaration type)
	{
		super(east, original, type);
		
		this.returnDataGroup = this.getDataGroup().append(new SimpleDataGroup("ret " + original.getName().toString()));

		this.binding = original.resolveBinding();
		
		this.east.addNode(this.binding, this);
		
		this.parameters = new ArrayList<EMethodDeclarationParameter>();
		for (Object param : original.parameters())
			this.parameters.add(EMethodDeclarationParameter.create(this.east, (SingleVariableDeclaration) param, this));
		
		this.body = EBlock.create(this.east, (Block) original.getBody(), this, this);
	}

	/* factory */
	public static EMethodDeclaration create(EAST east, MethodDeclaration method, ETypeDeclaration type)
	{
		return new EMethodDeclaration(east, method, type);
	}

	@Override
	public MethodDeclaration getOriginal()
	{
		return (MethodDeclaration) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		this.body.checkSignatures();
	}

	@Override
	public Signature getFullSignature()
	{
		Signature sig = new Signature();
		
		sig.addAll(this.signature);
		sig.addAll(this.body.getFullSignature());
		
		return sig;
	}
	
	public Signature undefer(DataGroup dgRet, DataGroup dgThis, ArrayList<DataGroup> dgsArgs)
	{
		Signature sig = new Signature();
		
		for (int i = 0; i < this.parameters.size(); i++)
			if (this.parameters.get(i).getOriginal().getType().isPrimitiveType())
				sig.addItem(new SignatureItemRead(dgsArgs.get(i)));

		outerLoop: for (SignatureItem item : this.getFullSignature().getItems())
		{
			// don't propagate local variable changes
			if (item.isLocalTo(this.body.getDataGroup()))
				continue;

			SignatureItem _item = item;

			for (int i = 0; i < this.parameters.size(); i++)
			{
				// This item refers a read/write/merge to a parameter passed in by copy (native)
				// it has no implications on outer dependencies and can be cut out
				if (this.parameters.get(i).getOriginal().getType().isPrimitiveType()
					&& item.isLocalTo(this.parameters.get(i).name.getDataGroup()))
					continue outerLoop;

				_item = _item.replace(this.parameters.get(i).name.getDataGroup(), dgsArgs.get(i));
			}
			
			if (!this.isStatic())
				_item = _item.replace(this.type.thisDataGroup, dgThis);

			if (!this.isVoid())
				_item = _item.replace(this.returnDataGroup, dgRet);
			
			sig.addItem(_item);
		}
		
		return sig;
	}

	@Override
	public void checkDependencies(DependencyStack stack)
	{
		this.body.checkDependencies(stack);
		this.strongDependencies.add(this.body);
	}
	
	public boolean isVoid()
	{
		return this.getOriginal().getReturnType2().toString().equals("void");
	}

	public boolean isMain()
	{
		return (this.getModifier("static") != null) && this.getOriginal().getName().toString().equals("main");
	}
	
	public EBlock getBody()
	{
		return this.body;
	}

	@Override
	public int optimize()
	{
		int sum = super.optimize();
		sum += this.body.optimize();
		
		return sum;
	}

	public void preTranslate()
	{
		String name = this.type.getOriginal().getName() + "_" + this.getOriginal().getName();
		
		this.preTranslate(MethodTask.create(this, name));		
	}

	@Override
	public void preTranslate(Task parent)
	{
		this.task = parent;
		this.body.preTranslate(this.task);
	}
	
	@SuppressWarnings("unchecked")
	public MethodDeclaration translate(ArrayList<CompilationUnit> out)
	{
		out.add(this.task.translate());

		MethodDeclaration execute = this.task.getExecute();
		
		execute.getBody().statements().addAll(this.body.translate(out));

		if (this.isMain())
			return this.buildMain();
		
		return (MethodDeclaration) ASTNode.copySubtree(this.getAST(), this.original);
	}

	@SuppressWarnings("unchecked")
	private MethodDeclaration buildMain()
	{
		AST ast = this.getAST();
		MethodDeclaration method = ast.newMethodDeclaration();
		
		method.setName((SimpleName) ASTNode.copySubtree(ast, this.getOriginal().getName()));
		method.parameters().addAll(ASTNode.copySubtrees(ast, this.getOriginal().parameters()));

		method.modifiers().add(ast.newModifier(ModifierKeyword.PUBLIC_KEYWORD));
		method.modifiers().add(ast.newModifier(ModifierKeyword.STATIC_KEYWORD));

		Block body = ast.newBlock();

		// AeminiumHelper.init();
		MethodInvocation init = ast.newMethodInvocation();
		init.setExpression(ast.newSimpleName("AeminiumHelper"));
		init.setName(ast.newSimpleName("init"));

		body.statements().add(ast.newExpressionStatement(init));

		ClassInstanceCreation creation = ast.newClassInstanceCreation();
		creation.setType(ast.newSimpleType(ast.newSimpleName(this.task.getName())));
		creation.arguments().add(ast.newNullLiteral());

		for (Object arg : this.getOriginal().parameters())
			creation.arguments().add((Expression) ASTNode.copySubtree(ast, ((SingleVariableDeclaration) arg).getName()));

		body.statements().add(ast.newExpressionStatement(creation));

		// AeminiumHelper.shutdown();
		MethodInvocation shutdown = ast.newMethodInvocation();
		shutdown.setExpression(ast.newSimpleName("AeminiumHelper"));
		shutdown.setName(ast.newSimpleName("shutdown"));

		body.statements().add(ast.newExpressionStatement(shutdown));

		method.setBody(body);

		return method;
	}

	public ArrayList<EMethodDeclarationParameter> getParameters()
	{
		return this.parameters;
	}

	public Type getThisType()
	{
		AST ast = this.getAST();
		
		// TODO: EMethodDeclaration Type from TypeDeclaration
		return ast.newSimpleType(this.type.getOriginal().getName());
	}
}