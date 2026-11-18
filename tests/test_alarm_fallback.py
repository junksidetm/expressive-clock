"""The last-resort asset must contain audible PCM, not a successfully decoded silent file."""
import array
import math
from pathlib import Path
import unittest
import wave

class AlarmFallbackTest(unittest.TestCase):
    def test_packaged_pcm_has_signal_without_clipping(self):
        path = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw/alarm_fallback.wav'
        with wave.open(str(path), 'rb') as sound:
            self.assertEqual(2, sound.getsampwidth())
            self.assertGreater(sound.getframerate(), 8000)
            self.assertGreater(sound.getnframes() / sound.getframerate(), .5)
            samples = array.array('h', sound.readframes(sound.getnframes()))
        rms = math.sqrt(sum(x*x for x in samples) / len(samples))
        self.assertGreater(rms, 1000)
        self.assertLess(max(abs(x) for x in samples), 32767)
