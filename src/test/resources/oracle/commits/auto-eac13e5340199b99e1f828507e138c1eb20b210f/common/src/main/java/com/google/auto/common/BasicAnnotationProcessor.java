/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.auto.common;

import static com.google.auto.common.SuperficialValidation.validateElement;
import static com.google.common.base.Preconditions.checkState;
import static javax.lang.model.element.ElementKind.PACKAGE;
import static javax.tools.Diagnostic.Kind.ERROR;

import com.google.common.base.Ascii;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ErrorType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleElementVisitor6;

/**
 * An abstract {@link Processor} implementation that ensures that top-level {@link Element}
 * instances are well-formed before attempting to perform processing on any of their children. In
 * the event that they are not, the element will be enqueued for processing in a subsequent round.
 * This ensures that processors will avoid many common pitfalls like {@link ErrorType} instances,
 * {@link ClassCastException}s and badly coerced types.
 *
 * <p>The primary disadvantage to this processor is that any {@link Element} that forms a circular
 * dependency with a type generated by a subclass of this processor will never compile because the
 * given {@link Element} will never be fully complete. All such compilations will fail with an error
 * message on the offending type that describes the issue.
 *
 * <p>Subclasses should put their processing logic in {@link ProcessingStep} implementations. The
 * steps are passed to the processor by returning them in the {@link #initSteps()} method - access
 * the {@link ProcessingEnvironment} using {@link #processingEnv}. Finally, any logic that needs to
 * happen once per round can be specified by overriding {@link #postProcess()}.
 *
 * @author Gregory Kick
 */
public abstract class BasicAnnotationProcessor extends AbstractProcessor {
  /*
   * It's unfortunate that we have to track types and packages separately, but since there are two
   * different methods to look them up in Elements, we end up with a lot of parallel logic. :(
   * Packages declared (and annotated) in package-info.java are tracked as deferred packages while
   * all other elements are tracked via the top-level type.
   */
  private final Set<String> deferredPackageNames = Sets.newLinkedHashSet();
  private final Set<String> deferredTypeNames = Sets.newLinkedHashSet();
  private final String processorName = getClass().getCanonicalName();

  private Elements elements;
  private Messager messager;
  private ImmutableList<? extends ProcessingStep> steps;

  @Override
  public final synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.elements = processingEnv.getElementUtils();
    this.messager = processingEnv.getMessager();
    this.steps = ImmutableList.copyOf(initSteps());
  }

  /**
   * The mechanism by which {@linkplain ProcessingStep processing steps} are associated with the
   * processor. #processinEnv is guaranteed to be set when this method is invoked.
   */
  protected abstract Iterable<? extends ProcessingStep> initSteps();

  /** An optional hook for logic to be executed at the end of each round. */
  protected void postProcess() {}

  private ImmutableSet<? extends Class<? extends Annotation>> getSupportedAnnotationClasses() {
    checkState(steps != null);
    ImmutableSet.Builder<Class<? extends Annotation>> builder = ImmutableSet.builder();
    for (ProcessingStep step : steps) {
      builder.addAll(step.annotations());
    }
    return builder.build();
  }

  /**
   * Returns the set of supported annotation types as a  collected from registered
   * {@linkplain ProcessingStep processing steps}.
   */
  @Override
  public final ImmutableSet<String> getSupportedAnnotationTypes() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (Class<? extends Annotation> annotationClass : getSupportedAnnotationClasses()) {
      builder.add(annotationClass.getCanonicalName());
    }
    return builder.build();
  }

  private static final String INVALID_ELEMENT_MESSAGE_FORMAT = "%s was unable to process %s"
      + " because not all of its dependencies could be resolved. Check for compilation errors or a"
      + " circular dependency with generated code.";

  @Override
  public final boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    checkState(elements != null);
    checkState(messager != null);
    checkState(steps != null);

    // First, collect all of the deferred elements and clear out the state from the previous rounds
    ImmutableMap.Builder<String, Optional<? extends Element>> deferredElementsBuilder =
        ImmutableMap.builder();
    for (String deferredTypeName : deferredTypeNames) {
      deferredElementsBuilder.put(deferredTypeName,
          Optional.fromNullable(elements.getTypeElement(deferredTypeName)));
    }
    for (String deferredPackageName : deferredPackageNames) {
      deferredElementsBuilder.put(deferredPackageName,
          Optional.fromNullable(elements.getPackageElement(deferredPackageName)));
    }
    ImmutableMap<String, Optional<? extends Element>> deferredElements =
        deferredElementsBuilder.build();

    deferredTypeNames.clear();
    deferredPackageNames.clear();

    // If this is the last round, report all of the missing elements
    if (roundEnv.processingOver()) {
      reportMissingElements(deferredElements);
      return false;
    }

    // For all of the elements that were deferred, find the annotated elements therein.  If we don't
    // find any, something is messed up and we just defer them again.
    ImmutableSetMultimap.Builder<Class<? extends Annotation>, Element>
        deferredElementsByAnnotationBuilder = ImmutableSetMultimap.builder();
    for (Entry<String, Optional<? extends Element>> deferredTypeElementEntry :
        deferredElements.entrySet()) {
      Optional<? extends Element> deferredElement = deferredTypeElementEntry.getValue();
      if (deferredElement.isPresent()) {
        findAnnotatedElements(deferredElement.get(), getSupportedAnnotationClasses(),
            deferredElementsByAnnotationBuilder);
      } else {
        deferredTypeNames.add(deferredTypeElementEntry.getKey());
      }
    }

    ImmutableSetMultimap<Class<? extends Annotation>, Element> deferredElementsByAnnotation =
        deferredElementsByAnnotationBuilder.build();

    ImmutableSetMultimap.Builder<Class<? extends Annotation>, Element> elementsByAnnotationBuilder =
        ImmutableSetMultimap.builder();

    Set<String> validPackageNames = Sets.newLinkedHashSet();
    Set<String> validTypeNames = Sets.newLinkedHashSet();

    // Look at the elements we've found and the new elements from this round and validate them.
    for (Class<? extends Annotation> annotationClass : getSupportedAnnotationClasses()) {
      // This should just call roundEnv.getElementsAnnotatedWith(Class) directly, but there is a bug
      // in some versions of eclipse that cause that method to crash.
      TypeElement annotationType = elements.getTypeElement(annotationClass.getCanonicalName());
      Set<? extends Element> elementsAnnotatedWith = (annotationType == null)
          ? ImmutableSet.<Element>of()
          : roundEnv.getElementsAnnotatedWith(annotationType);
      for (Element annotatedElement : Sets.union(
          elementsAnnotatedWith,
          deferredElementsByAnnotation.get(annotationClass))) {
        if (annotatedElement.getKind().equals(PACKAGE)) {
          PackageElement annotatedPackageElement = (PackageElement) annotatedElement;
          String annotatedPackageName = annotatedPackageElement.getQualifiedName().toString();
          boolean validPackage = validPackageNames.contains(annotatedPackageName)
              || (!deferredPackageNames.contains(annotatedPackageName)
                  && validateElement(annotatedPackageElement));
          if (validPackage) {
            elementsByAnnotationBuilder.put(annotationClass, annotatedPackageElement);
            validPackageNames.add(annotatedPackageName);
          } else {
            deferredPackageNames.add(annotatedPackageName);
          }
        } else {
          TypeElement enclosingType = getEnclosingType(annotatedElement);
          String enclosingTypeName = enclosingType.getQualifiedName().toString();
          boolean validEnclosingType = validTypeNames.contains(enclosingTypeName)
              || (!deferredTypeNames.contains(enclosingTypeName)
                  && validateElement(enclosingType));
          if (validEnclosingType) {
            elementsByAnnotationBuilder.put(annotationClass, annotatedElement);
            validTypeNames.add(enclosingTypeName);
          } else {
            deferredTypeNames.add(enclosingTypeName);
          }
        }
      }
    }

    ImmutableSetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation =
        elementsByAnnotationBuilder.build();

    // Finally, process the valid elements
    for (ProcessingStep step : steps) {
      SetMultimap<Class<? extends Annotation>, Element> filteredMap =
          Multimaps.filterKeys(elementsByAnnotation, Predicates.in(step.annotations()));
      if (!filteredMap.isEmpty()) {
        step.process(filteredMap);
      }
    }

    postProcess();

    return false;
  }

  private void reportMissingElements(
      Map<String, ? extends Optional<? extends Element>> missingElements) {
    for (Entry<String, ? extends Optional<? extends Element>> missingElementEntry :
        missingElements.entrySet()) {
      Optional<? extends Element> missingElement = missingElementEntry.getValue();
      if (missingElement.isPresent()) {
        processingEnv.getMessager().printMessage(ERROR,
            String.format(INVALID_ELEMENT_MESSAGE_FORMAT, processorName,
                "this " + Ascii.toLowerCase(missingElement.get().getKind().name())),
                missingElement.get());
      } else {
        processingEnv.getMessager().printMessage(ERROR,
            String.format(INVALID_ELEMENT_MESSAGE_FORMAT, processorName,
                missingElementEntry.getKey()));
      }
    }
  }

  private static void findAnnotatedElements(Element element,
      ImmutableSet<? extends Class<? extends Annotation>> annotationClasses,
      ImmutableSetMultimap.Builder<Class<? extends Annotation>, Element> builder) {
    for (Element enclosedElement : element.getEnclosedElements()) {
      findAnnotatedElements(enclosedElement, annotationClasses, builder);
    }
    // element.getEnclosedElements() does NOT return parameter elements
    if (element instanceof ExecutableElement) {
      for (Element parameterElement : ((ExecutableElement) element).getParameters()) {
        findAnnotatedElements(parameterElement, annotationClasses, builder);
      }
    }
    for (Class<? extends Annotation> annotationClass : annotationClasses) {
      if (MoreElements.isAnnotationPresent(element, annotationClass)) {
        builder.put(annotationClass, element);
      }
    }
  }

  /**
   * Returns the nearest enclosing {@link TypeElement} to the current element, throwing
   * an {@link IllegalArgumentException} if the provided {@link Element} is a
   * {@link PackageElement} or is otherwise not enclosed by a type.
   */
  // TODO(cgruber) move to MoreElements and make public.
  private static TypeElement getEnclosingType(Element element) {
    return element.accept(new SimpleElementVisitor6<TypeElement, Void>() {
      @Override protected TypeElement defaultAction(Element e, Void p) {
        return e.getEnclosingElement().accept(this, p);
      }

      @Override public TypeElement visitType(TypeElement e, Void p) {
        return e;
      }

      @Override public TypeElement visitPackage(PackageElement e, Void p) {
        throw new IllegalArgumentException();
      }
    }, null);
  }

  /**
   * The unit of processing logic that runs under the guarantee that all elements are complete and
   * well-formed.
   */
  public interface ProcessingStep {
    /** The set of annotation types processed by this step. */
    Set<? extends Class<? extends Annotation>> annotations();

    /**
     * The implementation of processing logic for the step. It is guaranteed that the keys in
     * {@code elementsByAnnotation} will be a subset of the set returned by {@link #annotations()}.
     */
    void process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation);
  }
}
