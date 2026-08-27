=== Deep Analysis: tmp/image/src (rust) -> src/commonMain/kotlin/io/github/kotlinmania/image (kotlin) ===

Scanning source codebase (rust)...
Codebase: tmp/image/src (rust)
  Files: 71
  Total imports: 526
  Most depended: error (3 dependents)

Scanning target codebase (kotlin)...
Codebase: src/commonMain/kotlin/io/github/kotlinmania/image (kotlin)
  Files: 129
  Total imports: 841
  Most depended: io.IoWrite (18 dependents)

Comparing codebases...
Computing AST similarities...

=== Codebase Comparison Report ===

Source: tmp/image/src (71 files)
Target: src/commonMain/kotlin/io/github/kotlinmania/image (129 files)
Scoring invariant: FnSim is required function body/parameter similarity. Class/type and symbol parity are reported beside it; whole-file shape is diagnostic only.

Matched:   71 files
Unmatched: 0 source, 3 target

=== Matched Files (by porting priority) ===

Source                        Target                        FnSim     Dependents FunctionParityTypeParity  Priority
 --------------------------------------------------------------------------------------------------------------------------
error                         image.Error                   0.58      3          9/11          12/12       3022304.2 
io                            io.Mod                        0.75      3          1/1           2/4         3020502.5 
math.rect                     math.Rect                     1.00      3          0/0           1/1         3000100.0 
pnm.autobreak                 pnm.AutoBreak                 0.66      2          7/7           1/1         2000803.4 
io.free_functions             io.FreeFunctions              0.59      1          10/16         1/1         1061704.1 
imageops.fast_blur            imageops.FastBlur             0.73      1          10/14         1/1         1041502.7 
metadata.cicp                 metadata.Cicp                 0.54      1          35/36         13/13       1014904.6 
images.sub_image              images.SubImage               0.55      1          18/18         5/6         1012404.5 
images.flat                   images.Flat                   0.59      1          51/51         10/10       1006104.1 
io.limits                     io.Limits                     0.62      1          9/9           2/2         1001103.8 
tga.header                    tga.Header                    0.53      1          7/7           2/2         1000904.7 
jpeg.transform                jpeg.Transform                0.74      1          1/1           0/0         1000102.6 
images.buffer                 images.Buffer                 0.15      0          35/93         11/25       731808.5  
bmp.decoder                   bmp.Decoder                   0.04      0          3/44          4/12        495609.6  
imageops.sample               imageops.Sample               0.34      0          31/63         2/5         356806.6  
images.dynimage               images.Dynimage               0.35      0          117/149       1/3         355206.5  
codecs.png                    png.Png                       0.08      0          3/28          3/7         293509.2  
pnm.decoder                   pnm.Decoder                   0.34      0          33/51         6/14        266506.6  
jpeg.encoder                  jpeg.Encoder                  0.35      0          25/46         5/6         225206.5  
avif.yuv                      avif.Yuv                      0.25      0          13/30         6/10        214007.5  
codecs.gif                    codecs.Gif                    0.28      0          15/29         3/7         183607.2  
pnm.encoder                   pnm.Encoder                   0.37      0          14/25         3/10        183506.3  
codecs.farbfeld               codecs.Farbfeld               0.17      0          6/22          2/3         172508.3  
codecs.openexr                codecs.Openexr                0.19      0          7/22          2/2         152408.1  
animation                     image.Animation               0.41      0          19/32         4/5         143705.9  
codecs.tiff                   codecs.Tiff                   0.26      0          9/22          2/3         142507.4  
color                         image.Color                   0.29      0          21/28         2/8         133607.1  
avif.decoder                  avif.Decoder                  0.14      0          5/15          1/4         131908.6  
images.buffer_par             images.BufferPar              0.23      0          9/19          4/5         112407.6  
io.image_reader_type          io.ImageReaderType            0.26      0          7/17          1/2         111907.4  
traits                        image.Traits                  0.13      0          7/13          9/13        102608.7  
ico.decoder                   ico.Decoder                   0.23      0          9/17          3/5         102207.7  
codecs.dds                    codecs.Dds                    0.37      0          5/10          1/5         91506.3   
avif.encoder                  avif.Encoder                  0.25      0          4/11          1/3         91407.5   
tga.encoder                   tga.Encoder                   0.46      0          18/25         3/3         72805.4   
webp.decoder                  webp.Decoder                  0.33      0          10/15         1/3         71806.7   
jpeg.decoder                  jpeg.Decoder                  0.25      0          10/15         1/2         61707.5   
imageops.colorops             imageops.Colorops             0.41      0          21/24         2/4         52805.9   
bmp.encoder                   bmp.Encoder                   0.57      0          13/18         1/1         51904.2   
images.generic_image          images.GenericImage           0.67      0          29/32         3/4         43603.3   
codecs.dxt                    codecs.Dxt                    0.64      0          16/18         2/3         32103.6   
pnm.mod                       pnm.Mod [STUB]                0.00      0          3/6           0/0         30610.0   
pnm.header                    pnm.Header                    0.64      0          13/14         9/10        22403.6   
hdr.encoder                   hdr.Encoder                   0.53      0          12/13         4/5         21804.7   
imageops.affine               imageops.Affine               0.55      0          21/22         0/0         12204.5   
io.decoder                    io.Decoder                    0.30      0          13/13         3/4         11707.0   
io.encoder                    io.Encoder                    0.56      0          4/5           3/3         10804.4   
hdr.decoder                   hdr.Decoder                   0.64      0          29/29         5/5         3403.6    
imageops.filter_1d            imageops.Filter1d             0.56      0          23/23         7/7         3004.4    
imageops.mod                  imageops.Mod [STUB]           0.00      0          26/26         0/0         2610.0    
hooks                         image.Hooks                   0.52      0          15/15         4/4         1904.8    
tga.decoder                   tga.Decoder                   0.63      0          12/12         3/3         1503.7    
io.format                     io.Format                     0.46      0          13/13         1/1         1405.4    
codecs.qoi                    codecs.Qoi                    0.60      0          9/9           2/2         1104.0    
metadata                      metadata.Mod                  0.46      0          7/7           2/2         905.4     
webp.encoder                  webp.Encoder                  0.56      0          8/8           1/1         904.4     
ico.encoder                   ico.Encoder                   0.68      0          7/7           2/2         903.2     
utils.mod                     utils.Mod [STUB]              0.00      0          7/7           0/0         710.0     
math.utils                    math.Utils                    0.73      0          7/7           0/0         702.7     
avif.ycgco                    avif.Ycgco                    0.71      0          4/4           0/0         402.9     
jpeg.entropy                  jpeg.Entropy                  0.76      0          2/2           0/0         202.4     
avif.mod                      avif.Mod [STUB]               0.00      0          0/0           0/0         10.0      
webp.mod                      webp.Mod [STUB]               0.00      0          0/0           0/0         10.0      
jpeg.mod                      jpeg.Mod [STUB]               0.00      0          0/0           0/0         10.0      
tga.mod                       tga.Mod [STUB]                0.00      0          0/0           0/0         10.0      
hdr.mod                       hdr.Mod [STUB]                0.00      0          0/0           0/0         10.0      
ico.mod                       ico.Mod [STUB]                0.00      0          0/0           0/0         10.0      
bmp.mod                       bmp.Mod [STUB]                0.00      0          0/0           0/0         10.0      
math.mod                      math.Mod [STUB]               0.00      0          0/0           0/0         10.0      
images                        images.Images                 1.00      0          0/0           0/0         0.0       
lib                           image.Lib                     1.00      0          0/0           0/0         0.0       

=== Function and Symbol Details ===

error -> image.Error
  similarity: 0.58, priority: 3022304.2, dependents: 3
  functions: 9/11 matched (target total: 52, required body score: 0.58)
  missing functions: kind, assert_send_sync
  types: 12/12 matched (target total: 36)
  missing types: none
  tests: 1/2 matched

io -> io.Mod
  similarity: 0.75, priority: 3020502.5, dependents: 3
  functions: 1/1 matched (target total: 1, required body score: 0.75)
  missing functions: none
  types: 2/4 matched (target total: 2)
  missing types: Limits, LimitSupport

math.rect -> math.Rect
  similarity: 1.00, priority: 3000100.0, dependents: 3
  functions: 0/0 matched (target total: 0, required body score: 1.00)
  missing functions: none
  types: 1/1 matched (target total: 1)
  missing types: none

pnm.autobreak -> pnm.AutoBreak
  similarity: 0.66, priority: 2000803.4, dependents: 2
  functions: 7/7 matched (target total: 8, required body score: 0.66)
  missing functions: none
  types: 1/1 matched (target total: 2)
  missing types: none
  tests: 2/2 matched

io.free_functions -> io.FreeFunctions
  similarity: 0.59, priority: 1061704.1, dependents: 1
  functions: 10/16 matched (target total: 25, required body score: 0.59)
  missing functions: dimensions, color_type, read_image, read_image_boxed, seek_scanline, read_scanline
  types: 1/1 matched (target total: 3)
  missing types: none
  tests: 2/8 matched

imageops.fast_blur -> imageops.FastBlur
  similarity: 0.73, priority: 1041502.7, dependents: 1
  functions: 10/14 matched (target total: 18, required body score: 0.73)
  missing functions: new, next_u32, next_u8, next_f32_in_range
  types: 1/1 matched (target total: 2)
  missing types: none
  tests: 1/5 matched

metadata.cicp -> metadata.Cicp
  similarity: 0.54, priority: 1014904.6, dependents: 1
  functions: 35/36 matched (target total: 51, required body score: 0.54)
  missing functions: no_coefficient_fallback
  types: 13/13 matched (target total: 14)
  missing types: none
  tests: 5/6 matched

images.sub_image -> images.SubImage
  similarity: 0.55, priority: 1012404.5, dependents: 1
  functions: 18/18 matched (target total: 24, required body score: 0.55)
  missing functions: none
  types: 5/6 matched (target total: 6)
  missing types: Pixel
  tests: 2/2 matched

images.flat -> images.Flat
  similarity: 0.59, priority: 1006104.1, dependents: 1
  functions: 51/51 matched (target total: 111, required body score: 0.59)
  missing functions: none
  types: 10/10 matched (target total: 17)
  missing types: none
  tests: 4/4 matched

io.limits -> io.Limits
  similarity: 0.62, priority: 1001103.8, dependents: 1
  functions: 9/9 matched (target total: 16, required body score: 0.62)
  missing functions: none
  types: 2/2 matched (target total: 3)
  missing types: none

tga.header -> tga.Header
  similarity: 0.53, priority: 1000904.7, dependents: 1
  functions: 7/7 matched (target total: 12, required body score: 0.53)
  missing functions: none
  types: 2/2 matched (target total: 3)
  missing types: none

jpeg.transform -> jpeg.Transform
  similarity: 0.74, priority: 1000102.6, dependents: 1
  functions: 1/1 matched (target total: 1, required body score: 0.74)
  missing functions: none
  types: 0/0 matched (target total: 0)
  missing types: none

images.buffer -> images.Buffer
  similarity: 0.15, priority: 731808.5, dependents: 0
  functions: 35/93 matched (target total: 105, required body score: 0.15)
  missing functions: next, size_hint, len, next_back, fmt, with_image, inner_pixels, pixels, check_image_fits, image_buffer_len, pixel_indices, pixel_indices_unchecked, as_flat_samples_mut, inner_pixels_mut, get_pixel_mut, get_pixel_mut_checked, set_rgb_color_space, save, save_with_format, write_to, write_with_encoder, default, deref, deref_mut, index, index_mut, clone_from, unsafe_get_pixel, unsafe_put_pixel, from_pixel, from_vec, into_vec, expand_palette, convert, as_transform, as_transform_fn, cast_in_color_space, copy_from_color_space, to_color_space, apply_color_space, from, slice_buffer, mut_iter, zero_width_zero_height, zero_width_nonzero_height, nonzero_width_zero_height, pixels_on_large_buffer, write_to_with_large_buffer, exact_size_iter_size_hint, color_conversion, gray_conversions, rgb_to_gray_conversion, apply_color, to_color, transformation_mismatch, conversion, image_access_row_by_row, image_access_col_by_col
  types: 11/25 matched (target total: 14)
  missing types: Pixels, Item, PixelsMut, Rows, RowsMut, EnumeratePixels, EnumerateRows, EnumeratePixelsMut, EnumerateRowsMut, Target, Output, Pixel, ConvertBuffer, ConvertColorOptions
  tests: 5/22 matched

bmp.decoder -> bmp.Decoder
  similarity: 0.04, priority: 495609.6, dependents: 0
  functions: 3/44 matched (target total: 16, required body score: 0.04)
  missing functions: next, fmt, from, check_for_overflow, num_bytes, with_rows, set_8bit_pixel_run, set_4bit_pixel_run, set_2bit_pixel_run, set_1bit_pixel_run, from_mask, read, new_decoder, new, new_without_file_header, new_with_ico_format, set_indexed_color, reader, read_file_header, read_bitmap_core_header, read_bitmap_info_header, read_bitmasks, read_metadata, read_metadata_in_ico_format, get_palette_size, bytes_per_color, read_palette, get_palette, num_channels, rows, read_palettized_pixel_data, read_16_bit_pixel_data, read_32_bit_pixel_data, read_full_byte_pixel_data, read_rle_data, read_image_data, read_image_boxed, read_rect, test_bitfield_len, read_rle_too_short, test_no_header
  types: 4/12 matched (target total: 4)
  missing types: BMPHeaderType, FormatFullBytes, Chunker, RowIterator, Item, DecoderError, ChannelWidthError, RLEInsn
  tests: 0/3 matched

imageops.sample -> imageops.Sample
  similarity: 0.34, priority: 356806.6, dependents: 0
  functions: 31/63 matched (target total: 47, required body score: 0.34)
  missing functions: to_i8, to_i16, to_i64, to_u8, to_u16, to_u64, to_f64, horizontal_sample, vertical_sample, zeroed, sample_val, add_pixel, thumbnail_sample_block, thumbnail_sample_fraction_horizontal, thumbnail_sample_fraction_vertical, thumbnail_sample_fraction_both, gaussian_blur_indirect, gaussian_blur_indirect_impl, bench_resize, test_resize_same_size, test_sample_bilinear, test_sample_nearest, bench_sample_bilinear, bench_resize_same_size, bench_thumbnail, bench_thumbnail_upsize, bench_thumbnail_upsize_irregular, resize_transparent_image, assert_resize, bug_1600, issue_2340, issue_2340_refl
  types: 2/5 matched (target total: 3)
  missing types: Filter, FloatNearest, ThumbnailSum
  tests: 3/17 matched

images.dynimage -> images.Dynimage
  similarity: 0.35, priority: 355206.5, dependents: 0
  functions: 117/149 matched (target total: 133, required body score: 0.35)
  missing functions: clone, clone_from, to, copy_from_color_space, apply_color_space, convert_color_space, write_with_encoder_impl, save, save_with_format, from, get_pixel_mut, default, open, image_dimensions, bench_conversion, open_16bpc_png, test_grayscale, test_grayscale_alpha_discarded, test_grayscale_alpha_preserved, test_dynamic_image_default_implementation, color_conversion_srgb_p3, color_conversion_preserves_sample, color_conversion_preserves_sample_in_fastpath, color_conversion_rgb_to_luma, copy_color_space_coverage, apply_color_space_coverage, into_luma_is_color_space_aware, from_luma_is_color_space_aware, from_luma_for_all_chromaticities, from_rgb_for_all_chromaticities, convert_color_space_coverage, color_space_independent_imageops
  types: 1/3 matched (target total: 12)
  missing types: Pixel, Foo
  tests: 14/32 matched

codecs.png -> png.Png
  similarity: 0.08, priority: 293509.2, dependents: 0
  functions: 3/28 matched (target total: 15, required body score: 0.08)
  missing functions: new, with_limits, gamma_value, apng, is_apng, unsupported_color, dimensions, color_type, icc_profile, exif_metadata, xmp_metadata, iptc_metadata, read_image, read_image_boxed, set_limits, mix_next_frame, animatable_color_type, into_frames, next, new_with_quality, encode_inner, from_png, ensure_no_decoder_off_by_one, underlying_error, encode_bad_color_type
  types: 3/7 matched (target total: 9)
  missing types: PngDecoder, ApngDecoder, FrameIterator, Item
  tests: 0/3 matched

pnm.decoder -> pnm.Decoder
  similarity: 0.34, priority: 266506.6, dependents: 0
  functions: 33/51 matched (target total: 49, required body score: 0.34)
  missing functions: fmt, from, source, sample_size, bytelen, new, read_magic_constant, read_image_boxed, read_samples, read_ascii, read_separated_ascii, from_bytes, from_ascii, tuple_type, pbm_binary_ascii_termination, read, issue_1508, issue_1616_overflow
  types: 6/14 matched (target total: 28)
  missing types: U8, U16, PbmBit, BWBit, DecodableImageHeader, HeaderReader, Representation, FailRead
  tests: 19/23 matched

jpeg.encoder -> jpeg.Encoder
  similarity: 0.35, priority: 225206.5, dependents: 0
  functions: 25/46 matched (target total: 32, required body score: 0.35)
  missing functions: new, default, fmt, from, new_with_quality, write_exif, encode_image, encode_gray, encode_rgb, pixel_at_or_near, copy_blocks_ycbcr, copy_blocks_gray, decode, test_build_jfif_header, test_build_frame_header, test_build_scan_header, test_build_huffman_segment, test_build_quantization_segment, check_color_types, bench_jpeg_encoder_new, sub_image_encoder_regression_1412
  types: 5/6 matched (target total: 6)
  missing types: EncoderError
  tests: 4/13 matched

avif.yuv -> avif.Yuv
  similarity: 0.25, priority: 214007.5, dependents: 0
  functions: 13/30 matched (target total: 21, required body score: 0.25)
  missing functions: to_integers, fmt, yuv400_to_rgba10, yuv400_to_rgba12, yuv400_to_rgbx_impl, yuv420_to_rgba10, yuv420_to_rgba12, yuv420_to_rgbx_invoker, yuv422_to_rgba10, yuv422_to_rgba12, yuv422_to_rgbx_invoker, yuv444_to_rgba10, yuv444_to_rgba12, yuv444_to_rgbx_impl, gbr_to_rgba10, gbr_to_rgba12, gbr_to_rgbx_impl
  types: 6/10 matched (target total: 7)
  missing types: ErrorSize, YuvConversionError, YuvBias, HalvedRowHandler

codecs.gif -> codecs.Gif
  similarity: 0.28, priority: 183607.2, dependents: 0
  functions: 15/29 matched (target total: 26, required body score: 0.28)
  missing functions: read, read_to_end, read_image_boxed, next, blend_and_dispose_pixel, new_from_frame, to_gif_enum, convert_frame, gif_dimensions, inner_dimensions, encode_gif, from_decoding, from_encoding, frames_exceeding_logical_screen_size
  types: 3/7 matched (target total: 6)
  missing types: GifReader, GifFrameIterator, Item, FrameInfo
  tests: 0/1 matched

pnm.encoder -> pnm.Encoder
  similarity: 0.37, priority: 183506.3, dependents: 0
  functions: 14/25 matched (target total: 23, required body score: 0.37)
  missing functions: new, check, check_header_dimensions, check_header_color, check_sample_values, write_header, header, write_samples_ascii, len, encoding_for, from
  types: 3/10 matched (target total: 9)
  missing types: CheckedImageBuffer, UncheckedHeader, CheckedDimensions, CheckedHeaderColor, CheckedHeader, TupleEncoding, SampleWriter
  tests: 3/3 matched

codecs.farbfeld -> codecs.Farbfeld
  similarity: 0.17, priority: 172508.3, dependents: 0
  functions: 6/22 matched (target total: 14, required body score: 0.17)
  missing functions: new, read_dimm, read, seek, parse_offset, consume_channel, cache_byte, read_image_boxed, encode_impl, read_rect_1x2, read_rect_2x2, read_rect_2x1, read_rect_2x3, read_rect_in_stream, dimension_overflow, degenerate_pixels
  types: 2/3 matched (target total: 3)
  missing types: FarbfeldReader
  tests: 0/7 matched

codecs.openexr -> codecs.Openexr
  similarity: 0.19, priority: 152408.1, dependents: 0
  functions: 7/22 matched (target total: 12, required body score: 0.19)
  missing functions: selected_exr_header, read_image_boxed, write_buffer, to_image_err, write_rgb_image, write_rgba_image, read_as_rgba_image_from_file, read_as_rgb_image_from_file, read_as_rgb_image, read_as_rgba_image, compare_exr_hdr, roundtrip_rgba, roundtrip_rgb, compare_rgba_rgb, compare_cropped
  types: 2/2 matched (target total: 3)
  missing types: none
  tests: 0/11 matched

animation -> image.Animation
  similarity: 0.41, priority: 143705.9, dependents: 0
  functions: 19/32 matched (target total: 30, required body score: 0.41)
  missing functions: new, next, clone, clone_from, from_parts, buffer_mut, into_buffer, from_ratio, into_ratio, from, eq, partial_cmp, cmp
  types: 4/5 matched (target total: 5)
  missing types: Item
  tests: 6/6 matched

codecs.tiff -> codecs.Tiff
  similarity: 0.26, priority: 142507.4, dependents: 0
  functions: 9/22 matched (target total: 13, required body score: 0.26)
  missing functions: total_bytes_buffer, check_sample_format, err_unknown_color_type, from_tiff_decode, from_tiff_encode, read, read_to_end, icc_profile, xmp_metadata, read_image_boxed, cmyk_to_rgb, cmyk_to_rgb16, u8_slice_as_pod
  types: 2/3 matched (target total: 3)
  missing types: TiffReader

color -> image.Color
  similarity: 0.29, priority: 133607.1, dependents: 0
  functions: 21/28 matched (target total: 72, required body score: 0.29)
  missing functions: try_from, from_primitive, normalize_float, into_color, rgb_to_luma, from_color, invert
  types: 2/8 matched (target total: 35)
  missing types: Error, FromPrimitive, FromColor, IntoColor, Blend, Invert
  tests: 12/12 matched

avif.decoder -> avif.Decoder
  similarity: 0.14, priority: 131908.6, dependents: 0
  functions: 5/15 matched (target total: 7, required body score: 0.14)
  missing functions: error_map, fmt, reshape_plane, default, transmute_y_plane16, transmute_chroma_plane16, get_matrix, read_image_boxed, process_16bit_picture, read_until_ready
  types: 1/4 matched (target total: 1)
  missing types: AvifDecoderError, Plane16View, YuvMatrixStrategy

images.buffer_par -> images.BufferPar
  similarity: 0.23, priority: 112407.6, dependents: 0
  functions: 9/19 matched (target total: 33, required body score: 0.23)
  missing functions: drive_unindexed, opt_len, drive, with_producer, fmt, from_par_fn, test_width_height, creation, creation_par, pixel_func
  types: 4/5 matched (target total: 5)
  missing types: Item
  tests: 4/8 matched

io.image_reader_type -> io.ImageReaderType
  similarity: 0.26, priority: 111907.4, dependents: 0
  functions: 7/17 matched (target total: 11, required body score: 0.26)
  missing functions: new, no_limits, limits, into_inner, make_decoder, guess_format, into_dimensions, require_format, open, open_impl
  types: 1/2 matched (target total: 2)
  missing types: Format

traits -> image.Traits
  similarity: 0.13, priority: 102608.7, dependents: 0
  functions: 7/13 matched (target total: 20, required body score: 0.13)
  missing functions: channels, layout, dispatch_transform_from_sealed, double_dispatch_transform_from_sealed, transform_on, alpha
  types: 9/13 matched (target total: 30)
  missing types: Larger, Ratio, TransformableSubpixel, HelpDispatchTransform

ico.decoder -> ico.Decoder
  similarity: 0.23, priority: 102207.7, dependents: 0
  functions: 9/17 matched (target total: 16, required body score: 0.23)
  missing functions: fmt, from, new, read_entry, seek_to_start, is_png, decoder, read_image_boxed
  types: 3/5 matched (target total: 11)
  missing types: IcoEntryImageFormat, InnerDecoder
  tests: 1/1 matched

codecs.dds -> codecs.Dds
  similarity: 0.37, priority: 91506.3, dependents: 0
  functions: 5/10 matched (target total: 12, required body score: 0.37)
  missing functions: fmt, from, new, read_image_boxed, dimension_overflow
  types: 1/5 matched (target total: 5)
  missing types: DecoderError, Header, DX10Header, PixelFormat
  tests: 0/1 matched

avif.encoder -> avif.Encoder
  similarity: 0.25, priority: 91407.5, dependents: 0
  functions: 4/11 matched (target total: 4, required body score: 0.25)
  missing functions: to_ravif, write_image, set_color, encode_as_img, try_from_raw, convert_into, cast_buffer
  types: 1/3 matched (target total: 1)
  missing types: AvifEncoder, RgbColor

tga.encoder -> tga.Encoder
  similarity: 0.46, priority: 72805.4, dependents: 0
  functions: 18/25 matched (target total: 25, required body score: 0.46)
  missing functions: fmt, from, new, make_compatible_img, round_trip_image, round_trip_bw, round_trip_single_pixel_rgba
  types: 3/3 matched (target total: 7)
  missing types: none
  tests: 12/15 matched

webp.decoder -> webp.Decoder
  similarity: 0.33, priority: 71806.7, dependents: 0
  functions: 10/15 matched (target total: 15, required body score: 0.33)
  missing functions: new, into_frames, next, from_webp_decode, add_with_overflow_size
  types: 1/3 matched (target total: 1)
  missing types: FramesInner, Item
  tests: 0/1 matched

jpeg.decoder -> jpeg.Decoder
  similarity: 0.25, priority: 61707.5, dependents: 0
  functions: 10/15 matched (target total: 28, required body score: 0.25)
  missing functions: new, from_jpeg, to_supported_color_space, new_zune_decoder, test_exif_orientation
  types: 1/2 matched (target total: 5)
  missing types: ZuneColorSpace
  tests: 0/1 matched

imageops.colorops -> imageops.Colorops
  similarity: 0.41, priority: 52805.9, dependents: 0
  functions: 21/24 matched (target total: 25, required body score: 0.41)
  missing functions: grayscale_with_type, grayscale_with_type_alpha, pixel_diffs
  types: 2/4 matched (target total: 3)
  missing types: Subpixel, Color
  tests: 5/6 matched

bmp.encoder -> bmp.Encoder
  similarity: 0.57, priority: 51904.2, dependents: 0
  functions: 13/18 matched (target total: 18, required body score: 0.57)
  missing functions: new, write_row_pad, make_compatible_img, round_trip_image, huge_files_return_error
  types: 1/1 matched (target total: 4)
  missing types: none
  tests: 6/8 matched

images.generic_image -> images.GenericImage
  similarity: 0.67, priority: 43603.3, dependents: 0
  functions: 29/32 matched (target total: 30, required body score: 0.67)
  missing functions: buffer_like, buffer_with_dimensions, clone
  types: 3/4 matched (target total: 4)
  missing types: Item
  tests: 17/17 matched

codecs.dxt -> codecs.Dxt
  similarity: 0.64, priority: 32103.6, dependents: 0
  functions: 16/18 matched (target total: 23, required body score: 0.64)
  missing functions: new, read_image_boxed
  types: 2/3 matched (target total: 3)
  missing types: Rgb

pnm.mod -> pnm.Mod [STUB]
  similarity: 0.00, priority: 30610.0, dependents: 0
  functions: 3/6 matched (target total: 6, required body score: 0.00)
  missing functions: execute_roundtrip_default, execute_roundtrip_with_subtype, execute_roundtrip_u16
  types: 0/0 matched (target total: 1)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies
  tests: 3/6 matched

pnm.header -> pnm.Header
  similarity: 0.64, priority: 22403.6, dependents: 0
  functions: 13/14 matched (target total: 20, required body score: 0.64)
  missing functions: fmt
  types: 9/10 matched (target total: 22)
  missing types: TupltypeWriter

hdr.encoder -> hdr.Encoder
  similarity: 0.53, priority: 21804.7, dependents: 0
  functions: 12/13 matched (target total: 14, required body score: 0.53)
  missing functions: a
  types: 4/5 matched (target total: 7)
  missing types: Item
  tests: 3/3 matched

imageops.affine -> imageops.Affine
  similarity: 0.55, priority: 12204.5, dependents: 0
  functions: 21/22 matched (target total: 21, required body score: 0.55)
  missing functions: pixel_diffs
  types: 0/0 matched (target total: 1)
  missing types: none
  tests: 8/9 matched

io.decoder -> io.Decoder
  similarity: 0.30, priority: 11707.0, dependents: 0
  functions: 13/13 matched (target total: 13, required body score: 0.30)
  missing functions: none
  types: 3/4 matched (target total: 5)
  missing types: D
  tests: 1/1 matched

io.encoder -> io.Encoder
  similarity: 0.56, priority: 10804.4, dependents: 0
  functions: 4/5 matched (target total: 4, required body score: 0.56)
  missing functions: write_image
  types: 3/3 matched (target total: 3)
  missing types: none

hdr.decoder -> hdr.Decoder
  similarity: 0.64, priority: 3403.6, dependents: 0
  functions: 29/29 matched (target total: 34, required body score: 0.64)
  missing functions: none
  types: 5/5 matched (target total: 20)
  missing types: none
  tests: 3/3 matched

imageops.filter_1d -> imageops.Filter1d
  similarity: 0.56, priority: 3004.4, dependents: 0
  functions: 23/23 matched (target total: 46, required body score: 0.56)
  missing functions: none
  types: 7/7 matched (target total: 10)
  missing types: none

imageops.mod -> imageops.Mod [STUB]
  similarity: 0.00, priority: 2610.0, dependents: 0
  functions: 26/26 matched (target total: 30, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 3)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies
  tests: 16/16 matched

hooks -> image.Hooks
  similarity: 0.52, priority: 1904.8, dependents: 0
  functions: 15/15 matched (target total: 27, required body score: 0.52)
  missing functions: none
  types: 4/4 matched (target total: 7)
  missing types: none

tga.decoder -> tga.Decoder
  similarity: 0.63, priority: 1503.7, dependents: 0
  functions: 12/12 matched (target total: 20, required body score: 0.63)
  missing functions: none
  types: 3/3 matched (target total: 4)
  missing types: none

io.format -> io.Format
  similarity: 0.46, priority: 1405.4, dependents: 0
  functions: 13/13 matched (target total: 18, required body score: 0.46)
  missing functions: none
  types: 1/1 matched (target total: 2)
  missing types: none
  tests: 2/2 matched

codecs.qoi -> codecs.Qoi
  similarity: 0.60, priority: 1104.0, dependents: 0
  functions: 9/9 matched (target total: 14, required body score: 0.60)
  missing functions: none
  types: 2/2 matched (target total: 3)
  missing types: none
  tests: 1/1 matched

metadata -> metadata.Mod
  similarity: 0.46, priority: 905.4, dependents: 0
  functions: 7/7 matched (target total: 23, required body score: 0.46)
  missing functions: none
  types: 2/2 matched (target total: 3)
  missing types: none

webp.encoder -> webp.Encoder
  similarity: 0.56, priority: 904.4, dependents: 0
  functions: 8/8 matched (target total: 13, required body score: 0.56)
  missing functions: none
  types: 1/1 matched (target total: 2)
  missing types: none
  tests: 1/1 matched

ico.encoder -> ico.Encoder
  similarity: 0.68, priority: 903.2, dependents: 0
  functions: 7/7 matched (target total: 11, required body score: 0.68)
  missing functions: none
  types: 2/2 matched (target total: 2)
  missing types: none

utils.mod -> utils.Mod [STUB]
  similarity: 0.00, priority: 710.0, dependents: 0
  functions: 7/7 matched (target total: 8, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies
  tests: 1/1 matched

math.utils -> math.Utils
  similarity: 0.73, priority: 702.7, dependents: 0
  functions: 7/7 matched (target total: 10, required body score: 0.73)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none
  tests: 5/5 matched

avif.ycgco -> avif.Ycgco
  similarity: 0.71, priority: 402.9, dependents: 0
  functions: 4/4 matched (target total: 10, required body score: 0.71)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none

jpeg.entropy -> jpeg.Entropy
  similarity: 0.76, priority: 202.4, dependents: 0
  functions: 2/2 matched (target total: 4, required body score: 0.76)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none

avif.mod -> avif.Mod [STUB]
  similarity: 0.00, priority: 10.0, dependents: 0
  functions: 0/0 matched (target total: 1, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 2)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies; no source functions found; target defines functions; report scoring is function-by-function only

webp.mod -> webp.Mod [STUB]
  similarity: 0.00, priority: 10.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies

jpeg.mod -> jpeg.Mod [STUB]
  similarity: 0.00, priority: 10.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 0)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies

tga.mod -> tga.Mod [STUB]
  similarity: 0.00, priority: 10.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies

hdr.mod -> hdr.Mod [STUB]
  similarity: 0.00, priority: 10.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies

ico.mod -> ico.Mod [STUB]
  similarity: 0.00, priority: 10.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies

bmp.mod -> bmp.Mod [STUB]
  similarity: 0.00, priority: 10.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 1)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies

math.mod -> math.Mod [STUB]
  similarity: 0.00, priority: 10.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 0.00)
  missing functions: none
  types: 0/0 matched (target total: 0)
  missing types: none
  *** CHEAT DETECTION / SCORING FAILURE ***
  function-by-function score forced to 0: target contains TODO/stub/placeholder markers in function bodies

images -> images.Images
  similarity: 1.00, priority: 0.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 1.00)
  missing functions: none
  types: 0/0 matched (target total: 0)
  missing types: none

lib -> image.Lib
  similarity: 1.00, priority: 0.0, dependents: 0
  functions: 0/0 matched (target total: 0, required body score: 1.00)
  missing functions: none
  types: 0/0 matched (target total: 21)
  missing types: none


=== Scores Forced To 0 ===

  - pnm.mod -> pnm.Mod: target contains TODO/stub/placeholder markers in function bodies
  - imageops.mod -> imageops.Mod: target contains TODO/stub/placeholder markers in function bodies
  - utils.mod -> utils.Mod: target contains TODO/stub/placeholder markers in function bodies
  - avif.mod -> avif.Mod: target contains TODO/stub/placeholder markers in function bodies; no source functions found; target defines functions; report scoring is function-by-function only
  - webp.mod -> webp.Mod: target contains TODO/stub/placeholder markers in function bodies
  - jpeg.mod -> jpeg.Mod: target contains TODO/stub/placeholder markers in function bodies
  - tga.mod -> tga.Mod: target contains TODO/stub/placeholder markers in function bodies
  - hdr.mod -> hdr.Mod: target contains TODO/stub/placeholder markers in function bodies
  - ico.mod -> ico.Mod: target contains TODO/stub/placeholder markers in function bodies
  - bmp.mod -> bmp.Mod: target contains TODO/stub/placeholder markers in function bodies
  - math.mod -> math.Mod: target contains TODO/stub/placeholder markers in function bodies

=== Porting Quality Summary ===

Matched by exact header:          71 / 71
Matched by provenance fallback:   0 / 71
Matched by name:                  0 / 71
Total TODOs in target: 0
Total lint errors:    0
Stub files:           11

=== Big Picture ===

- Missing files: 0
- Incomplete ports (similarity < 60%): 54
- Stub files: 11
- Files missing functions: 38 (total deficit: 439 functions)
- Type definitions missing: 94
- Files missing tests: 23 (total deficit: 116 unported `#[test]` functions)
- Documentation coverage: 1406 / 8909 lines (16%)

Primary focus: replace stub files with real implementations

=== Files with Issues ===

File                          Similarity LineRatio  FunctionParityTests     TODOs Lint  Status
----------------------------------------------------------------------------------------------------
image.Error                   0.58       0.00       9/11          1/2       0     0     MISSING_FUNCS
  missing functions: `kind`, `assert_send_sync`
io.Mod                        0.75       0.00       1/1           -         0     0     MISSING_TYPES
  missing types: `Limits`, `LimitSupport`
io.FreeFunctions              0.59       0.00       10/16         2/8       0     0     MISSING_FUNCS
  missing functions: `dimensions`, `color_type`, `read_image`, `read_image_boxed`, `seek_scanline`, `read_scanline`
imageops.FastBlur             0.73       0.00       10/14         1/5       0     0     MISSING_FUNCS
  missing functions: `new`, `next_u32`, `next_u8`, `next_f32_in_range`
metadata.Cicp                 0.54       0.00       35/36         5/6       0     0     MISSING_FUNCS
  missing functions: `no_coefficient_fallback`
images.SubImage               0.55       0.00       18/18         2/2       0     0     MISSING_TYPES
  missing types: `Pixel`
images.Flat                   0.59       0.00       51/51         4/4       0     0     
tga.Header                    0.53       0.00       7/7           -         0     0     
images.Buffer                 0.15       0.00       35/93         5/22      0     0     LOW_SIM
  missing functions: `next`, `size_hint`, `len`, `next_back`, `fmt`, `with_image`, `inner_pixels`, `pixels`, `check_image_fits`, `image_buffer_len`, `pixel_indices`, `pixel_indices_unchecked`, `as_flat_samples_mut`, `inner_pixels_mut`, `get_pixel_mut`, `get_pixel_mut_checked`, `set_rgb_color_space`, `save`, `save_with_format`, `write_to`, `write_with_encoder`, `default`, `deref`, `deref_mut`, `index`, `index_mut`, `clone_from`, `unsafe_get_pixel`, `unsafe_put_pixel`, `from_pixel`, `from_vec`, `into_vec`, `expand_palette`, `convert`, `as_transform`, `as_transform_fn`, `cast_in_color_space`, `copy_from_color_space`, `to_color_space`, `apply_color_space`, `from`, `slice_buffer`, `mut_iter`, `zero_width_zero_height`, `zero_width_nonzero_height`, `nonzero_width_zero_height`, `pixels_on_large_buffer`, `write_to_with_large_buffer`, `exact_size_iter_size_hint`, `color_conversion`, `gray_conversions`, `rgb_to_gray_conversion`, `apply_color`, `to_color`, `transformation_mismatch`, `conversion`, `image_access_row_by_row`, `image_access_col_by_col`
  missing types: `Pixels`, `Item`, `PixelsMut`, `Rows`, `RowsMut`, `EnumeratePixels`, `EnumerateRows`, `EnumeratePixelsMut`, `EnumerateRowsMut`, `Target`, `Output`, `Pixel`, `ConvertBuffer`, `ConvertColorOptions`
bmp.Decoder                   0.04       0.00       3/44          0/3       0     0     LOW_SIM
  missing functions: `next`, `fmt`, `from`, `check_for_overflow`, `num_bytes`, `with_rows`, `set_8bit_pixel_run`, `set_4bit_pixel_run`, `set_2bit_pixel_run`, `set_1bit_pixel_run`, `from_mask`, `read`, `new_decoder`, `new`, `new_without_file_header`, `new_with_ico_format`, `set_indexed_color`, `reader`, `read_file_header`, `read_bitmap_core_header`, `read_bitmap_info_header`, `read_bitmasks`, `read_metadata`, `read_metadata_in_ico_format`, `get_palette_size`, `bytes_per_color`, `read_palette`, `get_palette`, `num_channels`, `rows`, `read_palettized_pixel_data`, `read_16_bit_pixel_data`, `read_32_bit_pixel_data`, `read_full_byte_pixel_data`, `read_rle_data`, `read_image_data`, `read_image_boxed`, `read_rect`, `test_bitfield_len`, `read_rle_too_short`, `test_no_header`
  missing types: `BMPHeaderType`, `FormatFullBytes`, `Chunker`, `RowIterator`, `Item`, `DecoderError`, `ChannelWidthError`, `RLEInsn`
imageops.Sample               0.34       0.00       31/63         3/17      0     0     LOW_SIM
  missing functions: `to_i8`, `to_i16`, `to_i64`, `to_u8`, `to_u16`, `to_u64`, `to_f64`, `horizontal_sample`, `vertical_sample`, `zeroed`, `sample_val`, `add_pixel`, `thumbnail_sample_block`, `thumbnail_sample_fraction_horizontal`, `thumbnail_sample_fraction_vertical`, `thumbnail_sample_fraction_both`, `gaussian_blur_indirect`, `gaussian_blur_indirect_impl`, `bench_resize`, `test_resize_same_size`, `test_sample_bilinear`, `test_sample_nearest`, `bench_sample_bilinear`, `bench_resize_same_size`, `bench_thumbnail`, `bench_thumbnail_upsize`, `bench_thumbnail_upsize_irregular`, `resize_transparent_image`, `assert_resize`, `bug_1600`, `issue_2340`, `issue_2340_refl`
  missing types: `Filter`, `FloatNearest`, `ThumbnailSum`
images.Dynimage               0.35       0.00       117/149       14/32     0     0     LOW_SIM
  missing functions: `clone`, `clone_from`, `to`, `copy_from_color_space`, `apply_color_space`, `convert_color_space`, `write_with_encoder_impl`, `save`, `save_with_format`, `from`, `get_pixel_mut`, `default`, `open`, `image_dimensions`, `bench_conversion`, `open_16bpc_png`, `test_grayscale`, `test_grayscale_alpha_discarded`, `test_grayscale_alpha_preserved`, `test_dynamic_image_default_implementation`, `color_conversion_srgb_p3`, `color_conversion_preserves_sample`, `color_conversion_preserves_sample_in_fastpath`, `color_conversion_rgb_to_luma`, `copy_color_space_coverage`, `apply_color_space_coverage`, `into_luma_is_color_space_aware`, `from_luma_is_color_space_aware`, `from_luma_for_all_chromaticities`, `from_rgb_for_all_chromaticities`, `convert_color_space_coverage`, `color_space_independent_imageops`
  missing types: `Pixel`, `Foo`
png.Png                       0.08       0.00       3/28          0/3       0     0     LOW_SIM
  missing functions: `new`, `with_limits`, `gamma_value`, `apng`, `is_apng`, `unsupported_color`, `dimensions`, `color_type`, `icc_profile`, `exif_metadata`, `xmp_metadata`, `iptc_metadata`, `read_image`, `read_image_boxed`, `set_limits`, `mix_next_frame`, `animatable_color_type`, `into_frames`, `next`, `new_with_quality`, `encode_inner`, `from_png`, `ensure_no_decoder_off_by_one`, `underlying_error`, `encode_bad_color_type`
  missing types: `PngDecoder`, `ApngDecoder`, `FrameIterator`, `Item`
pnm.Decoder                   0.34       0.00       33/51         19/23     0     0     LOW_SIM
  missing functions: `fmt`, `from`, `source`, `sample_size`, `bytelen`, `new`, `read_magic_constant`, `read_image_boxed`, `read_samples`, `read_ascii`, `read_separated_ascii`, `from_bytes`, `from_ascii`, `tuple_type`, `pbm_binary_ascii_termination`, `read`, `issue_1508`, `issue_1616_overflow`
  missing types: `U8`, `U16`, `PbmBit`, `BWBit`, `DecodableImageHeader`, `HeaderReader`, `Representation`, `FailRead`
jpeg.Encoder                  0.35       0.00       25/46         4/13      0     0     LOW_SIM
  missing functions: `new`, `default`, `fmt`, `from`, `new_with_quality`, `write_exif`, `encode_image`, `encode_gray`, `encode_rgb`, `pixel_at_or_near`, `copy_blocks_ycbcr`, `copy_blocks_gray`, `decode`, `test_build_jfif_header`, `test_build_frame_header`, `test_build_scan_header`, `test_build_huffman_segment`, `test_build_quantization_segment`, `check_color_types`, `bench_jpeg_encoder_new`, `sub_image_encoder_regression_1412`
  missing types: `EncoderError`
avif.Yuv                      0.25       0.00       13/30         -         0     0     LOW_SIM
  missing functions: `to_integers`, `fmt`, `yuv400_to_rgba10`, `yuv400_to_rgba12`, `yuv400_to_rgbx_impl`, `yuv420_to_rgba10`, `yuv420_to_rgba12`, `yuv420_to_rgbx_invoker`, `yuv422_to_rgba10`, `yuv422_to_rgba12`, `yuv422_to_rgbx_invoker`, `yuv444_to_rgba10`, `yuv444_to_rgba12`, `yuv444_to_rgbx_impl`, `gbr_to_rgba10`, `gbr_to_rgba12`, `gbr_to_rgbx_impl`
  missing types: `ErrorSize`, `YuvConversionError`, `YuvBias`, `HalvedRowHandler`
codecs.Gif                    0.28       0.00       15/29         0/1       0     0     LOW_SIM
  missing functions: `read`, `read_to_end`, `read_image_boxed`, `next`, `blend_and_dispose_pixel`, `new_from_frame`, `to_gif_enum`, `convert_frame`, `gif_dimensions`, `inner_dimensions`, `encode_gif`, `from_decoding`, `from_encoding`, `frames_exceeding_logical_screen_size`
  missing types: `GifReader`, `GifFrameIterator`, `Item`, `FrameInfo`
pnm.Encoder                   0.37       0.00       14/25         3/3       0     0     LOW_SIM
  missing functions: `new`, `check`, `check_header_dimensions`, `check_header_color`, `check_sample_values`, `write_header`, `header`, `write_samples_ascii`, `len`, `encoding_for`, `from`
  missing types: `CheckedImageBuffer`, `UncheckedHeader`, `CheckedDimensions`, `CheckedHeaderColor`, `CheckedHeader`, `TupleEncoding`, `SampleWriter`
codecs.Farbfeld               0.17       0.00       6/22          0/7       0     0     LOW_SIM
  missing functions: `new`, `read_dimm`, `read`, `seek`, `parse_offset`, `consume_channel`, `cache_byte`, `read_image_boxed`, `encode_impl`, `read_rect_1x2`, `read_rect_2x2`, `read_rect_2x1`, `read_rect_2x3`, `read_rect_in_stream`, `dimension_overflow`, `degenerate_pixels`
  missing types: `FarbfeldReader`
codecs.Openexr                0.19       0.00       7/22          0/11      0     0     LOW_SIM
  missing functions: `selected_exr_header`, `read_image_boxed`, `write_buffer`, `to_image_err`, `write_rgb_image`, `write_rgba_image`, `read_as_rgba_image_from_file`, `read_as_rgb_image_from_file`, `read_as_rgb_image`, `read_as_rgba_image`, `compare_exr_hdr`, `roundtrip_rgba`, `roundtrip_rgb`, `compare_rgba_rgb`, `compare_cropped`
image.Animation               0.41       0.00       19/32         6/6       0     0     MISSING_FUNCS
  missing functions: `new`, `next`, `clone`, `clone_from`, `from_parts`, `buffer_mut`, `into_buffer`, `from_ratio`, `into_ratio`, `from`, `eq`, `partial_cmp`, `cmp`
  missing types: `Item`
codecs.Tiff                   0.26       0.00       9/22          -         0     0     LOW_SIM
  missing functions: `total_bytes_buffer`, `check_sample_format`, `err_unknown_color_type`, `from_tiff_decode`, `from_tiff_encode`, `read`, `read_to_end`, `icc_profile`, `xmp_metadata`, `read_image_boxed`, `cmyk_to_rgb`, `cmyk_to_rgb16`, `u8_slice_as_pod`
  missing types: `TiffReader`
image.Color                   0.29       0.00       21/28         12/12     0     0     LOW_SIM
  missing functions: `try_from`, `from_primitive`, `normalize_float`, `into_color`, `rgb_to_luma`, `from_color`, `invert`
  missing types: `Error`, `FromPrimitive`, `FromColor`, `IntoColor`, `Blend`, `Invert`
avif.Decoder                  0.14       0.00       5/15          -         0     0     LOW_SIM
  missing functions: `error_map`, `fmt`, `reshape_plane`, `default`, `transmute_y_plane16`, `transmute_chroma_plane16`, `get_matrix`, `read_image_boxed`, `process_16bit_picture`, `read_until_ready`
  missing types: `AvifDecoderError`, `Plane16View`, `YuvMatrixStrategy`
images.BufferPar              0.23       0.00       9/19          4/8       0     0     LOW_SIM
  missing functions: `drive_unindexed`, `opt_len`, `drive`, `with_producer`, `fmt`, `from_par_fn`, `test_width_height`, `creation`, `creation_par`, `pixel_func`
  missing types: `Item`
io.ImageReaderType            0.26       0.00       7/17          -         0     0     LOW_SIM
  missing functions: `new`, `no_limits`, `limits`, `into_inner`, `make_decoder`, `guess_format`, `into_dimensions`, `require_format`, `open`, `open_impl`
  missing types: `Format`
image.Traits                  0.13       0.00       7/13          -         0     0     LOW_SIM
  missing functions: `channels`, `layout`, `dispatch_transform_from_sealed`, `double_dispatch_transform_from_sealed`, `transform_on`, `alpha`
  missing types: `Larger`, `Ratio`, `TransformableSubpixel`, `HelpDispatchTransform`
ico.Decoder                   0.23       0.00       9/17          1/1       0     0     LOW_SIM
  missing functions: `fmt`, `from`, `new`, `read_entry`, `seek_to_start`, `is_png`, `decoder`, `read_image_boxed`
  missing types: `IcoEntryImageFormat`, `InnerDecoder`
codecs.Dds                    0.37       0.00       5/10          0/1       0     0     LOW_SIM
  missing functions: `fmt`, `from`, `new`, `read_image_boxed`, `dimension_overflow`
  missing types: `DecoderError`, `Header`, `DX10Header`, `PixelFormat`
avif.Encoder                  0.25       0.00       4/11          -         0     0     LOW_SIM
  missing functions: `to_ravif`, `write_image`, `set_color`, `encode_as_img`, `try_from_raw`, `convert_into`, `cast_buffer`
  missing types: `AvifEncoder`, `RgbColor`
tga.Encoder                   0.46       0.00       18/25         12/15     0     0     MISSING_FUNCS
  missing functions: `fmt`, `from`, `new`, `make_compatible_img`, `round_trip_image`, `round_trip_bw`, `round_trip_single_pixel_rgba`
webp.Decoder                  0.33       0.00       10/15         0/1       0     0     LOW_SIM
  missing functions: `new`, `into_frames`, `next`, `from_webp_decode`, `add_with_overflow_size`
  missing types: `FramesInner`, `Item`
jpeg.Decoder                  0.25       0.00       10/15         0/1       0     0     LOW_SIM
  missing functions: `new`, `from_jpeg`, `to_supported_color_space`, `new_zune_decoder`, `test_exif_orientation`
  missing types: `ZuneColorSpace`
imageops.Colorops             0.41       0.00       21/24         5/6       0     0     MISSING_FUNCS
  missing functions: `grayscale_with_type`, `grayscale_with_type_alpha`, `pixel_diffs`
  missing types: `Subpixel`, `Color`
bmp.Encoder                   0.57       0.00       13/18         6/8       0     0     MISSING_FUNCS
  missing functions: `new`, `write_row_pad`, `make_compatible_img`, `round_trip_image`, `huge_files_return_error`
images.GenericImage           0.67       0.00       29/32         17/17     0     0     MISSING_FUNCS
  missing functions: `buffer_like`, `buffer_with_dimensions`, `clone`
  missing types: `Item`
codecs.Dxt                    0.64       0.00       16/18         -         0     0     MISSING_FUNCS
  missing functions: `new`, `read_image_boxed`
  missing types: `Rgb`
pnm.Mod [STUB]                0.00       0.00       3/6           3/6       0     0     STUB
  missing functions: `execute_roundtrip_default`, `execute_roundtrip_with_subtype`, `execute_roundtrip_u16`
pnm.Header                    0.64       0.00       13/14         -         0     0     MISSING_FUNCS
  missing functions: `fmt`
  missing types: `TupltypeWriter`
hdr.Encoder                   0.53       0.00       12/13         3/3       0     0     MISSING_FUNCS
  missing functions: `a`
  missing types: `Item`
imageops.Affine               0.55       0.00       21/22         8/9       0     0     MISSING_FUNCS
  missing functions: `pixel_diffs`
io.Decoder                    0.30       0.00       13/13         1/1       0     0     LOW_SIM
  missing types: `D`
io.Encoder                    0.56       0.00       4/5           -         0     0     MISSING_FUNCS
  missing functions: `write_image`
imageops.Filter1d             0.56       0.00       23/23         -         0     0     
imageops.Mod [STUB]           0.00       0.00       26/26         16/16     0     0     STUB
image.Hooks                   0.52       0.00       15/15         -         0     0     
io.Format                     0.46       0.00       13/13         2/2       0     0     
codecs.Qoi                    0.60       0.00       9/9           1/1       0     0     
metadata.Mod                  0.46       0.00       7/7           -         0     0     
webp.Encoder                  0.56       0.00       8/8           1/1       0     0     
utils.Mod [STUB]              0.00       0.00       7/7           1/1       0     0     STUB
avif.Mod [STUB]               0.00       0.00       -             -         0     0     STUB
webp.Mod [STUB]               0.00       0.00       -             -         0     0     STUB
jpeg.Mod [STUB]               0.00       0.00       -             -         0     0     STUB
tga.Mod [STUB]                0.00       0.00       -             -         0     0     STUB
hdr.Mod [STUB]                0.00       0.00       -             -         0     0     STUB
ico.Mod [STUB]                0.00       0.00       -             -         0     0     STUB
bmp.Mod [STUB]                0.00       0.00       -             -         0     0     STUB
math.Mod [STUB]               0.00       0.00       -             -         0     0     STUB

=== Porting Recommendations ===

Incomplete ports (similarity < 60%): 54
Missing files: 0

Incomplete ports to complete:
  error                          similarity=0.58 function_parity=9/11 dependents=3
    missing functions: `kind`, `assert_send_sync`
  io.free_functions              similarity=0.59 function_parity=10/16 dependents=1
    missing functions: `dimensions`, `color_type`, `read_image`, `read_image_boxed`, `seek_scanline`, `read_scanline`
  metadata.cicp                  similarity=0.54 function_parity=35/36 dependents=1
    missing functions: `no_coefficient_fallback`
  images.sub_image               similarity=0.55 function_parity=18/18 dependents=1
    missing types: `Pixel`
  images.flat                    similarity=0.59 function_parity=51/51 dependents=1
  tga.header                     similarity=0.53 function_parity=7/7 dependents=1
  images.buffer                  similarity=0.15 function_parity=35/93 dependents=0
    missing functions: `next`, `size_hint`, `len`, `next_back`, `fmt`, `with_image`, `inner_pixels`, `pixels`, `check_image_fits`, `image_buffer_len`, `pixel_indices`, `pixel_indices_unchecked`, `as_flat_samples_mut`, `inner_pixels_mut`, `get_pixel_mut`, `get_pixel_mut_checked`, `set_rgb_color_space`, `save`, `save_with_format`, `write_to`, `write_with_encoder`, `default`, `deref`, `deref_mut`, `index`, `index_mut`, `clone_from`, `unsafe_get_pixel`, `unsafe_put_pixel`, `from_pixel`, `from_vec`, `into_vec`, `expand_palette`, `convert`, `as_transform`, `as_transform_fn`, `cast_in_color_space`, `copy_from_color_space`, `to_color_space`, `apply_color_space`, `from`, `slice_buffer`, `mut_iter`, `zero_width_zero_height`, `zero_width_nonzero_height`, `nonzero_width_zero_height`, `pixels_on_large_buffer`, `write_to_with_large_buffer`, `exact_size_iter_size_hint`, `color_conversion`, `gray_conversions`, `rgb_to_gray_conversion`, `apply_color`, `to_color`, `transformation_mismatch`, `conversion`, `image_access_row_by_row`, `image_access_col_by_col`
    missing types: `Pixels`, `Item`, `PixelsMut`, `Rows`, `RowsMut`, `EnumeratePixels`, `EnumerateRows`, `EnumeratePixelsMut`, `EnumerateRowsMut`, `Target`, `Output`, `Pixel`, `ConvertBuffer`, `ConvertColorOptions`
  bmp.decoder                    similarity=0.04 function_parity=3/44 dependents=0
    missing functions: `next`, `fmt`, `from`, `check_for_overflow`, `num_bytes`, `with_rows`, `set_8bit_pixel_run`, `set_4bit_pixel_run`, `set_2bit_pixel_run`, `set_1bit_pixel_run`, `from_mask`, `read`, `new_decoder`, `new`, `new_without_file_header`, `new_with_ico_format`, `set_indexed_color`, `reader`, `read_file_header`, `read_bitmap_core_header`, `read_bitmap_info_header`, `read_bitmasks`, `read_metadata`, `read_metadata_in_ico_format`, `get_palette_size`, `bytes_per_color`, `read_palette`, `get_palette`, `num_channels`, `rows`, `read_palettized_pixel_data`, `read_16_bit_pixel_data`, `read_32_bit_pixel_data`, `read_full_byte_pixel_data`, `read_rle_data`, `read_image_data`, `read_image_boxed`, `read_rect`, `test_bitfield_len`, `read_rle_too_short`, `test_no_header`
    missing types: `BMPHeaderType`, `FormatFullBytes`, `Chunker`, `RowIterator`, `Item`, `DecoderError`, `ChannelWidthError`, `RLEInsn`
  imageops.sample                similarity=0.34 function_parity=31/63 dependents=0
    missing functions: `to_i8`, `to_i16`, `to_i64`, `to_u8`, `to_u16`, `to_u64`, `to_f64`, `horizontal_sample`, `vertical_sample`, `zeroed`, `sample_val`, `add_pixel`, `thumbnail_sample_block`, `thumbnail_sample_fraction_horizontal`, `thumbnail_sample_fraction_vertical`, `thumbnail_sample_fraction_both`, `gaussian_blur_indirect`, `gaussian_blur_indirect_impl`, `bench_resize`, `test_resize_same_size`, `test_sample_bilinear`, `test_sample_nearest`, `bench_sample_bilinear`, `bench_resize_same_size`, `bench_thumbnail`, `bench_thumbnail_upsize`, `bench_thumbnail_upsize_irregular`, `resize_transparent_image`, `assert_resize`, `bug_1600`, `issue_2340`, `issue_2340_refl`
    missing types: `Filter`, `FloatNearest`, `ThumbnailSum`
  images.dynimage                similarity=0.35 function_parity=117/149 dependents=0
    missing functions: `clone`, `clone_from`, `to`, `copy_from_color_space`, `apply_color_space`, `convert_color_space`, `write_with_encoder_impl`, `save`, `save_with_format`, `from`, `get_pixel_mut`, `default`, `open`, `image_dimensions`, `bench_conversion`, `open_16bpc_png`, `test_grayscale`, `test_grayscale_alpha_discarded`, `test_grayscale_alpha_preserved`, `test_dynamic_image_default_implementation`, `color_conversion_srgb_p3`, `color_conversion_preserves_sample`, `color_conversion_preserves_sample_in_fastpath`, `color_conversion_rgb_to_luma`, `copy_color_space_coverage`, `apply_color_space_coverage`, `into_luma_is_color_space_aware`, `from_luma_is_color_space_aware`, `from_luma_for_all_chromaticities`, `from_rgb_for_all_chromaticities`, `convert_color_space_coverage`, `color_space_independent_imageops`
    missing types: `Pixel`, `Foo`
  codecs.png                     similarity=0.08 function_parity=3/28 dependents=0
    missing functions: `new`, `with_limits`, `gamma_value`, `apng`, `is_apng`, `unsupported_color`, `dimensions`, `color_type`, `icc_profile`, `exif_metadata`, `xmp_metadata`, `iptc_metadata`, `read_image`, `read_image_boxed`, `set_limits`, `mix_next_frame`, `animatable_color_type`, `into_frames`, `next`, `new_with_quality`, `encode_inner`, `from_png`, `ensure_no_decoder_off_by_one`, `underlying_error`, `encode_bad_color_type`
    missing types: `PngDecoder`, `ApngDecoder`, `FrameIterator`, `Item`
  pnm.decoder                    similarity=0.34 function_parity=33/51 dependents=0
    missing functions: `fmt`, `from`, `source`, `sample_size`, `bytelen`, `new`, `read_magic_constant`, `read_image_boxed`, `read_samples`, `read_ascii`, `read_separated_ascii`, `from_bytes`, `from_ascii`, `tuple_type`, `pbm_binary_ascii_termination`, `read`, `issue_1508`, `issue_1616_overflow`
    missing types: `U8`, `U16`, `PbmBit`, `BWBit`, `DecodableImageHeader`, `HeaderReader`, `Representation`, `FailRead`
  jpeg.encoder                   similarity=0.35 function_parity=25/46 dependents=0
    missing functions: `new`, `default`, `fmt`, `from`, `new_with_quality`, `write_exif`, `encode_image`, `encode_gray`, `encode_rgb`, `pixel_at_or_near`, `copy_blocks_ycbcr`, `copy_blocks_gray`, `decode`, `test_build_jfif_header`, `test_build_frame_header`, `test_build_scan_header`, `test_build_huffman_segment`, `test_build_quantization_segment`, `check_color_types`, `bench_jpeg_encoder_new`, `sub_image_encoder_regression_1412`
    missing types: `EncoderError`
  avif.yuv                       similarity=0.25 function_parity=13/30 dependents=0
    missing functions: `to_integers`, `fmt`, `yuv400_to_rgba10`, `yuv400_to_rgba12`, `yuv400_to_rgbx_impl`, `yuv420_to_rgba10`, `yuv420_to_rgba12`, `yuv420_to_rgbx_invoker`, `yuv422_to_rgba10`, `yuv422_to_rgba12`, `yuv422_to_rgbx_invoker`, `yuv444_to_rgba10`, `yuv444_to_rgba12`, `yuv444_to_rgbx_impl`, `gbr_to_rgba10`, `gbr_to_rgba12`, `gbr_to_rgbx_impl`
    missing types: `ErrorSize`, `YuvConversionError`, `YuvBias`, `HalvedRowHandler`
  codecs.gif                     similarity=0.28 function_parity=15/29 dependents=0
    missing functions: `read`, `read_to_end`, `read_image_boxed`, `next`, `blend_and_dispose_pixel`, `new_from_frame`, `to_gif_enum`, `convert_frame`, `gif_dimensions`, `inner_dimensions`, `encode_gif`, `from_decoding`, `from_encoding`, `frames_exceeding_logical_screen_size`
    missing types: `GifReader`, `GifFrameIterator`, `Item`, `FrameInfo`
  pnm.encoder                    similarity=0.37 function_parity=14/25 dependents=0
    missing functions: `new`, `check`, `check_header_dimensions`, `check_header_color`, `check_sample_values`, `write_header`, `header`, `write_samples_ascii`, `len`, `encoding_for`, `from`
    missing types: `CheckedImageBuffer`, `UncheckedHeader`, `CheckedDimensions`, `CheckedHeaderColor`, `CheckedHeader`, `TupleEncoding`, `SampleWriter`
  codecs.farbfeld                similarity=0.17 function_parity=6/22 dependents=0
    missing functions: `new`, `read_dimm`, `read`, `seek`, `parse_offset`, `consume_channel`, `cache_byte`, `read_image_boxed`, `encode_impl`, `read_rect_1x2`, `read_rect_2x2`, `read_rect_2x1`, `read_rect_2x3`, `read_rect_in_stream`, `dimension_overflow`, `degenerate_pixels`
    missing types: `FarbfeldReader`
  codecs.openexr                 similarity=0.19 function_parity=7/22 dependents=0
    missing functions: `selected_exr_header`, `read_image_boxed`, `write_buffer`, `to_image_err`, `write_rgb_image`, `write_rgba_image`, `read_as_rgba_image_from_file`, `read_as_rgb_image_from_file`, `read_as_rgb_image`, `read_as_rgba_image`, `compare_exr_hdr`, `roundtrip_rgba`, `roundtrip_rgb`, `compare_rgba_rgb`, `compare_cropped`
  animation                      similarity=0.41 function_parity=19/32 dependents=0
    missing functions: `new`, `next`, `clone`, `clone_from`, `from_parts`, `buffer_mut`, `into_buffer`, `from_ratio`, `into_ratio`, `from`, `eq`, `partial_cmp`, `cmp`
    missing types: `Item`
  codecs.tiff                    similarity=0.26 function_parity=9/22 dependents=0
    missing functions: `total_bytes_buffer`, `check_sample_format`, `err_unknown_color_type`, `from_tiff_decode`, `from_tiff_encode`, `read`, `read_to_end`, `icc_profile`, `xmp_metadata`, `read_image_boxed`, `cmyk_to_rgb`, `cmyk_to_rgb16`, `u8_slice_as_pod`
    missing types: `TiffReader`
  color                          similarity=0.29 function_parity=21/28 dependents=0
    missing functions: `try_from`, `from_primitive`, `normalize_float`, `into_color`, `rgb_to_luma`, `from_color`, `invert`
    missing types: `Error`, `FromPrimitive`, `FromColor`, `IntoColor`, `Blend`, `Invert`
  avif.decoder                   similarity=0.14 function_parity=5/15 dependents=0
    missing functions: `error_map`, `fmt`, `reshape_plane`, `default`, `transmute_y_plane16`, `transmute_chroma_plane16`, `get_matrix`, `read_image_boxed`, `process_16bit_picture`, `read_until_ready`
    missing types: `AvifDecoderError`, `Plane16View`, `YuvMatrixStrategy`
  images.buffer_par              similarity=0.23 function_parity=9/19 dependents=0
    missing functions: `drive_unindexed`, `opt_len`, `drive`, `with_producer`, `fmt`, `from_par_fn`, `test_width_height`, `creation`, `creation_par`, `pixel_func`
    missing types: `Item`
  io.image_reader_type           similarity=0.26 function_parity=7/17 dependents=0
    missing functions: `new`, `no_limits`, `limits`, `into_inner`, `make_decoder`, `guess_format`, `into_dimensions`, `require_format`, `open`, `open_impl`
    missing types: `Format`
  traits                         similarity=0.13 function_parity=7/13 dependents=0
    missing functions: `channels`, `layout`, `dispatch_transform_from_sealed`, `double_dispatch_transform_from_sealed`, `transform_on`, `alpha`
    missing types: `Larger`, `Ratio`, `TransformableSubpixel`, `HelpDispatchTransform`
  ico.decoder                    similarity=0.23 function_parity=9/17 dependents=0
    missing functions: `fmt`, `from`, `new`, `read_entry`, `seek_to_start`, `is_png`, `decoder`, `read_image_boxed`
    missing types: `IcoEntryImageFormat`, `InnerDecoder`
  codecs.dds                     similarity=0.37 function_parity=5/10 dependents=0
    missing functions: `fmt`, `from`, `new`, `read_image_boxed`, `dimension_overflow`
    missing types: `DecoderError`, `Header`, `DX10Header`, `PixelFormat`
  avif.encoder                   similarity=0.25 function_parity=4/11 dependents=0
    missing functions: `to_ravif`, `write_image`, `set_color`, `encode_as_img`, `try_from_raw`, `convert_into`, `cast_buffer`
    missing types: `AvifEncoder`, `RgbColor`
  tga.encoder                    similarity=0.46 function_parity=18/25 dependents=0
    missing functions: `fmt`, `from`, `new`, `make_compatible_img`, `round_trip_image`, `round_trip_bw`, `round_trip_single_pixel_rgba`
  webp.decoder                   similarity=0.33 function_parity=10/15 dependents=0
    missing functions: `new`, `into_frames`, `next`, `from_webp_decode`, `add_with_overflow_size`
    missing types: `FramesInner`, `Item`
  jpeg.decoder                   similarity=0.25 function_parity=10/15 dependents=0
    missing functions: `new`, `from_jpeg`, `to_supported_color_space`, `new_zune_decoder`, `test_exif_orientation`
    missing types: `ZuneColorSpace`
  imageops.colorops              similarity=0.41 function_parity=21/24 dependents=0
    missing functions: `grayscale_with_type`, `grayscale_with_type_alpha`, `pixel_diffs`
    missing types: `Subpixel`, `Color`
  bmp.encoder                    similarity=0.57 function_parity=13/18 dependents=0
    missing functions: `new`, `write_row_pad`, `make_compatible_img`, `round_trip_image`, `huge_files_return_error`
  pnm.mod                        similarity=0.00 function_parity=3/6 dependents=0 [STUB]
    missing functions: `execute_roundtrip_default`, `execute_roundtrip_with_subtype`, `execute_roundtrip_u16`
  hdr.encoder                    similarity=0.53 function_parity=12/13 dependents=0
    missing functions: `a`
    missing types: `Item`
  imageops.affine                similarity=0.55 function_parity=21/22 dependents=0
    missing functions: `pixel_diffs`
  io.decoder                     similarity=0.30 function_parity=13/13 dependents=0
    missing types: `D`
  io.encoder                     similarity=0.56 function_parity=4/5 dependents=0
    missing functions: `write_image`
  imageops.filter_1d             similarity=0.56 function_parity=23/23 dependents=0
  imageops.mod                   similarity=0.00 function_parity=26/26 dependents=0 [STUB]
  hooks                          similarity=0.52 function_parity=15/15 dependents=0
  io.format                      similarity=0.46 function_parity=13/13 dependents=0
  codecs.qoi                     similarity=0.60 function_parity=9/9 dependents=0
  metadata                       similarity=0.46 function_parity=7/7 dependents=0
  webp.encoder                   similarity=0.56 function_parity=8/8 dependents=0
  utils.mod                      similarity=0.00 function_parity=7/7 dependents=0 [STUB]
  avif.mod                       similarity=0.00 function_parity=- dependents=0 [STUB]
  webp.mod                       similarity=0.00 function_parity=- dependents=0 [STUB]
  jpeg.mod                       similarity=0.00 function_parity=- dependents=0 [STUB]
  tga.mod                        similarity=0.00 function_parity=- dependents=0 [STUB]
  hdr.mod                        similarity=0.00 function_parity=- dependents=0 [STUB]
  ico.mod                        similarity=0.00 function_parity=- dependents=0 [STUB]
  bmp.mod                        similarity=0.00 function_parity=- dependents=0 [STUB]
  math.mod                       similarity=0.00 function_parity=- dependents=0 [STUB]

=== Documentation Gaps ===

There is missing documentation that is hurting overall scoring.
Documentation coverage: 1406 / 8909 lines (16%)
Files with >20% doc gap: 53

File                          Src Docs    Tgt Docs    Gap %     DocSim    DocAmt    DocEq     
----------------------------------------------------------------------------------------------
images.flat                   1240        46          96%       0.64      0.04      0.34      
images.dynimage               946         5           99%       0.54      0.01      0.27      
images.buffer                 636         3           99%       0.48      0.00      0.24      
metadata.cicp                 440         42          90%       0.68      0.10      0.39      
avif.yuv                      388         51          86%       0.53      0.13      0.33      
lib                           318         3           99%       0.26      0.01      0.13      
imageops.sample               366         79          78%       0.53      0.22      0.37      
io.image_reader_type          254         6           97%       0.62      0.02      0.32      
imageops.mod                  270         38          85%       0.49      0.14      0.32      
error                         256         43          83%       0.78      0.17      0.47      
images.generic_image          228         24          89%       0.80      0.11      0.45      
color                         204         39          80%       0.88      0.19      0.53      
imageops.filter_1d            162         6           96%       0.13      0.04      0.09      
codecs.png                    166         27          83%       0.57      0.16      0.37      
imageops.colorops             166         42          74%       0.65      0.25      0.45      
io.decoder                    172         51          70%       0.67      0.30      0.48      
io.format                     168         51          69%       0.90      0.30      0.60      
images.sub_image              116         6           94%       0.40      0.05      0.23      
animation                     126         18          85%       0.34      0.14      0.24      
codecs.openexr                110         6           94%       0.23      0.05      0.14      
jpeg.encoder                  120         24          80%       0.80      0.20      0.50      
traits                        138         43          68%       0.90      0.31      0.60      
pnm.encoder                   98          3           96%       0.28      0.03      0.16      
bmp.decoder                   96          3           96%       0.26      0.03      0.14      
pnm.decoder                   100         15          85%       0.41      0.15      0.28      
io.encoder                    108         24          77%       0.57      0.22      0.40      
codecs.gif                    92          11          88%       0.57      0.12      0.34      
io.limits                     106         34          67%       0.62      0.32      0.47      
pnm.header                    106         36          66%       0.67      0.34      0.51      
metadata                      68          3           95%       0.17      0.04      0.11      
webp.encoder                  58          3           94%       0.38      0.05      0.22      
hooks                         76          24          68%       0.67      0.32      0.49      
hdr.decoder                   104         56          46%       0.79      0.54      0.66      
codecs.farbfeld               56          9           83%       0.60      0.16      0.38      
avif.encoder                  48          8           83%       0.48      0.17      0.32      
codecs.tiff                   40          9           77%       0.65      0.22      0.44      
avif.decoder                  32          3           90%       0.38      0.09      0.24      
io.free_functions             52          24          53%       0.59      0.46      0.52      
tga.encoder                   40          12          70%       0.68      0.30      0.49      
ico.decoder                   34          6           82%       0.38      0.18      0.28      
codecs.dds                    40          12          70%       0.76      0.30      0.53      
avif.ycgco                    40          18          55%       0.31      0.45      0.38      
images.buffer_par             56          36          35%       0.66      0.64      0.65      
codecs.dxt                    95          75          21%       0.93      0.79      0.86      
tga.decoder                   22          3           86%       0.32      0.14      0.23      
bmp.encoder                   28          12          57%       0.58      0.43      0.50      
pnm.mod                       12          0           100%      0.00      0.00      0.00      
ico.encoder                   38          26          31%       0.88      0.68      0.78      
jpeg.mod                      14          3           78%       0.56      0.21      0.39      
webp.decoder                  12          3           75%       0.49      0.25      0.37      
math.utils                    28          20          28%       1.00      0.71      0.86      
avif.mod                      10          5           50%       0.22      0.50      0.36      
math.rect                     10          7           30%       1.00      0.70      0.85      

=== Generating Reports ===

✅ Generated: port_status_report.md
✅ Generated: high_priority_ports.md
✅ Generated: NEXT_ACTIONS.md
✅ Generated: port_lint_proposed_changes.md

📁 All reports generated successfully!
