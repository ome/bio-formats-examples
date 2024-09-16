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
import loci.formats.ImageReader;
import loci.formats.ImageWriter;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

/**
 * Example class for converting a file from one format to another, using
 * Bio-Formats version 4.2 or later.
 *
 * @author Melissa Linkert melissa at glencoesoftware.com
 */
public class FileConvert {

  /** The file format reader. */
  private ImageReader reader;

  /** The file format writer. */
  private ImageWriter writer;

  /** The file to be read. */
  private String inputFile;

  /** The file to be written. */
  private String outputFile;

  /**
   * Construct a new FileConvert to convert the specified input file.
   *
   * @param inputFile the file to be read
   * @param outputFile the file to be written
   */
  public FileConvert(String inputFile, String outputFile) {
    this.inputFile = inputFile;
    this.outputFile = outputFile;
  }

  /** Do the actual work of converting the input file to the output file. */
  public void convert() {
    // initialize the files
    boolean initializationSuccess = initialize();

    // if we could not initialize one of the files,
    // then it does not make sense to convert the planes
    if (initializationSuccess) {
      convertPlanes();
    }

    // close the files
    cleanup();
  }

  /**
   * Set up the file reader and writer, ensuring that the input file is
   * associated with the reader and the output file is associated with the
   * writer.
   *
   * @return true if the reader and writer were successfully set up, or false
   *   if an error occurred
   */
  private boolean initialize() {
    Exception exception = null;
    try {
      // construct the object that stores OME-XML metadata
      ServiceFactory factory = new ServiceFactory();
      OMEXMLService service = factory.getInstance(OMEXMLService.class);
      IMetadata omexml = service.createOMEXMLMetadata();

      // set up the reader and associate it with the input file
      reader = new ImageReader();
      reader.setMetadataStore(omexml);
      reader.setId(inputFile);

      // set up the writer and associate it with the output file
      writer = new ImageWriter();
      writer.setMetadataRetrieve(omexml);
      writer.setInterleaved(reader.isInterleaved());
      writer.setId(outputFile);
    }
    catch (FormatException e) {
      exception = e;
    }
    catch (IOException e) {
      exception = e;
    }
    catch (DependencyException e) {
      exception = e;
    }
    catch (ServiceException e) {
      exception = e;
    }
    if (exception != null) {
      System.err.println("Failed to initialize files.");
      exception.printStackTrace();
    }
    return exception == null;
  }

  /** Save every plane in the input file to the output file. */
  private void convertPlanes() {
    for (int series=0; series<reader.getSeriesCount(); series++) {
      // tell the reader and writer which series to work with
      // in FV1000 OIB/OIF, there are at most two series - one
      // is the actual data, and one is the preview image
      reader.setSeries(series);
      try {
        writer.setSeries(series);
      }
      catch (FormatException e) {
        System.err.println("Failed to set writer's series #" + series);
        e.printStackTrace();
        break;
      }

      // construct a buffer to hold one image's pixels
      byte[] plane = new byte[FormatTools.getPlaneSize(reader)];

      // convert each image in the current series
      for (int image=0; image<reader.getImageCount(); image++) {
        try {
          reader.openBytes(image, plane);
          writer.saveBytes(image, plane);
        }
        catch (IOException e) {
          System.err.println("Failed to convert image #" + image +
            " in series #" + series);
          e.printStackTrace();
        }
        catch (FormatException e) {
          System.err.println("Failed to convert image #" + image +
            " in series #" + series);
          e.printStackTrace();
        }
      }
    }
  }

  /** Close the file reader and writer. */
  private void cleanup() {
    try {
      reader.close();
      writer.close();
    }
    catch (IOException e) {
      System.err.println("Failed to cleanup reader and writer.");
      e.printStackTrace();
    }
  }

  /**
   * To convert a file on the command line:
   * 
   * $ java FileConvert input-file.oib output-file.ome.tiff
   * @param args Input File and Output file.
   */
  public static void main(String[] args) {
    FileConvert converter = new FileConvert(args[0], args[1]);
    converter.convert();
  }

}
