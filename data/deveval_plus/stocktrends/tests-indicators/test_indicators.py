"""Tests for indicators.py"""

import pytest
import pandas as pd
from datetime import datetime
from indicators import Instrument, Renko, LineBreak, PnF


@pytest.fixture
def sample_df():
    """Sample OHLC DataFrame with trend reversals for testing algorithms."""
    return pd.DataFrame({
        'date': [datetime(2023, 1, i+1) for i in range(6)],
        'open':  [100.0, 101.0, 104.0, 107.0, 94.0, 88.0],
        'high':  [102.0, 105.0, 108.0, 108.0, 95.0, 115.0],
        'low':   [99.0, 100.0, 103.0, 92.0, 85.0, 87.0],
        'close': [101.0, 104.0, 107.0, 94.0, 88.0, 112.0],
    })


@pytest.fixture
def renko(sample_df):
    """Renko instance for testing"""
    return Renko(sample_df)


@pytest.fixture
def line_break(sample_df):
    """LineBreak instance for testing"""
    return LineBreak(sample_df)


@pytest.fixture
def pnf(sample_df):
    """PnF instance for testing"""
    return PnF(sample_df)


# === Instrument class tests ===

def test_Instrument_constants():
    df = pd.DataFrame({'date': [1], 'open': [1], 'high': [2], 'low': [0], 'close': [1]})
    inst = Instrument(df)
    assert inst.UPTREND_CONTINUAL == 0
    assert inst.UPTREND_REVERSAL == 1
    assert inst.DOWNTREND_CONTINUAL == 2
    assert inst.DOWNTREND_REVERSAL == 3

def test_Instrument_ohlc_set():
    df = pd.DataFrame({'date': [1], 'open': [1], 'high': [2], 'low': [0], 'close': [1]})
    inst = Instrument(df)
    assert inst.ohlc == {'open', 'high', 'low', 'close'}


# === Instrument.__init__ tests ===

def test_Instrument___init___valid(sample_df):
    inst = Instrument(sample_df)
    assert len(inst.df) == 6
    assert set(inst.df.columns) == {'date', 'open', 'high', 'low', 'close'}

def test_Instrument___init___stores_copy(sample_df):
    inst = Instrument(sample_df)
    sample_df.iloc[0, sample_df.columns.get_loc('close')] = 999.0
    assert inst.df.iloc[0]['close'] == 101.0

def test_Instrument___init___missing_columns():
    with pytest.raises(ValueError):
        Instrument(pd.DataFrame({'open': [1], 'high': [2]}))

def test_Instrument___init___stores_odf(sample_df):
    inst = Instrument(sample_df)
    assert hasattr(inst, 'odf')
    assert inst.odf.equals(inst.df)
    assert inst.odf is not inst.df


# === Instrument._validate_df tests ===

def test_Instrument__validate_df_partial_columns():
    with pytest.raises(ValueError):
        Instrument(pd.DataFrame({'open': [1], 'high': [2], 'low': [0]}))

def test_Instrument__validate_df_extra_columns_ok(sample_df):
    sample_df['volume'] = [100, 200, 300, 400, 500, 600]
    inst = Instrument(sample_df)
    assert len(inst.df) == 6


# === Renko class tests ===

def test_Renko_inherits_and_defaults(sample_df):
    r = Renko(sample_df)
    assert isinstance(r, Instrument)
    assert r.brick_size == 1
    assert r.chart_type == r.PERIOD_CLOSE
    assert r.PERIOD_CLOSE == 1
    assert r.PRICE_MOVEMENT == 2


# === Renko.period_close_bricks tests ===

def test_Renko_period_close_bricks(renko):
    result = renko.period_close_bricks()
    assert len(result) == 48
    assert list(result.columns) == ['date', 'open', 'high', 'low', 'close', 'uptrend']
    assert result.iloc[0]['open'] == 100.0
    assert result.iloc[0]['close'] == 101.0

def test_Renko_period_close_bricks_has_reversals(renko):
    result = renko.period_close_bricks()
    assert True in result['uptrend'].values
    assert False in result['uptrend'].values

def test_Renko_period_close_bricks_first_reversal(renko):
    result = renko.period_close_bricks()
    for i in range(1, len(result)):
        if result.iloc[i]['uptrend'] != result.iloc[i-1]['uptrend']:
            assert i == 7
            assert result.iloc[i]['uptrend'] == False
            assert result.iloc[i]['open'] == 106.0
            assert result.iloc[i]['close'] == 105.0
            break

def test_Renko_period_close_bricks_updown_counts(renko):
    result = renko.period_close_bricks()
    up_count = result['uptrend'].sum()
    down_count = len(result) - up_count
    assert up_count == 30
    assert down_count == 18

def test_Renko_period_close_bricks_last_brick(renko):
    result = renko.period_close_bricks()
    assert result.iloc[-1]['open'] == 111.0
    assert result.iloc[-1]['close'] == 112.0
    assert result.iloc[-1]['uptrend'] == True


# === Renko.price_movement_bricks tests ===

def test_Renko_price_movement_bricks(renko):
    renko.price_movement_bricks()
    assert len(renko.cdf) == 94
    assert True in renko.cdf['uptrend'].values
    assert False in renko.cdf['uptrend'].values

def test_Renko_price_movement_bricks_first_brick(renko):
    renko.price_movement_bricks()
    assert renko.cdf.iloc[0]['open'] == 100.0
    assert renko.cdf.iloc[0]['close'] == 101.0
    assert renko.cdf.iloc[0]['uptrend'] == True

def test_Renko_price_movement_bricks_updown_counts(renko):
    renko.price_movement_bricks()
    up_count = renko.cdf['uptrend'].sum()
    down_count = len(renko.cdf) - up_count
    assert up_count == 41
    assert down_count == 53

def test_Renko_price_movement_bricks_first_reversal(renko):
    renko.price_movement_bricks()
    for i in range(1, len(renko.cdf)):
        if renko.cdf.iloc[i]['uptrend'] != renko.cdf.iloc[i-1]['uptrend']:
            assert i == 5
            assert renko.cdf.iloc[i]['uptrend'] == False
            assert renko.cdf.iloc[i]['open'] == 104.0
            assert renko.cdf.iloc[i]['close'] == 103.0
            break


# === Renko.get_ohlc_data tests ===

def test_Renko_get_ohlc_data_period_close(renko):
    renko.chart_type = renko.PERIOD_CLOSE
    result = renko.get_ohlc_data()
    assert len(result) == 48

def test_Renko_get_ohlc_data_price_movement(renko):
    renko.chart_type = renko.PRICE_MOVEMENT
    result = renko.get_ohlc_data()
    assert len(result) == 94


# === LineBreak class tests ===

def test_LineBreak_inherits_and_defaults(sample_df):
    lb = LineBreak(sample_df)
    assert isinstance(lb, Instrument)
    assert lb.line_number == 3


# === LineBreak.uptrend_reversal tests ===

def test_LineBreak_uptrend_reversal_true(line_break):
    line_break.cdf = pd.DataFrame({'low': [1, 2, 3]})
    assert line_break.uptrend_reversal(0.5) == True

def test_LineBreak_uptrend_reversal_false(line_break):
    line_break.cdf = pd.DataFrame({'low': [1, 2, 3]})
    assert line_break.uptrend_reversal(2.5) == False

def test_LineBreak_uptrend_reversal_insufficient_rows(line_break):
    line_break.cdf = pd.DataFrame({'low': [1, 2]})
    assert line_break.uptrend_reversal(0.5) == False

def test_LineBreak_uptrend_reversal_boundary_equal(line_break):
    line_break.cdf = pd.DataFrame({'low': [1, 2, 3]})
    assert line_break.uptrend_reversal(1.0) == False

def test_LineBreak_uptrend_reversal_boundary_just_below(line_break):
    line_break.cdf = pd.DataFrame({'low': [1, 2, 3]})
    assert line_break.uptrend_reversal(0.99) == True


# === LineBreak.downtrend_reversal tests ===

def test_LineBreak_downtrend_reversal_true(line_break):
    line_break.cdf = pd.DataFrame({'high': [3, 4, 5]})
    assert line_break.downtrend_reversal(6) == True

def test_LineBreak_downtrend_reversal_false(line_break):
    line_break.cdf = pd.DataFrame({'high': [3, 4, 5]})
    assert line_break.downtrend_reversal(4) == False

def test_LineBreak_downtrend_reversal_insufficient_rows(line_break):
    line_break.cdf = pd.DataFrame({'high': [3, 4]})
    assert line_break.downtrend_reversal(10) == False

def test_LineBreak_downtrend_reversal_boundary_equal(line_break):
    line_break.cdf = pd.DataFrame({'high': [3, 4, 5]})
    assert line_break.downtrend_reversal(5) == False


# === LineBreak.get_ohlc_data tests ===

def test_LineBreak_get_ohlc_data(line_break):
    result = line_break.get_ohlc_data()
    assert len(result) == 6
    assert 'uptrend' in result.columns
    assert True in result['uptrend'].values
    assert False in result['uptrend'].values

def test_LineBreak_get_ohlc_data_initial_entries(line_break):
    result = line_break.get_ohlc_data()
    assert result.iloc[0]['open'] == 100.0
    assert result.iloc[0]['close'] == 101.0
    assert result.iloc[0]['uptrend'] == True
    assert result.iloc[2]['open'] == 104.0
    assert result.iloc[2]['close'] == 107.0

def test_LineBreak_get_ohlc_data_reversal_values(line_break):
    result = line_break.get_ohlc_data()
    assert result.iloc[3]['uptrend'] == False
    assert result.iloc[3]['open'] == 107.0
    assert result.iloc[3]['close'] == 94.0
    assert result.iloc[5]['uptrend'] == True
    assert result.iloc[5]['close'] == 112.0

def test_LineBreak_get_ohlc_data_short_data():
    short_df = pd.DataFrame({
        'date': [datetime(2023, 1, i+1) for i in range(2)],
        'open':  [100.0, 102.0],
        'high':  [103.0, 105.0],
        'low':   [99.0, 101.0],
        'close': [102.0, 104.0],
    })
    lb = LineBreak(short_df)
    result = lb.get_ohlc_data()
    assert len(result) == 2


# === PnF class tests ===

def test_PnF_inherits_and_defaults(sample_df):
    p = PnF(sample_df)
    assert isinstance(p, Instrument)
    assert p.box_size == 2
    assert p.reversal_size == 3


# === PnF.brick_size tests ===

def test_PnF_brick_size(pnf):
    assert pnf.brick_size == pnf.box_size
    assert pnf.brick_size == 2

def test_PnF_brick_size_follows_box_size(pnf):
    pnf.box_size = 5
    assert pnf.brick_size == 5


# === PnF.get_state tests ===

def test_PnF_get_state_uptrend_continual(pnf):
    assert pnf.get_state(True, 1) == pnf.UPTREND_CONTINUAL

def test_PnF_get_state_uptrend_reversal(pnf):
    assert pnf.get_state(True, -1) == pnf.UPTREND_REVERSAL

def test_PnF_get_state_downtrend_continual(pnf):
    assert pnf.get_state(False, -1) == pnf.DOWNTREND_CONTINUAL

def test_PnF_get_state_downtrend_reversal(pnf):
    assert pnf.get_state(False, 1) == pnf.DOWNTREND_REVERSAL

def test_PnF_get_state_zero_bricks(pnf):
    assert pnf.get_state(True, 0) is None

def test_PnF_get_state_large_bricks(pnf):
    assert pnf.get_state(True, 100) == pnf.UPTREND_CONTINUAL
    assert pnf.get_state(True, -100) == pnf.UPTREND_REVERSAL
    assert pnf.get_state(False, 100) == pnf.DOWNTREND_REVERSAL
    assert pnf.get_state(False, -100) == pnf.DOWNTREND_CONTINUAL


# === PnF.roundit tests ===

def test_PnF_roundit(pnf):
    assert pnf.roundit(12, 5) == 10
    assert pnf.roundit(13, 5) == 15
    assert pnf.roundit(10, 5) == 10

def test_PnF_roundit_negative(pnf):
    assert pnf.roundit(-3, 5) == -5
    assert pnf.roundit(0, 5) == 0

def test_PnF_roundit_different_base(pnf):
    assert pnf.roundit(6, 2) == 6
    assert pnf.roundit(7, 2) == 8


# === PnF.get_ohlc_data tests ===

def test_PnF_get_ohlc_data(pnf):
    result = pnf.get_ohlc_data()
    assert len(result) == 23
    assert list(result.columns) == ['date', 'open', 'high', 'low', 'close', 'uptrend']
    assert True in result['uptrend'].values
    assert False in result['uptrend'].values

def test_PnF_get_ohlc_data_first_box(pnf):
    result = pnf.get_ohlc_data()
    assert result.iloc[0]['open'] == 98
    assert result.iloc[0]['close'] == 100
    assert result.iloc[0]['uptrend'] == True

def test_PnF_get_ohlc_data_reversal_point(pnf):
    result = pnf.get_ohlc_data()
    assert result.iloc[3]['uptrend'] == True
    assert result.iloc[4]['uptrend'] == False
    assert result.iloc[4]['open'] == 104
    assert result.iloc[4]['close'] == 102

def test_PnF_get_ohlc_data_updown_counts(pnf):
    result = pnf.get_ohlc_data()
    up_count = result['uptrend'].sum()
    down_count = len(result) - up_count
    assert up_count == 15
    assert down_count == 8


# === PnF.get_bar_ohlc_data tests ===

def test_PnF_get_bar_ohlc_data(pnf):
    result = pnf.get_bar_ohlc_data()
    assert len(result) == 1
    assert 'open' in result.columns
    assert 'close' in result.columns

def test_PnF_get_bar_ohlc_data_values(pnf):
    result = pnf.get_bar_ohlc_data()
    assert result.iloc[0]['open'] == 100.0
    assert result.iloc[0]['close'] == 112
    assert result.iloc[0]['high'] == 112

def test_PnF_get_bar_ohlc_data_columns(pnf):
    result = pnf.get_bar_ohlc_data()
    assert 'date' in result.columns
    assert 'high' in result.columns
    assert 'low' in result.columns
