from deck_builder import build_presentation
from content_en import SLIDES


def test_content_en_has_thirteen_slides_with_titles():
    assert len(SLIDES) == 13
    for spec in SLIDES:
        assert spec["title"].strip() != ""


def test_content_en_builds_a_valid_presentation():
    prs = build_presentation(SLIDES)
    assert len(prs.slides) == 13
