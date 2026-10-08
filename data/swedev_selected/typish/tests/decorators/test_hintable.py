import sys
from typing import Type
from unittest import TestCase

# Assuming that 'typish.hintable' is the only required import from main.py
from main import hintable, T

class TestHintable(TestCase):
    @hintable
    def some_func(self, hint: Type[T]) -> Type[T]:
        """Some docstring"""
        return hint

    def test_hintable_without_any_hint(self):
        # Test that when a hintable function is called without hint, it receives None.
        x = self.some_func()
        self.assertEqual(None, x)

    def test_hintable_class(self):
        # Test that decorating a class raises an error.
        with self.assertRaises(TypeError):
            @hintable
            class DecoratedClass:
                pass

    def test_meta_data(self):
        # Test that any meta data is copied properly.
        self.assertEqual('Some docstring', self.some_func.__doc__)

    def test_hintable_with_flawed_function(self):
        with self.assertRaises(TypeError):
            @hintable
            def some_flawed_func():
                pass

    def test_hintable_with_flawed_custom_param_name(self):
        # Test that when a custom param name is used, it is checked if a parameter with that name is accepted by the decorated function.
        with self.assertRaises(TypeError):
            @hintable(param='cls')
            def some_func_with_flawed_custom_param_name(hint):
                return hint