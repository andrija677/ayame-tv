import hashlib
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location('metadata', Path(__file__).with_name('create-release-metadata.py'))
metadata = importlib.util.module_from_spec(spec)
spec.loader.exec_module(metadata)


class ReleaseMetadataTest(unittest.TestCase):
    def test_manifest_matches_the_exact_uploaded_apk(self):
        with tempfile.TemporaryDirectory() as temp:
            project = Path(temp)
            (project / 'version.properties').write_text('versionCode=5\nversionName=0.2.0\n')
            apk = project / 'app-release.apk'
            apk.write_bytes(b'example APK payload')
            output = project / 'release'
            result = metadata.create(apk, output, project)
            self.assertEqual(5, result['versionCode'])
            self.assertEqual('ayame-tv-0.2.0.apk', result['apkName'])
            self.assertEqual(hashlib.sha256(apk.read_bytes()).hexdigest(), result['sha256'])
            self.assertEqual(apk.read_bytes(), (output / result['apkName']).read_bytes())
            self.assertEqual(result, json.loads((output / 'update.json').read_text()))

    def test_invalid_version_is_not_published(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / 'version.properties').write_text('versionCode=0\nversionName=0.2.0\n')
            with self.assertRaises(ValueError):
                metadata.version(root)


if __name__ == '__main__':
    unittest.main()
