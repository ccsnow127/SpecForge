"""Tests for tradingpatterns.py"""

import pytest
import pandas as pd
import numpy as np
from tradingpatterns import (
    detect_head_shoulder,
    detect_multiple_tops_bottoms,
    calculate_support_resistance,
    detect_triangle_pattern,
    detect_wedge,
    detect_channel,
    detect_double_top_bottom,
    detect_trendline,
    find_pivots,
)


def make_ohlc_df(n=15, seed=42):
    """Create a sample OHLC DataFrame with uppercase columns."""
    rng = np.random.RandomState(seed)
    close = 100 + np.cumsum(rng.randn(n))
    high = close + rng.uniform(0.5, 2.0, n)
    low = close - rng.uniform(0.5, 2.0, n)
    open_ = close + rng.randn(n) * 0.5
    return pd.DataFrame({
        'Open': open_,
        'High': high,
        'Low': low,
        'Close': close,
    })


def make_ohlc_lowercase_df(n=15, seed=42):
    """Create a sample OHLC DataFrame with lowercase columns for find_pivots."""
    rng = np.random.RandomState(seed)
    close = 100 + np.cumsum(rng.randn(n))
    high = close + rng.uniform(0.5, 2.0, n)
    low = close - rng.uniform(0.5, 2.0, n)
    open_ = close + rng.randn(n) * 0.5
    return pd.DataFrame({
        'open': open_,
        'high': high,
        'low': low,
        'close': close,
    })


# === detect_head_shoulder tests ===

def test_detect_head_shoulder_returns_dataframe():
    df = make_ohlc_df()
    result = detect_head_shoulder(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_detect_head_shoulder_adds_columns():
    df = make_ohlc_df()
    result = detect_head_shoulder(df.copy())
    assert 'head_shoulder_pattern' in result.columns
    assert 'high_roll_max' in result.columns
    assert 'low_roll_min' in result.columns

def test_detect_head_shoulder_pattern_values():
    df = make_ohlc_df(n=20)
    result = detect_head_shoulder(df.copy())
    valid_values = {'Head and Shoulder', 'Inverse Head and Shoulder'}
    non_null = result['head_shoulder_pattern'].dropna().unique()
    for val in non_null:
        assert val in valid_values


# === detect_multiple_tops_bottoms tests ===

def test_detect_multiple_tops_bottoms_returns_dataframe():
    df = make_ohlc_df()
    result = detect_multiple_tops_bottoms(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_detect_multiple_tops_bottoms_adds_columns():
    df = make_ohlc_df()
    result = detect_multiple_tops_bottoms(df.copy())
    assert 'multiple_top_bottom_pattern' in result.columns
    assert 'high_roll_max' in result.columns
    assert 'low_roll_min' in result.columns
    assert 'close_roll_max' in result.columns
    assert 'close_roll_min' in result.columns

def test_detect_multiple_tops_bottoms_pattern_values():
    df = make_ohlc_df(n=20)
    result = detect_multiple_tops_bottoms(df.copy())
    valid_values = {'Multiple Top', 'Multiple Bottom'}
    non_null = result['multiple_top_bottom_pattern'].dropna().unique()
    for val in non_null:
        assert val in valid_values


# === calculate_support_resistance tests ===

def test_calculate_support_resistance_returns_dataframe():
    df = make_ohlc_df()
    result = calculate_support_resistance(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_calculate_support_resistance_adds_columns():
    df = make_ohlc_df()
    result = calculate_support_resistance(df.copy())
    assert 'support' in result.columns
    assert 'resistance' in result.columns
    assert 'high_roll_max' in result.columns
    assert 'low_roll_min' in result.columns

def test_calculate_support_resistance_values_are_numeric():
    df = make_ohlc_df(n=15)
    result = calculate_support_resistance(df.copy(), window=3)
    # After the rolling window warm-up, values should be numeric (not all NaN)
    support_valid = result['support'].dropna()
    resistance_valid = result['resistance'].dropna()
    assert len(support_valid) > 0
    assert len(resistance_valid) > 0


# === detect_triangle_pattern tests ===

def test_detect_triangle_pattern_returns_dataframe():
    df = make_ohlc_df()
    result = detect_triangle_pattern(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_detect_triangle_pattern_adds_columns():
    df = make_ohlc_df()
    result = detect_triangle_pattern(df.copy())
    assert 'triangle_pattern' in result.columns
    assert 'high_roll_max' in result.columns
    assert 'low_roll_min' in result.columns

def test_detect_triangle_pattern_values():
    df = make_ohlc_df(n=20)
    result = detect_triangle_pattern(df.copy())
    valid_values = {'Ascending Triangle', 'Descending Triangle'}
    non_null = result['triangle_pattern'].dropna().unique()
    for val in non_null:
        assert val in valid_values


# === detect_wedge tests ===

def test_detect_wedge_returns_dataframe():
    df = make_ohlc_df()
    result = detect_wedge(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_detect_wedge_adds_columns():
    df = make_ohlc_df()
    result = detect_wedge(df.copy())
    assert 'wedge_pattern' in result.columns
    assert 'high_roll_max' in result.columns
    assert 'low_roll_min' in result.columns
    assert 'trend_high' in result.columns
    assert 'trend_low' in result.columns

def test_detect_wedge_pattern_values():
    df = make_ohlc_df(n=20)
    result = detect_wedge(df.copy())
    valid_values = {'Wedge Up', 'Wedge Down'}
    non_null = result['wedge_pattern'].dropna().unique()
    for val in non_null:
        assert val in valid_values


# === detect_channel tests ===

def test_detect_channel_returns_dataframe():
    df = make_ohlc_df()
    result = detect_channel(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_detect_channel_adds_columns():
    df = make_ohlc_df()
    result = detect_channel(df.copy())
    assert 'channel_pattern' in result.columns
    assert 'high_roll_max' in result.columns
    assert 'low_roll_min' in result.columns
    assert 'trend_high' in result.columns
    assert 'trend_low' in result.columns

def test_detect_channel_pattern_values():
    df = make_ohlc_df(n=20)
    result = detect_channel(df.copy())
    valid_values = {'Channel Up', 'Channel Down'}
    non_null = result['channel_pattern'].dropna().unique()
    for val in non_null:
        assert val in valid_values


# === detect_double_top_bottom tests ===

def test_detect_double_top_bottom_returns_dataframe():
    df = make_ohlc_df()
    result = detect_double_top_bottom(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_detect_double_top_bottom_adds_columns():
    df = make_ohlc_df()
    result = detect_double_top_bottom(df.copy())
    assert 'double_pattern' in result.columns
    assert 'high_roll_max' in result.columns
    assert 'low_roll_min' in result.columns

def test_detect_double_top_bottom_pattern_values():
    df = make_ohlc_df(n=20)
    result = detect_double_top_bottom(df.copy())
    valid_values = {'Double Top', 'Double Bottom'}
    non_null = result['double_pattern'].dropna().unique()
    for val in non_null:
        assert val in valid_values


# === detect_trendline tests ===

def test_detect_trendline_returns_dataframe():
    df = make_ohlc_df()
    result = detect_trendline(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_detect_trendline_adds_columns():
    df = make_ohlc_df()
    result = detect_trendline(df.copy())
    assert 'slope' in result.columns
    assert 'intercept' in result.columns
    assert 'support' in result.columns
    assert 'resistance' in result.columns

def test_detect_trendline_slope_computed():
    df = make_ohlc_df(n=15)
    result = detect_trendline(df.copy(), window=2)
    # After the window warm-up, slope should have numeric values
    slope_valid = result['slope'].dropna()
    assert len(slope_valid) > 0


# === find_pivots tests ===

def test_find_pivots_returns_dataframe():
    df = make_ohlc_lowercase_df()
    result = find_pivots(df.copy())
    assert isinstance(result, pd.DataFrame)

def test_find_pivots_adds_signal_column():
    df = make_ohlc_lowercase_df()
    result = find_pivots(df.copy())
    assert 'signal' in result.columns

def test_find_pivots_signal_values():
    df = make_ohlc_lowercase_df(n=20)
    result = find_pivots(df.copy())
    valid_values = {'HH', 'LL', 'LH', 'HL', ''}
    for val in result['signal'].unique():
        assert val in valid_values
