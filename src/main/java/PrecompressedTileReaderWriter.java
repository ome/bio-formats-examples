/*
 * #%L
 * Bio-Formats examples
 * %%
 * Copyright (C) 2005 - 2025 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import java.io.IOException;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ICompressedTileReader;
import loci.formats.ICompressedTileWriter;
import loci.formats.IFormatReader;
import loci.formats.IFormatWriter;
import loci.formats.ImageReader;
import loci.formats.ImageWriter;
import loci.formats.FormatTools;
import loci.formats.MetadataTools;
import loci.formats.codec.CompressionType;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

/**
 */
public class PrecompressedTileReaderWriter {

  /** The file format reader. */
  private IFormatReader reader;

  /** The file format writer. */
  private IFormatWriter writer;

  /** The file to be read. */
  private String inputFile;

  /** The file to be written. */
  private String outputFile;

  /**
   * Construct a new PrecompressedTileReaderWriter to read the specified input file
   * and write the given output file. Tile sizes are calculated automatically.
   *
   * @param inputFile the file to be read
   * @param outputFile the file to be written
   */
  public PrecompressedTileReaderWriter(String inputFile, String outputFile) {
    this.inputFile = inputFile;
    this.outputFile = outputFile;
  }

  /**
   * Set up the file reader and writer, ensuring that the input file is
   * associated with the reader and the output file is associated with the
   * writer.
   *
   * @throws DependencyException thrown if failed to create an OMEXMLService
   * @throws IOException thrown if unable to setup input or output stream for reader or writer
   * @throws FormatException thrown if invalid ID set for reader or writer or invalid tile size set
   * @throws ServiceException thrown if unable to create OME-XML meta data
   */
  private void initialize() throws DependencyException, FormatException, IOException, ServiceException {
    // construct the object that stores OME-XML metadata
    ServiceFactory factory = new ServiceFactory();
    OMEXMLService service = factory.getInstance(OMEXMLService.class);
    IMetadata omexml = service.createOMEXMLMetadata();

    // set up the reader and associate it with the input file
    ImageReader baseReader = new ImageReader();
    baseReader.setFlattenedResolutions(false);
    baseReader.setMetadataStore(omexml);
    baseReader.setId(inputFile);
    reader = baseReader.getReader();

    MetadataTools.populatePixelsOnly(omexml, reader);

    ICompressedTileReader tileReader = (ICompressedTileReader) reader;
    CompressionType type = CompressionType.get(tileReader.getTileCodec(0));
    String compression = type.getCompression();

    // set up the writer and associate it with the output file
    ImageWriter baseWriter = new ImageWriter();
    // indicating that tiles will be written in their natural order
    // is usually important
    baseWriter.setWriteSequentially(true);
    baseWriter.setMetadataRetrieve(omexml);
    baseWriter.setInterleaved(reader.isInterleaved() || compression.startsWith("JPEG"));

    // set the tile size height and width for writing
    // tile size must match between reader and writer when
    // working with precompressed tiles
    baseWriter.setTileSizeX(reader.getOptimalTileWidth());
    baseWriter.setTileSizeY(reader.getOptimalTileHeight());

    baseWriter.setId(outputFile);
    writer = baseWriter.getWriter();

    // input and output compression types need to match
    writer.setCompression(compression);
  }

  /**
   * Read tiles from input file and write tiles to output file.
   *
   * @throws IOException thrown if unable to setup input or output stream for reader or writer
   * @throws FormatException thrown by FormatWriter if attempting to set invalid series
   */
  public void readWriteTiles() throws FormatException, IOException {
    byte[] buf = null;

    for (int series=0; series<reader.getSeriesCount(); series++) {
      reader.setSeries(series);
      writer.setSeries(series);

      for (int res=0; res<reader.getResolutionCount(); res++) {
        reader.setResolution(res);
        writer.setResolution(res);

        // tile size can vary across resolutions,
        // so make sure it gets updated before starting to write
        int tileWidth = reader.getOptimalTileWidth();
        int tileHeight = reader.getOptimalTileHeight();
        writer.setTileSizeX(tileWidth);
        writer.setTileSizeY(tileHeight);

        // in practice, warning and switching to a normal decompress/recompress
        // workflow here may be a better strategy
        // an exception is thrown here to make it very clear that not all
        // input/output combinations can be used with this feature
        if (!FormatTools.canUsePrecompressedTiles(reader, writer, series, res)) {
          throw new FormatException("Cannot use precompressed tiles for series " +
            series + ", resolution " + res);
        }

        // convert each image in the current series
        for (int image=0; image<reader.getImageCount(); image++) {
          ICompressedTileReader tileReader = (ICompressedTileReader) reader;

          // precompressed API operates on tile row/column indexes, not XY pixel coordinates
          // this is to prevent trying to read a tile that doesn't align with the boundaries
          // of the compressed tile
          int nXTiles = tileReader.getTileColumns(image);
          int nYTiles = tileReader.getTileRows(image);

          for (int y=0; y<nYTiles; y++) {
            for (int x=0; x<nXTiles; x++) {
              // the x and y coordinates for the current tile
              int tileX = x * tileWidth;
              int tileY = y * tileHeight;
              int width = (int) Math.min(tileWidth, reader.getSizeX() - tileX);
              int height = (int) Math.min(tileHeight, reader.getSizeY() - tileY);

              // read tiles from the input file and write them to the output file
              buf = tileReader.openCompressedBytes(image, x, y);
              ((ICompressedTileWriter) writer).saveCompressedBytes(image, buf, tileX, tileY, width, height);
            }
          }
        }
      }
    }
  }

  /** Close the file reader and writer. */
  private void cleanup() {
    try {
      reader.close();
    }
    catch (IOException e) {
      System.err.println("Failed to close reader.");
      e.printStackTrace();
    }
    try {
      writer.close();
    }
    catch (IOException e) {
      System.err.println("Failed to close writer.");
      e.printStackTrace();
    }
  }

  /**
   * To read an image file and write out a tiled image file on the command line:
   *
   * $ java PrecompressedTileReaderWriter input-file.svs output-file.dcm
   *
   * @param args input file, output file
   */
  public static void main(String[] args) throws Exception {
    PrecompressedTileReaderWriter tiledReadWriter = new PrecompressedTileReaderWriter(args[0], args[1]);
    // initialize the files
    tiledReadWriter.initialize();

    try {
      // read and write the image using tiles
      tiledReadWriter.readWriteTiles();
    }
    finally {
      // close the files
      tiledReadWriter.cleanup();
    }
  }

}
