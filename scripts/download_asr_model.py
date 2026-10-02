"""Online setup for the multilingual ASR model only."""
from backend.config import ASR_DIR


def main():
    from faster_whisper.utils import download_model

    ASR_DIR.mkdir(parents=True, exist_ok=True)
    if (ASR_DIR / "model.bin").is_file():
        print(f"ASR model already present at {ASR_DIR}")
        return
    print(f"Downloading multilingual faster-whisper base model to {ASR_DIR}...")
    download_model("base", output_dir=str(ASR_DIR))
    if not (ASR_DIR / "model.bin").is_file():
        raise RuntimeError("ASR download did not create model.bin")
    print("ASR model is ready for local inference.")


if __name__ == "__main__":
    main()
