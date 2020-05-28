# imports
from loci.plugins import BF
from loci.plugins.in import ImporterOptions
from loci.formats import ImageReader
from loci.formats import MetadataTools
from loci.formats import FormatTools
from loci.formats.out import PyramidOMETiffWriter
from loci.common.image import IImageScaler
from loci.common.image import SimpleImageScaler
from ome.xml.model.primitives import PositiveInteger
from java.lang import Math
from ij import IJ
import math

# configuration
file = "/path/to/inputFile.tiff"
outFile = "/path/to/outputFile.ome.tiff"

# the number of resolutions in the output file
resolutions = 2

# the scale to be used for the downsampling
scale = 2

# set the tile sizes to be used
tileSizeX = 1024
tileSizeY = 1024

# setup reader
reader = ImageReader()
omeMeta = MetadataTools.createOMEXMLMetadata()
reader.setMetadataStore(omeMeta)
reader.setId(file)

# add resolution metadata
for i in range(resolutions):
    divScale = Math.pow(scale, i + 1)
    omeMeta.setResolutionSizeX(PositiveInteger(int(reader.getSizeX() / divScale)), 0, i + 1)
    omeMeta.setResolutionSizeY(PositiveInteger(int(reader.getSizeY() / divScale)), 0, i + 1)

# setup writer with tiling
writer = PyramidOMETiffWriter()
writer.setMetadataRetrieve(omeMeta)
tileSizeX = writer.setTileSizeX(tileSizeX)
tileSizeY = writer.setTileSizeY(tileSizeY)
writer.setId(outFile)
type = reader.getPixelType()

# create image scaler for downsampling
scaler = SimpleImageScaler()

# convert to Pyramidal OME-TIFF using tiling
for series in range(reader.getSeriesCount()):
	reader.setSeries(series)
	writer.setSeries(series)

	# convert each image in the current series
	for image in range(reader.getImageCount()):
		width = reader.getSizeX()
		height = reader.getSizeY()

		# Determined the number of tiles to read and write
		nXTiles = int(math.floor(width / tileSizeX))
		nYTiles = int(math.floor(height / tileSizeY))
		if nXTiles * tileSizeX != width:
			nXTiles = nXTiles + 1
		if nYTiles * tileSizeY != height:
			nYTiles = nYTiles + 1;

		# Convert the main image 
		for y in range(nYTiles):
			for x in range(nXTiles):
				writer.setResolution(0);
				# The x and y coordinates for the current tile
				tileX = x * tileSizeX;
				tileY = y * tileSizeY;
				effTileSizeX = tileSizeX
				if (tileX + tileSizeX) >= width:
					effTileSizeX = width - tileX
				effTileSizeY = tileSizeY
				if (tileY + tileSizeY) >= height:
					effTileSizeY = height - tileY
				# Read tiles from the input file and write them to the output OME-Tiff
				buf = reader.openBytes(image, tileX, tileY, effTileSizeX, effTileSizeY)
				writer.saveBytes(image, buf, tileX, tileY, effTileSizeX, effTileSizeY)

		# Create the downsampled resolutions and write to output
		for i in range(resolutions):
			currentScale = int(Math.pow(scale, i + 1))
			writer.setResolution(i + 1)
			resolutionWidth = width / currentScale
			resolutionHeight = height / currentScale
			nXTiles = int(math.floor(resolutionWidth / tileSizeX))
			nYTiles = int(math.floor(resolutionHeight / tileSizeY))
			if nXTiles * tileSizeX != resolutionWidth:
				nXTiles = nXTiles + 1
			if nYTiles * tileSizeY != resolutionHeight:
				nYTiles = nYTiles + 1
			for y in range(nYTiles):
				for x in range(nXTiles):
					# Calculate the correct size and offset for each tile
					tileX = x * tileSizeX
					tileY = y * tileSizeY
					effTileSizeX = tileSizeX * currentScale
					if ((tileX * currentScale) + effTileSizeX) >= width:
						effTileSizeX = width - (tileX * currentScale)
					effTileSizeY = tileSizeY * currentScale
					if ((tileY * currentScale) + effTileSizeY) >= height:
						effTileSizeY = height - (tileY * currentScale)

					# Read the tile, create the downsampled version and then write to output
					tile = reader.openBytes(image, tileX * currentScale, tileY * currentScale, effTileSizeX, effTileSizeY)
					downsample = scaler.downsample(tile, effTileSizeX, effTileSizeY, currentScale, FormatTools.getBytesPerPixel(type), reader.isLittleEndian(), 
					    FormatTools.isFloatingPoint(type), reader.getRGBChannelCount(), reader.isInterleaved())
					writer.saveBytes(image, downsample, tileX, tileY, effTileSizeX / currentScale, effTileSizeY / currentScale)

writer.close();
reader.close();

IJ.log("Done")

options = ImporterOptions()
options.setColorMode(ImporterOptions.COLOR_MODE_COMPOSITE)
options.setId(outFile)
options.setSeriesOn(2, True);
imps = BF.openImagePlus(options)
for imp in imps:
    imp.show()