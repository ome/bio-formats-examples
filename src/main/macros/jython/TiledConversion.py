# imports
from loci.plugins import BF
from loci.formats import ImageReader
from loci.formats import MetadataTools
from loci.formats import FormatTools
from loci.formats.out import OMETiffWriter
from loci.common.image import IImageScaler
from loci.common.image import SimpleImageScaler
from ome.xml.model.primitives import PositiveInteger
from java.lang import Math
from ij import IJ
import math

# configuation
file = "/path/to/inputFile.tiff"
outFile = "/path/to/outputFile.ome.tiff"
resolutions = 3;
scale = 2;
tileSizeX = 512
tileSizeY = 512

# setup image reader and writer
reader = ImageReader()
omeMeta = MetadataTools.createOMEXMLMetadata()
reader.setMetadataStore(omeMeta)
reader.setId(file)

writer = OMETiffWriter();
writer.setMetadataRetrieve(omeMeta)
writer.setInterleaved(reader.isInterleaved())
writer.setTileSizeX(tileSizeX)
writer.setTileSizeY(tileSizeY)
writer.setId(outFile)

# convert to OME-TIFF using tiled reading and writing
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
			nYTiles = nYTiles + 1
		for y in range(nYTiles):
			for x in range(nXTiles):
				# The x and y coordinates for the current tile
				tileX = x * tileSizeX
				tileY = y * tileSizeY
				# Read tiles from the input file and write them to the output OME-Tiff
				buf = reader.openBytes(image, tileX, tileY, tileSizeX, tileSizeY)
				writer.saveBytes(image, buf, tileX, tileY, tileSizeX, tileSizeY)


# close reader and writer
writer.close()
reader.close()
IJ.log("Done")