JPEGLosslessDecoder
=====
A common DICOM compression format is JPEG Lossless.  This format is generally not supported in standard JPEG decoder libraries. 

This decoder can read data from the following DICOM transfer syntaxes:

- 1.2.840.10008.1.2.4.57    JPEG Lossless, Nonhierarchical (Processes 14)
- 1.2.840.10008.1.2.4.70    JPEG Lossless, Nonhierarchical (Processes 14 [Selection 1])

###Usage
```java
JPEGLosslessDecoder decoder = new JPEGLosslessDecoder(compressedBytes);

// single component
int[] decompressedData = decoder.decode()[0];  

// rgb components
final int[][] decompressedData = decoder.decode();
final int[] redData = decompressedData[0];
final int[] greenData = decompressedData[1];
final int[] blueData = decompressedData[2];
```

### Building
```unix
ant build.xml
```

###Acknowledgments
This decoder was originally written by Helmut Dersch.  I added support for selection values 2 through 7, contributed bug fixes and code cleanup.
