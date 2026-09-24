"""Builds the flashable Box for Root zip from module/box_for_root.

usage: python module/build.py [output.zip]
Unix permissions are set in the archive and line endings are normalised to LF.
"""
import os, re, sys, zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "box_for_root")
prop = open(os.path.join(SRC, "module.prop"), encoding="utf-8").read()
version = re.search(r"^version=(.*)$", prop, re.M).group(1).strip()
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "box_for_root-%s.zip" % version)

EXEC = ("META-INF/com/google/android/update-binary", "sbfr", "uninstall.sh", "box_service.sh", "action.sh", "customize.sh")
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for root, dirs, files in os.walk(SRC):
        dirs.sort()
        for f in sorted(files):
            full = os.path.join(root, f)
            rel = os.path.relpath(full, SRC).replace(os.sep, "/")
            data = open(full, "rb").read().replace(b"\r\n", b"\n")
            info = zipfile.ZipInfo(rel)
            info.compress_type = zipfile.ZIP_DEFLATED
            executable = rel in EXEC or rel.startswith("box/scripts/")
            info.external_attr = (0o100755 if executable else 0o100644) << 16
            z.writestr(info, data)
print(out)
