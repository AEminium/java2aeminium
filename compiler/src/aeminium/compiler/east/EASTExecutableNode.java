package aeminium.compiler.east;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import aeminium.compiler.DependencyStack;
import aeminium.compiler.signature.Signature;

public abstract class EASTExecutableNode extends EASTNode
{
	protected final Signature signature;
	
	protected final Set<EASTExecutableNode> strongDependencies;
	protected final Set<EASTExecutableNode> weakDependencies;
	protected final Set<EASTExecutableNode> children;
		
	public EASTExecutableNode(EAST east, ASTNode original)
	{
		super(east, original);

		this.signature = new Signature();
		
		this.strongDependencies = new HashSet<EASTExecutableNode>();
		this.weakDependencies = new HashSet<EASTExecutableNode>();
		this.children = new HashSet<EASTExecutableNode>();
	}
	
	public abstract Signature getFullSignature();

	public abstract void checkDependencies(DependencyStack stack);
}