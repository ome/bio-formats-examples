/*
 * #%L
 * Bio-Formats examples
 * %%
 * Copyright (C) 2018 Open Microscopy Environment:
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

import java.util.Arrays;

import loci.common.image.IImageScaler;
import loci.common.image.SimpleImageScaler;
import loci.common.services.ServiceFactory;
import loci.formats.*;
import loci.formats.ome.OMEPyramidStore;
import loci.formats.services.OMEXMLService;

import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;

/**
 * Demonstrates writing an image pyramid the source dataset
 * only contains the full resolution image.
 */
public class GeneratePyramidResolutions {

  public static void main(String[] args) throws Exception {
    if (args.length < 4) {
      System.out.println("GeneratePyramidResolutions input-file scale-factor resolution-count output-file");
      System.exit(1);
    }
    String in = args[0];
    String out = args[3];
    int scale = Integer.parseInt(args[1]);
    int resolutions = Integer.parseInt(args[2]);

    ImageReader reader = new ImageReader();
    ServiceFactory factory = new ServiceFactory();
    OMEXMLService service = factory.getInstance(OMEXMLService.class);
    OMEPyramidStore meta = (OMEPyramidStore) service.createOMEXMLMetadata();
    reader.setMetadataStore(meta);

    reader.setId(in);

    for (int i=1; i<resolutions; i++) {
      int divScale = (int) Math.pow(scale, i);
      meta.setResolutionSizeX(new PositiveInteger(reader.getSizeX() / divScale), 0, i);
      meta.setResolutionSizeY(new PositiveInteger(reader.getSizeY() / divScale), 0, i);
    }

    IImageScaler scaler = new SimpleImageScaler();
    byte[] img = reader.openBytes(0);

    // write image plane to disk
    System.out.println("Writing image to '" + out + "'...");
    IFormatWriter writer = new ImageWriter();
    writer.setMetadataRetrieve(meta);
    writer.setId(out);
    writer.saveBytes(0, img);
    int type = reader.getPixelType();
    for (int i=1; i<resolutions; i++) {
      writer.setResolution(i);
      int x = meta.getResolutionSizeX(0, i).getValue();
      int y = meta.getResolutionSizeY(0, i).getValue();
      byte[] downsample = scaler.downsample(img, reader.getSizeX(),
        reader.getSizeY(), Math.pow(scale, i),
        FormatTools.getBytesPerPixel(type), reader.isLittleEndian(),
        FormatTools.isFloatingPoint(type), reader.getRGBChannelCount(),
        reader.isInterleaved());
      writer.saveBytes(0, downsample);
    }
    writer.close();
    reader.close();

    System.out.println("Done.");
  }

}
