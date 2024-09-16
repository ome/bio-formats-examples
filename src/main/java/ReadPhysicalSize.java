/*
 * #%L
 * Bio-Formats examples
 * %%
 * Copyright (C) 2016 - 2017 Open Microscopy Environment:
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

import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;

/**
 * Example class that shows how to read and convert the physical X, Y and Z dimensions of a file
 * Bio-Formats version 5.1 or later.
 */
public class ReadPhysicalSize {

  /**
   * Reads the physical dimensions of the input file provided then converts and displays them in micrometers
   *
   * @param inputFile the file to be read
   * @throws FormatException if a parsing error occurs processing the file.
   * @throws IOException if an I/O error occurs processing the file
   */
  public static void readPhysicalSize(final String inputFile) throws FormatException, IOException {
    final ImageReader reader = new ImageReader();
    final IMetadata omeMeta = MetadataTools.createOMEXMLMetadata();
    reader.setMetadataStore(omeMeta);
    reader.setId(inputFile);

    final Unit<Length> targetUnit = UNITS.MICROMETER;

    for (int image=0; image<omeMeta.getImageCount(); image++) {
      final Length physSizeX = omeMeta.getPixelsPhysicalSizeX(image);
      final Length physSizeY = omeMeta.getPixelsPhysicalSizeY(image);
      final Length physSizeZ = omeMeta.getPixelsPhysicalSizeZ(image);

      System.out.println("Physical calibration - Image: " + image);

      if (physSizeX != null) {
        final Length convertedSizeX = new Length(physSizeX.value(targetUnit), targetUnit);
        System.out.println("\tX = " + physSizeX.value() + " " + physSizeX.unit().getSymbol()
            + " = " + convertedSizeX.value() + " " + convertedSizeX.unit().getSymbol());
      }
      if (physSizeY != null) {
        final Length convertedSizeY = new Length(physSizeY.value(targetUnit), targetUnit);
        System.out.println("\tY = " + physSizeY.value() + " " + physSizeY.unit().getSymbol()
            + " = " + convertedSizeY.value() + " " + convertedSizeY.unit().getSymbol());
      }
      if (physSizeZ != null) {
        final Length convertedSizeZ = new Length(physSizeZ.value(targetUnit), targetUnit);
        System.out.println("\tZ = " + physSizeZ.value() + " " + physSizeZ.unit().getSymbol()
            + " = " + convertedSizeZ.value() + " " + convertedSizeZ.unit().getSymbol());
      }
    }
    reader.close();
  }
    
  /**
   * To read the physical size dimensions and units of a file and display them in micrometers:
   *
   * $ java ReadPhysicalSize input-file.ome.tiff
   * @param args Input file.
   * @throws FormatException if a parsing error occurs processing the file.
   * @throws IOException if an I/O error occurs processing the file
   */
  public static void main(String[] args) throws Exception {
    readPhysicalSize(args[0]);
  }

}
