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
import java.util.UUID;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeUuidCodecTest extends CodecTestBase<UUID> {

  public static final UUID TIME_BASED = new UUID(6342305776366260711L, -5736720392086604862L);
  public static final UUID NOT_TIME_BASED = new UUID(2, 1);

  public TimeUuidCodecTest() {
    this.codec = TypeCodecs.TIMEUUID;

    assertThat(TIME_BASED.version()).isEqualTo(1);
    assertThat(NOT_TIME_BASED.version()).isNotEqualTo(1);
  }

  @Test
  public void should_encode_time_uuid() {
    assertThat(encode(TIME_BASED)).isEqualTo("0x58046580293811e7b0631332a5f033c2");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void should_not_encode_non_time_uuid() {
    assertThat(codec.canEncode(NOT_TIME_BASED)).isFalse();
    encode(NOT_TIME_BASED);
  }

  @Test
  public void should_format_time_uuid() {
    assertThat(format(TIME_BASED)).isEqualTo("58046580-2938-11e7-b063-1332a5f033c2");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void should_not_format_non_time_uuid() {
    format(NOT_TIME_BASED);
  }
}
