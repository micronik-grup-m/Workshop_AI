from deck_builder import build_presentation
from content_en import SLIDES as EN_SLIDES
from content_fr import SLIDES as FR_SLIDES


def test_content_fr_has_thirteen_slides_with_titles():
    assert len(FR_SLIDES) == 13
    for spec in FR_SLIDES:
        assert spec["title"].strip() != ""


def test_content_fr_builds_a_valid_presentation():
    prs = build_presentation(FR_SLIDES)
    assert len(prs.slides) == 13


def test_content_en_and_fr_have_matching_structure():
    assert len(EN_SLIDES) == len(FR_SLIDES)
    for en_spec, fr_spec in zip(EN_SLIDES, FR_SLIDES):
        assert en_spec["kind"] == fr_spec["kind"]
