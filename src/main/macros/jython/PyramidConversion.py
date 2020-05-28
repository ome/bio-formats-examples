# imports
from loci.plugins import BF
from loci.plugins.in import ImporterOptions
from loci.formats import ImageReader
from loci.formats import MetadataTools
from loci.formats import FormatTools
from loci.formats.out import OMETiffWriter
from loci.common.image import IImageScaler
from loci.common.image import SimpleImageScaler
from ome.xml.model.primitives import PositiveInteger
from ome.units import UNITS
from java.lang import Math
from ij import IJ

# settings
file = "/path/to/inputFile.tiff"
outFile = "/path/to/outputFile.ome.tiff"
resolutions = 4
scale = 2

# setup reader and parse metadata
reader = ImageReader()
omeMeta = MetadataTools.createOMEXMLMetadata()
reader.setMetadataStore(omeMeta)
reader.setId(file)

# setup resolutions
for i in range(resolutions):
    divScale = Math.pow(scale, i + 1)
    omeMeta.setResolutionSizeX(PositiveInteger(int(reader.getSizeX() / divScale)), 0, i + 1)
    omeMeta.setResolutionSizeY(PositiveInteger(int(reader.getSizeY() / divScale)), 0, i + 1)

# setup writer
writer = OMETiffWriter()
writer.setMetadataRetrieve(omeMeta)
writer.setId(outFile)
type = reader.getPixelType()

# read and write main image
img = reader.openBytes(0)
writer.saveBytes(0, img)

# create ImageScaler for downsampling
scaler = SimpleImageScaler()

# generate downsampled resolutions and write to output
for i in range(resolutions):
    writer.setResolution(i + 1)
    x = omeMeta.getResolutionSizeX(0, i + 1).getValue()
    y = omeMeta.getResolutionSizeY(0, i + 1).getValue()
    downsample = scaler.downsample(img, reader.getSizeX(), reader.getSizeY(), Math.pow(scale, i + 1),
         FormatTools.getBytesPerPixel(type), reader.isLittleEndian(),
         FormatTools.isFloatingPoint(type), reader.getRGBChannelCount(),
         reader.isInterleaved())
    writer.saveBytes(0, downsample)

writer.close()
reader.close()