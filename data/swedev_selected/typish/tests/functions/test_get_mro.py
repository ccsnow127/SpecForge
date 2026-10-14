import typing
from typing import Union
from unittest import TestCase

# Updated import of the get_mro function from the main.py file
from main import get_mro


class A:
    pass


class B(A):
    pass


class TestGetMRO(TestCase):
    def test_get_mro(self):
        mro_b = get_mro(B)
        self.assertTupleEqual((B, A, object), mro_b)

    def test_get_mro_union(self):
        mro_u = get_mro(Union[int, str])

        # Below is to stay compatible with Python 3.5+
        super_cls = getattr(typing, '_GenericAlias',
                            getattr(typing, 'GenericMeta', None))
        expected = (typing.Union, super_cls, object)

        self.assertTupleEqual(expected, mro_u)

    def test_get_mro_object(self):
        mro_b = get_mro(B())
        self.assertTupleEqual((B, A, object), mro_b)