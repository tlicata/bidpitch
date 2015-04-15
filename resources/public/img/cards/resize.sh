#!/bin/bash

# The invidual playing card SVG files were larger than the cards
# themselves. This script loops over all card SVGs in the invidual/
# directory and shrinks the canvas to exactly fit the card.

for card in ./individual/*; do
    inkscape --file=$card --verb=FitCanvasToDrawing --verb=FileSave --verb=FileClose
done
