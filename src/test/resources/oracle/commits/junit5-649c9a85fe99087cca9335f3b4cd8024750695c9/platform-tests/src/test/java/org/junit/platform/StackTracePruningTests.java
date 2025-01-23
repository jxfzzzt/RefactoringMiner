/*
 * Copyright 2015-2023 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Test cases for stacktrace pruning.
 *
 * <p>Note: the package {@code org.junit.platform} this class resides in is
 * chosen on purpose. If it was in {@code org.junit.platform.launcher}
 * stack traces would be fully pruned.
 *
 * @since 5.10
 */
class StackTracePruningTests {

	@Test
	void shouldPruneStackTraceByDefault() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.selectors(selectMethod(StackTracePruningTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				\\Qorg.junit.jupiter.api.Assertions.fail(Assertions.java:\\E.+
				""");

		assertStackTraceDoesNotContain(stackTrace, "java.util.ArrayList.forEach(ArrayList.java:");
		assertStackTraceDoesNotContain(stackTrace,
			"jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:");
	}

	@Test
	void shouldPruneStackTraceWhenEnabled() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.selectors(selectMethod(StackTracePruningTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				\\Qorg.junit.jupiter.api.Assertions.fail(Assertions.java:\\E.+
				""");

		assertStackTraceDoesNotContain(stackTrace, "java.util.ArrayList.forEach(ArrayList.java:");
		assertStackTraceDoesNotContain(stackTrace,
			"jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:");
	}

	@Test
	void shouldNotPruneStackTraceWhenDisabled() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "false") //
				.selectors(selectMethod(StackTracePruningTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				\\Qorg.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:\\E.+
				\\Qorg.junit.jupiter.api.Assertions.fail(Assertions.java:\\E.+
				>>>>
				\\Qjava.base/java.util.ArrayList.forEach(ArrayList.java:\\E.+
				>>>>
				""");
	}

	@Test
	void shouldPruneStackTraceAccordingToPattern() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.configurationParameter("junit.platform.stacktrace.pruning.pattern", "jdk.*") //
				.selectors(selectMethod(StackTracePruningTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				\\Qorg.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:\\E.+
				\\Qorg.junit.jupiter.api.Assertions.fail(Assertions.java:\\E.+
				>>>>
				\\Qjava.base/java.util.ArrayList.forEach(ArrayList.java:\\E.+
				>>>>
				""");

		assertStackTraceDoesNotContain(stackTrace, "jdk.");
	}

	@Test
	void shouldAlwaysKeepJupiterAssertionStackTraceElement() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.configurationParameter("junit.platform.stacktrace.pruning.pattern", "*") //
				.selectors(selectMethod(StackTracePruningTestCase.class, "failingAssertion")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				\\Qorg.junit.jupiter.api.Assertions.fail(Assertions.java:\\E.+
				""");
	}

	@Test
	void shouldAlwaysKeepJupiterAssumptionStackTraceElement() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.configurationParameter("junit.platform.stacktrace.pruning.pattern", "*") //
				.selectors(selectMethod(StackTracePruningTestCase.class, "failingAssumption")) //
				.execute();

		List<StackTraceElement> stackTrace = extractStackTrace(results);

		assertStackTraceMatch(stackTrace, """
				\\Qorg.junit.jupiter.api.Assumptions.assumeTrue(Assumptions.java:\\E.+
				""");
	}

	@Test
	void shouldPruneStackTracesOfSuppressedExceptions() {
		EngineExecutionResults results = EngineTestKit.engine("junit-jupiter") //
				.configurationParameter("junit.platform.stacktrace.pruning.enabled", "true") //
				.selectors(selectMethod(StackTracePruningTestCase.class, "multipleFailingAssertions")) //
				.execute();

		Throwable throwable = getThrowable(results);

		for (Throwable suppressed : throwable.getSuppressed()) {
			List<StackTraceElement> stackTrace = Arrays.asList(suppressed.getStackTrace());
			assertStackTraceDoesNotContain(stackTrace, "java.util.ArrayList.forEach(ArrayList.java:");
		}
	}

	private static List<StackTraceElement> extractStackTrace(EngineExecutionResults results) {
		return Arrays.asList(getThrowable(results).getStackTrace());
	}

	private static Throwable getThrowable(EngineExecutionResults results) {
		var failedTestEvent = results.testEvents().failed().list().get(0);
		var testResult = failedTestEvent.getRequiredPayload(TestExecutionResult.class);
		return testResult.getThrowable().orElseThrow();
	}

	private static void assertStackTraceMatch(List<StackTraceElement> stackTrace, String expectedLines) {
		List<String> stackStraceAsLines = stackTrace.stream() //
				.map(StackTraceElement::toString) //
				.collect(Collectors.toList());
		assertLinesMatch(expectedLines.lines().toList(), stackStraceAsLines);
	}

	private static void assertStackTraceDoesNotContain(List<StackTraceElement> stackTrace, String element) {
		String stackStraceAsString = stackTrace.stream() //
				.map(StackTraceElement::toString) //
				.collect(Collectors.joining());
		assertThat(stackStraceAsString).doesNotContain(element);
	}

	// -------------------------------------------------------------------

	static class StackTracePruningTestCase {

		@Test
		void failingAssertion() {
			Assertions.fail();
		}

		@Test
		void multipleFailingAssertions() {
			Assertions.assertAll(Assertions::fail, Assertions::fail);
		}

		@Test
		void failingAssumption() {
			Assumptions.assumeTrue(() -> {
				throw new RuntimeException();
			});
		}

	}

}
