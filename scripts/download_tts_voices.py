"""One-time online installation of only the configured local Piper voices."""
import subprocess
import sys

from backend.config import VOICES, VOICES_DIR


def main():
    VOICES_DIR.mkdir(parents=True, exist_ok=True)
    for name in VOICES.values():
        model = VOICES_DIR / f"{name}.onnx"
        config = VOICES_DIR / f"{name}.onnx.json"
        if model.is_file() and config.is_file():
            print(f"Already installed: {name}")
            continue
        subprocess.run(
            [sys.executable, "-m", "piper.download_voices", "--download-dir", str(VOICES_DIR), name],
            check=True,
        )
        if not model.is_file() or not config.is_file():
            raise RuntimeError(f"Piper voice files missing after download: {name}")
    print(f"Local English, Chinese, and Russian voices are ready in {VOICES_DIR}")


if __name__ == "__main__":
    main()
