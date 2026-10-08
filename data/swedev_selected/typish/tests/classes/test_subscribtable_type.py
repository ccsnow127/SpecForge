from unittest import TestCase
from main import SubscriptableType  # Update import to reference main.py

class TestSubscriptableType(TestCase):
    def test_subscribing(self):
        class C(metaclass=SubscriptableType):
            pass  # Use pass as a placeholder for an empty class

        self.assertEqual('arg', C['arg'].__args__)
        self.assertEqual(C, C['arg'].__origin__)

    def test_after_subscription(self):
        class C(metaclass=SubscriptableType):
            @staticmethod
            def _after_subscription(item):
                C.item = item

        C2 = C['arg']
        self.assertEqual('arg', C2.item)

    def test_equility(self):
        class SomeType(metaclass=SubscriptableType):
            pass  # Use pass as a placeholder for an empty class

        self.assertEqual(SomeType['test'], SomeType['test'])
        self.assertNotEqual(SomeType['test1'], SomeType['test2'])

# If this script is run directly, it will execute the tests
if __name__ == '__main__':
    import unittest
    unittest.main()