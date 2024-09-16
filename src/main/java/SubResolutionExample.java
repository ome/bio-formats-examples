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

import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;

/**
 * Demonstration of the sub-resolution API.
 */
public class SubResolutionExample {

  /**
   * Use the sub-resolution API.
   *
   * @param args inputFile
   * @throws IOException thrown if unable to setup input or output stream for reader or writer
   * @throws FormatException thrown when setting invalid values in reader or writer 
   */
  public static void main(String[] args) throws FormatException, IOException {
    // parse command line arguments
    if (args.length != 1) {
      System.err.println("Usage: java SubResolutionExample imageFile");
      System.exit(1);
    }
    String id = args[0];

    // configure reader
    IFormatReader reader = new ImageReader();
    reader.setFlattenedResolutions(false);
    System.out.println("Initializing file: " + id);
    reader.setId(id); // parse metadata

    int seriesCount = reader.getSeriesCount();

    System.out.println("  Series count = " + seriesCount);

    for (int series=0; series<seriesCount; series++) {
      reader.setSeries(series);
      int resolutionCount = reader.getResolutionCount();

      System.out.println("    Resolution count for series #" + series +
        " = " + resolutionCount);

      for (int r=0; r<resolutionCount; r++) {
        reader.setResolution(r);
        System.out.println("      Resolution #" + r + " dimensions = " +
          reader.getSizeX() + " x " + reader.getSizeY());
      }
    }

    reader.close();
  }

}
