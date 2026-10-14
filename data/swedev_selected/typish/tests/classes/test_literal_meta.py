import unittest

from main import LiteralAlias  # Assuming LiteralAlias is defined in main.py


class TestLiteralMeta(unittest.TestCase):
    def test_from_literal(self):
        class LiteralMock:
            __args__ = (42,)

        alias = LiteralAlias.from_literal(LiteralMock)

        self.assertTrue(isinstance(42, alias))

    def test_str(self):
        self.assertEqual('Literal[42]', str(LiteralAlias[42]))

    def test_multiple_args(self):
        self.assertTrue(isinstance(1, LiteralAlias[1, 2]))
        self.assertTrue(isinstance(2, LiteralAlias[1, 2]))
        self.assertTrue(isinstance(1, LiteralAlias[(1, 2)]))
        self.assertTrue(isinstance(2, LiteralAlias[(1, 2)]))
        self.assertTrue(isinstance(1, LiteralAlias[((1, 2),)]))
        self.assertTrue(isinstance(2, LiteralAlias[((1, 2),)]))


if __name__ == "__main__":
    unittest.main()  # This allows us to run the test case when the script is executed