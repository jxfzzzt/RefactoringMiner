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

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.data.UdtValue;
import com.datastax.oss.driver.api.core.detach.AttachmentPoint;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.UserDefinedType;
import com.datastax.oss.driver.api.core.type.codec.PrimitiveIntCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.core.type.codec.registry.CodecRegistry;
import com.datastax.oss.driver.internal.core.type.DefaultUserDefinedType;
import com.datastax.oss.protocol.internal.util.Bytes;
import com.google.common.collect.ImmutableList;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UdtCodecTest extends CodecTestBase<UdtValue> {

  @Mock private AttachmentPoint attachmentPoint;
  @Mock private CodecRegistry codecRegistry;
  private PrimitiveIntCodec intCodec;
  private TypeCodec<Double> doubleCodec;
  private TypeCodec<String> textCodec;

  private UserDefinedType userType;

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);

    Mockito.when(attachmentPoint.codecRegistry()).thenReturn(codecRegistry);
    Mockito.when(attachmentPoint.protocolVersion()).thenReturn(ProtocolVersion.DEFAULT);

    intCodec = Mockito.spy(TypeCodecs.INT);
    doubleCodec = Mockito.spy(TypeCodecs.DOUBLE);
    textCodec = Mockito.spy(TypeCodecs.TEXT);

    // Called by the getters/setters
    Mockito.when(codecRegistry.codecFor(DataTypes.INT, Integer.class)).thenAnswer(i -> intCodec);
    Mockito.when(codecRegistry.codecFor(DataTypes.DOUBLE, Double.class))
        .thenAnswer(i -> doubleCodec);
    Mockito.when(codecRegistry.codecFor(DataTypes.TEXT, String.class)).thenAnswer(i -> textCodec);

    // Called by format/parse
    Mockito.when(codecRegistry.codecFor(DataTypes.INT)).thenAnswer(i -> intCodec);
    Mockito.when(codecRegistry.codecFor(DataTypes.DOUBLE)).thenAnswer(i -> doubleCodec);
    Mockito.when(codecRegistry.codecFor(DataTypes.TEXT)).thenAnswer(i -> textCodec);

    userType =
        new DefaultUserDefinedType(
            CqlIdentifier.fromInternal("ks"),
            CqlIdentifier.fromInternal("type"),
            ImmutableList.of(
                CqlIdentifier.fromInternal("field1"),
                CqlIdentifier.fromInternal("field2"),
                CqlIdentifier.fromInternal("field3")),
            ImmutableList.of(DataTypes.INT, DataTypes.DOUBLE, DataTypes.TEXT),
            attachmentPoint);

    codec = TypeCodecs.udtOf(userType);
  }

  @Test
  public void should_encode_null_udt() {
    assertThat(encode(null)).isNull();
  }

  @Test
  public void should_encode_udt() {
    UdtValue udt = userType.newValue();
    udt.setInt("field1", 1);
    udt.setToNull("field2");
    udt.setString("field3", "a");

    assertThat(encode(udt))
        .isEqualTo(
            "0x"
                + ("00000004" + "00000001") // size and contents of field 0
                + "ffffffff" // null field 1
                + ("00000001" + "61") // size and contents of field 2
            );

    Mockito.verify(intCodec).encodePrimitive(1, ProtocolVersion.DEFAULT);
    // null values are handled directly in the udt codec, without calling the child codec:
    Mockito.verifyZeroInteractions(doubleCodec);
    Mockito.verify(textCodec).encode("a", ProtocolVersion.DEFAULT);
  }

  @Test
  public void should_decode_null_udt() {
    assertThat(decode(null)).isNull();
  }

  @Test
  public void should_decode_udt() {
    UdtValue udt = decode("0x" + ("00000004" + "00000001") + "ffffffff" + ("00000001" + "61"));

    assertThat(udt.getInt(0)).isEqualTo(1);
    assertThat(udt.isNull(1)).isTrue();
    assertThat(udt.getString(2)).isEqualTo("a");

    Mockito.verify(intCodec)
        .decodePrimitive(Bytes.fromHexString("0x00000001"), ProtocolVersion.DEFAULT);
    Mockito.verifyZeroInteractions(doubleCodec);
    Mockito.verify(textCodec).decode(Bytes.fromHexString("0x61"), ProtocolVersion.DEFAULT);
  }

  @Test
  public void should_format_null_udt() {
    assertThat(format(null)).isEqualTo("NULL");
  }

  @Test
  public void should_format_udt() {
    UdtValue udt = userType.newValue();
    udt.setInt(0, 1);
    udt.setToNull(1);
    udt.setString(2, "a");

    assertThat(format(udt)).isEqualTo("{field1:1,field2:NULL,field3:'a'}");

    Mockito.verify(intCodec).format(1);
    Mockito.verify(doubleCodec).format(null);
    Mockito.verify(textCodec).format("a");
  }

  @Test
  public void should_parse_null_udt() {
    assertThat(parse(null)).isNull();
    assertThat(parse("null")).isNull();
    assertThat(parse("NULL")).isNull();
  }

  @Test
  public void should_parse_udt() {
    UdtValue udt = parse("{field1:1,field2:NULL,field3:'a'}");

    assertThat(udt.getInt(0)).isEqualTo(1);
    assertThat(udt.isNull(1));
    assertThat(udt.getString(2)).isEqualTo("a");

    Mockito.verify(intCodec).parse("1");
    Mockito.verify(doubleCodec).parse("NULL");
    Mockito.verify(textCodec).parse("'a'");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void should_fail_to_parse_invalid_input() {
    parse("not a udt");
  }
}
