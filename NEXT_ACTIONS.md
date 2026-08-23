# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 52/71 (73.2%)
- **Function parity:** 478/1383 matched (target 827) — 34.6%
- **Class/type parity:** 105/279 matched (targCodebase: src/commonMain/kotlin (kotlin)
  Files: 88
  Total imports: 414
  Most depended: io.IoWrite (9 dependents)

Comparing codebases...
Computing AST similarities...
*Average documentation cosine:** 0.52 (doc text across 40 matched files)
- **Cheat-zeroed Files:** 17
- **Critical Issues:** 48 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. error

- **Target:** `image.Error`
- **Similarity:** 0.26
- **Dependents:** 3
- **Priority Score:** 3072307.5
- **Functions:** 5/11 matched (target 25)
- **Missing functions:** `new`, `from`, `fmt`, `source`, `test_send_sync_stability`, `assert_send_sync`
- **Types:** 11/12 matched (target 35)
- **Missing types:** `ImageResult`
- **Tests:** 0/2 matched

### 2. io

- **Target:** `io.Mod [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 3
- **Priority Score:** 3050510.0
- **Functions:** 0/1 matched (target 0)
- **Missing functions:** `read_exact_vec`
- **Types:** 0/4 matched (target 0)
- **Missing types:** `Reader`, `Limits`, `LimitSupport`, `ReadExt`

### 3. math.rect

- **Target:** `math.Rect`
- **Similarity:** 1.00
- **Dependents:** 3
- **Priority Score:** 3000100.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 4. pnm.autobreak

- **Target:** `pnm.AutoBreak`
- **Similarity:** 0.54
- **Dependents:** 2
- **Priority Score:** 2020804.6
- **Functions:** 5/7 matched (target 6)
- **Missing functions:** `new`, `drop`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 2/2 matched

### 5. images.flat

- **Target:** `images.Flat`
- **Similarity:** 0.04
- **Dependents:** 1
- **Priority Score:** 1556109.6
- **Functions:** 4/51 matched (target 10)
- **Missing functions:** `extents`, `bounds`, `min_length`, `fits`, `increasing_stride_dims`, `has_aliased_samples`, `is_normal`, `in_bounds`, `index`, `index_ignoring_bounds`, `in_bounds_index`, `shrink_to`, `stride`, `checked_len`, `len`, `as_ref`, `as_mut`, `to_vec`, `get_mut_sample`, `as_view`, `as_view_with_mut_samples`, `as_view_mut`, `as_slice`, `as_mut_slice`, `image_slice`, `image_mut_slice`, `try_into_buffer`, `with_monocolor`, `into_inner`, `flat`, `samples`, `try_upgrade`, `panic_cwh_out_of_bounds`, `panic_pixel_out_of_bounds`, `index_mut`, `dimensions`, `get_pixel`, `get_pixel_mut`, `put_pixel`, `blend_pixel`, `from`, `fmt`, `partial_cmp`, `aliasing_view`, `mutable_view`, `normal_forms`, `image_buffer_conversion`
- **Types:** 2/10 matched (target 3)
- **Missing types:** `Dim`, `View`, `ViewMut`, `Error`, `NormalForm`, `Output`, `Pixel`, `NormalFormRequiredError`
- **Tests:** 0/4 matched

### 6. metadata.cicp

- **Target:** `metadata.Cicp`
- **Similarity:** 0.08
- **Dependents:** 1
- **Priority Score:** 1364909.2
- **Functions:** 5/36 matched (target 14)
- **Missing functions:** `to_moxcms`, `supported_transform_fn`, `check_applicable`, `build_transforms`, `transform_dynamic`, `select_transform_u8`, `select_transform_u16`, `select_transform_f32`, `expand_luma_rgb`, `expand_luma_rgba`, `expand_rgb`, `expand_rgba`, `clamp_rgb`, `clamp_rgba`, `clamp_rgb_luma`, `clamp_rgba_luma`, `cast_pixels`, `cast_pixels_by_fallback`, `cast_pixels_from_subpixels`, `expand_to_f32`, `clamp_from_f32`, `select_transform`, `to_moxcms_compute_profile`, `from`, `map_layout`, `moxcms`, `can_create_transforms`, `no_coefficient_fallback`, `transform_pixels_srgb`, `transform_pixels_srgb_16`, `transform_pixels_srgb_luma_alpha`
- **Types:** 8/13 matched (target 9)
- **Missing types:** `CicpApplicable`, `RgbTransforms`, `CicpPixelCast`, `ColorComponentForCicp`, `ColorProfile`
- **Tests:** 0/6 matched

### 7. images.sub_image

- **Target:** `images.SubImage`
- **Similarity:** 0.32
- **Dependents:** 1
- **Priority Score:** 1132406.9
- **Functions:** 10/18 matched (target 10)
- **Missing functions:** `new`, `inner_mut`, `deref`, `deref_mut`, `buffer_with_dimensions`, `get_pixel_mut`, `preserves_color_space`, `deep_preserves_color_space`
- **Types:** 1/6 matched (target 1)
- **Missing types:** `SubImageInner`, `DerefPixel`, `DerefSubpixel`, `Target`, `Pixel`
- **Tests:** 0/2 matched

### 8. imageops.fast_blur

- **Target:** `imageops.FastBlur`
- **Similarity:** 0.17
- **Dependents:** 1
- **Priority Score:** 1121508.2
- **Functions:** 3/14 matched (target 8)
- **Missing functions:** `test_radius_size`, `rounding_saturating_mul`, `box_blur_horizontal_pass_strategy`, `box_blur_vertical_pass_strategy`, `box_blur_horizontal_pass_impl`, `box_blur_vertical_pass_impl`, `new`, `next_u32`, `next_u8`, `next_f32_in_range`, `test_box_blur`
- **Types:** 0/1 matched
- **Missing types:** `Rng`
- **Tests:** 0/5 matched

### 9. io.free_functions

- **Target:** `io.FreeFunctions`
- **Similarity:** 0.45
- **Dependents:** 1
- **Priority Score:** 1081705.5
- **Functions:** 8/16 matched (target 18)
- **Missing functions:** `save_buffer`, `dimensions`, `color_type`, `read_image`, `read_image_boxed`, `seek_scanline`, `read_scanline`, `test_load_rect_single_scanline`
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_
- **Tests:** 1/8 matched

### 10. io.limits

- **Target:** `io.Limits`
- **Similarity:** 0.42
- **Dependents:** 1
- **Priority Score:** 1031105.8
- **Functions:** 6/9 matched (target 11)
- **Missing functions:** `default`, `reserve_usize`, `free_usize`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 11. tga.header

- **Target:** `tga.Header`
- **Similarity:** 0.48
- **Dependents:** 1
- **Priority Score:** 1010905.1
- **Functions:** 6/7 matched (target 10)
- **Missing functions:** `new`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 12. jpeg.transform

- **Target:** `jpeg.Transform [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 1
- **Priority Score:** 1000110.0
- **Functions:** 1/1 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 13. images.buffer

- **Target:** `images.Buffer`
- **Similarity:** 0.12
- **Dependents:** 0
- **Priority Score:** 771808.8
- **Functions:** 31/93 matched (target 92)
- **Missing functions:** `next`, `size_hint`, `len`, `next_back`, `fmt`, `with_image`, `inner_pixels`, `pixels`, `check_image_fits`, `image_buffer_len`, `pixel_indices`, `pixel_indices_unchecked`, `sample_layout`, `into_flat_samples`, `as_flat_samples`, `as_flat_samples_mut`, `inner_pixels_mut`, `get_pixel_mut`, `get_pixel_mut_checked`, `set_rgb_color_space`, `save`, `save_with_format`, `write_to`, `write_with_encoder`, `default`, `deref`, `deref_mut`, `index`, `index_mut`, `clone_from`, `unsafe_get_pixel`, `unsafe_put_pixel`, `from_pixel`, `from_vec`, `into_vec`, `copy_color_space_from`, `expand_palette`, `convert`, `as_transform`, `as_transform_fn`, `cast_in_color_space`, `copy_from_color_space`, `to_color_space`, `apply_color_space`, `from`, `slice_buffer`, `mut_iter`, `zero_width_zero_height`, `zero_width_nonzero_height`, `nonzero_width_zero_height`, `pixels_on_large_buffer`, `write_to_with_large_buffer`, `exact_size_iter_size_hint`, `color_conversion`, `gray_conversions`, `rgb_to_gray_conversion`, `apply_color`, `to_color`, `transformation_mismatch`, `conversion`, `image_access_row_by_row`, `image_access_col_by_col`
- **Types:** 11/25 matched (target 12)
- **Missing types:** `Pixels`, `Item`, `PixelsMut`, `Rows`, `RowsMut`, `EnumeratePixels`, `EnumerateRows`, `EnumeratePixelsMut`, `EnumerateRowsMut`, `Target`, `Output`, `Pixel`, `ConvertBuffer`, `ConvertColorOptions`
- **Tests:** 5/22 matched

### 14. imageops.sample

- **Target:** `imageops.Sample`
- **Similarity:** 0.04
- **Dependents:** 0
- **Priority Score:** 626809.6
- **Functions:** 5/63 matched (target 9)
- **Missing functions:** `to_i8`, `to_i16`, `to_i64`, `to_u8`, `to_u16`, `to_u64`, `to_f64`, `bc_cubic_spline`, `gaussian`, `lanczos3_kernel`, `catmullrom_kernel`, `triangle_kernel`, `box_kernel`, `horizontal_sample`, `sample_bilinear`, `sample_nearest`, `interpolate_nearest`, `interpolate_bilinear`, `vertical_sample`, `zeroed`, `sample_val`, `add_pixel`, `thumbnail_sample_block`, `thumbnail_sample_fraction_horizontal`, `thumbnail_sample_fraction_vertical`, `thumbnail_sample_fraction_both`, `filter3x3`, `blur`, `blur_advanced`, `get_gaussian_kernel_1d`, `new_from_radius`, `new_from_kernel_size`, `new_anisotropic_kernel_size`, `new_from_sigma`, `round_to_nearest_odd`, `sigma_size`, `kernel_size_from_sigma`, `gaussian_blur_dyn_image`, `gaussian_blur_indirect`, `gaussian_blur_indirect_impl`, `unsharpen`, `bench_resize`, `test_resize_same_size`, `test_sample_bilinear`, `test_sample_nearest`, `test_sample_bilinear_correctness`, `bench_sample_bilinear`, `test_sample_nearest_correctness`, `bench_resize_same_size`, `test_issue_186`, `bench_thumbnail`, `bench_thumbnail_upsize`, `bench_thumbnail_upsize_irregular`, `resize_transparent_image`, `assert_resize`, `bug_1600`, `issue_2340`, `issue_2340_refl`
- **Types:** 1/5 matched (target 2)
- **Missing types:** `Filter`, `FloatNearest`, `ThumbnailSum`, `GaussianBlurParameters`
- **Tests:** 0/17 matched

### 15. images.dynimage

- **Target:** `images.Dynimage`
- **Similarity:** 0.30
- **Dependents:** 0
- **Priority Score:** 585207.0
- **Functions:** 94/149 matched (target 104)
- **Missing functions:** `clone`, `clone_from`, `to`, `to_luma32f`, `to_luma_alpha32f`, `as_mut_rgb8`, `as_mut_rgba8`, `as_mut_luma8`, `as_mut_luma_alpha8`, `as_mut_rgb16`, `as_mut_rgba16`, `as_mut_rgb32f`, `as_mut_rgba32f`, `as_mut_luma16`, `as_mut_luma_alpha16`, `as_flat_samples_u8`, `as_flat_samples_u16`, `as_flat_samples_f32`, `set_rgb_primaries`, `set_transfer_function`, `color_space`, `set_color_space`, `blur_advanced`, `flipv_in_place`, `fliph_in_place`, `rotate180_in_place`, `copy_from_color_space`, `apply_color_space`, `convert_color_space`, `write_with_encoder_impl`, `save`, `save_with_format`, `from`, `get_pixel_mut`, `default`, `open`, `image_dimensions`, `bench_conversion`, `open_16bpc_png`, `test_grayscale`, `test_grayscale_alpha_discarded`, `test_grayscale_alpha_preserved`, `test_dynamic_image_default_implementation`, `color_conversion_srgb_p3`, `color_conversion_preserves_sample`, `color_conversion_preserves_sample_in_fastpath`, `color_conversion_rgb_to_luma`, `copy_color_space_coverage`, `apply_color_space_coverage`, `into_luma_is_color_space_aware`, `from_luma_is_color_space_aware`, `from_luma_for_all_chromaticities`, `from_rgb_for_all_chromaticities`, `convert_color_space_coverage`, `color_space_independent_imageops`
- **Types:** 1/3 matched (target 12)
- **Missing types:** `Pixel`, `Foo`
- **Tests:** 14/32 matched

### 16. bmp.decoder

- **Target:** `bmp.Decoder`
- **Similarity:** 0.04
- **Dependents:** 0
- **Priority Score:** 495609.6
- **Functions:** 3/44 matched (target 15)
- **Missing functions:** `next`, `fmt`, `from`, `check_for_overflow`, `num_bytes`, `with_rows`, `set_8bit_pixel_run`, `set_4bit_pixel_run`, `set_2bit_pixel_run`, `set_1bit_pixel_run`, `from_mask`, `read`, `new_decoder`, `new`, `new_without_file_header`, `new_with_ico_format`, `set_indexed_color`, `reader`, `read_file_header`, `read_bitmap_core_header`, `read_bitmap_info_header`, `read_bitmasks`, `read_metadata`, `read_metadata_in_ico_format`, `get_palette_size`, `bytes_per_color`, `read_palette`, `get_palette`, `num_channels`, `rows`, `read_palettized_pixel_data`, `read_16_bit_pixel_data`, `read_32_bit_pixel_data`, `read_full_byte_pixel_data`, `read_rle_data`, `read_image_data`, `read_image_boxed`, `read_rect`, `test_bitfield_len`, `read_rle_too_short`, `test_no_header`
- **Types:** 4/12 matched (target 4)
- **Missing types:** `BMPHeaderType`, `FormatFullBytes`, `Chunker`, `RowIterator`, `Item`, `DecoderError`, `ChannelWidthError`, `RLEInsn`
- **Tests:** 0/3 matched

### 17. pnm.decoder

- **Target:** `pnm.Decoder`
- **Similarity:** 0.34
- **Dependents:** 0
- **Priority Score:** 266506.6
- **Functions:** 33/51 matched (target 49)
- **Missing functions:** `fmt`, `from`, `source`, `sample_size`, `bytelen`, `new`, `read_magic_constant`, `read_image_boxed`, `read_samples`, `read_ascii`, `read_separated_ascii`, `from_bytes`, `from_ascii`, `tuple_type`, `pbm_binary_ascii_termination`, `read`, `issue_1508`, `issue_1616_overflow`
- **Types:** 6/14 matched (target 28)
- **Missing types:** `U8`, `U16`, `PbmBit`, `BWBit`, `DecodableImageHeader`, `HeaderReader`, `Representation`, `FailRead`
- **Tests:** 19/23 matched
- **Lint issues:** 3

### 18. imageops.filter_1d

- **Target:** `imageops.Filter1d`
- **Similarity:** 0.03
- **Dependents:** 0
- **Priority Score:** 263009.7
- **Functions:** 2/23 matched (target 4)
- **Missing functions:** `make_arena_row`, `make_columns_arenas`, `to_`, `filter_symmetric_column`, `filter_symmetric_row`, `transform`, `prepare_symmetric_kernel`, `filter_2d_separable_ring_queue`, `filter_2d_separable`, `filter_2d_sep_plane`, `filter_2d_sep_la`, `filter_2d_sep_rgb`, `filter_2d_sep_rgba`, `filter_2d_sep_la_f32`, `filter_2d_sep_plane_f32`, `filter_2d_sep_rgb_f32`, `filter_2d_sep_rgba_f32`, `filter_2d_sep_rgb_u16`, `filter_2d_sep_rgba_u16`, `filter_2d_sep_la_u16`, `filter_2d_sep_plane_u16`
- **Types:** 2/7 matched (target 2)
- **Missing types:** `SafeMul`, `SafeAdd`, `ArenaColumns`, `ToStorage`, `KernelTransformer`

### 19. pnm.encoder

- **Target:** `pnm.Encoder`
- **Similarity:** 0.37
- **Dependents:** 0
- **Priority Score:** 183506.3
- **Functions:** 14/25 matched (target 23)
- **Missing functions:** `new`, `check`, `check_header_dimensions`, `check_header_color`, `check_sample_values`, `write_header`, `header`, `write_samples_ascii`, `len`, `encoding_for`, `from`
- **Types:** 3/10 matched (target 9)
- **Missing types:** `CheckedImageBuffer`, `UncheckedHeader`, `CheckedDimensions`, `CheckedHeaderColor`, `CheckedHeader`, `TupleEncoding`, `SampleWriter`
- **Tests:** 3/3 matched

### 20. codecs.farbfeld

- **Target:** `codecs.Farbfeld`
- **Similarity:** 0.15
- **Dependents:** 0
- **Priority Score:** 182508.5
- **Functions:** 5/22 matched (target 8)
- **Missing functions:** `new`, `read_dimm`, `read`, `seek`, `parse_offset`, `consume_channel`, `cache_byte`, `read_image_boxed`, `read_rect`, `encode_impl`, `read_rect_1x2`, `read_rect_2x2`, `read_rect_2x1`, `read_rect_2x3`, `read_rect_in_stream`, `dimension_overflow`, `degenerate_pixels`
- **Types:** 2/3 matched
- **Missing types:** `FarbfeldReader`
- **Tests:** 0/7 matched

### 21. imageops.mod

- **Target:** `imageops.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 162610.0
- **Functions:** 10/26 matched (target 14)
- **Missing functions:** `test_overlay_bounds_ext`, `test_image_in_image`, `test_image_in_image_outside_of_bounds`, `test_image_outside_image_no_wrap_around`, `test_image_coordinate_overflow`, `test_image_horizontal_gradient_limits`, `test_image_vertical_gradient_limits`, `test_blur_zero`, `test_fast_blur_zero`, `test_fast_blur_negative`, `test_fast_large_sigma`, `test_fast_blur_empty`, `test_fast_blur_3_channels`, `test_fast_blur_2_channels`, `test_fast_blur_1_channels`, `fast_blur_approximates_gaussian_blur_well`
- **Types:** 0/0 matched (target 2)
- **Missing types:** _none_
- **Tests:** 0/16 matched

### 22. traits

- **Target:** `image.Traits`
- **Similarity:** 0.08
- **Dependents:** 0
- **Priority Score:** 162609.2
- **Functions:** 4/13 matched (target 17)
- **Missing functions:** `from`, `channels`, `layout`, `dispatch_transform_from_sealed`, `double_dispatch_transform_from_sealed`, `transform_on`, `alpha`, `map_without_alpha`, `apply_without_alpha`
- **Types:** 6/13 matched (target 27)
- **Missing types:** `Larger`, `Ratio`, `LayoutWithColor`, `PrivateToken`, `SealedPixelWithColorType`, `TransformableSubpixel`, `HelpDispatchTransform`

### 23. animation

- **Target:** `image.Animation`
- **Similarity:** 0.41
- **Dependents:** 0
- **Priority Score:** 143705.9
- **Functions:** 19/32 matched (target 30)
- **Missing functions:** `new`, `next`, `clone`, `clone_from`, `from_parts`, `buffer_mut`, `into_buffer`, `from_ratio`, `into_ratio`, `from`, `eq`, `partial_cmp`, `cmp`
- **Types:** 4/5 matched
- **Missing types:** `Item`
- **Tests:** 6/6 matched

### 24. color

- **Target:** `image.Color`
- **Similarity:** 0.29
- **Dependents:** 0
- **Priority Score:** 133607.1
- **Functions:** 21/28 matched (target 70)
- **Missing functions:** `try_from`, `from_primitive`, `normalize_float`, `into_color`, `rgb_to_luma`, `from_color`, `invert`
- **Types:** 2/8 matched (target 35)
- **Missing types:** `Error`, `FromPrimitive`, `FromColor`, `IntoColor`, `Blend`, `Invert`
- **Tests:** 12/12 matched

### 25. io.image_reader_type

- **Target:** `io.ImageReaderType`
- **Similarity:** 0.26
- **Dependents:** 0
- **Priority Score:** 111907.4
- **Functions:** 7/17 matched (target 11)
- **Missing functions:** `new`, `no_limits`, `limits`, `into_inner`, `make_decoder`, `guess_format`, `into_dimensions`, `require_format`, `open`, `open_impl`
- **Types:** 1/2 matched
- **Missing types:** `Format`

### 26. bmp.encoder

- **Target:** `bmp.Encoder`
- **Similarity:** 0.30
- **Dependents:** 0
- **Priority Score:** 111907.0
- **Functions:** 7/18 matched (target 11)
- **Missing functions:** `new`, `write_row_pad`, `make_compatible_img`, `round_trip_image`, `round_trip_single_pixel_rgb`, `huge_files_return_error`, `round_trip_single_pixel_rgba`, `round_trip_3px_rgb`, `round_trip_gray`, `round_trip_graya`, `regression_issue_2604`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 0/8 matched

### 27. webp.encoder

- **Target:** `bmp.BmpTest [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 90910.0
- **Functions:** 0/8 matched (target 7)
- **Missing functions:** `new_lossless`, `encode`, `write_image`, `set_icc_profile`, `set_exif_metadata`, `make_compatible_img`, `from_webp_encode`, `write_webp`
- **Types:** 0/1 matched
- **Missing types:** `WebPEncoder`
- **Tests:** 0/1 matched
- **Provenance warning:** port-lint provenance header matched only by basename: `codecs/bmp/encoder.rs` vs expected `codecs/webp/encoder.rs`
- **Proposed provenance header:** `// port-lint: source codecs/webp/encoder.rs` (current: `// port-lint: source codecs/bmp/encoder.rs`)
- **Lint issues:** 1

### 28. tga.encoder

- **Target:** `tga.Encoder`
- **Similarity:** 0.46
- **Dependents:** 0
- **Priority Score:** 72805.4
- **Functions:** 18/25 matched
- **Missing functions:** `fmt`, `from`, `new`, `make_compatible_img`, `round_trip_image`, `round_trip_bw`, `round_trip_single_pixel_rgba`
- **Types:** 3/3 matched (target 7)
- **Missing types:** _none_
- **Tests:** 12/15 matched

### 29. io.decoder

- **Target:** `io.Decoder`
- **Similarity:** 0.16
- **Dependents:** 0
- **Priority Score:** 71708.4
- **Functions:** 8/13 matched (target 8)
- **Missing functions:** `dimensions`, `color_type`, `read_image`, `read_image_boxed`, `total_bytes_overflow`
- **Types:** 2/4 matched (target 2)
- **Missing types:** `AnimationDecoder`, `D`
- **Tests:** 0/1 matched

### 30. pnm.mod

- **Target:** `pnm.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 60610.0
- **Functions:** 0/6 matched (target 0)
- **Missing functions:** `execute_roundtrip_default`, `execute_roundtrip_with_subtype`, `execute_roundtrip_u16`, `roundtrip_gray`, `roundtrip_rgb`, `roundtrip_u16`
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Tests:** 0/6 matched

### 31. imageops.colorops

- **Target:** `imageops.Colorops`
- **Similarity:** 0.41
- **Dependents:** 0
- **Priority Score:** 52805.9
- **Functions:** 21/24 matched (target 25)
- **Missing functions:** `grayscale_with_type`, `grayscale_with_type_alpha`, `pixel_diffs`
- **Types:** 2/4 matched (target 3)
- **Missing types:** `Subpixel`, `Color`
- **Tests:** 5/6 matched
- **Lint issues:** 4

### 32. codecs.qoi

- **Target:** `codecs.Qoi [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 51110.0
- **Functions:** 4/9 matched (target 8)
- **Missing functions:** `new`, `read_image_boxed`, `decoding_error`, `encoding_error`, `decode_test_image`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_
- **Tests:** 0/1 matched

### 33. io.encoder

- **Target:** `io.Encoder`
- **Similarity:** 0.28
- **Dependents:** 0
- **Priority Score:** 50807.2
- **Functions:** 2/5 matched (target 2)
- **Missing functions:** `make_compatible_img`, `write_image`, `dynimage_conversion_8bit`
- **Types:** 1/3 matched (target 1)
- **Missing types:** `MethodSealedToImage`, `ImageEncoderBoxed`

### 34. images.generic_image

- **Target:** `images.GenericImage`
- **Similarity:** 0.67
- **Dependents:** 0
- **Priority Score:** 43603.3
- **Functions:** 29/32 matched (target 30)
- **Missing functions:** `buffer_like`, `buffer_with_dimensions`, `clone`
- **Types:** 3/4 matched
- **Missing types:** `Item`
- **Tests:** 17/17 matched

### 35. utils.mod

- **Target:** `utils.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 40710.0
- **Functions:** 3/7 matched (target 4)
- **Missing functions:** `expand_packed`, `expand_bits`, `vec_copy_to_u8`, `clamp`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 1/1 matched

### 36. tga.decoder

- **Target:** `tga.Decoder`
- **Similarity:** 0.51
- **Dependents:** 0
- **Priority Score:** 31504.9
- **Functions:** 9/12 matched (target 11)
- **Missing functions:** `new`, `fixup_orientation`, `read_image_boxed`
- **Types:** 3/3 matched
- **Missing types:** _none_

### 37. pnm.header

- **Target:** `pnm.Header`
- **Similarity:** 0.64
- **Dependents:** 0
- **Priority Score:** 22403.6
- **Functions:** 13/14 matched (target 20)
- **Missing functions:** `fmt`
- **Types:** 9/10 matched (target 22)
- **Missing types:** `TupltypeWriter`

### 38. imageops.affine

- **Target:** `imageops.Affine`
- **Similarity:** 0.55
- **Dependents:** 0
- **Priority Score:** 12204.5
- **Functions:** 21/22 matched (target 21)
- **Missing functions:** `pixel_diffs`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 8/9 matched

### 39. io.format

- **Target:** `io.Format`
- **Similarity:** 0.39
- **Dependents:** 0
- **Priority Score:** 11406.1
- **Functions:** 12/13 matched (target 16)
- **Missing functions:** `inner`
- **Types:** 1/1 matched (target 2)
- **Missing types:** _none_
- **Tests:** 2/2 matched

### 40. metadata

- **Target:** `metadata.Mod [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10910.0
- **Functions:** 6/7 matched (target 22)
- **Missing functions:** `test_extraction_and_clearing`
- **Types:** 2/2 matched (target 3)
- **Missing types:** _none_

### 41. hooks

- **Target:** `image.Hooks`
- **Similarity:** 0.55
- **Dependents:** 0
- **Priority Score:** 1904.5
- **Functions:** 15/15 matched (target 27)
- **Missing functions:** _none_
- **Types:** 4/4 matched (target 7)
- **Missing types:** _none_

### 42. math.utils

- **Target:** `math.Utils`
- **Similarity:** 0.73
- **Dependents:** 0
- **Priority Score:** 702.7
- **Functions:** 7/7 matched (target 10)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 5/5 matched

### 43. jpeg.entropy

- **Target:** `jpeg.Entropy [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 210.0
- **Functions:** 2/2 matched (target 4)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 44. jpeg.mod

- **Target:** `jpeg.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 45. tga.mod

- **Target:** `tga.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_

### 46. bmp.mod

- **Target:** `bmp.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 47. images

- **Target:** `images.Images [ZERO]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 48. math.mod

- **Target:** `math.Mod [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 49. webp.mod

- **Target:** `utils.Clamp [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `utils/mod.rs` vs expected `codecs/webp/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/webp/mod.rs` (current: `// port-lint: source utils/mod.rs`)
- **Lint issues:** 1

### 50. hdr.mod

- **Target:** `utils.ExpandBits [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `utils/mod.rs` vs expected `codecs/hdr/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/hdr/mod.rs` (current: `// port-lint: source utils/mod.rs`)
- **Lint issues:** 1

### 51. avif.mod

- **Target:** `utils.ExpandPacked [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 1)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `utils/mod.rs` vs expected `codecs/avif/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/avif/mod.rs` (current: `// port-lint: source utils/mod.rs`)
- **Lint issues:** 1

### 52. ico.mod

- **Target:** `imageops.ImageopsTest [STUB] [PROVENANCE-FALLBACK]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 14)
- **Missing functions:** _none_
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Provenance warning:** port-lint provenance header matched only by basename: `imageops/mod.rs` vs expected `codecs/ico/mod.rs`
- **Proposed provenance header:** `// port-lint: source codecs/ico/mod.rs` (current: `// port-lint: source imageops/mod.rs`)
- **Lint issues:** 1

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                             ✅ Generated: NEXT_ACTIONS.md
✅ Generated: port_lint_proposed_changes.md

📁 All reports generated successfully!
