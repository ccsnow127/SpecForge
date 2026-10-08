from typing import List, Set, Tuple
from unittest import TestCase

# Assuming TypingType is defined and imported from main.py
from main import TypingType 

class TestSomething(TestCase):
    def test_list_isinstance_generic_collection_type(self):
        self.assertTrue(isinstance(List[int], TypingType))

    def test_set_isinstance_generic_collection_type(self):
        self.assertTrue(isinstance(Set[int], TypingType))

    def test_tuple_isinstance_generic_collection_type(self):
        self.assertTrue(isinstance(Tuple[int, str], TypingType))