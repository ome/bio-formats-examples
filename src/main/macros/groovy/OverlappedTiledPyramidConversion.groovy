// imports
import loci.plugins.BF
import loci.plugins.in.ImporterOptions
import loci.formats.ImageReader
import loci.formats.MetadataTools
import loci.formats.FormatTools
import loci.formats.out.PyramidOMETiffWriter
import loci.formats.ome.OMEXMLMetadata
import loci.common.image.IImageScaler
import loci.common.image.SimpleImageScaler
import ome.xml.model.primitives.PositiveInteger
import java.lang.Math
import ij.IJ
import ij.ImagePlus

// configuration
String file = "/path/to/inputFile.tiff"
String outFile = "/path/to/outputFile.ome.tiff"
int resolutions = 2
int scale = 2
int tileSizeX = 1024
int tileSizeY = 1024

// setup reader
ImageReader reader = new ImageReader()
OMEXMLMetadata omeMeta = MetadataTools.createOMEXMLMetadata()
reader.setMetadataStore(omeMeta)
reader.setId(file)

// add resolution metadata
for (int i = 0; i < resolutions; i++) {
    int divScale = Math.pow(scale, i + 1)
    omeMeta.setResolutionSizeX(new PositiveInteger((int)(reader.getSizeX() / divScale)), 0, i + 1)
    omeMeta.setResolutionSizeY(new PositiveInteger((int)(reader.getSizeY() / divScale)), 0, i + 1)
}

// setup writer with tiling
PyramidOMETiffWriter writer = new PyramidOMETiffWriter()
writer.setMetadataRetrieve(omeMeta)
tileSizeX = writer.setTileSizeX(tileSizeX)
tileSizeY = writer.setTileSizeY(tileSizeY)
writer.setId(outFile)
int type = reader.getPixelType()

// create image scaler for downsampling
SimpleImageScaler scaler = new SimpleImageScaler()

// convert to Pyramidal OME-TIFF using tiling
for (series = 0; series < reader.getSeriesCount(); series++) {
	reader.setSeries(series)
	writer.setSeries(series)

	// convert each image in the current series
	for (image = 0; image < reader.getImageCount(); image++) {
		int width = reader.getSizeX()
		int height = reader.getSizeY()

		// Determined the number of tiles to read and write
		int nXTiles = width / tileSizeX
		int nYTiles = height / tileSizeY
		if (nXTiles * tileSizeX != width) {
			nXTiles = nXTiles + 1
		}
		if (nYTiles * tileSizeY != height) {
			nYTiles = nYTiles + 1
		}

		// Convert the main image 
		for (int y = 0; y < nYTiles; y++) {
			for (int x = 0; x < nXTiles; x++) {
				writer.setResolution(0)
				// The x and y coordinates for the current tile
				tileX = x * tileSizeX
				tileY = y * tileSizeY
				int effTileSizeX = tileSizeX
				if ((tileX + tileSizeX) >= width) {
					effTileSizeX = width - tileX;
				}
				int effTileSizeY = tileSizeY
				if ((tileY + tileSizeY) >= height) {
					effTileSizeY = height - tileY
				}
				// Read tiles from the input file and write them to the output OME-Tiff
				byte[] buf = reader.openBytes(image, tileX, tileY, effTileSizeX, effTileSizeY)
				writer.saveBytes(image, buf, tileX, tileY, effTileSizeX, effTileSizeY)
			}
		}

		// Create the downsampled resolutions and write to output
		for (int i = 0; i < resolutions; i++) {
			int currentScale = Math.pow(scale, i + 1)
			writer.setResolution(i + 1)
			int resolutionWidth = width / currentScale
			int resolutionHeight = height / currentScale
			nXTiles = resolutionWidth / tileSizeX
			nYTiles = resolutionHeight / tileSizeY
			if ((nXTiles * tileSizeX) != resolutionWidth) {
				nXTiles = nXTiles + 1
			}
			if ((nYTiles * tileSizeY) != resolutionHeight) {
				nYTiles = nYTiles + 1
			}
			for (int y = 0; y < nYTiles; y++) {
				for (int x = 0; x < nXTiles; x++) {
					tileX = x * tileSizeX
					tileY = y * tileSizeY
					effTileSizeX = tileSizeX * currentScale
					if (((tileX * currentScale) + effTileSizeX) >= width) {
						effTileSizeX = width - (tileX * currentScale)
					}
					effTileSizeY = tileSizeY * currentScale
					if (((tileY * currentScale) + effTileSizeY) >= height) {
						effTileSizeY = height - (tileY * currentScale)
					}
					byte[] tile = reader.openBytes(image, tileX * currentScale, tileY * currentScale, effTileSizeX, effTileSizeY)
					byte[] downsample = scaler.downsample(tile, effTileSizeX, effTileSizeY, currentScale, FormatTools.getBytesPerPixel(type), reader.isLittleEndian(), 
					    FormatTools.isFloatingPoint(type), reader.getRGBChannelCount(), reader.isInterleaved())
					writer.saveBytes(image, downsample, tileX, tileY, (int)(effTileSizeX / currentScale), (int)(effTileSizeY / currentScale))
				}
			}
		}
	}
}	

writer.close()
reader.close()

IJ.log("Done")

ImporterOptions options = new ImporterOptions()
options.setColorMode(ImporterOptions.COLOR_MODE_COMPOSITE)
options.setId(outFile)
options.setSeriesOn(2, true)
ImagePlus[] imps = BF.openImagePlus(options)
imps[0].show()