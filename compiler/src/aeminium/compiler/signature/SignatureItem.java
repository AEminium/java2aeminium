package aeminium.compiler.signature;

import aeminium.compiler.Dependency;

public abstract class SignatureItem
{
	public abstract SignatureItem replace(DataGroup what, DataGroup with);
	public abstract SignatureItem setDependency(Dependency dep);

	/**
	 * True if any of the DataGroups referred start with scope
	 * @param scope
	 * @return
	 */
	public abstract boolean isLocalTo(DataGroup scope);
}
