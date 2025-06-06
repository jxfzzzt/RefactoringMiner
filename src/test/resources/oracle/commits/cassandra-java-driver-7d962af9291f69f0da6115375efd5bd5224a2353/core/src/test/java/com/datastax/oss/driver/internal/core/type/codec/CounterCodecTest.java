/*
 * Copyright (C) 2017-2017 DataStax Inc.
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
package com.datastax.oss.driver.internal.core.type.codec;

import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CounterCodecTest extends CodecTestBase<Long> {

  public CounterCodecTest() {
    this.codec = TypeCodecs.COUNTER;
  }

  @Test
  public void should_encode() {
    assertThat(encode(1L)).isEqualTo("0x0000000000000001");
    assertThat(encode(null)).isNull();
  }

  @Test
  public void should_decode() {
    assertThat(decode("0x0000000000000001")).isEqualTo(1L);
    assertThat(decode("0x")).isNull();
    assertThat(decode(null)).isNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_decode_if_too_many_bytes() {
    decode("0x0000000000000000" + "0000");
  }

  @Test
  public void should_format() {
    assertThat(format(1L)).isEqualTo("1");
    assertThat(format(null)).isEqualTo("NULL");
  }

  @Test
  public void should_parse() {
    assertThat(parse("1")).isEqualTo(1L);
    assertThat(parse("NULL")).isNull();
    assertThat(parse("null")).isNull();
    assertThat(parse("")).isNull();
    assertThat(parse(null)).isNull();
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_parse_invalid_input() {
    parse("not a number");
  }

  @Test(expected = IllegalArgumentException.class)
  public void should_fail_to_parse_if_out_of_range() {
    parse(Long.toString(Long.MAX_VALUE) + "0");
  }
}
