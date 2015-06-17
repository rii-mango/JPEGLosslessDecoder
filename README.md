JPEGLosslessDecoder
=====
A common DICOM compression format is JPEG lossless.  This format is generally not supported in standard JPEG decoder libraries. 

This decoder can read the following DICOM transfer syntaxes:

- 1.2.840.10008.1.2.4.57    JPEG Lossless, Nonhierarchical (Processes 14)
- 1.2.840.10008.1.2.4.70    JPEG Lossless, Nonhierarchical (Processes 14 [Selection 1])

Acknowledgments
-----
This library was originally written by Helmut Dersch, later released by JNode.  I added support for selection values 2 to 7 along with some other minor changes.
