from layouts import (
    new_presentation,
    add_title_slide,
    add_bullet_slide,
    add_two_column_slide,
    add_closing_slide,
)


def all_text(slide):
    return "\n".join(
        shape.text_frame.text for shape in slide.shapes if shape.has_text_frame
    )


def test_add_title_slide_sets_title_and_subtitle_text():
    prs = new_presentation()
    slide = add_title_slide(prs, "Workshop AI", "Local models with MCP")

    text = all_text(slide)
    assert "Workshop AI" in text
    assert "Local models with MCP" in text


def test_add_bullet_slide_creates_one_paragraph_per_bullet():
    prs = new_presentation()
    bullets = ["First point", "Second point", "Third point"]
    slide = add_bullet_slide(prs, "Agenda", bullets)

    bullet_box = next(
        shape for shape in slide.shapes
        if shape.has_text_frame and shape.text_frame.text.startswith("•")
    )
    paragraphs = bullet_box.text_frame.paragraphs
    assert len(paragraphs) == len(bullets)
    for paragraph, bullet in zip(paragraphs, bullets):
        assert bullet in paragraph.text


def test_add_two_column_slide_creates_both_columns():
    prs = new_presentation()
    slide = add_two_column_slide(
        prs, "Comparison",
        "Pros", ["Fast", "Simple"],
        "Cons", ["Limited"],
    )

    text = all_text(slide)
    assert "Pros" in text
    assert "Fast" in text
    assert "Cons" in text
    assert "Limited" in text


def test_add_closing_slide_shows_title_and_subtitle():
    prs = new_presentation()
    slide = add_closing_slide(prs, "Thank you", "Questions?")

    text = all_text(slide)
    assert "Thank you" in text
    assert "Questions?" in text


def test_add_title_slide_includes_presenter_line_when_provided():
    prs = new_presentation()
    slide = add_title_slide(
        prs, "Workshop AI", "Local models with MCP",
        presenter_line="(Your name — date)",
    )

    text = all_text(slide)
    assert "Workshop AI" in text
    assert "Local models with MCP" in text
    assert "(Your name — date)" in text


def test_add_title_slide_without_presenter_line_adds_no_extra_textbox():
    prs = new_presentation()
    slide_without = add_title_slide(prs, "Workshop AI", "Local models with MCP")
    slide_with = add_title_slide(
        prs, "Workshop AI", "Local models with MCP",
        presenter_line="(Your name — date)",
    )

    assert len(slide_without.shapes) == len(slide_with.shapes) - 1


def test_add_closing_slide_has_no_presenter_line_textbox():
    # add_closing_slide must keep calling add_title_slide with no
    # presenter_line — omitting it should not crash and should not add a
    # third textbox.
    prs = new_presentation()
    slide = add_closing_slide(prs, "Thank you", "Questions?")

    text_shapes = [shape for shape in slide.shapes if shape.has_text_frame]
    assert len(text_shapes) == 2
