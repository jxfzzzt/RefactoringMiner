/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.annotation;

import static org.hibernate.processor.util.Constants.UNI;

public class LifecycleMethod extends AbstractAnnotatedMethod {
	private final String entity;
	private final String methodName;
	private final String parameterName;
	private final String operationName;
	private final boolean addNonnullAnnotation;
	private final boolean iterateParameter;
	private final boolean returnArgument;

	public LifecycleMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String entity,
			String methodName,
			String parameterName,
			String sessionName,
			String sessionType,
			String operationName,
			boolean addNonnullAnnotation,
			boolean iterateParameter,
			boolean returnArgument) {
		super(annotationMetaEntity, sessionName, sessionType);
		this.entity = entity;
		this.methodName = methodName;
		this.parameterName = parameterName;
		this.operationName = operationName;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.iterateParameter = iterateParameter;
		this.returnArgument = returnArgument;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
		StringBuilder declaration = new StringBuilder();
		preamble(declaration);
		nullCheck(declaration);
		declaration.append("\ttry {\n");
		delegateCall(declaration);
		returnArgument(declaration);
		declaration.append("\t}\n");
		if ( operationName.equals("insert") ) {
			convertException(declaration,
					"jakarta.persistence.EntityExistsException",
					"jakarta.data.exceptions.EntityExistsException");
		}
		else {
			convertException(declaration,
					"jakarta.persistence.OptimisticLockException",
					"jakarta.data.exceptions.OptimisticLockingFailureException");
		}
		convertException(declaration,
				"jakarta.persistence.PersistenceException",
				"jakarta.data.exceptions.DataException");
		declaration.append("}");
		return declaration.toString();
	}

	private void returnArgument(StringBuilder declaration) {
		if ( returnArgument ) {
			if ( isReactive() ) {
				declaration
					.append(".replaceWith(")
					.append(parameterName)
					.append(")");
			}
			else {
				declaration
						.append("\t\treturn ")
						.append(parameterName);
			}
			declaration
					.append(";\n");
		}
		else {
			if ( isReactive() ) {
				declaration
						.append(";\n");
			}
		}
	}

	private void delegateCall(StringBuilder declaration) {
		if ( isReactive() ) {
			declaration
					.append("\t\treturn ")
					.append(sessionName);
			if ( isReactiveSessionAccess() ) {
				declaration
						.append(".chain(")
						.append(localSessionName())
						.append(" -> ")
						.append(localSessionName());
			}
			declaration
					.append('.')
					.append(operationName)
					.append('(')
					.append(parameterName)
					.append(')');
			if ( isReactiveSessionAccess() ) {
				declaration
						.append(')');
			}
		}
		else {
			if ( iterateParameter ) {
				declaration
						.append("\t\tfor (var _entity : ")
						.append(parameterName)
						.append(") {\n\t");
			}
			declaration
					.append("\t\t")
					.append(sessionName)
					.append('.')
					.append(operationName)
					.append('(')
					.append(iterateParameter ? "_entity" : parameterName)
					.append(')')
					.append(";\n");
			if ( iterateParameter ) {
				declaration
						.append("\t\t}\n");
			}
		}
	}

	private void preamble(StringBuilder declaration) {
		declaration
				.append("\n@Override\npublic ")
				.append(returnType())
				.append(' ')
				.append(methodName)
				.append('(');
		notNull(declaration);
		declaration
				.append(annotationMetaEntity.importType(entity))
				.append(' ')
				.append(parameterName)
				.append(')')
				.append(" {\n");
	}

	private String returnType() {
		final String entityType = annotationMetaEntity.importType(entity);
		if ( isReactive() ) {
			return annotationMetaEntity.importType(UNI)
					+ '<' + (returnArgument ? entityType : "Void") + '>';
		}
		else {
			return returnArgument
					? entityType
					: "void";
		}
	}

	private void nullCheck(StringBuilder declaration) {
		declaration
				.append("\tif (")
				.append(parameterName)
				.append(" == null) throw new IllegalArgumentException(\"Null ")
				.append(parameterName)
				.append("\");\n");
	}

	private void convertException(StringBuilder declaration, String exception, String convertedException) {
		declaration
				.append("\tcatch (")
				.append(annotationMetaEntity.importType(exception))
				.append(" exception) {\n")
				.append("\t\tthrow new ")
				.append(annotationMetaEntity.importType(convertedException))
				.append("(exception);\n")
				.append("\t}\n");
	}

	private void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType("jakarta.annotation.Nonnull"))
					.append(' ');
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	@Override
	public String getTypeDeclaration() {
		return entity;
	}
}
