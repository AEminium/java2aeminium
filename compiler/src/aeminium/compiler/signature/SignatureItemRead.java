package aeminium.compiler.signature;

import java.util.Set;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;

public class SignatureItemRead extends SignatureItem implements SignatureItemModification
{
	protected final DataGroup datagroup;
	protected final Dependency dependency;
	
	public SignatureItemRead(Dependency dependency, DataGroup datagroup)
	{
		this.datagroup = datagroup;
		this.dependency = dependency;
	}

	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof SignatureItemRead))
			return false;
		
		SignatureItemRead _other = (SignatureItemRead) other;
		
		return this.datagroup.equals(_other.datagroup) &&
			(this.dependency == _other.dependency || this.dependency.equals(_other.dependency));
	}

	@Override
	public int hashCode()
	{
		return this.datagroup.hashCode();
	}

	@Override
	public String toString()
	{
		return "[READ] " + this.datagroup + " " + this.dependency;
	}
	
	@Override
	public SignatureItemRead replace(DataGroup what, DataGroup with)
	{
		return new SignatureItemRead(this.dependency, this.datagroup.replace(what, with));
	}

	@Override
	public Set<Dependency> getDependencies(DependencyStack dependencyStack)
	{
		return dependencyStack.read(this.dependency, this.datagroup);
	}

	@Override
	public boolean isLocalTo(DataGroup scope)
	{
		return this.datagroup.beginsWith(scope);
	}

	@Override
	public SignatureItemRead setDependency(Dependency dep)
	{
		return new SignatureItemRead(dep, this.datagroup);
	}
}
