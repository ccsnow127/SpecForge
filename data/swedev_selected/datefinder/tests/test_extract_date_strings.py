import unittest
import sys
import logging
from main import DateFinder  # Assuming the main.py file contains DateFinder class

logging.basicConfig(level=logging.DEBUG, stream=sys.stdout)
logger = logging.getLogger(__name__)

class TestExtractDateStrings(unittest.TestCase):

    @unittest.skip("TODO: many more TZINFO examples")
    def test_extract_date_strings(self):
        test_cases = [
            ['March 20, 2015 3:30 pm GMT ', 'March 20, 2015 3:30 pm GMT'],
            ['March 20, 2015 3:30 pm ACWDT in the parking lot', 'March 20, 2015 3:30 pm ACWDT'],
            ['blah blah March 20, 2015 3pm MADMT for some thing', 'March 20, 2015 3pm MADMT'],
            ['we need it back on Friday 2p.m. central standard time', 'on Friday 2p.m. central standard time'],
            ['the big fight at 2p.m. mountain standard time on ufc.com', 'at 2p.m. mountain standard time on'],
            # issue: Thu not recognised by regex #138
            ['starting Thursday 2020-11-05 13:50 GMT', 'Thursday 2020-11-05 13:50 GMT'],
            ['starting Thu 2020-11-05 13:50 GMT', 'Thu 2020-11-05 13:50 GMT'],
        ]

        dt = DateFinder()
        for date_string, expected_match_date_string in test_cases:
            for actual_date_string, indexes, captures in dt.extract_date_strings(date_string):
                logger.debug("actual={}  expected={}".format(actual_date_string, expected_match_date_string))
                self.assertEqual(actual_date_string, expected_match_date_string)
                self.assertGreater(len(captures.get('timezones', [])), 0, "timezone expected in result")

    @unittest.skip("TODO: Handle edge cases where no date substring is found")
    def test_extract_date_strings_with_strict_option(self):
        test_cases = [
            ['the Friday after next Tuesday the 20th', ''],  # no matches
            ['This Tuesday March 2015 in the evening', ''],  # no matches
            ['They said it was on 01-03-2015', 'on 01-03-2015'],  # 3 digits strict match
            ['May 20 2015 is nowhere near the other date', 'May 20 2015'],  # one month two digit match
        ]

        dt = DateFinder()
        for date_string, expected_match_date_string in test_cases:
            for actual_date_string, indexes, captures in dt.extract_date_strings(date_string, strict=True):
                logger.debug("actual={}  expected={}".format(actual_date_string, expected_match_date_string))
                self.assertEqual(actual_date_string, expected_match_date_string)

if __name__ == '__main__':
    unittest.main()