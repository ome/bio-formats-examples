/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2017 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

import java.io.IOException;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.FormatTools;
import loci.formats.meta.IMetadata;
import loci.formats.out.OMETiffWriter;
import loci.formats.services.OMEXMLService;

/**
 * Example class for reading and writing a file in a tiled OME-Tiff format.
 *
 * @author David Gault dgault at dundee.ac.uk
 */
public class TiledReaderWriter {

  /** The file format reader. */
  private ImageReader reader;

  /** The file format writer. */
  private OMETiffWriter writer;

  /** The file to be read. */
  private String inputFile;

  /** The file to be written. */
  private String outputFile;

  /** The tile width to be used. */
  private int tileSizeX;

  /** The tile height to be used. */
  private int tileSizeY;

  /**
   * Construct a new TiledReaderWriter to read the specified input file 
   * and write the given output file using the tile sizes provided.
   *
   * @param inputFile the file to be read
   * @param outputFile the file to be written
   * @param tileSizeX the width of tile to attempt to use
   * @param tileSizeY the height of tile to attempt to use
   */
  public TiledReaderWriter(String inputFile, String outputFile, int tileSizeX, int tileSizeY) {
    this.inputFile = inputFile;
    this.outputFile = outputFile;
    this.tileSizeX = tileSizeX;
    this.tileSizeY = tileSizeY;
  }

  /**
   * Set up the file reader and writer, ensuring that the input file is
   * associated with the reader and the output file is associated with the
   * writer.
   *
   * @return true if the reader and writer were successfully set up, or false
   *   if an error occurred
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
    reader = new ImageReader();
    reader.setMetadataStore(omexml);
    reader.setId(inputFile);

    // set up the writer and associate it with the output file
    writer = new OMETiffWriter();
    writer.setMetadataRetrieve(omexml);
    writer.setInterleaved(reader.isInterleaved());

    // set the tile size height and width for writing
    this.tileSizeX = writer.setTileSizeX(tileSizeX);
    this.tileSizeY = writer.setTileSizeY(tileSizeY);

    writer.setId(outputFile);
  }

  /** Read tiles from input file and write tiles to output OME-Tiff. 
   * @throws IOException thrown if unable to setup input or output stream for reader or writer
   * @throws FormatException thrown by FormatWriter if attempting to set invalid series
   */
  public void readWriteTiles() throws FormatException, IOException {
    int bpp = FormatTools.getBytesPerPixel(reader.getPixelType());
    int tilePlaneSize = tileSizeX * tileSizeY * reader.getRGBChannelCount() * bpp;
    byte[] buf = new byte[tilePlaneSize];

    for (int series=0; series<reader.getSeriesCount(); series++) {
      reader.setSeries(series);
      writer.setSeries(series);

      // convert each image in the current series
      for (int image=0; image<reader.getImageCount(); image++) {
        /* tiling-calculations-example-start */
        int width = reader.getSizeX();
        int height = reader.getSizeY();

        // Determined the number of tiles to read and write
        int nXTiles = width / tileSizeX;
        int nYTiles = height / tileSizeY;
        if (nXTiles * tileSizeX != width) nXTiles++;
        if (nYTiles * tileSizeY != height) nYTiles++;
        /* tiling-calculations-example-end */

        /* tiling-example-start */
        for (int y=0; y<nYTiles; y++) {
          for (int x=0; x<nXTiles; x++) {
            // The x and y coordinates for the current tile
            int tileX = x * tileSizeX;
            int tileY = y * tileSizeY;

            // Read tiles from the input file and write them to the output OME-Tiff
            buf = reader.openBytes(image, tileX, tileY, tileSizeX, tileSizeY);
            writer.saveBytes(image, buf, tileX, tileY, tileSizeX, tileSizeY);
          }
        }
        /* tiling-example-end */
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
   * To read an image file and write out an OME-Tiff tiled image on the command line:
   *
   * $ java TiledReaderWriter input-file.oib output-file.ome.tiff 256 256
   * @param args inputFile, outputFile, tileSizeX and tileSizeY
   * @throws IOException thrown if unable to setup input or output stream for reader or writer
   * @throws FormatException thrown when setting invalid values in reader or writer
   * @throws ServiceException thrown if unable to create OME-XML meta data
   * @throws DependencyException thrown if failed to create an OMEXMLService
   */
  public static void main(String[] args) throws FormatException, IOException, DependencyException, ServiceException {
    int tileSizeX = Integer.parseInt(args[2]);
    int tileSizeY = Integer.parseInt(args[3]);
    TiledReaderWriter tiledReadWriter = new TiledReaderWriter(args[0], args[1], tileSizeX, tileSizeY);
    // initialize the files
    tiledReadWriter.initialize();

    try {
      // read and write the image using tiles
      tiledReadWriter.readWriteTiles();
    }
    catch(Exception e) {
      System.err.println("Failed to read and write tiles.");
      e.printStackTrace();
      throw e;
    }
    finally {
      // close the files
      tiledReadWriter.cleanup();
    }
  }

}
