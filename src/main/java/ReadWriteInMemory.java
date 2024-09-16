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

import java.io.*;
import loci.common.ByteArrayHandle;
import loci.common.Location;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.*;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;

/**
 * Tests the Bio-Formats I/O logic to and from byte arrays in memory.
 */
public class ReadWriteInMemory {


  /**
   * Reads to and from byte arrays in memory.
   * 
   * @param args Input file.
   * @throws IOException thrown if unable to setup input or output stream for reader or writer
   * @throws FormatException thrown when setting invalid values in reader or writer
   * @throws ServiceException thrown if unable to create OME-XML meta data
   * @throws DependencyException thrown if failed to create an OMEXMLService
   */
  public static void main(String[] args)
    throws DependencyException, FormatException, IOException, ServiceException
  {
    if (args.length < 1) {
      System.out.println("Please specify a (small) image file.");
      System.exit(1);
    }
    String path = args[0];

    /* file-read-start */
    // read in entire file
    System.out.println("Reading file into memory from disk...");
    File inputFile = new File(path);
    int fileSize = (int) inputFile.length();
    DataInputStream in = new DataInputStream(new FileInputStream(inputFile));
    byte[] inBytes = new byte[fileSize];
    in.readFully(inBytes);
    System.out.println(fileSize + " bytes read.");
    /* file-read-end */

    /* mapping-start */
    // determine input file suffix
    String fileName = inputFile.getName();
    int dot = fileName.lastIndexOf(".");
    String suffix = dot < 0 ? "" : fileName.substring(dot);

    // map input id string to input byte array
    String inId = "inBytes" + suffix;
    Location.mapFile(inId, new ByteArrayHandle(inBytes));
    /* mapping-end */

    // read data from byte array using ImageReader
    System.out.println();
    System.out.println("Reading image data from memory...");

    /* read-start */
    ServiceFactory factory = new ServiceFactory();
    OMEXMLService service = factory.getInstance(OMEXMLService.class);
    IMetadata omeMeta = service.createOMEXMLMetadata();

    ImageReader reader = new ImageReader();
    reader.setMetadataStore(omeMeta);
    reader.setId(inId);
    /* read-end */

    int seriesCount = reader.getSeriesCount();
    int imageCount = reader.getImageCount();
    int sizeX = reader.getSizeX();
    int sizeY = reader.getSizeY();
    int sizeZ = reader.getSizeZ();
    int sizeC = reader.getSizeC();
    int sizeT = reader.getSizeT();

    // output some details
    System.out.println("Series count: " + seriesCount);
    System.out.println("First series:");
    System.out.println("\tImage count = " + imageCount);
    System.out.println("\tSizeX = " + sizeX);
    System.out.println("\tSizeY = " + sizeY);
    System.out.println("\tSizeZ = " + sizeZ);
    System.out.println("\tSizeC = " + sizeC);
    System.out.println("\tSizeT = " + sizeT);

    /* out—mapping-start */
    // map output id string to output byte array
    String outId = fileName + ".ome.tif";
    ByteArrayHandle outputFile = new ByteArrayHandle();
    Location.mapFile(outId, outputFile);
    /* out—mapping-end */

    /* write—init-start */
    // write data to byte array using ImageWriter
    System.out.println();
    System.out.print("Writing planes to destination in memory: ");
    ImageWriter writer = new ImageWriter();
    writer.setMetadataRetrieve(omeMeta);
    writer.setId(outId);
    /* write—init-end */

    /* write-start */
    byte[] plane = null;
    for (int i=0; i<imageCount; i++) {
      if (plane == null) {
        // allow reader to allocate a new byte array
        plane = reader.openBytes(i);
      }
      else {
        // reuse previously allocated byte array
        reader.openBytes(i, plane);
      }
      writer.saveBytes(i, plane);
      System.out.print(".");
    }
    reader.close();
    writer.close();
    System.out.println();

    byte[] outBytes = outputFile.getBytes();
    outputFile.close();
    /* write-end */

    /* flush-start */
    // flush output byte array to disk
    System.out.println();
    System.out.println("Flushing image data to disk...");
    File outFile = new File(fileName + ".ome.tif");
    DataOutputStream out = new DataOutputStream(new FileOutputStream(outFile));
    out.write(outBytes);
    out.close();
    /* flush-end */
  }

}
