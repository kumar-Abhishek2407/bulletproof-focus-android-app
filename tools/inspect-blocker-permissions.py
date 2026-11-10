"""Read-only comparison of focus-app permissions; never reads screens or transaction data.
Run: python tools/inspect-blocker-permissions.py [path/to/adb.exe]
"""
import json
from pathlib import Path
import subprocess
import sys

ADB = sys.argv[1] if len(sys.argv) > 1 else str(Path(__file__).resolve().parents[1] / ".tools/android-sdk/platform-tools/adb.exe")
PACKAGES = ("com.stillfocus.app", "ai.regainapp")

def shell(*args):
    result = subprocess.run([ADB, "shell", *args], capture_output=True, text=True, errors="replace", timeout=20)
    if result.returncode:
        raise RuntimeError("ADB command failed: " + " ".join(args[:3]))
    return result.stdout

def inspect():
    services = shell("settings", "get", "secure", "enabled_accessibility_services").strip().split(":")
    report = {}
    for package in PACKAGES:
        installed = any(line.split(" ")[0] == "package:" + package for line in shell("pm", "list", "packages", "-i", package).splitlines())
        entry = {"installed_in_current_user": installed}
        report[package] = entry
        if not installed:
            continue
        metadata = shell("dumpsys", "package", package)
        fields = ("versionName=", "versionCode=", "installerPackageName=", "initiatingPackageName=", "pkgFlags=")
        entry["package_metadata"] = [line.strip() for line in metadata.splitlines() if any(line.strip().startswith(field) for field in fields)]
        entry["enabled_accessibility_services"] = [service for service in services if service.split("/")[0] == package]
        entry["usage_access"] = shell("cmd", "appops", "get", package, "GET_USAGE_STATS").strip()
        entry["overlay_access"] = shell("cmd", "appops", "get", package, "SYSTEM_ALERT_WINDOW").strip()
    print(json.dumps(report, indent=2))

if __name__ == "__main__":
    try:
        inspect()
    except (RuntimeError, subprocess.TimeoutExpired) as error:
        sys.exit(str(error))
