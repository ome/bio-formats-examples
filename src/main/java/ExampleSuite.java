/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
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

import java.net.URL;
import java.nio.file.Files;
import java.io.File;


public class ExampleSuite {

  public static void execute(String name, String[] args) throws Exception {
    System.out.println("Executing " + name);
    Class<?> c = Class.forName(name);
    Object passedArgs[] = {args};
    c.getMethod("main", args.getClass()).invoke(null, passedArgs);
    System.out.println("Success");
  }

  /**
   * Execute a series of examples using the test files
   *
   * $ java ExampleSuite
   * @param args Possible configuration
   * @throws java.lang.Exception thrown if an error occurred while reading data.
   */
  public static void main(String[] args) throws Exception {

    // Retrieve local test files
    URL resource =  ExampleSuite.class.getResource("test.fake");
    URL overlappedResource =  ExampleSuite.class.getResource("test&sizeX=1024&sizeY=1024.fake");
    URL pyramidResource =  ExampleSuite.class.getResource("test&resolutions=4.fake");
    File inputFile = new File(resource.toURI());
    File overlappedInputFile = new File(overlappedResource.toURI());
    File pyramidFile = new File(pyramidResource.toURI());
    File parentDir = inputFile.getParentFile();
    File convertedFile = new File(parentDir, "converted.ome.tiff");
    File exportFile = new File(parentDir, "export.ome.tiff");
    File exportSPWFile = new File(parentDir, "exportSPW.ome.tiff");
    File simpleTiledFile = new File(parentDir, "simpleTiledFile.ome.tiff");
    File tiledFile = new File(parentDir, "tiledFile.ome.tiff");
    File tiledFile2 = new File(parentDir, "tiledFile2.ome.tiff");
    File overlappedTiledFile = new File(parentDir, "overlappedTiledFile.ome.tiff");
    File overlappedTiledFile2 = new File(parentDir, "overlappedTiledFile2.ome.tiff");
    File inMemoryFile = new File(parentDir, inputFile.getName() +".ome.tif");
    File orthogonalFile = new File(parentDir, "orthogonal.ome.tiff");
    File pyramidOutputFile = new File(parentDir, "generatedPyramid.ome.tiff");
    
    // Remove any existing output files
    Files.deleteIfExists(convertedFile.toPath());
    Files.deleteIfExists(exportFile.toPath());
    Files.deleteIfExists(exportSPWFile.toPath());
    Files.deleteIfExists(simpleTiledFile.toPath());
    Files.deleteIfExists(tiledFile.toPath());
    Files.deleteIfExists(tiledFile2.toPath());
    Files.deleteIfExists(overlappedTiledFile.toPath());
    Files.deleteIfExists(overlappedTiledFile2.toPath());
    Files.deleteIfExists(inMemoryFile.toPath());
    Files.deleteIfExists(orthogonalFile.toPath());
    Files.deleteIfExists(pyramidOutputFile.toPath());

    // Execute examples
    execute("ReadPhysicalSize", new String[] {inputFile.getAbsolutePath()});
    execute("FileConvert", new String[] {
      inputFile.getAbsolutePath(), convertedFile.getAbsolutePath()});
    execute("FileExport", new String[] {exportFile.getAbsolutePath()});
    execute("FileExportSPW", new String[] {exportSPWFile.getAbsolutePath()});
    execute("SimpleTiledWriter", new String[] {
        inputFile.getAbsolutePath(), simpleTiledFile.getAbsolutePath(), "256", "256"});
    execute("TiledReaderWriter", new String[] {
        inputFile.getAbsolutePath(), tiledFile.getAbsolutePath(), "256", "256"});
    execute("TiledReaderWriter", new String[] {
        inputFile.getAbsolutePath(), tiledFile2.getAbsolutePath(), "256", "128"});
    execute("OverlappedTiledWriter", new String[] {
        overlappedInputFile.getAbsolutePath(), overlappedTiledFile.getAbsolutePath(), "96", "96"});
    execute("OverlappedTiledWriter", new String[] {
        overlappedInputFile.getAbsolutePath(), overlappedTiledFile2.getAbsolutePath(), "192", "96"});
    execute("ReadWriteInMemory", new String[] {inputFile.getAbsolutePath()});
    execute("OrthogonalReader", new String[] {"--input", inputFile.getAbsolutePath(),
      "--output", orthogonalFile.getAbsolutePath()});
    execute("SubResolutionExample", new String[] { pyramidFile.getAbsolutePath()});
    execute("GeneratePyramidResolutions", new String[] {
      overlappedInputFile.getAbsolutePath(), "2", "4", pyramidOutputFile.getAbsolutePath()});
  }
}
