"""Dispatches slide-spec dictionaries from a language's content module to
the matching layout builder, producing a full Presentation."""

from architecture import add_architecture_diagram_slide
from layouts import (
    new_presentation,
    add_title_slide,
    add_bullet_slide,
    add_two_column_slide,
    add_closing_slide,
)

_BUILDERS = {
    "title": lambda prs, s: add_title_slide(prs, s["title"], s["subtitle"], s.get("presenter_line")),
    "bullets": lambda prs, s: add_bullet_slide(prs, s["title"], s["bullets"]),
    "two_column": lambda prs, s: add_two_column_slide(
        prs, s["title"], s["left_heading"], s["left_bullets"], s["right_heading"], s["right_bullets"]
    ),
    "architecture": lambda prs, s: add_architecture_diagram_slide(
        prs, s["title"], s["frontend_label"], s["backend_label"], s["lmstudio_label"], s["rest_label"], s["mcp_label"]
    ),
    "closing": lambda prs, s: add_closing_slide(prs, s["title"], s["subtitle"]),
}


def build_presentation(slides):
    prs = new_presentation()
    for spec in slides:
        builder = _BUILDERS[spec["kind"]]
        builder(prs, spec)
    return prs
