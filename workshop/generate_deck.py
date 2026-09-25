"""CLI entry point: generate the workshop .pptx deck for one language."""

import argparse
from pathlib import Path

from deck_builder import build_presentation

OUTPUT_DIR = Path(__file__).parent.parent / "docs"


def generate(lang):
    if lang == "en":
        from content_en import SLIDES
    elif lang == "fr":
        from content_fr import SLIDES
    else:
        raise ValueError(f"Unsupported language: {lang}")

    prs = build_presentation(SLIDES)
    OUTPUT_DIR.mkdir(exist_ok=True)
    output_path = OUTPUT_DIR / f"workshop-ai-{lang}.pptx"
    prs.save(str(output_path))
    return output_path


def main():
    parser = argparse.ArgumentParser(description="Generate the workshop .pptx deck")
    parser.add_argument("--lang", choices=["en", "fr"], required=True)
    args = parser.parse_args()

    output_path = generate(args.lang)
    print(f"Saved {output_path}")


if __name__ == "__main__":
    main()
