/*
 * Copyright 2015-2023 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.params.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.lang.Thread.State;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DefaultArgumentConverter}.
 *
 * @since 5.0
 */
class DefaultArgumentConverterTests {

	@Test
	void isAwareOfNull() {
		assertConverts(null, Object.class, null);
		assertConverts(null, String.class, null);
	}

	@Test
	void isAwareOfWrapperTypesForPrimitiveTypes() {
		assertConverts(true, boolean.class, true);
		assertConverts((byte) 1, byte.class, (byte) 1);
		assertConverts('o', char.class, 'o');
		assertConverts((short) 1, short.class, (short) 1);
		assertConverts(1, int.class, 1);
		assertConverts(1L, long.class, 1L);
		assertConverts(1.0f, float.class, 1.0f);
		assertConverts(1.0d, double.class, 1.0d);
	}

	@Test
	void isAwareOfWideningConversions() {
		assertConverts((byte) 1, short.class, (byte) 1);
		assertConverts((short) 1, int.class, (short) 1);
		assertConverts((char) 1, int.class, (char) 1);
		assertConverts((byte) 1, long.class, (byte) 1);
		assertConverts(1, long.class, 1);
		assertConverts((char) 1, float.class, (char) 1);
		assertConverts(1, float.class, 1);
		assertConverts(1L, double.class, 1L);
		assertConverts(1.0f, double.class, 1.0f);
	}

	@Test
	void convertsStringsToPrimitiveTypes() {
		assertConverts("true", boolean.class, true);
		assertConverts("o", char.class, 'o');
		assertConverts("1", byte.class, (byte) 1);
		assertConverts("1_0", byte.class, (byte) 10);
		assertConverts("1", short.class, (short) 1);
		assertConverts("1_2", short.class, (short) 12);
		assertConverts("42", int.class, 42);
		assertConverts("700_050_000", int.class, 700_050_000);
		assertConverts("42", long.class, 42L);
		assertConverts("4_2", long.class, 42L);
		assertConverts("42.23", float.class, 42.23f);
		assertConverts("42.2_3", float.class, 42.23f);
		assertConverts("42.23", double.class, 42.23);
		assertConverts("42.2_3", double.class, 42.23);
	}

	@Test
	void throwsExceptionOnInvalidStringForPrimitiveTypes() {
		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert("ab", char.class)) //
				.withMessage("Failed to convert String \"ab\" to type char") //
				.havingCause() //
				.withMessage("String must have length of 1: ab");

		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert("tru", boolean.class)) //
				.withMessage("Failed to convert String \"tru\" to type boolean") //
				.havingCause() //
				.withMessage("String must be 'true' or 'false' (ignoring case): tru");
	}

	@Test
	void throwsExceptionWhenImplicitConverstionIsUnsupported() {
		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert("foo", Enigma.class)) //
				.withMessage("No built-in converter for source type java.lang.String and target type %s",
					Enigma.class.getName());

		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert(new Enigma(), int[].class)) //
				.withMessage("No built-in converter for source type %s and target type int[]", Enigma.class.getName());

		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert(new long[] {}, int[].class)) //
				.withMessage("No built-in converter for source type long[] and target type int[]");

		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert(new String[] {}, boolean.class)) //
				.withMessage("No built-in converter for source type java.lang.String[] and target type boolean");

		assertThatExceptionOfType(ArgumentConversionException.class) //
				.isThrownBy(() -> convert(Class.class, int[].class)) //
				.withMessage("No built-in converter for source type java.lang.Class and target type int[]");
	}

	/**
	 * @since 5.4
	 */
	@Test
	@SuppressWarnings("OctalInteger") // We test parsing octal integers here as well as hex.
	void convertsEncodedStringsToIntegralTypes() {
		assertConverts("0x1f", byte.class, (byte) 0x1F);
		assertConverts("-0x1F", byte.class, (byte) -0x1F);
		assertConverts("010", byte.class, (byte) 010);

		assertConverts("0x1f00", short.class, (short) 0x1F00);
		assertConverts("-0x1F00", short.class, (short) -0x1F00);
		assertConverts("01000", short.class, (short) 01000);

		assertConverts("0x1f000000", int.class, 0x1F000000);
		assertConverts("-0x1F000000", int.class, -0x1F000000);
		assertConverts("010000000", int.class, 010000000);

		assertConverts("0x1f000000000", long.class, 0x1F000000000L);
		assertConverts("-0x1F000000000", long.class, -0x1F000000000L);
		assertConverts("0100000000000", long.class, 0100000000000L);
	}

	@Test
	void convertsStringsToEnumConstants() {
		assertConverts("DAYS", TimeUnit.class, TimeUnit.DAYS);
	}

	// --- java.io and java.nio ------------------------------------------------

	@Test
	void convertsStringToCharset() {
		assertConverts("ISO-8859-1", Charset.class, StandardCharsets.ISO_8859_1);
		assertConverts("UTF-8", Charset.class, StandardCharsets.UTF_8);
	}

	@Test
	void convertsStringToFile() {
		assertConverts("file", File.class, new File("file"));
		assertConverts("/file", File.class, new File("/file"));
		assertConverts("/some/file", File.class, new File("/some/file"));
	}

	@Test
	void convertsStringToPath() {
		assertConverts("path", Path.class, Paths.get("path"));
		assertConverts("/path", Path.class, Paths.get("/path"));
		assertConverts("/some/path", Path.class, Paths.get("/some/path"));
	}

	// --- java.lang -----------------------------------------------------------

	@Test
	void convertsStringToClass() {
		assertConverts("java.lang.Integer", Class.class, Integer.class);
		assertConverts("java.lang.Thread$State", Class.class, State.class);
		assertConverts("byte", Class.class, byte.class);
		assertConverts("char[]", Class.class, char[].class);
		assertConverts("java.lang.Long[][]", Class.class, Long[][].class);
		assertConverts("[[[I", Class.class, int[][][].class);
		assertConverts("[[Ljava.lang.String;", Class.class, String[][].class);
	}

	// --- java.math -----------------------------------------------------------

	@Test
	void convertsStringToBigDecimal() {
		assertConverts("123.456e789", BigDecimal.class, new BigDecimal("123.456e789"));
	}

	@Test
	void convertsStringToBigInteger() {
		assertConverts("1234567890123456789", BigInteger.class, new BigInteger("1234567890123456789"));
	}

	// --- java.net ------------------------------------------------------------

	@Test
	void convertsStringToURI() {
		assertConverts("https://docs.oracle.com/en/java/javase/12/", URI.class,
			URI.create("https://docs.oracle.com/en/java/javase/12/"));
	}

	@Test
	void convertsStringToURL() throws Exception {
		assertConverts("https://junit.org/junit5", URL.class, URI.create("https://junit.org/junit5").toURL());
	}

	// --- java.time -----------------------------------------------------------

	@Test
	void convertsStringsToJavaTimeInstances() {
		assertConverts("PT1234.5678S", Duration.class, Duration.ofSeconds(1234, 567800000));
		assertConverts("1970-01-01T00:00:00Z", Instant.class, Instant.ofEpochMilli(0));
		assertConverts("2017-03-14", LocalDate.class, LocalDate.of(2017, 3, 14));
		assertConverts("2017-03-14T12:34:56.789", LocalDateTime.class,
			LocalDateTime.of(2017, 3, 14, 12, 34, 56, 789_000_000));
		assertConverts("12:34:56.789", LocalTime.class, LocalTime.of(12, 34, 56, 789_000_000));
		assertConverts("--03-14", MonthDay.class, MonthDay.of(3, 14));
		assertConverts("2017-03-14T12:34:56.789Z", OffsetDateTime.class,
			OffsetDateTime.of(2017, 3, 14, 12, 34, 56, 789_000_000, ZoneOffset.UTC));
		assertConverts("12:34:56.789Z", OffsetTime.class, OffsetTime.of(12, 34, 56, 789_000_000, ZoneOffset.UTC));
		assertConverts("P2M6D", Period.class, Period.of(0, 2, 6));
		assertConverts("2017", Year.class, Year.of(2017));
		assertConverts("2017-03", YearMonth.class, YearMonth.of(2017, 3));
		assertConverts("2017-03-14T12:34:56.789Z", ZonedDateTime.class,
			ZonedDateTime.of(2017, 3, 14, 12, 34, 56, 789_000_000, ZoneOffset.UTC));
		assertConverts("Europe/Berlin", ZoneId.class, ZoneId.of("Europe/Berlin"));
		assertConverts("+02:30", ZoneOffset.class, ZoneOffset.ofHoursMinutes(2, 30));
	}

	// --- java.util -----------------------------------------------------------

	@Test
	void convertsStringToCurrency() {
		assertConverts("JPY", Currency.class, Currency.getInstance("JPY"));
	}

	@Test
	void convertsStringToLocale() {
		assertConverts("en", Locale.class, Locale.ENGLISH);
		assertConverts("en_us", Locale.class, new Locale(Locale.US.toString()));
	}

	@Test
	void convertsStringToUUID() {
		var uuid = "d043e930-7b3b-48e3-bdbe-5a3ccfb833db";
		assertConverts(uuid, UUID.class, UUID.fromString(uuid));
	}

	// -------------------------------------------------------------------------

	private void assertConverts(Object input, Class<?> targetClass, Object expectedOutput) {
		var result = convert(input, targetClass);

		assertThat(result) //
				.describedAs(input + " --(" + targetClass.getName() + ")--> " + expectedOutput) //
				.isEqualTo(expectedOutput);
	}

	private Object convert(Object input, Class<?> targetClass) {
		return DefaultArgumentConverter.INSTANCE.convert(input, targetClass);
	}

	private static class Enigma {
	}

}
