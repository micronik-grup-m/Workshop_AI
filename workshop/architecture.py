"""Builds the one architecture-diagram slide used in the deck: three
labeled boxes connected by two labeled arrows, drawn with plain
python-pptx shapes/connectors rather than an imported image."""

from pptx.util import Inches, Pt
from pptx.enum.shapes import MSO_SHAPE, MSO_CONNECTOR
from pptx.enum.text import PP_ALIGN
from pptx.dml.color import RGBColor

from layouts import blank_slide, add_heading, ACCENT_COLOR, BODY_COLOR

BOX_WIDTH = Inches(2.8)
BOX_HEIGHT = Inches(1.0)
BOX_TOP = Inches(3.4)

FRONTEND_X = Inches(0.6)
BACKEND_X = Inches(5.3)
LMSTUDIO_X = Inches(10.0)


def add_architecture_diagram_slide(prs, title, frontend_label, backend_label, lmstudio_label, rest_label, mcp_label):
    slide = blank_slide(prs)
    add_heading(slide, title)

    _add_box(slide, FRONTEND_X, frontend_label)
    _add_box(slide, BACKEND_X, backend_label)
    _add_box(slide, LMSTUDIO_X, lmstudio_label)

    _add_arrow(slide, FRONTEND_X + BOX_WIDTH, BACKEND_X, rest_label)
    _add_arrow(slide, LMSTUDIO_X, BACKEND_X + BOX_WIDTH, mcp_label)

    return slide


def _add_box(slide, x, text):
    shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, x, BOX_TOP, BOX_WIDTH, BOX_HEIGHT)
    shape.fill.solid()
    shape.fill.fore_color.rgb = ACCENT_COLOR
    shape.line.color.rgb = ACCENT_COLOR

    text_frame = shape.text_frame
    text_frame.word_wrap = True
    text_frame.text = text
    for paragraph in text_frame.paragraphs:
        paragraph.alignment = PP_ALIGN.CENTER
        run = paragraph.runs[0]
        run.font.size = Pt(14)
        run.font.bold = True
        run.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    return shape


def _add_arrow(slide, start_x, end_x, label):
    y = BOX_TOP + BOX_HEIGHT // 2
    connector = slide.shapes.add_connector(MSO_CONNECTOR.STRAIGHT, start_x, y, end_x, y)
    connector.line.color.rgb = BODY_COLOR
    connector.line.width = Pt(1.5)

    label_left = min(start_x, end_x)
    label_width = abs(end_x - start_x)
    label_box = slide.shapes.add_textbox(label_left, BOX_TOP - Inches(0.5), label_width, Inches(0.4))
    label_box.text_frame.text = label
    label_paragraph = label_box.text_frame.paragraphs[0]
    label_paragraph.alignment = PP_ALIGN.CENTER
    label_run = label_paragraph.runs[0]
    label_run.font.size = Pt(12)
    label_run.font.color.rgb = BODY_COLOR
