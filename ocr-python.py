#!/usr/bin/env python

from PIL import Image, ImageFilter
import pytesseract
from matplotlib import pyplot as plt

def Basic_Tesseract(image):
    image = image.convert('L')
    return image


def Zoom_Tesseract(image):
    image = image.convert('L').resize(
        [3 * _ for _ in image.size], Image.BICUBIC)
    image = image.point(lambda p: p > 75 and p + 100)
    return image


# Open image
image_obj_basic = Image.open("UCD_Lehmann_0036.jpg")
# converts jpg file extention to tiff
img_tiff = image_obj_basic.save('UCD_Lehmann_0036.tiff')
# Filter tesseract function
image_obj_basic = Basic_Tesseract(Image.open("UCD_Lehmann_0036.jpg"))


# run tesseract on image object
text = pytesseract.image_to_string(image_obj_basic, lang='eng')

plt.imshow(image_obj_basic)
plt.show()

# Log image to string recognition conversion for basic
print(text)


# Get verbose data including boxes, confidences, line and page numbers
print(pytesseract.image_to_data(Image.open('UCD_Lehmann_0036.tiff')))


print("#####################################################################")
print("Zoom Function:")

# image_obj_zoom = Image.open("UCD_Lehmann_0036.jpg")
