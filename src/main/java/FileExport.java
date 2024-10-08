/*
 * #%L
 * Bio-Formats examples
 * %%
 * Copyright (C) 2005 - 2017 Open Microscopy Environment:
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
import loci.formats.FormatTools;
import loci.formats.ImageWriter;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;

/**
 * Example class that shows how to export raw pixel data to OME-TIFF using
 * Bio-Formats version 4.2 or later.
 */
public class FileExport {

  /** The file writer. */
  private ImageWriter writer;

  /** The name of the output file. */
  private String outputFile;

  /**
   * Construct a new FileExport that will save to the specified file.
   *
   * @param outputFile the file to which we will export
   */
  public FileExport(String outputFile) {
    this.outputFile = outputFile;
  }

  /** Save a single 512x512 uint16 plane of random data. */
  public void export() {
    int width = 512, height = 512;
    int pixelType = FormatTools.UINT16;

    IMetadata omexml = initializeMetadata(width, height, pixelType);

    // only save a plane if the file writer was initialized successfully
    boolean initializationSuccess = initializeWriter(omexml);
    if (initializationSuccess) {
      savePlane(width, height, pixelType);
    }
    cleanup();
  }

  /**
   * Set up the file writer.
   *
   * @param omexml the IMetadata object that is to be associated with the writer
   * @return true if the file writer was successfully initialized; false if an
   *   error occurred
   */
  private boolean initializeWriter(IMetadata omexml) {
    // create the file writer and associate the OME-XML metadata with it
    writer = new ImageWriter();
    writer.setMetadataRetrieve(omexml);

    Exception exception = null;
    try {
      writer.setId(outputFile);
    }
    catch (FormatException e) {
      exception = e;
    }
    catch (IOException e) {
      exception = e;
    }
    if (exception != null) {
      System.err.println("Failed to initialize file writer.");
      exception.printStackTrace();
    }
    return exception == null;
  }

  /**
   * Populate the minimum amount of metadata required to export an image.
   *
   * @param width the width (in pixels) of the image
   * @param height the height (in pixels) of the image
   * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
   */
  private IMetadata initializeMetadata(int width, int height, int pixelType) {
    Exception exception = null;
    try {
      // create the OME-XML metadata storage object
      ServiceFactory factory = new ServiceFactory();
      OMEXMLService service = factory.getInstance(OMEXMLService.class);
      IMetadata meta = service.createOMEXMLMetadata();
      meta.createRoot();

      // define each stack of images - this defines a single stack of images
      meta.setImageID("Image:0", 0);
      meta.setPixelsID("Pixels:0", 0);

      // specify that the pixel data is stored in big-endian format
      // change 'TRUE' to 'FALSE' to specify little-endian format
      meta.setPixelsBinDataBigEndian(Boolean.TRUE, 0, 0);

      // specify that the images are stored in ZCT order
      meta.setPixelsDimensionOrder(DimensionOrder.XYZCT, 0);

      // specify that the pixel type of the images
      meta.setPixelsType(
        PixelType.fromString(FormatTools.getPixelTypeString(pixelType)), 0);

      // specify the dimensions of the images
      meta.setPixelsSizeX(new PositiveInteger(width), 0);
      meta.setPixelsSizeY(new PositiveInteger(height), 0);
      meta.setPixelsSizeZ(new PositiveInteger(1), 0);
      meta.setPixelsSizeC(new PositiveInteger(1), 0);
      meta.setPixelsSizeT(new PositiveInteger(1), 0);

      // define each channel and specify the number of samples in the channel
      // the number of samples is 3 for RGB images and 1 otherwise
      meta.setChannelID("Channel:0:0", 0, 0);
      meta.setChannelSamplesPerPixel(new PositiveInteger(1), 0, 0);

      return meta;
    }
    catch (DependencyException e) {
      exception = e;
    }
    catch (ServiceException e) {
      exception = e;
    }
    catch (EnumerationException e) {
      exception = e;
    }

    System.err.println("Failed to populate OME-XML metadata object.");
    exception.printStackTrace();
    return null;
  }

  /**
   * Generate a random plane of pixel data and save it to the output file.
   *
   * @param width the width of the image in pixels
   * @param height the height of the image in pixels
   * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
   */
  private void savePlane(int width, int height, int pixelType) {
    byte[] plane = createImage(width, height, pixelType);
    Exception exception = null;
    try {
      writer.saveBytes(0, plane);
    }
    catch (FormatException e) {
      exception = e;
    }
    catch (IOException e) {
      exception = e;
    }
    if (exception != null) {
      System.err.println("Failed to save plane.");
      exception.printStackTrace();
    }
  }

  /**
   * Generate a random plane of pixel data.
   *
   * @param width the width of the image in pixels
   * @param height the height of the image in pixels
   * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
   */
  private byte[] createImage(int width, int height, int pixelType) {
    // create a blank image of the specified size
    byte[] img =
      new byte[width * height * FormatTools.getBytesPerPixel(pixelType)];

    // fill it with random data
    for (int i=0; i<img.length; i++) {
      img[i] = (byte) (256 * Math.random());
    }
    return img;
  }

  /** Close the file writer. */
  private void cleanup() {
    try {
      writer.close();
    }
    catch (IOException e) {
      System.err.println("Failed to close file writer.");
      e.printStackTrace();
    }
  }

  /**
   * To export a file to OME-TIFF:
   * 
   * $ java FileExport output-file.ome.tiff
   * @param args Output file.
   * @throws java.lang.Exception thrown if an error occurred while exporting data.
   */
  public static void main(String[] args) throws Exception {
    FileExport exporter = new FileExport(args[0]);
    exporter.export();
  }

}
