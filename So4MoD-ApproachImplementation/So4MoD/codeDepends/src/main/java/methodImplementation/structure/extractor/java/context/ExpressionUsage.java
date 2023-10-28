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

package com.nju.bysj.softwaremodularisation.structure.extractor.java.context;

import com.nju.bysj.softwaremodularisation.structure.entity.Expression;
import com.nju.bysj.softwaremodularisation.structure.entity.GenericName;
import com.nju.bysj.softwaremodularisation.structure.entity.repo.IdGenerator;
import com.nju.bysj.softwaremodularisation.structure.extractor.HandlerContext;
import com.nju.bysj.softwaremodularisation.structure.extractor.java.JavaParser;
import org.antlr.v4.runtime.RuleContext;

public class ExpressionUsage {
	HandlerContext context;
	IdGenerator idGenerator;
	public ExpressionUsage(HandlerContext context,IdGenerator idGenerator) {
		this.context = context;
		this.idGenerator = idGenerator;
	}

	public void foundExpression(JavaParser.ExpressionContext ctx) {
		Expression parent = findParentInStack(ctx);
		/* create expression and link it with parent*/
		Expression expression = new Expression(idGenerator.generateId());
		context.lastContainer().addExpression(ctx,expression);

		expression.setParent(parent);

		
		if (ctx.primary()!=null) {
			tryFillExpressionTypeAndIdentifier(ctx.primary(),expression);
			return;
		}
		
		expression.isSet = isSet(ctx);
		expression.setCall(ctx.methodCall()==null?false:true);
		expression.isLogic = isLogic(ctx);
		expression.isDot = isDot(ctx);
		if (ctx.creator()!=null ||ctx.innerCreator()!=null){
			expression.isCreate = true;
		}		
/**
 *    | expression bop='.'
      ( IDENTIFIER
      | methodCall
      | THIS
      | NEW nonWildcardTypeArguments? innerCreator
      | SUPER superSuffix
      | explicitGenericInvocation
      )
 */
		//method call
		if (ctx.methodCall()!=null) {
			expression.setIdentifier(getMethodCallIdentifier(ctx.methodCall()));
			expression.setCall(true);
		}
		//new 
		if (ctx.NEW()!=null && ctx.creator()!=null) {
			expression.setRawType(CreatorContextHelper.getCreatorType(ctx.creator()));
			expression.setCall(true);
			expression.deriveTypeFromChild = false;
		}
		
		if (ctx.typeCast()!=null) {
			expression.isCast=true;
			expression.setRawType(ctx.typeCast().typeType().getText());
			expression.deriveTypeFromChild = false;
		}
		
		if (ctx.bop!=null && ctx.bop.getText().equals("instanceof")) {
			expression.isCast=true;
			expression.setRawType(ctx.typeType().getText());
			expression.deriveTypeFromChild = false;
		}
		
		if (ctx.creator()!=null) {
			expression.deriveTypeFromChild = false;
		}
		
		if (expression.isDot) {
			if (ctx.IDENTIFIER()!=null)
				expression.setIdentifier(ctx.IDENTIFIER().getText());
			else if (ctx.methodCall()!=null)
				expression.setIdentifier(getMethodCallIdentifier(ctx.methodCall()));
			else if (ctx.THIS()!=null)
				expression.setIdentifier("this");
			else if (ctx.innerCreator()!=null) //innner creator like new List(){}
				expression.setIdentifier(ctx.innerCreator().IDENTIFIER().getText());
			else if (ctx.SUPER()!=null)
				expression.setIdentifier("super");
			return;
		}
	}

	private GenericName getMethodCallIdentifier(JavaParser.MethodCallContext methodCall) {
		if (methodCall.THIS()!=null) {
			return GenericName.build("this");
		}else if (methodCall.SUPER()!=null) {
			return GenericName.build("super");
		}else {
			return GenericName.build(methodCall.IDENTIFIER().getText());
		}
	}

	private boolean isDot(JavaParser.ExpressionContext ctx) {
		if (ctx.bop!=null)
			if (ctx.bop.getText().equals(".")) return true;
		return false;
	}
	
	private boolean isLogic(JavaParser.ExpressionContext ctx) {
		if (ctx.bop != null) {
			if (OpHelper.isLogic(ctx.bop.getText())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isSet(JavaParser.ExpressionContext ctx) {
		if (ctx.bop != null) {
			if (OpHelper.isAssigment(ctx.bop.getText())) {
				return true;
			}
		}
		if (ctx.prefix != null) {
			if (OpHelper.isIncrementalDecremental(ctx.prefix.getText())) {
				return true;
			}
		}
		if (ctx.postfix != null) {
			if (OpHelper.isIncrementalDecremental(ctx.postfix.getText())) {
				return true;
			}
		}
		
		return false;
	}
	
//  primary
//    : '(' expression ')'
//    | THIS
//    | SUPER
//    | literal
//    | IDENTIFIER
//    | typeTypeOrVoid '.' CLASS
//    | nonWildcardTypeArguments (explicitGenericInvocationSuffix | THIS arguments) //Just USE relation
//    
	private void tryFillExpressionTypeAndIdentifier(JavaParser.PrimaryContext ctx, Expression expression) {
		if (ctx.expression()!=null) return; 
		//1. we only handle leaf node. if there is still expression,
		//   the type will be determined by child node in the expression
		if (ctx.literal()!=null) {
		//2. if it is a build-in type like "hello"(string), 10(integer), etc.
			expression.setRawType("<Built-in>");
			expression.setIdentifier("<Literal>");
		}else if (ctx.IDENTIFIER()!=null) {
		//2. if it is a var name, dertermine the type based on context.
			expression.setIdentifier(ctx.IDENTIFIER().getText());
		}else if (ctx.typeTypeOrVoid()!=null) {
		//3. if given type directly
			expression.setRawType(ClassTypeContextHelper.getClassName(ctx.typeTypeOrVoid()));
		}else if (ctx.THIS()!=null){
			expression.setIdentifier("this");
		}else if (ctx.SUPER()!=null){
			expression.setIdentifier("super");
		}
	}

	private Expression findParentInStack(RuleContext ctx) {
		if (ctx==null) return null;
		if (ctx.parent==null) return null;
		if (context.lastContainer()==null) {
			return null;
		}
		if (context.lastContainer().expressions().containsKey(ctx.parent)) return context.lastContainer().expressions().get(ctx.parent);
		return findParentInStack(ctx.parent);
	}
}
