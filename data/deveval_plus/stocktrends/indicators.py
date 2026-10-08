import pandas as pd
from typing import Optional

class Instrument:
    odf: pd.DataFrame
    df: pd.DataFrame
    ohlc: set = {'open', 'high', 'low', 'close'}
    UPTREND_CONTINUAL: int = 0
    UPTREND_REVERSAL: int = 1
    DOWNTREND_CONTINUAL: int = 2
    DOWNTREND_REVERSAL: int = 3

    def __init__(self, df: pd.DataFrame):
        self.odf = df.copy()
        self.df = df.copy()
        self._validate_df()

    def _validate_df(self) -> None:
        if not self.ohlc.issubset(self.df.columns):
            raise ValueError("DataFrame must contain 'open', 'high', 'low', 'close' columns")

class Renko(Instrument):
    cdf: pd.DataFrame
    PERIOD_CLOSE: int = 1
    PRICE_MOVEMENT: int = 2
    brick_size: int = 1
    chart_type: int = PERIOD_CLOSE

    def get_ohlc_data(self) -> pd.DataFrame:
        if self.chart_type == self.PERIOD_CLOSE:
            return self.period_close_bricks()
        else:
            self.price_movement_bricks()
            return self.cdf

    def price_movement_bricks(self) -> None:
        bricks = []
        first_close = self.df['close'].iloc[0]
        first_open = first_close // self.brick_size * self.brick_size - self.brick_size
        first_brick_close = first_open + self.brick_size
        bricks.append({'date': self.df['date'].iloc[0], 'open': first_open, 'high': first_brick_close, 'low': first_open, 'close': first_brick_close, 'uptrend': True})
        current_close = first_brick_close
        current_open = first_open
        uptrend = True
        for idx in range(1, len(self.df)):
            row = self.df.iloc[idx]
            high = row['high']
            low = row['low']
            date = row['date']
            if uptrend:
                while high >= current_close + self.brick_size:
                    new_open = current_close
                    new_close = new_open + self.brick_size
                    bricks.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                    current_open = new_open
                    current_close = new_close
                if low <= current_open - 2 * self.brick_size:
                    uptrend = False
                    new_open = current_open
                    new_close = new_open - self.brick_size
                    bricks.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                    current_open = new_open
                    current_close = new_close
                    while low <= current_close - self.brick_size:
                        new_open = current_close
                        new_close = new_open - self.brick_size
                        bricks.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                        current_open = new_open
                        current_close = new_close
            else:
                while low <= current_close - self.brick_size:
                    new_open = current_close
                    new_close = new_open - self.brick_size
                    bricks.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                    current_open = new_open
                    current_close = new_close
                if high >= current_open + 2 * self.brick_size:
                    uptrend = True
                    new_open = current_open
                    new_close = new_open + self.brick_size
                    bricks.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                    current_open = new_open
                    current_close = new_close
                    while high >= current_close + self.brick_size:
                        new_open = current_close
                        new_close = new_open + self.brick_size
                        bricks.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                        current_open = new_open
                        current_close = new_close
        self.cdf = pd.DataFrame(bricks, columns=['date', 'open', 'high', 'low', 'close', 'uptrend'])

    def period_close_bricks(self) -> pd.DataFrame:
        bricks = []
        first_close = self.df['close'].iloc[0]
        first_open = first_close // self.brick_size * self.brick_size - self.brick_size
        first_brick_close = first_open + self.brick_size
        bricks.append({'date': self.df['date'].iloc[0], 'open': first_open, 'high': first_brick_close, 'low': first_open, 'close': first_brick_close, 'uptrend': True})
        current_close = first_brick_close
        current_open = first_open
        uptrend = True
        for idx in range(1, len(self.df)):
            row = self.df.iloc[idx]
            close_price = row['close']
            date = row['date']
            if uptrend:
                while close_price >= current_close + self.brick_size:
                    new_open = current_close
                    new_close = new_open + self.brick_size
                    bricks.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                    current_open = new_open
                    current_close = new_close
                if close_price <= current_open - 2 * self.brick_size:
                    uptrend = False
                    new_open = current_open
                    new_close = new_open - self.brick_size
                    bricks.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                    current_open = new_open
                    current_close = new_close
                    while close_price <= current_close - self.brick_size:
                        new_open = current_close
                        new_close = new_open - self.brick_size
                        bricks.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                        current_open = new_open
                        current_close = new_close
            else:
                while close_price <= current_close - self.brick_size:
                    new_open = current_close
                    new_close = new_open - self.brick_size
                    bricks.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                    current_open = new_open
                    current_close = new_close
                if close_price >= current_open + 2 * self.brick_size:
                    uptrend = True
                    new_open = current_open
                    new_close = new_open + self.brick_size
                    bricks.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                    current_open = new_open
                    current_close = new_close
                    while close_price >= current_close + self.brick_size:
                        new_open = current_close
                        new_close = new_open + self.brick_size
                        bricks.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                        current_open = new_open
                        current_close = new_close
        self.cdf = pd.DataFrame(bricks, columns=['date', 'open', 'high', 'low', 'close', 'uptrend'])
        return self.cdf

class LineBreak(Instrument):
    cdf: pd.DataFrame
    line_number: int = 3

    def uptrend_reversal(self, close: float) -> bool:
        if len(self.cdf) < self.line_number:
            return False
        last_n = self.cdf.tail(self.line_number)
        return close < last_n['low'].min()

    def downtrend_reversal(self, close: float) -> bool:
        if len(self.cdf) < self.line_number:
            return False
        last_n = self.cdf.tail(self.line_number)
        return close > last_n['high'].max()

    def get_ohlc_data(self) -> pd.DataFrame:
        lines = []
        for idx in range(min(self.line_number, len(self.df))):
            row = self.df.iloc[idx]
            lines.append({'index': idx, 'date': row['date'], 'open': row['open'], 'high': row['high'], 'low': row['low'], 'close': row['close'], 'uptrend': True})
        self.cdf = pd.DataFrame(lines, columns=['index', 'date', 'open', 'high', 'low', 'close', 'uptrend'])
        if len(self.df) <= self.line_number:
            return self.cdf
        uptrend = self.cdf['close'].iloc[-1] >= self.cdf['open'].iloc[0]
        for idx in range(self.line_number, len(self.df)):
            row = self.df.iloc[idx]
            close_price = row['close']
            last_close = self.cdf['close'].iloc[-1]
            last_open = self.cdf['open'].iloc[-1]
            if uptrend:
                if close_price > last_close:
                    new_line = {'index': idx, 'date': row['date'], 'open': last_close, 'high': close_price, 'low': last_close, 'close': close_price, 'uptrend': True}
                    self.cdf = pd.concat([self.cdf, pd.DataFrame([new_line])], ignore_index=True)
                elif self.uptrend_reversal(close_price):
                    uptrend = False
                    last_n = self.cdf.tail(self.line_number)
                    low_val = last_n['low'].min()
                    new_line = {'index': idx, 'date': row['date'], 'open': last_close, 'high': last_close, 'low': close_price, 'close': close_price, 'uptrend': False}
                    self.cdf = pd.concat([self.cdf, pd.DataFrame([new_line])], ignore_index=True)
            elif close_price < last_close:
                new_line = {'index': idx, 'date': row['date'], 'open': last_close, 'high': last_close, 'low': close_price, 'close': close_price, 'uptrend': False}
                self.cdf = pd.concat([self.cdf, pd.DataFrame([new_line])], ignore_index=True)
            elif self.downtrend_reversal(close_price):
                uptrend = True
                last_n = self.cdf.tail(self.line_number)
                high_val = last_n['high'].max()
                new_line = {'index': idx, 'date': row['date'], 'open': last_close, 'high': close_price, 'low': last_close, 'close': close_price, 'uptrend': True}
                self.cdf = pd.concat([self.cdf, pd.DataFrame([new_line])], ignore_index=True)
        return self.cdf

class PnF(Instrument):
    cdf: pd.DataFrame
    box_size: int = 2
    reversal_size: int = 3

    @property
    def brick_size(self) -> int:
        return self.box_size

    def get_state(self, uptrend_p1: bool, bricks: int) -> Optional[int]:
        if bricks == 0:
            return None
        if uptrend_p1:
            if bricks > 0:
                return self.UPTREND_CONTINUAL
            else:
                return self.UPTREND_REVERSAL
        elif bricks < 0:
            return self.DOWNTREND_CONTINUAL
        else:
            return self.DOWNTREND_REVERSAL

    def roundit(self, x: float, base: int=5) -> int:
        return int(base * round(float(x) / base))

    def get_ohlc_data(self, source: str='close') -> pd.DataFrame:
        boxes = []
        first_value = self.df[source].iloc[0]
        first_open = self.roundit(first_value, self.box_size) - self.box_size
        first_close = first_open + self.box_size
        boxes.append({'date': self.df['date'].iloc[0], 'open': first_open, 'high': first_close, 'low': first_open, 'close': first_close, 'uptrend': True})
        current_close = first_close
        current_open = first_open
        uptrend = True
        for idx in range(1, len(self.df)):
            row = self.df.iloc[idx]
            price = row[source]
            date = row['date']
            if uptrend:
                while price >= current_close + self.box_size:
                    new_open = current_close
                    new_close = new_open + self.box_size
                    boxes.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                    current_open = new_open
                    current_close = new_close
                reversal_threshold = current_open - self.reversal_size * self.box_size
                if price < reversal_threshold:
                    uptrend = False
                    new_open = current_open
                    new_close = new_open - self.box_size
                    boxes.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                    current_open = new_open
                    current_close = new_close
                    while price <= current_close - self.box_size:
                        new_open = current_close
                        new_close = new_open - self.box_size
                        boxes.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                        current_open = new_open
                        current_close = new_close
            else:
                while price <= current_close - self.box_size:
                    new_open = current_close
                    new_close = new_open - self.box_size
                    boxes.append({'date': date, 'open': new_open, 'high': new_open, 'low': new_close, 'close': new_close, 'uptrend': False})
                    current_open = new_open
                    current_close = new_close
                reversal_threshold = current_open + self.reversal_size * self.box_size
                if price > reversal_threshold:
                    uptrend = True
                    new_open = current_open
                    new_close = new_open + self.box_size
                    boxes.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                    current_open = new_open
                    current_close = new_close
                    while price >= current_close + self.box_size:
                        new_open = current_close
                        new_close = new_open + self.box_size
                        boxes.append({'date': date, 'open': new_open, 'high': new_close, 'low': new_open, 'close': new_close, 'uptrend': True})
                        current_open = new_open
                        current_close = new_close
        self.cdf = pd.DataFrame(boxes, columns=['date', 'open', 'high', 'low', 'close', 'uptrend'])
        return self.cdf

    def get_bar_ohlc_data(self, source: str='close') -> pd.DataFrame:
        if not hasattr(self, 'cdf') or self.cdf is None or len(self.cdf) == 0:
            self.get_ohlc_data(source)
        bar = {'date': self.odf['date'].iloc[0], 'open': self.odf['open'].iloc[0], 'close': max(self.cdf['close']), 'high': max(self.cdf['high']), 'low': self.odf['open'].iloc[0]}
        return pd.DataFrame([bar], columns=['date', 'open', 'close', 'high', 'low'])