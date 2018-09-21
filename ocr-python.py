#!/usr/bin/env python

from PIL import Image, ImageFilter
import pytesseract
import cv2
import numpy as np
from matplotlib import pyplot as plt


def Basic_Tesseract(image):
    return image


def Filter_Tesseract(image):
    image = image.convert('L').resize(
        [2 * _ for _ in image.size], Image.BICUBIC)
    image = image.point(lambda p: p > 75 and p + 100)
    return image


# Open jpg image
image_obj = Image.open("UCD_Lehmann_0036.jpg")
# converts jpg file extention to tiff
img_tiff = image_obj.save("UCD_Lehmann_0036.tiff")
# Close jpg image
image_obj.close()

# Open Tiff image
image_obj = Image.open("UCD_Lehmann_0036.tiff")

# Basic Filter function
image_obj_basic = Basic_Tesseract(image_obj)

# run tesseract on image object
text = pytesseract.image_to_string(image_obj_basic, lang='eng')

print("***************************************************************************************************")
print("Basic Function:")
print("***************************************************************************************************")

# Log image to string recognition conversion for basic
print(text)

# Get verbose data including boxes, confidences, line and page numbers
print(pytesseract.image_to_data(image_obj))

plt.imshow(image_obj_basic)

print("***************************************************************************************************")
print("Filter Function:")
print("***************************************************************************************************")

# close edited Tiff image
image_obj.close()
# Re-open Tiff image for filtering
image_obj = Image.open("UCD_Lehmann_0036.tiff")

# Basic Filter function
image_obj_filter = Filter_Tesseract(image_obj)

# run tesseract on image object
text = pytesseract.image_to_string(image_obj_filter, lang='eng')
# log image to text translation
print(text)

# Get verbose data including boxes, confidences, line and page numbers
print(pytesseract.image_to_data(image_obj))


# plot image filters
fig = plt.figure(figsize=(1, 1))
fig.add_subplot(1, 2, 2)
plt.imshow(image_obj_filter)
plt.show(block=False)
plt.pause(60)
plt.close()
