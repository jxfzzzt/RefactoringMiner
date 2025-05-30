/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.measure.index;

import static com.opengamma.strata.basics.index.IborIndices.GBP_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_3M;
import static com.opengamma.strata.basics.index.IborIndices.USD_LIBOR_6M;
import static com.opengamma.strata.collect.TestHelper.assertSerialization;
import static com.opengamma.strata.collect.TestHelper.coverBeanEquals;
import static com.opengamma.strata.collect.TestHelper.coverImmutableBean;
import static com.opengamma.strata.collect.TestHelper.date;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.calc.runner.FunctionRequirements;
import com.opengamma.strata.data.MarketData;
import com.opengamma.strata.data.scenario.ScenarioMarketData;
import com.opengamma.strata.measure.curve.TestMarketDataMap;
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilities;
import com.opengamma.strata.pricer.index.IborFutureOptionVolatilitiesId;

/**
 * Test {@link IborFutureOptionMarketDataLookup}.
 */
public class IborFutureOptionMarketDataLookupTest {

  private static final IborFutureOptionVolatilitiesId VOL_ID1 = IborFutureOptionVolatilitiesId.of("USD1");
  private static final IborFutureOptionVolatilities MOCK_VOLS = mock(IborFutureOptionVolatilities.class);
  private static final MarketData MOCK_MARKET_DATA = mock(MarketData.class);
  private static final ScenarioMarketData MOCK_CALC_MARKET_DATA = mock(ScenarioMarketData.class);

  static {
    when(MOCK_MARKET_DATA.getValue(VOL_ID1)).thenReturn(MOCK_VOLS);
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_of_single() {
    IborFutureOptionMarketDataLookup test = IborFutureOptionMarketDataLookup.of(USD_LIBOR_3M, VOL_ID1);
    assertThat(test.queryType()).isEqualTo(IborFutureOptionMarketDataLookup.class);
    assertThat(test.getVolatilityIndices()).containsOnly(USD_LIBOR_3M);
    assertThat(test.getVolatilityIds(USD_LIBOR_3M)).containsOnly(VOL_ID1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getVolatilityIds(GBP_LIBOR_3M));

    assertThat(test.requirements(USD_LIBOR_3M)).isEqualTo(FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThat(test.requirements(ImmutableSet.of(USD_LIBOR_3M))).isEqualTo(FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.requirements(ImmutableSet.of(GBP_LIBOR_3M)));
  }

  @Test
  public void test_of_map() {
    ImmutableMap<IborIndex, IborFutureOptionVolatilitiesId> ids =
        ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1);
    IborFutureOptionMarketDataLookup test = IborFutureOptionMarketDataLookup.of(ids);
    assertThat(test.queryType()).isEqualTo(IborFutureOptionMarketDataLookup.class);
    assertThat(test.getVolatilityIndices()).containsOnly(USD_LIBOR_3M, USD_LIBOR_6M);
    assertThat(test.getVolatilityIds(USD_LIBOR_3M)).containsOnly(VOL_ID1);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.getVolatilityIds(GBP_LIBOR_3M));

    assertThat(test.requirements(USD_LIBOR_3M)).isEqualTo(FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThat(test.requirements(ImmutableSet.of(USD_LIBOR_3M))).isEqualTo(FunctionRequirements.builder().valueRequirements(VOL_ID1).build());
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.requirements(ImmutableSet.of(GBP_LIBOR_3M)));

    assertThat(test.volatilities(USD_LIBOR_3M, MOCK_MARKET_DATA)).isEqualTo(MOCK_VOLS);
    assertThatIllegalArgumentException()
        .isThrownBy(() -> test.volatilities(GBP_LIBOR_3M, MOCK_MARKET_DATA));
  }

  //-------------------------------------------------------------------------
  @Test
  public void test_marketDataView() {
    IborFutureOptionMarketDataLookup test = IborFutureOptionMarketDataLookup.of(USD_LIBOR_3M, VOL_ID1);
    LocalDate valDate = date(2015, 6, 30);
    ScenarioMarketData md = new TestMarketDataMap(valDate, ImmutableMap.of(), ImmutableMap.of());
    IborFutureOptionScenarioMarketData multiScenario = test.marketDataView(md);
    assertThat(multiScenario.getLookup()).isEqualTo(test);
    assertThat(multiScenario.getMarketData()).isEqualTo(md);
    assertThat(multiScenario.getScenarioCount()).isEqualTo(1);
    IborFutureOptionMarketData scenario = multiScenario.scenario(0);
    assertThat(scenario.getLookup()).isEqualTo(test);
    assertThat(scenario.getMarketData()).isEqualTo(md.scenario(0));
    assertThat(scenario.getValuationDate()).isEqualTo(valDate);
  }

  //-------------------------------------------------------------------------
  @Test
  public void coverage() {
    DefaultIborFutureOptionMarketDataLookup test =
        DefaultIborFutureOptionMarketDataLookup.of(ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1));
    coverImmutableBean(test);
    DefaultIborFutureOptionMarketDataLookup test2 = DefaultIborFutureOptionMarketDataLookup.of(USD_LIBOR_3M, VOL_ID1);
    coverBeanEquals(test, test2);

    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_CALC_MARKET_DATA));
    coverImmutableBean((ImmutableBean) test.marketDataView(MOCK_MARKET_DATA));
  }

  @Test
  public void test_serialization() {
    DefaultIborFutureOptionMarketDataLookup test =
        DefaultIborFutureOptionMarketDataLookup.of(ImmutableMap.of(USD_LIBOR_3M, VOL_ID1, USD_LIBOR_6M, VOL_ID1));
    assertSerialization(test);
  }

}
