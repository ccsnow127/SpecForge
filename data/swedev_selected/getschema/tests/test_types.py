import unittest
from main import infer_schema  # Updated import to reference the correct target object

class TestNullRecords(unittest.TestCase):

    def test_null_records(self):
        records = [
            {
                "field": "1",
                "null_field": None,
                "array": [],
                "null_array": [],
                "nested_field": {
                    "some_date": "2021-05-25",
                    "number": 1,
                    "null_subfield": None,
                },
            },
            {
                "field": "10.0",
                "null_field": None,
                "array": [
                    "1",
                    "a",
                ],
                "null_array": [],
                "nested_field": {
                    "some_date": "2021-05-25",
                    "integer": 1,
                    "number": 1.5,
                    "null_subfield": None,
                },
            },
        ]
        schema = infer_schema(records)  # Updated to reference infer_schema from main
        self.assertEqual(schema["properties"]["field"]["type"], ["null", "number"])
        self.assertEqual(schema["properties"]["null_field"]["type"], ["null", "string"])
        self.assertEqual(schema["properties"]["nested_field"]["properties"]["some_date"]["type"], ["null", "string"])
        self.assertEqual(schema["properties"]["nested_field"]["properties"]["some_date"]["format"], "date-time")
        self.assertEqual(schema["properties"]["nested_field"]["properties"]["integer"]["type"], ["null", "integer"])
        self.assertEqual(schema["properties"]["nested_field"]["properties"]["number"]["type"], ["null", "number"])
        self.assertEqual(schema["properties"]["nested_field"]["properties"]["null_subfield"]["type"], ["null", "string"])

if __name__ == '__main__':
    unittest.main()