import ast
import os
import pathlib
import sys
import unittest
from unittest.mock import patch

# Assuming we are importing the relevant functions from main.py instead of vulture.utils
from main import format_path, get_decorator_name


class TestFormatPath(unittest.TestCase):
    @patch('os.getcwd')
    @patch('pathlib.Path.is_absolute', return_value=False)
    def setUp(self, mock_is_absolute, mock_getcwd):
        self.tmp_path = pathlib.Path("tmp")
        self.cwd = self.tmp_path / "workingdir"
        self.cwd.mkdir(parents=True, exist_ok=True)
        mock_getcwd.return_value = str(self.cwd)

    def test_relative_inside(self):
        filepath = pathlib.Path("testfile.py")
        formatted = format_path(filepath)
        self.assertEqual(formatted, filepath)
        self.assertFalse(formatted.is_absolute())

    def test_relative_outside(self):
        filepath = pathlib.Path(os.pardir) / "testfile.py"
        formatted = format_path(filepath)
        self.assertEqual(formatted, filepath)
        self.assertFalse(formatted.is_absolute())

    def test_absolute_inside(self):
        filepath = self.cwd / "testfile.py"
        formatted = format_path(filepath)
        self.assertEqual(formatted, pathlib.Path("testfile.py"))
        self.assertFalse(formatted.is_absolute())

    def test_absolute_outside(self):
        filepath = (self.cwd / os.pardir / "testfile.py").resolve()
        formatted = format_path(filepath)
        self.assertEqual(formatted, filepath)
        self.assertTrue(formatted.is_absolute())


class TestDecoratorNames(unittest.TestCase):
    def check_decorator_names(self, code, expected_names):
        decorator_names = []

        def visit_FunctionDef(node):
            for decorator in node.decorator_list:
                decorator_names.append(get_decorator_name(decorator))

        node_visitor = ast.NodeVisitor()
        node_visitor.visit_AsyncFunctionDef = visit_FunctionDef
        node_visitor.visit_ClassDef = visit_FunctionDef
        node_visitor.visit_FunctionDef = visit_FunctionDef
        node_visitor.visit(ast.parse(code))
        self.assertEqual(expected_names, decorator_names)

    def test_get_decorator_name_simple(self):
        code = """\
@foobar
def hoo():
    pass
"""
        self.check_decorator_names(code, ["@foobar"])

    def test_get_decorator_name_call(self):
        code = """\
@xyz()
def bar():
    pass
"""
        self.check_decorator_names(code, ["@xyz"])

    def test_get_decorator_name_async(self):
        code = """\
@foo.bar.route('/foobar')
async def async_function(request):
    print(request)
"""
        self.check_decorator_names(code, ["@foo.bar.route"])

    def test_get_decorator_name_multiple_attrs(self):
        code = """\
@x.y.z
def doo():
    pass
"""
        self.check_decorator_names(code, ["@x.y.z"])

    def test_get_decorator_name_multiple_attrs_called(self):
        code = """\
@a.b.c.d.foo("Foo and Bar")
def hoofoo():
    pass
"""
        self.check_decorator_names(code, ["@a.b.c.d.foo"])

    def test_get_decorator_name_multiple_decorators(self):
        code = """\
@foo
@bar()
@x.y.z.a('foobar')
def func():
    pass
"""
        self.check_decorator_names(code, ["@foo", "@bar", "@x.y.z.a"])

    def test_get_decorator_name_class(self):
        code = """\
@foo
@bar.yz
class Foo:
    pass
"""
        self.check_decorator_names(code, ["@foo", "@bar.yz"])

    def test_get_decorator_name_end_function_call(self):
        code = """\
@foo.bar(x, y, z)
def bar():
    pass
"""
        self.check_decorator_names(code, ["@foo.bar"])

    @unittest.skipIf(sys.version_info < (3, 9), "requires Python 3.9 or higher")
    @unittest.expectedFailure  # Marking it for expected failure due to a placeholder
    def test_get_decorator_name_multiple_callables(self):
        decorated = "def foo():"
        code = f"""\
@foo
@bar.prop
@z.func("hi").bar().k.foo
@k("hello").doo("world").x
@k.hello("world")
@foo[2]
{decorated}
"""
        self.check_decorator_names(
            code,
            ["@foo", "@bar.prop", "@", "@", "@k.hello", "@"],
        )


if __name__ == "__main__":
    unittest.main()