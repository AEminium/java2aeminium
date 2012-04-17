package aeminium.compiler.signature;

import java.util.Set;

import aeminium.compiler.Dependency;
import aeminium.compiler.DependencyStack;

public class SignatureItemWrite extends SignatureItem implements SignatureItemModification
{
	protected final DataGroup datagroup;
	protected final Dependency dependency;
	
	public SignatureItemWrite(Dependency dependency, DataGroup datagroup)
	{
		this.dependency = dependency;
		this.datagroup = datagroup;
	}
	
	@Override
	public boolean equals(Object other)
	{
		if (other == null || ! (other instanceof SignatureItemWrite))
			return false;
		
		SignatureItemWrite _other = (SignatureItemWrite) other;
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
		return "[WRITE] " + this.datagroup + " " + this.dependency;
	}
	
	@Override
	public SignatureItemWrite replace(DataGroup what, DataGroup with)
	{
		return new SignatureItemWrite(this.dependency, this.datagroup.replace(what, with));
	}
	
	@Override
	public Set<Dependency> getDependencies(DependencyStack dependencyStack)
	{
		return dependencyStack.write(this.dependency, this.datagroup);
	}
	
	@Override
	public boolean isLocalTo(DataGroup scope)
	{
		return this.datagroup.beginsWith(scope);
	}
	
	@Override
	public SignatureItemWrite setDependency(Dependency dep)
	{
//		assert(this.dependency == null);
		return new SignatureItemWrite(dep, this.datagroup);
	}
}
