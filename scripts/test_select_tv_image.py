import unittest
from importlib.util import module_from_spec, spec_from_file_location
from pathlib import Path

spec = spec_from_file_location("selector", Path(__file__).with_name("select-tv-image.py"))
selector = module_from_spec(spec)
spec.loader.exec_module(selector)


class ImageSelectionTest(unittest.TestCase):
    def test_new_android_cli_slash_paths(self):
        text = """Available packages:
  system-images/android-34/google-tv/x86     3.0.0 Google TV Intel x86 Atom System Image
  system-images/android-36/android-tv/x86    4.0.0 Android TV Intel x86 Atom System Image
  system-images/android-36/android-tv/x86_64 4.0.0 Android TV Intel x86_64 Atom System Image
  system-images/android-36/google-tv/x86_64  4.0.0 Google TV Intel x86_64 Atom System Image
"""
        self.assertEqual("system-images;android-36;android-tv;x86_64", selector.select(text))

    def test_accepts_newer_tv_images_than_the_compile_sdk(self):
        text = "system-images;android-35;android-tv;x86 | 1\nsystem-images;android-36;android-tv;x86 | 2"
        self.assertEqual("system-images;android-36;android-tv;x86", selector.select(text))

    def test_prefers_64_bit_at_the_same_api(self):
        text = "system-images;android-34;android-tv;x86\nsystem-images;android-34;google-tv;x86_64"
        self.assertEqual("system-images;android-34;google-tv;x86_64", selector.select(text))

    def test_ignores_phone_arm_and_preview_images(self):
        text = """system-images;android-36;google_apis;x86_64
system-images;android-36;android-tv;arm64-v8a
system-images;android-Baklava;android-tv;x86_64
system-images;android-30;google-tv;x86"""
        self.assertEqual("system-images;android-30;google-tv;x86", selector.select(text))

    def test_empty_repository_does_not_invent_a_package(self):
        with self.assertRaises(ValueError):
            selector.select("Warning: Failed to download repository manifest")


if __name__ == "__main__":
    unittest.main()
