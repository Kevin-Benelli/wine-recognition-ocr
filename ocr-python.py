#!/usr/bin/env python

from PIL import Image, ImageFilter
import pytesseract


def Basic_Tesseract(image):

    return image


image_obj_basic = Image.open("UCD_Lehmann_0036.jpg")

# run tesseract on image object
text = pytesseract.image_to_string(image_obj_basic, lang='eng')

# Log image to string recognition conversion
print(text)


# Get verbose data including boxes, confidences, line and page numbers
print(pytesseract.image_to_data(Image.open('test.png')))
