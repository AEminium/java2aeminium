package aeminium.compiler.east;

import java.util.ArrayList;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import aeminium.compiler.signature.SimpleDataGroup;

public class ETypeDeclaration extends EASTNode
{
	protected final ECompilationUnit cu;
	
	protected final ArrayList<EFieldDeclaration> fields;
	protected final ArrayList<EMethodDeclaration> methods;

	/**
	 * used in static contexts
	 */
	protected final SimpleDataGroup staticDataGroup;
	
	/**
	 * used as a placeholder for object contexts
	 */
	protected final SimpleDataGroup thisDataGroup;
	
	protected final ITypeBinding binding;
	
	public ETypeDeclaration(EAST east, TypeDeclaration original, ECompilationUnit cu)
	{
		super(east, original);

		this.cu = cu;
		
		this.binding = original.resolveBinding();
		this.east.addNode(this.binding, this);
		
		this.staticDataGroup = new SimpleDataGroup("static " + original.getName());
		this.thisDataGroup = new SimpleDataGroup("this " + original.getName());
		
		this.fields = new ArrayList<EFieldDeclaration>();
		for (Object field : original.getFields())
			this.fields.add(EFieldDeclaration.create(this.east, (FieldDeclaration) field, this));

		this.methods = new ArrayList<EMethodDeclaration>();
		for (Object method : original.getMethods())
			this.methods.add(EMethodDeclaration.create(this.east, (MethodDeclaration) method, this));

	}

	/* Factory */
	public static ETypeDeclaration create(EAST east, TypeDeclaration original, ECompilationUnit cu)
	{
		return new ETypeDeclaration(east, original, cu);
	}
	
	@Override
	public TypeDeclaration getOriginal()
	{
		return (TypeDeclaration) this.original;
	}
	
	@Override
	public void checkSignatures()
	{
		for (EFieldDeclaration field : this.fields)
			field.checkSignatures();
		
		for (EMethodDeclaration method : this.methods)
			method.checkSignatures();
	}

	public void checkDependencies()
	{
		// TODO/FIXME how to handle dependencies between constructors and field initializers?
		
		for (EFieldDeclaration field : this.fields)
			field.checkDependencies();
		
		for (EMethodDeclaration method : this.methods)
			method.checkDependencies();
	}
}
