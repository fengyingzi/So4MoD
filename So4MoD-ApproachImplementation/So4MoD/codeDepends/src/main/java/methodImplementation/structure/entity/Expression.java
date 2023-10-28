/*
MIT License

Copyright (c) 2018-2019 Gang ZHANG

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.nju.bysj.softwaremodularisation.structure.entity;

import com.nju.bysj.softwaremodularisation.structure.entity.repo.EntityRepo;
import com.nju.bysj.softwaremodularisation.structure.relations.Inferer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Expression implements Serializable{
	private static final long serialVersionUID = 1L;
	public Integer id;
	public String text; // for debug purpose
	private GenericName rawType; //the raw type name
	private GenericName identifier; // the varName, or method name, etc.
	public boolean isSet = false; // is a set relation from right to leftHand
	public boolean isDot = false; // is a dot expression, will decuce variable tfype left to right
	public boolean isCall() {
		return isCall;
	}

	public void setCall(boolean isCall) {
		this.isCall = isCall;
	}

	private boolean isCall = false;
	public boolean isLogic = false;
	public boolean isCreate = false;
	public boolean isCast = false;
	public boolean isThrow = false;
	public boolean isStatement = false; //statement is only used for return type calcuation in some langs such as ruby
	                                    //they will not be treat as real expressions in case of relation calculation
	public boolean deriveTypeFromChild = true;
	public Integer deduceTypeBasedId; //by default, parent expression type determined by most left child
	
	private Integer parentId = -1; 
	private List<Integer> deducedTypeVarsId = new ArrayList<>();
	private List<Integer> deducedTypeFunctionsId = new ArrayList<>();
	
	private transient Expression parent;
	private transient TypeEntity type; // the type we care - for relation calculation. 
	                         //for leaf, it equals to referredEntity.getType. otherwise, depends on child's type strategy
	private transient Entity referredEntity;
	
	private transient List<VarEntity> deducedTypeVars = new ArrayList<>();
	private transient List<FunctionEntity> deducedTypeFunctions= new ArrayList<>();
	public TypeEntity getType() {
		return type;
	}

	public void setType(TypeEntity type, Entity referredEntity, Inferer inferer) {
		if (this.referredEntity==null && referredEntity!=null) {
			this.referredEntity  = referredEntity;
		}
		
		if (type instanceof CandidateTypes) {
			return ;
		}
		boolean changedType = false;
		if (this.type==null && type!=null) {
			this.type = type;
			for (VarEntity var:deducedTypeVars) {
				if (var!=null) {
					var.setType(this.type);
				}
			}
			for (FunctionEntity func:deducedTypeFunctions) {
				if (func!=null) {
					func.addReturnType(this.type);
				}
			}
			changedType = true;
		}
		if (this.referredEntity==null)
			this.referredEntity = this.type;

		if (changedType)
			deduceTheParentType(inferer);
	}
	
	public Expression() {
		deducedTypeVars = new ArrayList<>();
		deducedTypeFunctions = new ArrayList<>();
	}
	public Expression(Integer id) {
		this.id = id;
		deducedTypeVars = new ArrayList<>();
		deducedTypeFunctions = new ArrayList<>();
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("[").append(text).append("]").append("|")
			.append("rawType:").append(rawType).append("|")
			.append("identifier:").append(identifier).append("|")
		    .append("prop:").append(isDot?"[dot]":"")
		                      .append(isSet?"[set]":"")
		                      .append(isLogic?"[bool]":"")
		                      .append(isCall?"[call]":"").append("|")
		                      .append(isCreate?"[new]":"").append("|")
		                      .append(isThrow?"[throw]":"").append("|")
		    .append("parent:").append(parent==null?"nil":parent.text).append("|")
			.append("type:").append(type).append("|");
		return s.toString();
	}

	/**
	 * deduce type of parent based on child's type
	 * @param expressionList
	 * @param inferer
	 */
	private void deduceTheParentType(Inferer inferer) {
		if (this.type==null) return;
		if (this.parent==null) return;
		Expression parent = this.parent;
		if (parent.type != null)return;
		if (!parent.deriveTypeFromChild) return;
		//parent's type depends on first child's type
		if (parent.deduceTypeBasedId!=this.id) return;
		
		//if child is a built-in/external type, then parent must also a built-in/external type
		if (this.type.equals(Inferer.buildInType)) {
			parent.setType(Inferer.buildInType,Inferer.buildInType,inferer);
			return;
		}else if (this.type.equals(Inferer.externalType)){
			parent.setType(Inferer.externalType,Inferer.externalType,inferer);
			return;
		}
		
		/* if it is a logic expression, the return type/type is boolean. */
		if (parent.isLogic) {
			parent.setType(Inferer.buildInType,null,inferer);
		}
		/* if it is a.b, and we already get a's type, b's type could be identified easily  */
		else if (parent.isDot) {
			if (parent.isCall()) {
				FunctionEntity func = this.getType().lookupFunctionInVisibleScope(parent.identifier);
				if (func!=null) {
					parent.setType(func.getType(), func,inferer);
					parent.setReferredEntity(func);
				}
			}else {
				VarEntity var = this.getType().lookupVarInVisibleScope(parent.identifier);
				if (var!=null) {
					parent.setType(var.getType(),var, inferer);
					parent.setReferredEntity(var);
				}else {
					FunctionEntity func = this.getType().lookupFunctionInVisibleScope(parent.identifier);
					if (func!=null) {
						parent.setType(func.getType(), func,inferer);
						parent.setReferredEntity(func);
					}
				}
			}
			if (parent.getType()==null) {
				parent.setType(inferer.inferTypeFromName(this.getType(), parent.identifier),null,inferer);
			}
		}
		/* if other situation, simple make the parent and child type same */
		else {
			parent.setType(type, null, inferer);
		}
		if (parent.referredEntity==null)
			parent.referredEntity = parent.type;
	}

	private void setReferredEntity(Entity referredEntity) {
		this.referredEntity = referredEntity;
	}

	public Entity getReferredEntity() {
		return referredEntity;
	}


	public void addDeducedTypeVar(VarEntity var) {
		this.deducedTypeVars.add(var);
		this.deducedTypeVarsId.add(var.getId());
	}

	public void addDeducedTypeFunction(FunctionEntity function) {
		this.deducedTypeFunctions.add(function);
		this.deducedTypeFunctionsId.add(function.id);
	}

	public void setParent(Expression parent) {
		this.parent = parent;
		if (parent!=null)
			this.parentId = parent.id;
		if (parent!=null) {
			if (parent.deduceTypeBasedId==null) 
				parent.deduceTypeBasedId = id;
			if (parent.isSet) {
				parent.deduceTypeBasedId = id;
			}
		}
	}

	public void reload(EntityRepo repo, ArrayList<Expression> expressionList) {
		this.deducedTypeFunctions = new ArrayList<>();
		this.deducedTypeVars = new ArrayList<>();
		if (parentId!=-1) {
			for (Expression expr:expressionList) {
				if (expr.id==parentId) {
					parent = expr;
					break;
				}
			}
		}
		
		if (deducedTypeFunctionsId!=null) {
			for (Integer funcId:this.deducedTypeFunctionsId) {
				this.deducedTypeFunctions.add((FunctionEntity) repo.getEntity(funcId));
			}
		}
		
		if (deducedTypeVarsId!=null) {
			for (Integer varId:this.deducedTypeVarsId) {
				this.deducedTypeVars.add((VarEntity) repo.getEntity(varId));
			}
		}
	}

	public GenericName getIdentifier() {
		return this.identifier;
	}

	public GenericName getRawType() {
		return this.rawType;
	}

	public void setIdentifier(String name) {
		if (!validName(name)){
			return;
		}
		this.identifier = GenericName.build(name);
	}

	private boolean validName(String name) {
		if (name==null) return false;
		if (name.toLowerCase().equals("<literal>")) return true;
		if (name.toLowerCase().equals("<built-in>")) return true;
		boolean result = name.matches("([a-zA-Z0-9_]|(\\.)|(\\-))*");
//		if (result==false) {
//			System.err.println("expression name " + name);
//		}
		return true;
	}

	public void setIdentifier(GenericName name) {
		if (name==null) return;
		if (!validName(name.getName())){
			return;
		}
		this.identifier = name;
	}

	public void setRawType(GenericName name) {
		if (name==null) return;
		if (!validName(name.getName())){
			return;
		}
		this.rawType = name;
		
	}

	public void setRawType(String name) {
		if (name==null) return;
		if (!validName(name)){
			return;
		}
		this.rawType = GenericName.build(name);
	}

	public Expression getParent() {
		return this.parent;
	}
	
	
}