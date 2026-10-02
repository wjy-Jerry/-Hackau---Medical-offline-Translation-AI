"""Online setup for the English/Chinese Argos packages only."""
from backend.config import TRANSLATION_DIR


def main():
    from argostranslate import package

    TRANSLATION_DIR.mkdir(parents=True, exist_ok=True)
    installed = {(p.from_code, p.to_code) for p in package.get_installed_packages()}
    required = {("en", "zh"), ("zh", "en")}
    if required <= installed:
        print(f"Translation packages already present at {TRANSLATION_DIR}")
        return
    print("Downloading the Argos package index...")
    package.update_package_index()
    available = package.get_available_packages()
    for source, target in sorted(required - installed):
        selected = next((p for p in available if p.from_code == source and p.to_code == target), None)
        if selected is None:
            raise RuntimeError(f"No direct Argos package found for {source}→{target}.")
        print(f"Installing {source}→{target} into {TRANSLATION_DIR}...")
        package.install_from_path(selected.download())
    present = {(p.from_code, p.to_code) for p in package.get_installed_packages()}
    if not required <= present:
        raise RuntimeError("English/Chinese translation packages were not installed successfully.")
    print("Local translation packages are ready.")


if __name__ == "__main__":
    main()
