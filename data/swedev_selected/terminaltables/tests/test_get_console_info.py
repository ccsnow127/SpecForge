import unittest
import ctypes
import pytest
from main import get_console_info, INVALID_HANDLE_VALUE, IS_WINDOWS
from terminaltables3 import terminal_io


class MockKernel32:
    """Mock kernel32 (inlined from tests/test_terminal_io/__init__.py)."""

    def __init__(self, stderr=terminal_io.INVALID_HANDLE_VALUE, stdout=terminal_io.INVALID_HANDLE_VALUE):
        self.stderr = stderr
        self.stdout = stdout
        self.csbi_err = b"x\x00)#\x00\x00\x87\x05\x07\x00\x00\x00j\x05w\x00\x87\x05x\x00J\x00"
        self.csbi_out = b"L\x00,\x01\x00\x00*\x01\x07\x00\x00\x00\x0e\x01K\x00*\x01L\x00L\x00"
        self.setConsoleTitleA_called = False
        self.setConsoleTitleW_called = False

    def GetConsoleScreenBufferInfo(self, handle, lpcsbi):
        if handle == self.stderr:
            lpcsbi.raw = self.csbi_err
        else:
            lpcsbi.raw = self.csbi_out
        return 1

    def GetStdHandle(self, handle):
        return self.stderr if handle == terminal_io.STD_ERROR_HANDLE else self.stdout

def test():
    """Test function."""
    if IS_WINDOWS:
        with pytest.raises(OSError):
            get_console_info(ctypes.windll.kernel32, 0)
    kernel32 = MockKernel32(stderr=1)
    with pytest.raises(OSError):
        get_console_info(kernel32, INVALID_HANDLE_VALUE)
    (width, height) = get_console_info(kernel32, 1)
    assert width == 119
    assert height == 29

class GeneratedTestCase(unittest.TestCase):
    pass